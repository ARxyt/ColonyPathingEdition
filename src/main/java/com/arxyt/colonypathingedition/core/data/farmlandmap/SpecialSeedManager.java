package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager of seeds with special farmland.
 */
public class SpecialSeedManager {

    public static final Map<Item, Block> SPECIAL_SEEDS = new HashMap<>();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FarmlandMapLoader());
    }

    /**
     * Called after datapack reload (includes tags), instead of TagsUpdatedEvent.
     */
    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Checking available seed-soil pairs...");

        SPECIAL_SEEDS.clear();

        for (var entry : FarmlandMapLoader.getMappings().entrySet()) {
            ResourceLocation seedId = entry.getKey();
            ResourceLocation soilId = entry.getValue();

            Item seed = BuiltInRegistries.ITEM.get(seedId);
            Block soil = BuiltInRegistries.BLOCK.get(soilId);

            if (seed == Items.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No seed: {}", seedId);
                continue;
            }
            if (soil == Blocks.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No farmland: {}", soilId);
                continue;
            }

            SPECIAL_SEEDS.put(seed, soil);
            ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Registered {} -> {}", seedId, soilId);
        }

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Active special seeds: {}", SPECIAL_SEEDS.size());
    }

    /** Is seed special */
    public static boolean isSpecialSeed(Item seed) {
        return SPECIAL_SEEDS.containsKey(seed);
    }

    /** Get farmland */
    public static Block getRequiredSoil(Item seed) {
        return SPECIAL_SEEDS.get(seed);
    }

    /** Is farmland special */
    public static boolean isSpecialSoil(Block farmland) {
        return SPECIAL_SEEDS.containsValue(farmland);
    }
}
