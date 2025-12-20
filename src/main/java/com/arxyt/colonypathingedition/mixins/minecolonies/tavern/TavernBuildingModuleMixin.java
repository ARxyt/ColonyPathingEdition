package com.arxyt.colonypathingedition.mixins.minecolonies.tavern;

import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModels.TAVERN_RECRUIT;

import com.minecolonies.api.colony.IVisitorData;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.core.colony.buildings.modules.TavernBuildingModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TavernBuildingModule.class, remap = false)
public abstract class TavernBuildingModuleMixin extends AbstractBuildingModule {

    @Inject(method = "spawnVisitor", at = @At("TAIL"), remap = false)
    public void a(CallbackInfoReturnable<IVisitorData> cir) {
        if (building.hasModule(TAVERN_RECRUIT)) {
            building.getModule(TAVERN_RECRUIT).markDirty();
        }
    }

}
