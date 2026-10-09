package com.arxyt.colonypathingedition.mixins.minecolonies.resolver;

import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.requestsystem.resolvers.core.AbstractRequestResolver;
import com.minecolonies.core.colony.requestsystem.resolvers.core.AbstractWarehouseRequestResolver;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = AbstractWarehouseRequestResolver.class, remap = false)
public abstract class AbstractWarehouseRequestResolverMixin extends AbstractRequestResolver<IDeliverable> {

    public AbstractWarehouseRequestResolverMixin(
            @NotNull final ILocation location,
            @NotNull final IToken<?> token)
    {
        super(location, token);
    }

    /**
     * @author ARxyt
     * @reason 未检测自指，补充简单的自指检测与仓库循环检测
     */
    @Overwrite(remap = false)
    public boolean isRequestChainValid(@NotNull final IRequestManager manager, final IRequest<?> requestToCheck)
    {
        final Colony colony = (Colony) manager.getColony();
        final Set<BlockPos> wareHouses = new HashSet<>();
        wareHouses.add(this.getLocation().getInDimensionLocation());
        if (colony.getServerBuildingManager().getBuilding(requestToCheck.getRequester().getLocation().getInDimensionLocation()) instanceof IWareHouse) {
            wareHouses.add(requestToCheck.getRequester().getLocation().getInDimensionLocation());
        }
        return isRequestChainValid(manager, colony, requestToCheck, new HashSet<>(), wareHouses);
    }

    @Unique
    private boolean isRequestChainValid(@NotNull final IRequestManager manager, Colony colony, final IRequest<?> requestToCheck, Set<IToken<?>> visited, Set<BlockPos> wareHouses)
    {
        if (!requestToCheck.hasParent())
        {
            return true;
        }

        final IToken<?> parentToken = requestToCheck.getParent();
        if (!visited.add(parentToken) || visited.size() > 25)
        {
            // 检测到循环，返回 false 或 true（视逻辑而定，通常应视为无效链）
            return false;
        }

        final IRequest<?> parentRequest = manager.getRequestForToken(parentToken);

        if (parentRequest == null)
        {
            return true;
        }

        final BlockPos parentPos = parentRequest.getRequester().getLocation().getInDimensionLocation();
        if (colony.getServerBuildingManager().getBuilding(parentPos) instanceof IWareHouse) {
            if(wareHouses.contains(parentPos)) {
                return false;
            }
            wareHouses.add(parentPos);
        }

        return isRequestChainValid(manager, colony, parentRequest, visited, wareHouses);
    }
}
