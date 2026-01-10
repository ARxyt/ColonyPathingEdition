package com.arxyt.colonypathingedition;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.minecolonies.module.ModBuildingInitializer;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod("colonypathingedition")
public class ColonyPathingEdition {
    public static final String MODID = "colonypathingedition";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ColonyPathingEdition() {
        // 注册配置文件
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,
                PathingConfig.init(new ForgeConfigSpec.Builder())
        );
        MinecraftForge.EVENT_BUS.register(new SpecialSeedManager());
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(this.getClass());
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(this.getClass());
        LOGGER.info("Colony Pathing Edition mod loaded");
    }

    @SubscribeEvent
    public static void onConfigLoad(@NotNull final ModConfigEvent event) {
        PathingConfig.onLoad();
    }

    @SubscribeEvent
    public static void preInit(@NotNull final FMLCommonSetupEvent event) {
        event.enqueueWork(ModBuildingInitializer::init);
    }
}