package com.arxyt.colonypathingedition.mixins.minecolonies.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.workerbuildings.IBuildingDeliveryman;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BuildingDeliveryman.class)
public abstract class BuildingDeliverymanMixin extends AbstractBuilding implements IBuildingDeliveryman
{
    public BuildingDeliverymanMixin(final IColony c, final BlockPos l)
    {
        super(c, l);
    }

    @Override
    public boolean canEat(final ItemStack stack)
    {
        return super.canEat(stack);
    }
}

