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

    public static final FarmlandMapLoader INSTANCE = new FarmlandMapLoader();

    private static final Map<ResourceLocation, ResourceLocation> mappings = new LinkedHashMap<>();

    private FarmlandMapLoader() {
        super(GSON, "farmland_map");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> object,
            @NotNull ResourceManager manager,
            @NotNull ProfilerFiller profiler) {

        mappings.clear();

        object.forEach((id, jsonElement) -> {
            try {
                JsonArray arr = jsonElement.getAsJsonObject().getAsJsonArray("values");
                if (arr == null) return;

                for (JsonElement el : arr) {

                    JsonObject obj = el.getAsJsonObject();

                    ResourceLocation seed =
                            ResourceLocation.parse(obj.get("seed").getAsString());

                    ResourceLocation soil =
                            ResourceLocation.parse(obj.get("farmland").getAsString());

                    mappings.put(seed, soil);
                }

            } catch (Exception e) {
                ColonyPathingEdition.LOGGER.error(
                        "[SpecialSeeds] Failed to parse {}: {}", id, e.getMessage());
            }
        });

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Loaded {} mappings", mappings.size());

        SpecialSeedManager.rebuildFromMappings(mappings);
    }

    public static Map<ResourceLocation, ResourceLocation> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }
}
