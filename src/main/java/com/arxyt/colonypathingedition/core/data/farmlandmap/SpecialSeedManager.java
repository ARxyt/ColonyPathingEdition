package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Manager of seeds with special framland。
 */
public class SpecialSeedManager {

    public static final Map<Item, Block> SPECIAL_SEEDS = new HashMap<>();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FarmlandMapLoader());
    }

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Checking available seed-soil pairs...");

        SPECIAL_SEEDS.clear();

        for (var entry : FarmlandMapLoader.getMappings().entrySet()) {
            ResourceLocation seedId = entry.getKey();
            ResourceLocation soilId = entry.getValue();

            RegistryAccess access = event.getRegistryAccess();

            Registry<Item> itemRegistry = access.registryOrThrow(Registries.ITEM);
            Registry<Block> blockRegistry = access.registryOrThrow(Registries.BLOCK);

            Item seed = itemRegistry.get(seedId);
            Block soil = blockRegistry.get(soilId);

            if (seed == null || seed == Items.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No seed {}", seedId);
                continue;
            }
            if (soil == null || soil == Blocks.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No farmland {}", soilId);
                continue;
            }

            SPECIAL_SEEDS.put(seed, soil);
            ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Registered {} -> {}", seedId, soilId);
        }
        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Active special seeds: {}", SPECIAL_SEEDS.size());
    }

    /** Is seeds active */
    public static boolean isSpecialSeed(Item seed) {
        return SPECIAL_SEEDS.containsKey(seed);
    }

    /** Get farmland */
    public static Block getRequiredSoil(Item seed) {
        return SPECIAL_SEEDS.get(seed);
    }

    public static boolean isSpecialSoil(Block farmland) {
        return SPECIAL_SEEDS.containsValue(farmland);
    }
}
