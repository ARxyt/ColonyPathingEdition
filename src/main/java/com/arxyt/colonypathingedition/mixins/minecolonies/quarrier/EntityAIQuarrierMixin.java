package com.arxyt.colonypathingedition.mixins.minecolonies.quarrier;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.minecolonies.FoodUtilExtra;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMiner;
import com.minecolonies.core.colony.jobs.JobQuarrier;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.production.EntityAIQuarrier;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_BUILDING;
import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_PLACE_SPEED;
import static com.minecolonies.api.util.constant.CitizenConstants.PROGRESS_MULTIPLIER;
import static com.minecolonies.api.util.constant.CitizenConstants.STANDARD_WORKING_RANGE;


@Mixin(value = EntityAIQuarrier.class, remap = false)
public abstract class EntityAIQuarrierMixin extends AbstractEntityAIStructureWithWorkOrder<JobQuarrier, BuildingMiner> {

    @Unique private PathResult<?> gotoPath;
    @Unique private int repathCounter = 0;

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

    @Unique
    private boolean hasFood()
    {
        return FoodUtilExtra.getBestFoodForCitizenWithRestaurantCheck(worker.getInventoryCitizen(), worker.getCitizenData() ,null ,true) != -1;
    }

    /**
     * Related explains on those modes are seen in EntityAIStructureBuilderMixin.
     */
    @Unique
    private boolean formalist(final BlockPos currentBlock) {
        workFrom = currentBlock;
        walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        return true;
    }

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

    @Unique
    private boolean god() {
        return true;
    }

    @Unique
    private boolean gibbon(final BlockPos currentBlock) {
        boolean success = MathUtils.twoDimDistance(worker.blockPosition(), currentBlock) < PathingConfig.GIBBON_RANGE.get();
        if (workFrom == null || success) {
            if (gotoPath == null || gotoPath.isCancelled()) {
                final PathJobMoveCloseToXNearY pathJob = new PathJobMoveCloseToXNearY(world,
                        currentBlock,
                        building.getWorkOrder().getLocation(),
                        4,
                        worker);
                gotoPath = ((MinecoloniesAdvancedPathNavigate) worker.getNavigation()).setPathJob(pathJob, currentBlock, 1.0, false);
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
            if (workFrom == null) {
                return success || repathCounter >= 300;
            }
        }
        boolean hasReached = walkToSafePos(workFrom);
        if(hasReached){
            workFrom = null;
            repathCounter = 300;
        }
        if(success || repathCounter >= 300) {
            return true;
        }
        final double decrease = 1 - worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(BLOCK_PLACE_SPEED);
        repathCounter += Math.max(2, (int)(BUILD_BLOCK_DELAY * PROGRESS_MULTIPLIER / (getPlaceSpeedLevel() / 2.0 + PROGRESS_MULTIPLIER) * decrease));
        if(repathCounter < 0) {
            repathCounter = 300;
            return true;
        }
        return false;
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
        switch (PathingConfig.QUARRIER_MODE.get()) {
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
