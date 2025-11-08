package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.function.Predicate;

@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin<B extends AbstractBuilding,J extends IJob<?>> implements AbstractAISkeletonAccessor<J>, AbstractEntityAIBasicExtra {
    @Final @Shadow(remap = false) public B building;
    @Shadow(remap = false) protected Tuple<Predicate<ItemStack>, Integer> needsCurrently;
    @Shadow(remap = false) protected BlockPos walkTo;

    @Shadow(remap = false) protected abstract boolean walkToBuilding();
    @Shadow(remap = false) protected abstract boolean walkToUnSafePos(BlockPos pos);
    @Shadow(remap = false) protected abstract boolean walkToWorkPos(BlockPos pos);
    @Shadow(remap = false) public abstract void incrementActionsDoneAndDecSaturation();
    @Shadow(remap = false) public abstract IAIState getStateAfterPickUp();
    @Shadow(remap = false) public abstract void setDelay(int timeout);

    @Unique Player nearestPlayer = null;

    @Unique
    public ImmutableList<IRequest<?>> getRequestCannotBeDone() {
        final ArrayList<IRequest<?>> requests = Lists.newArrayList();
        final IRequestManager requestManager = getWorker().getCitizenData().getColony().getRequestManager();
        final IPlayerRequestResolver resolver = requestManager.getPlayerResolver();
        final Set<IToken<?>> requestTokens = new HashSet<>(resolver.getAllAssignedRequests());
        for (final IToken<?> token : requestTokens) {
            IRequest<?> request = requestManager.getRequestForToken(token);

            while (request != null && request.hasParent()) {
                request = requestManager.getRequestForToken(Objects.requireNonNull(request.getParent()));
            }

            if (request != null && !requests.contains(request)) {
                requests.add(request);
            }
        }

        return ImmutableList.copyOf(requests);
    }

    @Unique
    private boolean checkRequestCannotBeDone() {
        ImmutableList<IRequest<?>> requests = getRequestCannotBeDone();
        for(IRequest<?> request : requests) {
            if (request.getRequester().getLocation().equals(building.getLocation()) && !getWorker().getCitizenData().isRequestAsync(request.getId())) {
                return true;
            }
        }
        return false;
    }

    @Redirect(
            method = "lookForRequests",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIBasic;walkToBuilding()Z",
                    remap = false
            ),
            remap = false
    )
    private boolean redirectWalkToBuilding(AbstractEntityAIBasic<?, ?> instance) {
        AbstractEntityCitizen worker = getWorker();
        ICitizenData citizenData = worker.getCitizenData();
        IColony colony = citizenData.getColony();
        if (colony.hasTownHall()) {
            IBuilding townHall = colony.getBuildingManager().getTownHall();
            if (checkRequestCannotBeDone()) {
                if (nearestPlayer != null) {
                    if(townHall.isInBuilding(nearestPlayer.blockPosition())) {
                        return walkToUnSafePos(nearestPlayer.blockPosition());
                    } else {
                        nearestPlayer = null;
                    }
                } else if (townHall.isInBuilding(worker.blockPosition())) {
                    // 在level中查找玩家实体
                    List<? extends Player> players = WorldUtil.getEntitiesWithinBuilding(getWorld(), Player.class, townHall,
                            player -> !player.isSpectator() && colony.getPermissions().hasPermission(player,Action.RIGHTCLICK_ENTITY));
                    Player nearestOfficer = players.stream()
                            .min(Comparator.comparingDouble(p -> p.distanceTo(worker)))
                            .orElse(null);
                    if (nearestOfficer != null) {
                        nearestPlayer = nearestOfficer;
                        return walkToUnSafePos(nearestPlayer.blockPosition());
                    }
                }
                return EntityNavigationUtils.walkToBuilding(worker,townHall);
            }
        }
        // 调用原方法行为：
        return walkToBuilding();
    }

    @ModifyVariable(method = "getNeededItem", at = @At("STORE"), ordinal = 0, remap = false)
    private BlockPos getNeedItem$pos(BlockPos pos) {
        if (!PathingConfig.PICK_MATERIAL_AT_HUT.get()) return pos;
        return pos == null ? null : building.getTileEntity().getTilePos();
    }
}
