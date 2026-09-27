package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.api.workersetting.BuildingPickupExtra;
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
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractAISkeleton;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.util.WorkerUtil;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;
import java.util.function.Predicate;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.NO_TOOL;

@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public abstract class AbstractEntityAIBasicMixin<B extends AbstractBuilding,J extends IJob<?>> extends AbstractAISkeleton<J> implements AbstractAISkeletonAccessor<J>, AbstractEntityAIBasicExtra {
    @Final @Shadow(remap = false) public B building;
    @Shadow(remap = false) protected Tuple<Predicate<ItemStack>, Integer> needsCurrently;
    @Shadow(remap = false) private int dumpedItems;;

    @Shadow(remap = false) protected abstract boolean walkToBuilding();
    @Shadow(remap = false) protected abstract boolean walkToUnSafePos(BlockPos pos);
    @Shadow(remap = false) public abstract void setDelay(int timeout);
    @Shadow(remap = false) protected abstract void checkForToolOrWeaponAsync(@NotNull EquipmentTypeEntry toolType, int minLevel, int maxLevel);
    @Shadow(remap = false) protected abstract void requestTool(@NotNull BlockState target, BlockPos pos);

    @Unique Player nearestPlayer = null;

    protected AbstractEntityAIBasicMixin(@NotNull final J job) {
        super(job);
    }

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

    /**
     * @author ARxyt
     * @reason Add more reliable tool finding method.
     */
    @Overwrite(remap = false)
    public boolean holdEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final int bestSlot = getMostEfficientTool(target, pos);
        if (bestSlot >= 0)
        {
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, bestSlot);
            return true;
        }
        else if (bestSlot == NO_TOOL)
        {
            worker.getCitizenData().setJobStatus(JobStatus.WORKING);
            CitizenItemUtils.removeHeldItem(worker);
            // We may find a block could mine in a higher speed, but we do not have tool to use.
            requestIfCanUseTool(target, pos);
            return true;
        }
        requestTool(target, pos);
        return false;
    }

    @Unique
    private void requestIfCanUseTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final EquipmentTypeEntry toolType = WorkerUtil.getBestToolForBlock(target, target.getDestroySpeed(world, pos), building, world, pos);
        if(toolType == ModEquipmentTypes.none.get()) {
            return;
        }
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);
        final int maxLevel = worker.getCitizenColonyHandler().getWorkBuilding() == null? building.getMaxEquipmentLevel() : worker.getCitizenColonyHandler().getWorkBuilding().getMaxEquipmentLevel();
        checkForToolOrWeaponAsync(toolType, required, maxLevel);
    }

    /**
     * @author ARxyt
     * @reason Add more reliable tool finding method.
     */
    @Overwrite(remap = false)
    protected int getMostEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final EquipmentTypeEntry toolType = WorkerUtil.getBestToolForBlock(target, target.getDestroySpeed(world, pos), building, world, pos);
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);

        @NotNull final InventoryCitizen inventory = worker.getInventoryCitizen();
        if (toolType == ModEquipmentTypes.none.get())
        {
            int bestSlot = NO_TOOL;
            int bestLevel = 0;
            // find tool with special enchantment.
            for (int i = 0; i < worker.getInventoryCitizen().getSlots(); i++)
            {
                final ItemStack item = inventory.getStackInSlot(i);
                boolean silkTouch = item.getEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH)) > 0;
                if(silkTouch) {
                    return i;
                }
                int fortune = item.getEnchantmentLevel(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE));
                if(fortune > bestLevel) {
                    bestLevel = fortune;
                    bestSlot = i;
                }
            }
            return bestSlot;
        }

        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding() == null ?
                building.getMaxEquipmentLevel() : worker.getCitizenColonyHandler().getWorkBuilding().getMaxEquipmentLevel();
        int bestSlot = -1;
        int bestLevel = Integer.MIN_VALUE;

        for (int i = 0; i < inventory.getSlots(); i++)
        {
            final ItemStack itemStack = inventory.getStackInSlot(i);
            final int miningLevel = toolType.getMiningLevel(itemStack);
            final int trueLevel = miningLevel + ItemStackUtils.getMaxEnchantmentLevel(itemStack);

            if (miningLevel > -1 && miningLevel >= required && trueLevel > bestLevel && trueLevel <= maxToolLevel)
            {
                bestSlot = i;
                bestLevel = trueLevel;
            }

            if(bestLevel == maxToolLevel) {
                break;
            }
        }
        return (bestSlot != -1 || target.requiresCorrectToolForDrops())? bestSlot : NO_TOOL;
    }

    // TODO: 此为强兼代码，提升依赖版本后需要删除
    @Redirect(
            method = "dumpInventory",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/AbstractEntityAIBasic;isAfterDumpPickupAllowed()Z"
            )
    )
    private boolean redirectIsAfterDumpPickupAllowed(AbstractEntityAIBasic<?,?> instance) {
        boolean original = instance.isAfterDumpPickupAllowed();

        // if we do not using new delivery AI, keep on using original codes.
        if (!PathingConfig.DELIVERYMAN_AI_MODULE.get()) {
            return original;
        }
        if(original && building.getPickUpPriority() > 0 && dumpedItems > 0 && building instanceof BuildingPickupExtra pickupExtra) {
            int newPriority = pickupExtra.shouldPickup(building.getPickUpPriority(), dumpedItems);
            dumpedItems = 0;
            // they reworked pickup structure, so if pickup request generate could fail, if that happens, we use original codes to generate.
            try{
                if(newPriority > 0) {
                    pickupExtra.newCreatePickupRequest(newPriority);
                }
                return false;
            } catch (Exception | Error e) {
                // nothing happens
            }
        }
        return original;
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
        if (colony.getServerBuildingManager().hasTownHall()) {
            IBuilding townHall = colony.getServerBuildingManager().getTownHall();
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
