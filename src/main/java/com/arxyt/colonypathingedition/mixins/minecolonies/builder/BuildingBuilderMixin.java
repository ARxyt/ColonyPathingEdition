package com.arxyt.colonypathingedition.mixins.minecolonies.builder;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuildingStructureBuilder;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BuildingBuilder.class, remap = false)
public abstract class BuildingBuilderMixin extends AbstractBuildingStructureBuilder {

    public BuildingBuilderMixin(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public void cancelAllRequestsOfCitizenOrBuilding(final ICitizenData data)
    {
        if (data == null) return;
        super.cancelAllRequestsOfCitizenOrBuilding(data);
    }
}
