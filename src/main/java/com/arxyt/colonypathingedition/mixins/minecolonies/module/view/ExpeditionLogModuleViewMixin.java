package com.arxyt.colonypathingedition.mixins.minecolonies.module.view;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.window.NewExpeditionModuleWindow;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.core.colony.buildings.moduleviews.ExpeditionLogModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExpeditionLogModuleView.class, remap = false)
public class ExpeditionLogModuleViewMixin {
    @Inject(method = "getWindow", at = @At("HEAD"), cancellable = true, remap = false)
    public void getWindow(CallbackInfoReturnable<BOWindow> cir)
    {
        if(PathingConfig.NETHER_WORKER_AI_MODULE.get()){
            cir.setReturnValue(new NewExpeditionModuleWindow((ExpeditionLogModuleView)((Object)this)));
        }
    }
}
