package com.arxyt.colonypathingedition.core.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.data.farmlandmap.FarmlandMapLoader;
import com.arxyt.colonypathingedition.core.manager.LargeBottleCauldronInteractions;
import com.arxyt.colonypathingedition.core.manager.LinkageManager;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ColonyPathingEdition.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FMLCommonSetupSubscriber {

    /**
     * 加载模组，判断当前是否已经加载了节气mod。
     *
     * @param event 模组加载事件
     * @author sxtkl
     * @since 2025/12/12
     */
    @SubscribeEvent
    public static void onFMLCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> LinkageManager.getInstance().loadMods(ModList.get()));
        event.enqueueWork(LargeBottleCauldronInteractions::register);
    }
}
