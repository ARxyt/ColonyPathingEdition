package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FarmlandMapLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<ResourceLocation, ResourceLocation> MAPPINGS = new LinkedHashMap<>();

    public FarmlandMapLoader() {
        super(GSON, "farmland_map");
    }

    @Override
    protected void apply(@NotNull Map<ResourceLocation, JsonElement> object,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {

        MAPPINGS.clear();

        object.forEach((id, jsonElement) -> {
            try {
                JsonObject root = jsonElement.getAsJsonObject();
                JsonArray arr = root.getAsJsonArray("values");
                if (arr == null) return;

                for (JsonElement el : arr) {
                    JsonObject obj = el.getAsJsonObject();
                    String seedStr = obj.get("seed").getAsString();
                    String soilStr = obj.get("farmland").getAsString();

                    ResourceLocation seed = ResourceLocation.parse(seedStr);
                    ResourceLocation soil = ResourceLocation.parse(soilStr);

                    MAPPINGS.put(seed, soil);
                }

            } catch (Exception e) {
                ColonyPathingEdition.LOGGER.error("[SpecialSeeds] Failed to parse {}: {}", id, e.getMessage());
            }
        });

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Total {} mappings loaded.", MAPPINGS.size());
    }

    public static Map<ResourceLocation, ResourceLocation> getMappings() {
        return Collections.unmodifiableMap(MAPPINGS);
    }
}
