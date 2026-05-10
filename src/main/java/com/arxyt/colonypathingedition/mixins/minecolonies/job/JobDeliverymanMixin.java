package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.api.JobDeliveryExtra;
import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkDeliveryman;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.WarehouseRequestQueueModule;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.colony.requestsystem.requests.StandardRequests;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkDeliveryman;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.DELIVERYMAN_AI_MODULE;
import static com.minecolonies.api.util.constant.BuildingConstants.TAG_ONGOING;
import static com.minecolonies.api.util.constant.Suppression.UNCHECKED;

@Mixin(value = JobDeliveryman.class, remap = false)
public abstract class JobDeliverymanMixin extends AbstractJob<EntityAIWorkDeliveryman, JobDeliveryman> implements JobDeliveryExtra {
    @Shadow(remap = false) private int ongoingDeliveries;
    @Shadow(remap = false) protected abstract LinkedList<IToken<?>> getTaskQueueFromDataStore();
    @Shadow(remap = false) public abstract IWareHouse findWareHouse();
    @Shadow(remap = false) protected abstract boolean haveTasksSameSourceAndDest(@NotNull Delivery requestA, @NotNull Delivery requestB);
    @Shadow(remap = false) public abstract boolean hasSameDestinationDelivery(@NotNull IRequest<? extends Delivery> request);

    public JobDeliverymanMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"), remap = false, cancellable = true)
    public void additionalSerializeNBT(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt(TAG_ONGOING, ongoingDeliveries);
        cir.setReturnValue(tag);
    }

    @Override
    public void createAI(){
        if(DELIVERYMAN_AI_MODULE.get()){
            final NewEntityAIWorkDeliveryman tempAI = new NewEntityAIWorkDeliveryman((JobDeliveryman)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }

    @Unique
    public void setOngoingDeliveries(int target) {
        ongoingDeliveries = target;
    }

    @Unique
    public boolean checkDeliveryFinished() {
        IToken<?> request = getTaskQueueFromDataStore().peekFirst();
        if (request == null) {
            return true;
        }
        IRequest<?> trueRequest = getColony().getRequestManager().getRequestForToken(request);
        return trueRequest == null || !(trueRequest.getRequest() instanceof Delivery);
    }

    /**
     * @author ARxyt
     * @reason Use queue like a stack, result in wrong sending order.
     */
    @Overwrite(remap = false)
    @SuppressWarnings(UNCHECKED)
    public void addRequest(@NotNull final IToken<?> token, final int insertionIndex)
    {
        final IRequestManager requestManager = getColony().getRequestManager();

        LinkedList<IToken<?>> taskQueue = getTaskQueueFromDataStore();

        int offset = 0;
        for (int i = 0; i < taskQueue.size(); i++)
        {
            final IToken<?> theToken = taskQueue.get(i);
            final IRequest<? extends IDeliverymanRequestable> request = (IRequest<? extends IDeliverymanRequestable>) (requestManager.getRequestForToken(theToken));
            if (request == null || request.getState() == RequestState.COMPLETED)
            {
                taskQueue.remove(theToken);
                i--;
                if(i >= taskQueue.size() - insertionIndex) {
                    offset--;
                }
            }
            else
            {
                request.getRequest().incrementPriorityDueToAging();
            }
        }

        getTaskQueueFromDataStore().add(Math.max(0, taskQueue.size() - insertionIndex + offset), token);
    }

    @SuppressWarnings(UNCHECKED)
    public IRequest<IDeliverymanRequestable> getCurrentTaskToDeliver()
    {
        IToken<?> request = getTaskQueueFromDataStore().peekLast();
        if (request == null)
        {
            IBuilding wareHouse = findWareHouse();
            if (wareHouse == null)
            {
                return null;
            }

            final WarehouseRequestQueueModule module = wareHouse.getModule(BuildingModules.WAREHOUSE_REQUEST_QUEUE);
            if (module.getMutableRequestList().isEmpty())
            {
                return null;
            }

            final List<IToken<?>> reqsToRemove = new ArrayList<>();
            int extendedReqs = 0;
            for (final IToken<?> reqId : module.getMutableRequestList())
            {
                final IRequest localRequest = getColony().getRequestManager().getRequestForToken(reqId);
                if (localRequest == null)
                {
                    reqsToRemove.add(reqId);
                    continue;
                }

                if (request == null)
                {
                    addRequest(reqId, 0);
                    request = reqId;
                    reqsToRemove.add(reqId);
                }
                else if (localRequest instanceof StandardRequests.DeliveryRequest && hasSameDestinationDelivery(localRequest))
                {
                    addRequest(reqId, 0);
                    extendedReqs++;
                    reqsToRemove.add(reqId);
                }

                if (extendedReqs > 5)
                {
                    break;
                }

            }

            module.getMutableRequestList().removeAll(reqsToRemove);
            module.markDirty();

            if (request == null)
            {
                return null;
            }

        }

        return (IRequest<IDeliverymanRequestable>) getColony().getRequestManager().getRequestForToken(request);
    }

    @Unique
    @SuppressWarnings(UNCHECKED)
    public IRequest<IDeliverymanRequestable> pickMoreDeliveryTask() {
        IToken<?> request = getTaskQueueFromDataStore().getLast();
        IRequest<?> trueRequest = getColony().getRequestManager().getRequestForToken(request);

        if(trueRequest == null || !(trueRequest.getRequest() instanceof Delivery delivery))
        {
            return null;
        }

        IBuilding wareHouse = findWareHouse();
        if (wareHouse == null)
        {
            return null;
        }

        final WarehouseRequestQueueModule module = wareHouse.getModule(BuildingModules.WAREHOUSE_REQUEST_QUEUE);
        if (module.getMutableRequestList().isEmpty())
        {
            return null;
        }

        final List<IToken<?>> reqsToRemove = new ArrayList<>();
        int extendedReqs = 0;
        for (final IToken<?> reqId : module.getMutableRequestList())
        {
            final IRequest<?> localRequest = getColony().getRequestManager().getRequestForToken(reqId);
            if (localRequest == null)
            {
                reqsToRemove.add(reqId);
                continue;
            }

            if (!(localRequest.getRequest() instanceof Delivery localDelivery))
            {
                continue;
            }

            if (localRequest instanceof StandardRequests.DeliveryRequest && haveTasksSameSourceAndDest(delivery, localDelivery))
            {
                addRequest(reqId, 0);
                extendedReqs++;
                reqsToRemove.add(reqId);
            }

            if (extendedReqs > 5)
            {
                break;
            }

        }

        if (reqsToRemove.isEmpty())
        {
            request = null;
            for (final IToken<?> reqId : module.getMutableRequestList())
            {
                final IRequest<?> localRequest = getColony().getRequestManager().getRequestForToken(reqId);
                if (localRequest == null)
                {
                    reqsToRemove.add(reqId);
                    continue;
                }

                if (!(localRequest.getRequest() instanceof Delivery localDelivery))
                {
                    continue;
                }

                if (request == null && hasSameStart(delivery, localDelivery))
                {
                    addRequest(reqId, 0);
                    request = reqId;
                    delivery = localDelivery;
                    reqsToRemove.add(reqId);
                }
                else if (localRequest instanceof StandardRequests.DeliveryRequest && haveTasksSameSourceAndDest(delivery, localDelivery))
                {
                    addRequest(reqId, 0);
                    extendedReqs++;
                    reqsToRemove.add(reqId);
                }

                if (extendedReqs > 5)
                {
                    break;
                }

            }
        }

        module.getMutableRequestList().removeAll(reqsToRemove);
        module.markDirty();

        return request == null ? null : (IRequest<IDeliverymanRequestable>) getColony().getRequestManager().getRequestForToken(request);
    }

    @Unique
    public boolean hasSameStart(@NotNull final Delivery requestA, @NotNull final Delivery requestB) {
        if (requestA.getStart().equals(requestB.getStart()))
        {
            return true;
        }
        for (final IWareHouse wareHouse : getColony().getServerBuildingManager().getWareHouses())
        {
            if (wareHouse.hasContainerPosition(requestA.getStart().getInDimensionLocation()) && wareHouse.hasContainerPosition(requestB.getStart().getInDimensionLocation()))
            {
                return true;
            }
        }
        return false;
    }
}
