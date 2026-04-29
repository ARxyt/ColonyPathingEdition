package com.arxyt.colonypathingedition.mixins.minecolonies.miner;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.minecolonies.FoodUtilExtra;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.jobs.JobMiner;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_BUILDING;
import static com.minecolonies.api.util.constant.CitizenConstants.STANDARD_WORKING_RANGE;

@Mixin(value = EntityAIStructureMiner.class, remap = false)
public abstract class EntityAIMinerMixin extends AbstractEntityAIStructureWithWorkOrder<JobMiner, BuildingMiner> {

    @Unique private PathResult<?> gotoPath;
    @Unique private int repathCounter = 0;

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

    @Unique
    private boolean hasFood()
    {
        return FoodUtilExtra.getBestFoodForCitizenWithRestaurantCheck(worker.getInventoryCitizen(), worker.getCitizenData() ,null ,true) != -1;
    }

    /**
     * 像矿工一样一边走一边工作，而非等到到达目的地，这个步骤需要开启一个寻路代理。
     * Work while moving like a miner, instead of waiting to reach the destination; this step requires activating a pathfinding proxy.
     * @return Whether block can be placed.
     * @author sxtkl
     * @since 2025/7/21
     */
    @Unique
    private boolean formalist(final BlockPos currentBlock) {
        workFrom = currentBlock;
        walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        return true;
    }

    /**
     * 像哨兵一样站在工地的某个位置开始工作。
     * Start working at a specific position on the construction site, like a sentinel.
     * @return Whether worker reached work site.
     * @author sxtkl ARxyt
     * @since 2025/8/19
     */
    @Unique
    private boolean sentry() {
        BlockPos workPos = building.getWorkOrder().getLocation();
        if (workFrom == null) {
            if (gotoPath == null || gotoPath.isCancelled()) {
                final PathJobMoveCloseToXNearY pathJob = new PathJobMoveCloseToXNearY(world,
                        workPos,
                        workPos,
                        4,
                        worker);
                gotoPath = ((MinecoloniesAdvancedPathNavigate) worker.getNavigation()).setPathJob(pathJob, workPos, 1.0, false);
                pathJob.getPathingOptions().dropCost = 1.5;
                pathJob.extraNodes = 0;
            }
            else if (gotoPath.isDone()) {
                if (gotoPath.getPath() != null)
                {
                    workFrom = gotoPath.getPath().getTarget();
                }
                gotoPath = null;
            }
            return repathCounter >= 3;
        }
        BlockPos workerPos = worker.blockPosition();
        if (!walkToSafePos(workFrom) && DistanceUtils.dist(workerPos, workFrom) >= 10 ){
            return repathCounter >= 3;
        }
        if(DistanceUtils.dist(workPos, workFrom) >= 10){
            if(++repathCounter >= 3) {
                return true;
            }
            else {
                workFrom = null;
                return false;
            }
        }
        return true;
    }

    /**
     * 你的建筑工人会和神一样，无视物理法则直接在小屋平地起高楼。
     * Your construction workers will be like gods, ignoring the laws of physics and building tall structures directly on flat hut grounds.
     * @return always true
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean god() {
        return true;
    }

    /**
     * 你的建筑工人会像长臂猿一样，一边在工地上蹿下跳，一边无限距离得建造，当然前提是他们在工地附近。
     * Your construction workers will behave like gibbons, leaping around the construction site while building at unlimited distances—of course, provided they are near the site.
     * @param currentBlock: As its name.
     * @return 待实现
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean gibbon(final BlockPos currentBlock) {
        workFrom = currentBlock;
        boolean stopPathing = walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        if(MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < PathingConfig.GIBBON_RANGE.get()){
            repathCounter = 0;
            return true;
        }
        return stopPathing && ++repathCounter >= 3;
    }

    /**
     * 注入修改，使建筑工可以一边走一边放置方块
     * Main @Inject for builder mode.
     * @param currentBlock: As its name, and sometime useless.
     * @param cir: Callback information
     * @author sxtkl
     * @since 2025/7/21
     */
    @Inject(at = @At("HEAD"), method = "walkToConstructionSite", cancellable = true, remap = false)
    private void injectWalkToConstructionSite(BlockPos currentBlock, CallbackInfoReturnable<Boolean> cir) {
        switch (PathingConfig.MINER_MODE.get()) {
            case FORMALIST -> cir.setReturnValue(formalist(currentBlock));
            case SENTRY -> cir.setReturnValue(sentry());
            case GOD -> cir.setReturnValue(god());
            case GIBBON -> cir.setReturnValue(gibbon(currentBlock));
        }
    }


    /**
     * Simply reset the repath count
     * @return original return value
     */
    @Override
    protected IAIState structureStep(){
        IAIState returnState = super.structureStep();
        if (returnState != getState()){
            repathCounter = 0;
        }
        return returnState;
    }

    /**
     * Simply reset the repath count
     * @return original return value
     */
    @Override
    public IAIState doMining(){
        IAIState returnState = super.doMining();
        if (returnState != getState()){
            repathCounter = 0;
        }
        return returnState;
    }

    /**
     * Take food before work.
     */
    @Inject(at = @At("RETURN"), method = "startWorkingAtOwnBuilding", remap = false)
    private void takeFoodAfterStartWorkingAtOwnBuilding(CallbackInfoReturnable<IAIState> cir) {
        if(cir.getReturnValue() == START_BUILDING) {
            if(!hasFood()) {
                final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(worker.getCitizenData(), null, building);
                if (storageToGet != null) {
                    InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(building, storageToGet, 16, worker.getInventoryCitizen());
                }
            }
        }
    }
}
