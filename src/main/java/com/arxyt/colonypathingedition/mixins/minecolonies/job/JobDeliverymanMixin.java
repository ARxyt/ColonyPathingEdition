package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.api.JobWithAdditionalHireCheck;
import com.arxyt.colonypathingedition.api.JobWithEatingLimit;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = JobDeliveryman.class, remap = false)
public abstract class JobDeliverymanMixin implements JobWithAdditionalHireCheck, JobWithEatingLimit {
    @Shadow(remap = false) public abstract IWareHouse findWareHouse();
    @Shadow(remap = false) public abstract IRequest<IDeliverymanRequestable> getCurrentTask();

    @Override
    public boolean IsHiredByAdditionalWorkPlace() {
        return findWareHouse() == null;
    }

    @Override
    public boolean canEat(final ItemStack stack) {
        final IRequest<? extends IRequestable> currentTask = getCurrentTask();
        if (currentTask == null)
        {
            return true;
        }
        final IRequestable request = currentTask.getRequest();
        return !(request instanceof Delivery) || !ItemStack.isSameItem(((Delivery) request).getStack(), stack);
    }
}
