package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.minecolonies.api.colony.buildingextensions.IBuildingExtension;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.core.colony.buildings.modules.BuildingExtensionsModule;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BuildingExtensionsModule.class, remap = false)
public abstract class BuildingExtensionsModuleMixin extends AbstractBuildingModule {
    @Shadow(remap = false) private boolean shouldAssignManually;

    @Shadow(remap = false) public abstract List<IBuildingExtension> getFreeExtensions();
    @Shadow(remap = false) public abstract boolean assignExtension(IBuildingExtension extension);
    @Shadow(remap = false) public @NotNull abstract List<IBuildingExtension> getOwnedExtensions();
    @Shadow(remap = false) protected abstract boolean canAssignExtensionOverride(IBuildingExtension extension);
    @Shadow(remap = false) public abstract void freeExtension(IBuildingExtension extension);

    @Inject(method = "claimExtensions", at = @At("HEAD"), remap = false, cancellable = true)
    private void checkAndClaimExtensions(CallbackInfo ci)
    {
        if (!shouldAssignManually)
        {
            for (final IBuildingExtension extension : getOwnedExtensions()) {
                if(!canAssignExtensionOverride(extension)){
                    freeExtension(extension);
                }
            }
            for (final IBuildingExtension extension : getFreeExtensions())
            {
                if (assignExtension(extension))
                {
                    break;
                }
            }
        }
        ci.cancel();
    }
}
