package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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

    // 客户端世界加载时
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onClientWorldLoad(ClientPlayerNetworkEvent.LoggingIn event) {
        if(event.getPlayer().level().isClientSide) {
            loadMappings();
        }
    }

    // 服务器启动时
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        loadMappings();
    }

    public void loadMappings() {
        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Checking available seed-soil pairs...");

        SPECIAL_SEEDS.clear();

        for (var entry : FarmlandMapLoader.getMappings().entrySet()) {
            ResourceLocation seedId = entry.getKey();
            ResourceLocation soilId = entry.getValue();

            Item seed = ForgeRegistries.ITEMS.getValue(seedId);
            Block soil = ForgeRegistries.BLOCKS.getValue(soilId);

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
