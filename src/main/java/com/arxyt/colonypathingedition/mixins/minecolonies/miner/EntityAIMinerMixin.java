package com.arxyt.colonypathingedition.mixins.minecolonies.miner;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.data.tag.ModTag;
import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColonyManager;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.BUILDING_STEP;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_BUILDING;
import static com.minecolonies.api.research.util.ResearchConstants.BLOCK_PLACE_SPEED;
import static com.minecolonies.api.research.util.ResearchConstants.MORE_ORES;
import static com.minecolonies.api.util.constant.CitizenConstants.PROGRESS_MULTIPLIER;
import static com.minecolonies.api.util.constant.CitizenConstants.STANDARD_WORKING_RANGE;
import static com.minecolonies.api.util.constant.Constants.ONE_HUNDRED_PERCENT;
import static com.minecolonies.api.util.constant.StatisticsConstants.BLOCKS_MINED;
import static com.minecolonies.api.util.constant.StatisticsConstants.ORES_MINED;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner.LUCKY_ORE_LOOT_TABLE;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner.LUCKY_ORE_PARAM_SET;

@Mixin(value = EntityAIStructureMiner.class, remap = false)
public abstract class EntityAIMinerMixin extends AbstractEntityAIStructureWithWorkOrder<JobMiner, BuildingMiner> {

    @Unique private PathResult<?> gotoPath;
    @Unique private int repathCounter = 0;

    public EntityAIMinerMixin(@NotNull final JobMiner job)
    {
        super(job);
    }

    private boolean handleNormalRocks(ItemStack itemStack) {
        if(itemStack.is(ModTag.MINER_MULTIPLY_ITEMS)) {
            return true;
        }
        Block block = Block.byItem(itemStack.getItem());
        return block.defaultBlockState().is(ModTag.MINER_MULTIPLY_BLOCKS);
    }

    @Unique
    private boolean hasFood()
    {
        return NewFoodUtils.getBestFoodForCitizenWithRestaurantCheck(worker.getInventoryCitizen(), worker.getCitizenData() ,null ,true) != -1;
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
     * Simply reset the repath count, restore mining surrounding ores.
     * @return original return value
     */
    @Override
    public IAIState doMining(){
        if (blockToMine == null)
        {
            return BUILDING_STEP;
        }

        final BlockState blockState = world.getBlockState(blockToMine);
        if (!IColonyManager.getInstance().getCompatibilityManager().isOre(blockState))
        {
            blockToMine = getSurroundingOreOrDefault(blockToMine);
        }

        if (world.getBlockState(blockToMine).getBlock() instanceof AirBlock)
        {
            return BUILDING_STEP;
        }

        if (!mineBlock(blockToMine, getCurrentWorkingPosition()))
        {
            worker.swing(InteractionHand.MAIN_HAND);
            return getState();
        }

        blockToMine = getSurroundingOreOrDefault(blockToMine);
        if (IColonyManager.getInstance().getCompatibilityManager().isOre(world.getBlockState(blockToMine)))
        {
            return getState();
        }

        worker.decreaseSaturationForContinuousAction();
        return BUILDING_STEP;
    }

    private BlockPos getSurroundingOreOrDefault(final BlockPos pos)
    {
        for (Direction direction : Direction.values())
        {
            final BlockPos offset = pos.relative(direction);
            if (IColonyManager.getInstance().getCompatibilityManager().isOre(world.getBlockState(offset)))
            {
                return offset;
            }
        }
        return pos;
    }


    // Bonus managers.
    @Override
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        int multiplier = bonusTimes();
        if(multiplier <= 1) {
            return drops;
        }
        for (ItemStack stack : drops) {
            if (!stack.isEmpty() && handleNormalRocks(stack)) {
                stack.setCount(stack.getCount() * multiplier);
            }
        }
        return drops;
    }

    @Override
    protected void triggerMinedBlock(@NotNull final BlockPos position, @NotNull final BlockState blockToMine)
    {
        super.triggerMinedBlock(position, blockToMine);

        if (IColonyManager.getInstance().getCompatibilityManager().isLuckyBlock(blockToMine.getBlock()))
        {
            final double chance = 1 + worker.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(MORE_ORES);
            final boolean canGetLuckyBlock =
                    worker.getRandom().nextDouble() * ONE_HUNDRED_PERCENT <= MinecoloniesAPIProxy.getInstance().getConfig().getServer().luckyBlockChance.get() * chance;

            if (canGetLuckyBlock)
            {
                final LootDataManager manager = building.getColony().getWorld().getServer().getLootData();
                final ResourceLocation lootTableId = LUCKY_ORE_LOOT_TABLE.withSuffix(String.valueOf(building.getBuildingLevel()));
                final LootParams lootParams = new LootParams.Builder((ServerLevel) this.world)
                        .withParameter(LootContextParams.ORIGIN, position.getCenter())
                        .withParameter(LootContextParams.THIS_ENTITY, worker)
                        .withParameter(LootContextParams.TOOL, worker.getMainHandItem())
                        .create(LUCKY_ORE_PARAM_SET);

                final ObjectArrayList<ItemStack> randomItems = new ObjectArrayList<>();
                for (int i = 0; i < bonusTimes(); i++) {
                    randomItems.addAll(manager.getLootTable(lootTableId).getRandomItems(lootParams));
                }
                for (final ItemStack stack : randomItems) {
                    InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(stack, worker.getInventoryCitizen());
                }
            }
        }

        if (IColonyManager.getInstance().getCompatibilityManager().isOre(blockToMine))
        {
            building.getColony().getStatisticsManager().increment(ORES_MINED, building.getColony().getDay());
        }
        building.getColony().getStatisticsManager().increment(BLOCKS_MINED, building.getColony().getDay());
    }

    @Unique
    private int bonusTimes() {
        return PathingConfig.ENABLE_DROP_MULTIPLIER.get()? 1 + Math.min(building.getBuildingLevel(), (building.getBuildingLevel() + getPrimarySkillLevel() / 15) / 2) : 1;
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
