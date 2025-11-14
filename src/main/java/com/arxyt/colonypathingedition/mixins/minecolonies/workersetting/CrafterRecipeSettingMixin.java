package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.colony.buildings.modules.settings.CrafterRecipeSetting;
import com.minecolonies.core.colony.buildings.modules.settings.StringSettingWithDesc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CrafterRecipeSetting.class, remap = false)
public class CrafterRecipeSettingMixin extends StringSettingWithDesc {
    @Inject(method = "<init>()V", at = @At("TAIL"), remap = false)
    private void init(CallbackInfo ci) {
        if (!PathingConfig.USE_MAX_STOCK_FIRST.get()) {
            return;
        }
        this.currentIndex = 1;
    }
}
