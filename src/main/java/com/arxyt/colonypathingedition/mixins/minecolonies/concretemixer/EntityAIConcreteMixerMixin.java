package com.arxyt.colonypathingedition.mixins.minecolonies.concretemixer;

import com.arxyt.colonypathingedition.api.workersetting.BuildingConcreteMixerExtra;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingConcreteMixer;
import com.minecolonies.core.colony.jobs.JobConcreteMixer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIConcreteMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.List;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.GATHERING_REQUIRED_MATERIALS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.START_WORKING;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.Constants.UPDATE_FLAG;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_CRAFTED_DETAIL;

@Mixin(value = EntityAIConcreteMixer.class, remap = false)
public abstract class EntityAIConcreteMixerMixin extends AbstractEntityAICrafting<JobConcreteMixer, BuildingConcreteMixer>{

    @Shadow(remap = false) @Final private static Predicate<ItemStack> CONCRETE;

    @Unique
    private final Predicate<ItemStack> REQUESTED_CONCRETE =
            stack -> CONCRETE.test(stack) &&
                    currentRequest != null && currentRecipeStorage != null &&
                    ItemHandlerHelper.canItemStacksStack(stack, currentRecipeStorage.getCleanedInput().get(0).getItemStack());

    @Unique
    private final Predicate<ItemStack> REQUEST_CONCRETE_BLOCK =
            stack -> stack.getItem() == currentRequest.getRequest().getStack().getItem();

    @Unique BlockPos posToMine = null;

    public EntityAIConcreteMixerMixin(@NotNull final JobConcreteMixer job) {
        super(job);
    }


    @Unique
    private int getSlotWithPowderWithRequest()
    {
        if (currentRequest != null && currentRecipeStorage != null)
        {
            final ItemStack inputStack = currentRecipeStorage.getCleanedInput().get(0).getItemStack();
            if (CONCRETE.test(inputStack))
            {
                return InventoryUtils.findFirstSlotInItemHandlerWith(worker.getInventoryCitizen(), s -> ItemStackUtils.compareItemStacksIgnoreStackSize(s, inputStack));
            }
        }
        return -1;
    }

    /**
     * @author ARxyt
     * @reason Accelerate work by combine several action at once.
     */
    @Overwrite(remap = false)
    private IAIState placePowder()
    {
        final BlockPos posToPlace = building.getBlockToPlace();
        if (posToPlace == null)
        {
            return START_WORKING;
        }

        final int slot = getSlotWithPowderWithRequest();
        if (slot == -1)
        {
            if (InventoryUtils.getCountFromBuilding(building, REQUESTED_CONCRETE) > 0)
            {
                needsCurrently = new Tuple<>(REQUESTED_CONCRETE, STACKSIZE);

                return GATHERING_REQUIRED_MATERIALS;
            }

            return START_WORKING;
        }

        if (!walkToWorkPos(posToPlace))
        {
            return getState();
        }

        final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
        final Block block = ((BlockItem) stack.getItem()).getBlock();
        int quantity = Math.min(stack.getCount(), 1 + getSecondarySkillLevel() / 6);
        if (InventoryUtils.attemptReduceStackInItemHandler(worker.getInventoryCitizen(), stack, quantity))
        {
            ((BuildingConcreteMixerExtra)building).addSimulatedBlock(posToPlace,quantity);
            world.setBlock(posToPlace, block.defaultBlockState().updateShape(Direction.DOWN, block.defaultBlockState(), world, posToPlace, posToPlace), UPDATE_FLAG);
        }

        return getState();
    }

    /**
     * @author ARxyt
     * @reason Accelerate work by combine several action at once.
     */
    @Overwrite(remap = false)
    private IAIState harvestConcrete()
    {
        posToMine = building.getBlockToMine();
        if (posToMine == null)
        {
            this.resetActionsDone();
            return START_WORKING;
        }

        if (!walkToWorkPos(posToMine))
        {
            return getState();
        }

        final BlockState blockToMine = world.getBlockState(posToMine);
        if (mineBlock(posToMine))
        {
            int multiplier = ((BuildingConcreteMixerExtra)building).getSimulatedTimes(posToMine);
            ((BuildingConcreteMixerExtra)building).deleteSimulateBlock(posToMine);

            StatsUtil.trackStatByName(building, ITEMS_CRAFTED_DETAIL, blockToMine.getBlock().getDescriptionId(), multiplier);
            if (currentRequest != null && currentRecipeStorage != null && blockToMine.getBlock().asItem().equals(currentRecipeStorage.getPrimaryOutput().getItem()))
            {
                job.setCraftCounter(job.getCraftCounter() + multiplier);
                if (job.getCraftCounter() >= job.getMaxCraftingCount() && building.getBlockToMine() == null)
                {
                    return finalizeCraftingTask();
                }
            }
        }

        return getState();
    }

    @Override
    protected List<ItemStack> increaseBlockDrops(final List<ItemStack> drops)
    {
        int multiplier = ((BuildingConcreteMixerExtra)building).getSimulatedTimes(posToMine);

        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                stack.setCount(stack.getCount() * multiplier);
            }
        }

        return drops;
    }

}
