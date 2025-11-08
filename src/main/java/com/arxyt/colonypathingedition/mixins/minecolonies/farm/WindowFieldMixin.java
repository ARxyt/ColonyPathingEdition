package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.arxyt.colonypathingedition.core.window.WindowCropRotation;
import com.minecolonies.api.tileentities.AbstractTileEntityScarecrow;
import com.minecolonies.core.client.gui.containers.WindowField;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WindowField.class)
public class WindowFieldMixin {

    @Shadow(remap = false) private @Nullable FarmField farmField;
    @Shadow(remap = false) @Final private @NotNull AbstractTileEntityScarecrow tileEntityScarecrow;

    @Inject(method = "selectSeed",at = @At("HEAD"),remap = false,cancellable = true)
    public void rewriteSelectSeed(CallbackInfo ci){
        // 打开你自己的窗口
        assert farmField != null;
        WindowCropRotation customWindow = new WindowCropRotation(tileEntityScarecrow,farmField,((WindowField)((Object)this)));
        customWindow.open();
        ci.cancel();
    }
}
