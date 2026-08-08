package com.arxyt.colonypathingedition;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.easycolony.event.HurtAlertEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.LinkageEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.ReadMindEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.ResurrectEvent;
import com.arxyt.colonypathingedition.core.event.CitizenTrackingHandler;
import com.arxyt.colonypathingedition.core.event.LargeBottleCauldronInteractions;
import com.arxyt.colonypathingedition.core.initializer.InteractionInitializer;
import com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModules;
import com.arxyt.colonypathingedition.core.minecolonies.module.ModBuildingInitializer;
import com.arxyt.colonypathingedition.core.network.CPENetwork;
import com.arxyt.colonypathingedition.core.update.UpdateManager;
import com.minecolonies.api.IMinecoloniesAPI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("colonypathingedition")
public class ColonyPathingEdition {
    public static final String MODID = "colonypathingedition";
    public static final Logger LOGGER = LogManager.getLogger();

    public ColonyPathingEdition(FMLModContainer modContainer, IEventBus modBus) {
        // 注册配置文件
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                PathingConfig.init(new ModConfigSpec.Builder())
        );
        modBus.addListener(LinkageEvent::onFMLCommonSetup);
        modBus.addListener(ColonyPathingEdition::preInit);
        modBus.addListener(CPENetwork::register);
        // 客户端初始化
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(UpdateManager.class);
        }
        NeoForge.EVENT_BUS.register(SpecialSeedManager.class);
        NeoForge.EVENT_BUS.register(CitizenTrackingHandler.class);
        NeoForge.EVENT_BUS.register(new HurtAlertEvent());
        NeoForge.EVENT_BUS.register(new ReadMindEvent());
        NeoForge.EVENT_BUS.register(new ResurrectEvent());
        NeoForge.EVENT_BUS.register(new LargeBottleCauldronInteractions());

        LOGGER.info("Colony Pathing Edition mod loaded");
    }

    @SubscribeEvent
    public static void onConfigLoad(@NotNull final ModConfigEvent event) {
        PathingConfig.onLoad();
        InteractionInitializer.init();
    }

    @SubscribeEvent
    private void onRegister(RegisterEvent e) {
        if (e.getRegistryKey() == IMinecoloniesAPI.getInstance().getBuildingRegistry().key()) {
            BuildingModules.init();
        }
    }

    @SubscribeEvent
    public static void preInit(@NotNull final FMLCommonSetupEvent event) {
        event.enqueueWork(ModBuildingInitializer::init);
    }
}