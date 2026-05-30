package com.arxyt.colonypathingedition.mixins.minecolonies.module.window;

import com.ldtteam.blockui.controls.Button;
import com.minecolonies.core.client.gui.modules.RequestTreeWindowModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(RequestTreeWindowModule.class)
public abstract class RequestTreeWindowModuleMixin {

    @Inject(method = "onFulfill", at = @At("TAIL"), remap = false)
    private void onFulfillTail(Button button, CallbackInfo ci)
    {
        button.enable();
    }
}
