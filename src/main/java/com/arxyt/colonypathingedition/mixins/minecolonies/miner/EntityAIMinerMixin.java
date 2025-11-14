package com.arxyt.colonypathingedition.mixins.minecolonies.miner;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.jobs.JobMiner;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = EntityAIStructureMiner.class, remap = false)
public abstract class EntityAIMinerMixin extends AbstractEntityAIStructureWithWorkOrder<JobMiner, BuildingMiner> {
    public EntityAIMinerMixin(@NotNull final JobMiner job)
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
