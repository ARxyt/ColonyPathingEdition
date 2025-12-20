package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.message.*;
import com.minecolonies.api.network.IMessage;
import com.minecolonies.core.network.NetworkChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

@Mixin(value = NetworkChannel.class, remap = false)
public abstract class NetworkChannelMixin {
    @Shadow(remap = false) protected abstract <MSG extends IMessage> void registerMessage(int id, Class<MSG> msgClazz, Supplier<MSG> msgCreator);

    @Inject(method = "registerCommonMessages",at = @At("TAIL"),locals = LocalCapture.CAPTURE_FAILEXCEPTION, remap = false)
    void registerNewMessage(CallbackInfo ci, int idx){
        registerMessage(++idx, CropRotationLengthUpdateMessage.class, CropRotationLengthUpdateMessage::new);
        registerMessage(++idx, CropRotationCurrentDayMessage.class, CropRotationCurrentDayMessage::new);
        registerMessage(++idx, CropRotationCurrentSeasonMessage.class, CropRotationCurrentSeasonMessage::new);
        registerMessage(++idx, CropRotationSeasonCountMessage.class, CropRotationSeasonCountMessage::new);
        registerMessage(++idx, CropRotationSeedUpdateMessage.class, CropRotationSeedUpdateMessage::new);
        registerMessage(++idx, CropRotationAdvanceDayMessage.class, CropRotationAdvanceDayMessage::new);
        registerMessage(++idx, TavernRecruitMessage.class, TavernRecruitMessage::new);
    }
}
