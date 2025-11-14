package com.arxyt.colonypathingedition.mixins.minecolonies.quarrier;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.jobs.JobQuarrier;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.production.EntityAIQuarrier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;


@Mixin(value = EntityAIQuarrier.class, remap = false)
public abstract class EntityAIQuarrierMixin extends AbstractEntityAIStructureWithWorkOrder<JobQuarrier, BuildingMiner> {

    public EntityAIQuarrierMixin(@NotNull final JobQuarrier job)
    {
        super(job);
    }

    @Override
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        int multiplier = 1 + building.getBuildingLevel();
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                stack.setCount(stack.getCount() * multiplier);
            }
        }
        return drops;
    }
}
