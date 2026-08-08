package com.arxyt.colonypathingedition.mixins.minecolonies.resolver;

import com.arxyt.colonypathingedition.core.job.NewJobDeliveryman;
import com.google.common.collect.Lists;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.WarehouseRequestQueueModule;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.colony.requestsystem.resolvers.DeliverymenRequestResolver;
import com.minecolonies.core.colony.requestsystem.resolvers.core.AbstractRequestResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = DeliverymenRequestResolver.class, remap = false)
public abstract class DeliverymenRequestResolverMixin<R extends IRequestable> extends AbstractRequestResolver<R> {

    public DeliverymenRequestResolverMixin(
            @NotNull final ILocation location,
            @NotNull final IToken<?> token)
    {
        super(location, token);
    }

    @Override
    public void onAssignedRequestCancelled(
            @NotNull final IRequestManager manager, @NotNull final IRequest<? extends R> request)
    {
        if (!manager.getColony().getWorld().isClientSide)
        {
            final Colony colony = (Colony) manager.getColony();
            final ICitizenData freeDeliveryMan = colony.getCitizenManager().getCitizens()
                    .stream()
                    .filter(c -> (c.getJob() instanceof JobDeliveryman job1 && job1.getTaskQueue().contains(request.getId())
                            || c.getJob() instanceof NewJobDeliveryman job2 && job2.getTaskQueue().contains(request.getId())))
                    .findFirst()
                    .orElse(null);

            if (freeDeliveryMan != null)
            {
                if(freeDeliveryMan.getJob() instanceof JobDeliveryman job) {
                    job.onTaskDeletion(request.getId());
                }
                else if (freeDeliveryMan.getJob() instanceof NewJobDeliveryman job) {
                    job.onTaskDeletion(request.getId());
                }
            }

            final IWareHouse wareHouse = colony.getServerBuildingManager().getBuilding(getLocation().getInDimensionLocation(), IWareHouse.class);
            if (wareHouse == null)
            {
                return;
            }

            final WarehouseRequestQueueModule module = wareHouse.getModule(BuildingModules.WAREHOUSE_REQUEST_QUEUE);
            module.getMutableRequestList().remove(request.getId());
        }
    }

    @Override
    public boolean canResolveRequest(@NotNull final IRequestManager manager, final IRequest<? extends R> requestToCheck)
    {
        if (manager.getColony().getWorld().isClientSide)
        {
            return false;
        }

        return isValidPlaceToRequest(manager, requestToCheck);
    }

    @Nullable
    @Override
    public List<IToken<?>> attemptResolveRequest(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends R> request)
    {
        if (manager.getColony().getWorld().isClientSide || !isValidPlaceToRequest(manager, request))
        {
            return null;
        }

        return Lists.newArrayList();
    }

    @Override
    public void resolveRequest(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends R> request) throws RuntimeException
    {
        final Colony colony = (Colony) manager.getColony();
        final IWareHouse wareHouse = colony.getServerBuildingManager().getBuilding(getLocation().getInDimensionLocation(), IWareHouse.class);
        if (wareHouse == null)
        {
            return;
        }

        if (!isValidPlaceToRequest(colony, wareHouse, request))
        {
            return;
        }

        final WarehouseRequestQueueModule module = wareHouse.getModule(BuildingModules.WAREHOUSE_REQUEST_QUEUE);
        module.addRequest(request.getId());
    }

    public boolean isValidPlaceToRequest(@NotNull final IRequestManager manager, final IRequest<? extends R> requestToCheck)
    {
        final Colony colony = (Colony) manager.getColony();
        final IWareHouse wareHouse = colony.getServerBuildingManager().getBuilding(getLocation().getInDimensionLocation(), IWareHouse.class);
        if (wareHouse == null)
        {
            return false;
        }
        return isValidPlaceToRequest(colony, wareHouse, requestToCheck);
    }

    public boolean isValidPlaceToRequest(final Colony colony, @NotNull final IWareHouse wareHouse, final IRequest<? extends R> requestToCheck) {

        if (colony.getServerBuildingManager().getBuilding(requestToCheck.getRequester().getLocation().getInDimensionLocation()) instanceof IWareHouse)
        {
            return requestToCheck.getRequester().getLocation().equals(getLocation());
        }
        return !wareHouse.getModule(BuildingModules.WAREHOUSE_COURIERS).getAssignedCitizen().isEmpty();
    }
}
