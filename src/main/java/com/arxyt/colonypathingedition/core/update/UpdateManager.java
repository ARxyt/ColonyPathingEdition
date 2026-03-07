package com.arxyt.colonypathingedition.core.update;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.google.gson.*;
import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.*;

public class UpdateManager {
    private static final String UPDATE_URL = "https://arxyt.github.io/ColonyPathingEdition/latest.json";
    private static final String UPDATE_URL_CHINESE = "https://gitee.com/wcngrz/pathfinding-edition-for-minecolonies/raw/master/latest.json";
    private static final int HTTP_TIMEOUT_MS = 5000;
    private static final String FALLBACK_LANGUAGE = "en_us";

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if(PathingConfig.MESSAGE_DISPLAY.get()) {
            Player player = event.getEntity();
            checkAndSendUpdateMessage(player);
        }
    }

    private static void checkAndSendUpdateMessage(Player player) {
        final String modVersion = getModVersion();

        CompletableFuture.runAsync(() -> {
            try {
                JsonObject responseJson = fetchUpdateInfo();
                if (responseJson == null) return;
                processUpdateInfo(player, modVersion, responseJson);
            } catch (Exception e) {
                ColonyPathingEdition.LOGGER.error("[Update Check] Failed to process update info", e);
            }
        });
    }

    private static String getModVersion() {
        return ModList.get()
                .getModContainerById(ColonyPathingEdition.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("UNKNOWN");
    }

    @Nullable
    private static JsonObject fetchUpdateInfo() {
        String[] urls = {UPDATE_URL, UPDATE_URL_CHINESE};
        for (String url : urls) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                configureConnection(connection);

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    ColonyPathingEdition.LOGGER.warn("[Update Check] HTTP {}: {}", connection.getResponseCode(), url);
                    continue;
                }

                return parseResponse(connection);
            } catch (IOException e) {
                ColonyPathingEdition.LOGGER.warn("[Update Check] Network error when accessing {}", url, e);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        ColonyPathingEdition.LOGGER.error("[Update Check] All update sources failed");
        return null;
    }

    private static void configureConnection(HttpURLConnection connection) throws ProtocolException {
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(HTTP_TIMEOUT_MS);
        connection.setReadTimeout(HTTP_TIMEOUT_MS);
    }

    private static JsonObject parseResponse(HttpURLConnection connection) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            ColonyPathingEdition.LOGGER.error("[Update Check] Invalid JSON response", e);
            return null;
        }
    }

    private static void processUpdateInfo(Player player, String currentVersion, JsonObject responseJson) {
        if (!isValidResponse(responseJson)) return;

        String latestVersion = responseJson.get("latest_version").getAsString();
        if (currentVersion.equals(latestVersion)) return;

        String latestStableVersion = responseJson.get("latest_stable_version").getAsString();
        if (currentVersion.equals(latestStableVersion)) {
            sendClientUnsafeUpdateMessage(player, UPDATE_MESSAGE, UNSAFE_UPDATE, latestVersion);
            return;
        }

        List<String> stableVersions = responseJson.getAsJsonArray("history_stable_version").asList().stream()
                .map(JsonElement::getAsString)
                .toList();
        if(stableVersions.contains(currentVersion)){
            if(!responseJson.has("latest_stable_changelog")) return;
            JsonObject content = responseJson.getAsJsonObject("latest_stable_changelog");
            String message = getLocalizedMessage(content);
            if (message == null) return;
            sendClientMessage(player, SAFE_UPDATE, message, latestStableVersion);
            return;
        }

        List<String> unstableVersions = responseJson.getAsJsonArray("history_unstable_version").asList().stream()
                .map(JsonElement::getAsString)
                .toList();
        if(unstableVersions.contains(currentVersion)){
            if(!responseJson.has("latest_changelog")) return;
            JsonObject content = responseJson.getAsJsonObject("latest_changelog");
            String message = getLocalizedMessage(content);
            if (message == null) return;
            sendClientMessage(player, UPDATE_MESSAGE, message, latestVersion);
            return;
        }
        if(!responseJson.has("latest_stable_changelog")) return;
        JsonObject content = responseJson.getAsJsonObject("latest_stable_changelog");
        String message = getLocalizedMessage(content);
        sendClientMessage(player, OUT_OF_DATE_MESSAGE, message, latestStableVersion);
    }

    private static boolean isValidResponse(JsonObject json) {
        if (!json.has("latest_version") || !json.has("latest_stable_version") || !json.has("history_stable_version") ||!json.has("history_unstable_version")) {
            ColonyPathingEdition.LOGGER.error("[Update Check] Missing required JSON fields");
            return false;
        }
        return true;
    }

    @Nullable
    private static String getLocalizedMessage(JsonObject content) {
        String language = getClientLanguage();

        if (content.has(language)) {
            return content.get(language).getAsString();
        }
        if (content.has(FALLBACK_LANGUAGE)) {
            return content.get(FALLBACK_LANGUAGE).getAsString();
        }

        ColonyPathingEdition.LOGGER.warn("[Update Check] No valid messages found");
        return null;
    }

    private static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    private static void sendClientUnsafeUpdateMessage(Player player, String pKey1, String pKey2, String version) {
        Minecraft.getInstance().execute(() -> {
            if (player.isAlive()) {
                Component updateMessage = Component.translatable(pKey1, version).withStyle(ChatFormatting.GREEN);
                player.sendSystemMessage(updateMessage);
                Component updateContent = Component.translatable(pKey2, version).withStyle(ChatFormatting.RED);
                player.sendSystemMessage(updateContent);
                Component closeMessage = Component.translatable(CLOSER, version).withStyle(ChatFormatting.YELLOW);
                player.sendSystemMessage(closeMessage);
            }
        });
    }

    private static void sendClientMessage(Player player, String pKey, String message, String version) {
        Minecraft.getInstance().execute(() -> {
            if (player.isAlive()) {
                Component updateMessage = Component.translatable(pKey, version).withStyle(ChatFormatting.GREEN);
                player.sendSystemMessage(updateMessage);
                Component changelogMessage = Component.translatable(CHANGELOG, version).withStyle(ChatFormatting.WHITE);
                player.sendSystemMessage(changelogMessage);
                Component updateContent = Component.literal(message).withStyle(ChatFormatting.WHITE);
                player.sendSystemMessage(updateContent);
                Component closeMessage = Component.translatable(CLOSER, version).withStyle(ChatFormatting.YELLOW);
                player.sendSystemMessage(closeMessage);
            }
        });
    }
}