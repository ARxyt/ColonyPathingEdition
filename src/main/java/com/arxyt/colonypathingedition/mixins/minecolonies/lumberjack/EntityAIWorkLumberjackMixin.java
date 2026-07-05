package com.arxyt.colonypathingedition.mixins.minecolonies.lumberjack;

import com.arxyt.colonypathingedition.api.AbstractEntityAIInteractExtra;
import com.arxyt.colonypathingedition.api.workersetting.BuildingLumberjackExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIInteractAccessor;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingLumberjack;
import com.minecolonies.core.colony.jobs.JobLumberjack;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkLumberjack;
import com.minecolonies.core.entity.ai.workers.util.Tree;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;

@Mixin(value = EntityAIWorkLumberjack.class, remap = false)
public abstract class EntityAIWorkLumberjackMixin extends AbstractEntityAICrafting<JobLumberjack, BuildingLumberjack> implements AbstractEntityAIInteractAccessor, AbstractEntityAIInteractExtra {

    @Final @Shadow(remap = false) public static float RANGE_HORIZONTAL_PICKUP;

    @Shadow(remap = false) protected abstract boolean mineIfEqualsBlockTag(List<BlockPos> blockPositions, TagKey<Block> tag);

    @Unique BlockPos thisTree;
    @Unique BlockPos lastTree;
    @Unique int gatherState;
    @Unique BlockPos itemPos;

    public EntityAIWorkLumberjackMixin(@NotNull JobLumberjack job) {
        super(job);
        throw new RuntimeException("EntityAIWorkLumberjackMixin 类不应被实例化！");
    }

    @Inject(method = "findSaplingSlot", at = @At("RETURN"), cancellable = true, remap = false)
    private void findSaplingSlotAlwaysTrue(CallbackInfoReturnable<Integer> cir){
        if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get() && cir.getReturnValue() == -1){
            Tree tree = job.getTree();
            InventoryCitizen inventory = getInventory();
            assert tree != null;
            ItemStack targetSapling = tree.getSapling();
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack slotStack = inventory.getStackInSlot(i);
                if (slotStack.isEmpty()) {
                    inventory.insertItem(i, targetSapling, false);
                    cir.setReturnValue(i);
                    return;
                }
            }
        }
    }

    /**
     * Slot merging / No saplings required
     */
    @Inject(method = "placeSaplings", at = @At("HEAD"), cancellable = true, remap = false)
    private void onPlaceSaplings(
            int saplingSlot,
            @NotNull ItemStack stack,
            @NotNull Block block,
            CallbackInfo ci
    ) {
        Tree tree = job.getTree();
        InventoryCitizen inventory = getInventory();
        assert tree != null;
        int required = tree.getStumpLocations().size();
        ItemStack targetSapling = tree.getSapling();

        int mainSaplingSlot = -1;

        for (int i = 0; i < inventory.getSlots(); i++)
        {
            ItemStack stackInSlot = inventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(stackInSlot, targetSapling))
            {
                mainSaplingSlot = i;
                break;
            }
        }

        if (mainSaplingSlot == -1)
        {
            if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get() && worker.getInventoryCitizen().hasSpace()){
                ItemStack sapling = new ItemStack(targetSapling.getItem(), required);
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack slotStack = inventory.getStackInSlot(i);
                    if (slotStack.isEmpty()) {
                        inventory.insertItem(i, sapling, false);
                        break;
                    }
                }
            }
            else{
                ci.cancel();
                return;
            }
        }

        ItemStack mainStack = inventory.getStackInSlot(mainSaplingSlot);
        int currentCount = mainStack.getCount();
        int needed = required - currentCount;

        if (needed > 0)
        {
            for (int i = mainSaplingSlot + 1; i < inventory.getSlots(); i++)
            {
                ItemStack stackInSlot = inventory.getStackInSlot(i);
                if (ItemStack.isSameItemSameTags(stackInSlot, targetSapling))
                {
                    int transferAmount = Math.min(needed, stackInSlot.getCount());
                    mainStack.grow(transferAmount);
                    stackInSlot.shrink(transferAmount);
                    needed -= transferAmount;

                    if (needed <= 0) break;
                }
            }

            if (mainStack.getCount() < required)
            {
                if(PathingConfig.LUMBERJACK_PLANT_WITHOUT_SAPLINGS.get()){
                    inventory.getStackInSlot(mainSaplingSlot).setCount(required);
                }
                ci.cancel();
            }
        }
    }

    /**
     * Store tree pos
     */
    @Inject(method = "findTrees", at = @At("RETURN"), remap = false)
    private void storeTrees(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_CHOP_TREE && job.getTree() != null){
            ((BuildingLumberjackExtra)building).thisTreeToLast();
            ((BuildingLumberjackExtra)building).setThisTree(job.getTree().getLocation());
        }
    }


    /**
     * @author ARxyt
     * @reason Remaster design of state transition
     */
    @Overwrite(remap = false)
    private IAIState gathering(){
        switch (gatherState){
            case 0: {
                lastTree = ((BuildingLumberjackExtra) building).getLastTree();
                if (getItemsForPickUp() == null && lastTree != null) {
                    searchForItems(new AABB(lastTree)
                            .expandTowards(RANGE_HORIZONTAL_PICKUP * 2, RANGE_VERTICAL_PICKUP * 4, RANGE_HORIZONTAL_PICKUP * 2)
                            .expandTowards(-RANGE_HORIZONTAL_PICKUP * 2, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 2));
                    gatherState = 1;
                }
            }
            case 1: {
                if(getItemsForPickUp() != null){
                    break;
                }
            }
            case 2: {
                thisTree = ((BuildingLumberjackExtra) building).getThisTree();
                if(getItemsForPickUp() == null && thisTree != null) {
                    searchForItems(new AABB(thisTree)
                            .expandTowards(RANGE_HORIZONTAL_PICKUP * 2, RANGE_VERTICAL_PICKUP * 4, RANGE_HORIZONTAL_PICKUP * 2)
                            .expandTowards(-RANGE_HORIZONTAL_PICKUP * 2, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 2));
                    gatherState = 3;
                }
            }
            case 3: {
                if(getItemsForPickUp() != null){
                    break;
                }
            }
            case 4: {
                if(getItemsForPickUp() == null && thisTree != null) {
                    searchForItems(worker.getBoundingBox()
                            .expandTowards(RANGE_HORIZONTAL_PICKUP * 2, RANGE_VERTICAL_PICKUP, RANGE_HORIZONTAL_PICKUP * 2)
                            .expandTowards(-RANGE_HORIZONTAL_PICKUP * 2, -RANGE_VERTICAL_PICKUP, -RANGE_HORIZONTAL_PICKUP * 2));
                    gatherState = 5;
                }
            }
            default: break;
        }
        if (getItemsForPickUp() != null)
        {
            gatherItems();
            return getState();
        }
        gatherState = 0;
        return LUMBERJACK_SEARCHING_TREE;
    }

    /**
     * Optional: teleport items to lumberjack's feet pos.
    */
    @Unique
    public void finalizeGatherIstantly(){
        List<ItemEntity> farItems = world.getEntitiesOfClass(ItemEntity.class, new AABB(itemPos)
                        .expandTowards(1, 1, 1)
                        .expandTowards(-1, -1, -1))
                .stream()
                .filter(item -> item != null && item.isAlive() &&
                        (!item.getPersistentData().contains("PreventRemoteMovement") || !item.getPersistentData().getBoolean("PreventRemoteMovement")) &&
                        isItemWorthPickingUp(item.getItem()))
                .toList();
        for (ItemEntity item: farItems) {
            item.absMoveTo(worker.getBlockX() + 0.5d,worker.getBlockY(),worker.getBlockZ() + 0.5d);
        }
    }

    /**
     * Optional: break leaves for items on trees.
     */
    @Unique
    public boolean finalizeGatherByBreakingLeaves(){
        List<ItemEntity> farItems = world.getEntitiesOfClass(ItemEntity.class, new AABB(itemPos)
                        .expandTowards(1, 1, 1)
                        .expandTowards(-1, -1, -1))
                .stream()
                .filter(item -> item != null && item.isAlive() &&
                        (!item.getPersistentData().contains("PreventRemoteMovement") || !item.getPersistentData().getBoolean("PreventRemoteMovement")) &&
                        isItemWorthPickingUp(item.getItem()))
                .toList();
        if(farItems.isEmpty()){
            return false;
        }
        for (ItemEntity item: farItems) {
            item.absMoveTo(itemPos.getX() + 0.5d, itemPos.getY(), itemPos.getZ() + 0.5d);
        }
        //break leaves below.
        List<BlockPos> BlockPosList = new ArrayList<>();
        BlockPos nowPos = itemPos;
        for (int y = nowPos.getY(); y >= worker.getBlockY(); y--) {
            if (world.getBlockState(nowPos).is(BlockTags.LEAVES)) {
                BlockPosList.add(nowPos);
            }
            else if (!world.getBlockState(nowPos).isAir()){
                return false;
            }
            nowPos = nowPos.below();
        }
        if (!BlockPosList.isEmpty()) {
            if (mineIfEqualsBlockTag(BlockPosList, BlockTags.LEAVES)) {
                resetStillTick();
                return true;
            }
        }
        return false;
    }

    /**
     * Remaster design
     */
    @Override
    public void gatherItems()
    {
        worker.setCanPickUpLoot(true);
        if (worker.getNavigation().isDone() || worker.getNavigation().getPath() == null) {
            if(itemPos != null) {
                if (PathingConfig.LUMBERJACK_BREAK_LEAVES_TO_GATHER.get()) {
                    if(finalizeGatherByBreakingLeaves()) {
                        return;
                    }
                }
                else{
                    finalizeGatherIstantly();
                }
            }
            if (checkPuckUpItems()) {
                resetPickUpItems();
                return;
            }
            final BlockPos pos = invokeGetAndRemoveClosestItemPosition();
            itemPos = pos;
            EntityNavigationUtils.walkToPos(worker, pos, 2, false);
            return;
        }

        final int currentIndex = worker.getNavigation().getPath().getNextNodeIndex();
        //We moved a bit, not stuck
        if (tryMoveForward(currentIndex))
        {
            return;
        }

        //Stuck for too long
        if (isStillTicksExceeded(PathingConfig.LUMBERJACK_GATHER_WAITING_TIME.get()))
        {
            //Skip this item
            worker.getNavigation().stop();
            if (checkPuckUpItems())
            {
                resetPickUpItems();
            }
        }
    }

    /**
     * Check drops before start working.
     */
    @Inject(method = "prepareForWoodcutting", at = @At("RETURN"), remap = false, cancellable = true)
    private void remasterPrepareOrderForWoodcutting(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == LUMBERJACK_SEARCHING_TREE && (building.shouldRestrict() || lastTree == null || !(DistanceUtils.dist(building.getPosition(),lastTree) > 50))){
            cir.setReturnValue(LUMBERJACK_GATHERING);
        }
    }
}
