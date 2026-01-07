package com.arxyt.colonypathingedition.mixins.minecolonies.home;

import com.arxyt.colonypathingedition.api.BedHandlingModuleExtra;
import com.minecolonies.api.colony.buildings.modules.IAssignsCitizen;
import com.minecolonies.api.colony.buildings.modules.IBuildingEventsModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.colony.buildings.modules.ITickingModule;
import com.minecolonies.core.colony.buildings.modules.AbstractAssignedCitizenModule;
import com.minecolonies.core.colony.buildings.modules.BedHandlingModule;
import com.minecolonies.core.colony.buildings.modules.LivingBuildingModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.minecolonies.core.colony.buildings.modules.BuildingModules.BED;

@Mixin(value = LivingBuildingModule.class, remap = false)
public abstract class LivingBuildingModuleMixin extends AbstractAssignedCitizenModule implements IAssignsCitizen, IBuildingEventsModule, ITickingModule, IPersistentModule{
    /**
     * @author ARxyt
     * @reason More Flexible Bed Requirements
     */
    @Overwrite(remap = false)
    public int getModuleMax()
    {
        if (building.getBuildingLevel() > 0 && building.hasModule(BED)){
            BedHandlingModule module = building.getFirstModuleOccurance(BedHandlingModule.class);
            int bedSize = ((BedHandlingModuleExtra)module).getBedSize();
            return Math.min(bedSize, Math.max(3, building.getBuildingLevel()));
        }
        return building.getBuildingLevel();
    }
}
