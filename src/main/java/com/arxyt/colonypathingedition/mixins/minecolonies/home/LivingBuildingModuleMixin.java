package com.arxyt.colonypathingedition.mixins.minecolonies.home;

import com.arxyt.colonypathingedition.api.BedHandlingModuleExtra;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IBuildingEventsModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.colony.buildings.modules.ITickingModule;
import com.minecolonies.core.colony.buildings.modules.AbstractAssignedCitizenModule;
import com.minecolonies.core.colony.buildings.modules.BedHandlingModule;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.BED;

@Mixin(value = LivingBuildingModule.class, remap = false)
public abstract class LivingBuildingModuleMixin extends AbstractAssignedCitizenModule implements IAssignsCitizen, IBuildingEventsModule, ITickingModule, IPersistentModule{
    @Unique
    private int bedCount = 0;

    /**
     * @author ARxyt
     * @reason More Flexible Bed Requirements
     */
    @Overwrite(remap = false)
    public int getModuleMax()
    {
        if (building.getBuildingLevel() > 0 && building.hasModule(BED)){
            BedHandlingModule module = building.getModule(BED);
            int bedSize = ((BedHandlingModuleExtra)module).getBedSize();
            if(bedSize <= 0){
                bedSize = bedCount;
            } else {
                bedCount = bedSize;
            }
            return Math.min(bedSize, Math.max(3, building.getBuildingLevel()));
        }
        return building.getBuildingLevel();
    }

    @Inject(method = "deserializeNBT", at = @At("HEAD"), remap = false)
    public void deserializeAdditionalNBT (CompoundTag compound, CallbackInfo ci){
        if(compound.contains("bed_size")) {
            bedCount = compound.getInt("bed_size");
        }
    }

    @Inject(method = "serializeNBT", at = @At("HEAD"), remap = false)
    public void serializeAdditionalNBT (CompoundTag compound, CallbackInfo ci){
        compound.putInt("bed_size", bedCount);
    }
}
