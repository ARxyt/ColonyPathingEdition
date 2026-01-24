package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.message.*;
import com.arxyt.colonypathingedition.core.message.compatible.CompatibleBuildingHiringModeMessage;
import com.arxyt.colonypathingedition.core.message.compatible.CompatibleHireFireMessage;
import com.llamalad7.mixinextras.sugar.Local;
import com.minecolonies.core.MineColonies;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MineColonies.class, remap = false)
public class MinecoloniesMixin {
    @Inject(method = "onNetworkRegistry", at = @At("TAIL"))
    private static void onNetworkRegistry(RegisterPayloadHandlersEvent event, CallbackInfo ci, @Local final PayloadRegistrar registry){
        AlterBlackListMenuItemMessage.TYPE.register(registry);
        CompatibleBuildingHiringModeMessage.TYPE.register(registry);
        CompatibleHireFireMessage.TYPE.register(registry);
        CropRotationAdvanceDayMessage.TYPE.register(registry);
        CropRotationCurrentDayMessage.TYPE.register(registry);
        CropRotationCurrentSeasonMessage.TYPE.register(registry);
        CropRotationLengthUpdateMessage.TYPE.register(registry);
        CropRotationSeasonCountMessage.TYPE.register(registry);
        CropRotationSeedUpdateMessage.TYPE.register(registry);
        FarmFieldResizeMessage.TYPE.register(registry);
        SyncBlackListMenuItemMessage.TYPE.register(registry);
        TavernRecruitMessage.TYPE.register(registry);
    }
}
