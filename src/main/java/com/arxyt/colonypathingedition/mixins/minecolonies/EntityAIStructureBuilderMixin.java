package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBuilder;
import com.minecolonies.core.colony.jobs.JobBuilder;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructureWithWorkOrder;
import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.LOAD_STRUCTURE;
import static com.minecolonies.api.util.constant.CitizenConstants.*;

@Mixin(value = EntityAIStructureBuilder.class, remap = false)
public abstract class EntityAIStructureBuilderMixin extends AbstractEntityAIStructureWithWorkOrder<JobBuilder, BuildingBuilder> {

    @Shadow(remap = false) PathResult<?> gotoPath;

    @Unique private int repathCounter = 0;

    /**
     * 这是为了通过 abstract 语法才创建的实例化方法，实际上任何情况下这个类都不应该被实例化。
     * @param job 工作
     * @author sxtkl
     * @since 2025/7/21
     */
    public EntityAIStructureBuilderMixin(@NotNull JobBuilder job) {
        super(job);
        throw new RuntimeException("EntityAIStructureBuilderMixin 类不应被实例化！");
    }

    @Unique
    private boolean hasFood()
    {
        return FoodUtils.getBestFoodForCitizen(worker.getInventoryCitizen(), worker.getCitizenData(), null) != -1;
    }

    /**
     * 像矿工一样一边走一边工作，而非等到到达目的地，这个步骤需要开启一个寻路代理。增加远距离支持。
     * @return 是否可以放置方块
     * @author sxtkl ARxyt
     * @since 2025/9/21
     */
    @Unique
    private boolean formalist(final BlockPos currentBlock) {
        workFrom = currentBlock;
        walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        return true;
    }

    /**
     * 像哨兵一样站在工地的某个位置开始工作。
     * @return 是否走到了工作地点
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
     * @return 只要有材料，一直都可以放置方块
     * @author sxtkl
     * @since 2025/7/22
     */
    @Unique
    private boolean god() {
        return true;
    }

    /**
     * 你的建筑工人会像长臂猿一样，一边在工地上蹿下跳，一边无限距离得建造，当然前提是他们在工地附近。增加远距离退化。
     * @param currentBlock 当前处理的方块
     * @return 是否已到达目标地点附近 / 已经无路可走
     * @author sxtkl ARxyt
     * @since 2025/7/22
     */
    @Unique
    private boolean gibbon(final BlockPos currentBlock) {
        workFrom = currentBlock;
        boolean stopPathing = walkWithProxy(workFrom, STANDARD_WORKING_RANGE);
        if(MathUtils.twoDimDistance(worker.blockPosition(), workFrom) < PathingConfig.BUILDER_GIBBON_RANGE.get()){
            repathCounter = 0;
            return true;
        }
        return stopPathing && ++repathCounter >= 3;
    }

    /**
     * 注入修改，使建筑工可以一边走一边放置方块
     * @param currentBlock 当前工作的方块位置，暂时用不到
     * @param cir 回调信息
     * @author sxtkl
     * @since 2025/7/21
     */
    @Inject(at = @At("HEAD"), method = "walkToConstructionSite", cancellable = true, remap = false)
    private void injectWalkToConstructionSite(BlockPos currentBlock, CallbackInfoReturnable<Boolean> cir) {
        switch (PathingConfig.BUILDER_MODE.get()) {
            case FORMALIST -> cir.setReturnValue(formalist(currentBlock));
            case SENTRY -> cir.setReturnValue(sentry());
            case GOD -> cir.setReturnValue(god());
            case GIBBON -> cir.setReturnValue(gibbon(currentBlock));
        }
    }


    /**
     * 只是重置一下重新寻路次数
     * @return 原本的返回值
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
     * 只是重置一下重新寻路次数
     * @return 原本的返回值
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
     * 如果工作方块内有食物，在临走前拿取一点食物
     */
    @Inject(at = @At("RETURN"), method = "checkForWorkOrder", remap = false)
    private void takeFoodAfterCheckForWorkOrder(CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            if(!hasFood()) {
                final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(worker.getCitizenData(), null, building);
                if (storageToGet != null) {
                    InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(building, storageToGet, 5, worker.getInventoryCitizen());
                }
            }
        }
    }

    /**
     * 每次建筑完成时，请求仓库取走物资
     */
    @Inject(at = @At("RETURN"),method = "sendCompletionMessage", remap = false)
    protected void pickUpAfterSendCompletionMessage(CallbackInfo ci) {
        if (building.getPickUpPriority() > 0) {
            building.createPickupRequest(building.getPickUpPriority());
        }
    }

    /**
     * 注入修改，修改建筑工人的接单位置，让建筑工人可以随时随地接单。
     * @param cir 注入返回值，当配置开启时设置为可以随时随地接单。
     * @author sxtkl
     * @since 2025/8/8
     */
    @Inject(at = @At("HEAD"), method = "startWorkingAtOwnBuilding", cancellable = true, remap = false)
    private void injectStartWorkingAtOwnBuilding(CallbackInfoReturnable<IAIState> cir) {
        if (!PathingConfig.BUILDER_TAKE_ORDERS_EVERYWHERE.get()) {
            return;
        }
        cir.setReturnValue(LOAD_STRUCTURE);
    }
}
