package com.arxyt.colonypathingedition.core;


import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("colonypathingedition")
public class ColonyPathingEdition {
    public static final String MODID = "colonypathingedition";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ColonyPathingEdition(FMLJavaModLoadingContext context) {
        // 注册配置文件
        context.registerConfig(
                ModConfig.Type.COMMON,
                PathingConfig.init(new ForgeConfigSpec.Builder())
        );
    }
}