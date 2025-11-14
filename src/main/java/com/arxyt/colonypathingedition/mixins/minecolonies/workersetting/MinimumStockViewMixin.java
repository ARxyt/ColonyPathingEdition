package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.window.WindowPreciseMinimumStock;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.MinimumStockModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinimumStockModuleView.class, remap = false)
public abstract class MinimumStockViewMixin extends AbstractBuildingModuleView{
    @Inject(method = "getWindow", at = @At("HEAD"), cancellable = true, remap = false)
    public void getWindow(CallbackInfoReturnable<BOWindow> cir)
    {
        if(PathingConfig.MINIMUM_STOCK_PRECISE.get()){
            cir.setReturnValue(new WindowPreciseMinimumStock(buildingView, (MinimumStockModuleView)((Object)this)));
        }
    }
}
