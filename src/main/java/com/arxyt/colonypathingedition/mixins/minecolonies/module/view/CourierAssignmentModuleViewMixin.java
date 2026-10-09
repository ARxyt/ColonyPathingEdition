package com.arxyt.colonypathingedition.mixins.minecolonies.module.view;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.CourierAssignmentModuleView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CourierAssignmentModuleView.class)
public abstract class CourierAssignmentModuleViewMixin extends AbstractBuildingModuleView {
    @Unique final private double moduleMaxMultiplier = PathingConfig.WAREHOUSE_ASSIGN_MULTIPLIER.get();

    @Inject(method = "getMaxInhabitants",at = @At("HEAD"), remap = false, cancellable = true)
    public void newGetModuleMax(CallbackInfoReturnable<Integer> cir) {
        if(PathingConfig.NEW_WAREHOUSE_ASSIGN_MAX.get()) {
            int level = this.buildingView.getBuildingLevel();
            cir.setReturnValue((int)Math.ceil(level * (level + 1) * moduleMaxMultiplier));
        }
    }
}
