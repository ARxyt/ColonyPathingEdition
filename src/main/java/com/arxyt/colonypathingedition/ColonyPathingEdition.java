package com.arxyt.colonypathingedition;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import com.arxyt.colonypathingedition.core.easycolony.event.HurtAlertEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.LinkageEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.ReadMindEvent;
import com.arxyt.colonypathingedition.core.easycolony.event.ResurrectEvent;
import com.arxyt.colonypathingedition.core.message.*;
import com.arxyt.colonypathingedition.core.update.UpdateManager;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod("colonypathingedition")
public class ColonyPathingEdition {
    public static final String MODID = "colonypathingedition";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ColonyPathingEdition(FMLModContainer modContainer, IEventBus modBus) {
        // 注册配置文件
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                PathingConfig.init(new ModConfigSpec.Builder())
        );
        modBus.addListener(LinkageEvent::onFMLCommonSetup);
        // 客户端初始化
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(UpdateManager.class);
        }
        NeoForge.EVENT_BUS.register(new SpecialSeedManager());
        NeoForge.EVENT_BUS.register(new HurtAlertEvent());
        NeoForge.EVENT_BUS.register(new ReadMindEvent());
        NeoForge.EVENT_BUS.register(new ResurrectEvent());
        LOGGER.info("Colony Pathing Edition mod loaded");
    }
}