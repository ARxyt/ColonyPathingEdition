package com.arxyt.colonypathingedition.core.data.farmlandmap;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.network.message.SpecialSeedSyncMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

import static com.arxyt.colonypathingedition.ColonyPathingEdition.MODID;

@Mod.EventBusSubscriber(modid = MODID)
public class SpecialSeedManager {

    private static final Map<Item, Block> SPECIAL_SEEDS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ColonyPathingEdition.LOGGER.info(
                "[SpecialSeeds] Syncing seed data to {}",
                player.getGameProfile().getName()
        );

        new SpecialSeedSyncMessage(FarmlandMapLoader.getMappings())
                .syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        if (event.getServerResources() != null) {
            event.addListener(FarmlandMapLoader.INSTANCE);
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                new SpecialSeedSyncMessage(FarmlandMapLoader.getMappings()).syncToAll();
            }
        }
    }

    public static void rebuildFromMappings(
            Map<ResourceLocation, ResourceLocation> mappings) {

        ColonyPathingEdition.LOGGER.info("[SpecialSeeds] Rebuilding mappings...");

        SPECIAL_SEEDS.clear();

        for (var entry : mappings.entrySet()) {

            Item seed = ForgeRegistries.ITEMS.getValue(entry.getKey());
            Block soil = ForgeRegistries.BLOCKS.getValue(entry.getValue());

            if (seed == null || seed == Items.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No seed {}", entry.getKey());
                continue;
            }
            if (soil == null || soil == Blocks.AIR) {
                ColonyPathingEdition.LOGGER.info("[SpecialSeeds] No farmland {}", entry.getValue());
                continue;
            }

            SPECIAL_SEEDS.put(seed, soil);
        }

        ColonyPathingEdition.LOGGER.info(
                "[SpecialSeeds] Active special seeds: {}", SPECIAL_SEEDS.size());
    }

    public static boolean isSpecialSeed(Item seed) {
        return SPECIAL_SEEDS.containsKey(seed);
    }

    public static Block getRequiredSoil(Item seed) {
        return SPECIAL_SEEDS.get(seed);
    }

    public static boolean isSpecialSoil(Block farmland) {
        return SPECIAL_SEEDS.containsValue(farmland);
    }
}
