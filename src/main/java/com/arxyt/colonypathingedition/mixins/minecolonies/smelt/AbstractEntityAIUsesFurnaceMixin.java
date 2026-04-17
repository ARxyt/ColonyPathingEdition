package com.arxyt.colonypathingedition.mixins.minecolonies.smelt;

import com.arxyt.colonypathingedition.api.FurnaceBlockEntityExtras;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIUsesFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.Constants.RESULT_SLOT;
import static com.minecolonies.api.util.constant.Constants.SMELTABLE_SLOT;

@Mixin(value = AbstractEntityAIUsesFurnace.class, remap = false)
public abstract class AbstractEntityAIUsesFurnaceMixin implements AbstractAISkeletonAccessor<IJob<?>>, AbstractEntityAIBasicAccessor<AbstractBuilding>
{
    @Final @Shadow(remap = false)  private static int RETRIEVE_SMELTABLE_IF_MORE_THAN;

    @Shadow(remap = false) protected abstract void extractFromFurnace(final FurnaceBlockEntity furnace);

    @Inject(
            method = "fillUpFurnace",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/util/InventoryUtils;transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(Lnet/minecraftforge/items/IItemHandler;Ljava/util/function/Predicate;ILnet/minecraftforge/items/IItemHandler;I)I",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void onAfterSmeltableTransfer(CallbackInfoReturnable<?> cir)
    {
        if (getWalkTo() != null)
        {
            BlockEntity entity = getWorld().getBlockEntity(getWalkTo());
            if (entity instanceof FurnaceBlockEntity furnace && !(ItemStackUtils.isEmpty(furnace.getItem(RESULT_SLOT))))
            {
                // Extract smelted products to prevent furnace blockage
                extractFromFurnace(furnace);
            }
        }
    }

    /**
     * @author ARxyt
     * @reason Slightly changed.
     */
    @Overwrite(remap = false)
    protected BlockPos getPositionOfOvenToRetrieveFrom()
    {
        for (final BlockPos pos : getBuilding().getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            final BlockEntity entity = getWorld().getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace)
            {
                final int countInResultSlot = ItemStackUtils.isEmpty(furnace.getItem(RESULT_SLOT)) ? 0 : furnace.getItem(RESULT_SLOT).getCount();

                if ( countInResultSlot > RETRIEVE_SMELTABLE_IF_MORE_THAN || (furnace.getItem(SMELTABLE_SLOT).isEmpty() && countInResultSlot > 0))
                {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * @author ARxyt
     * @reason Boost acceleration, extend burn duration, and reduce serverTick() usage.
     */
    @Overwrite(remap = false)
    private IAIState accelerateFurnaces()
    {
        final Level world = getBuilding().getColony().getWorld();
        for (final BlockPos pos : getBuilding().getModule(BuildingModules.FURNACE).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace && furnace.getBlockState().getValue(BlockStateProperties.LIT))
                {
                    FurnaceBlockEntityExtras extrasFurnace = (FurnaceBlockEntityExtras) furnace;
                    extrasFurnace.colonyPathingEdition$addLitTime(getWorker().getCitizenData().getCitizenSkillHandler().getLevel(invokeGetModuleForJob().getSecondarySkill()) / 15);
                    if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty()))
                    {
                        int addProgress = getWorker().getCitizenData().getCitizenSkillHandler().getLevel(invokeGetModuleForJob().getPrimarySkill()) / 2;
                        while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                            addProgress = extrasFurnace.colonyPathingEdition$addProgress(addProgress);
                            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                        }
                    }
                }
            }
        }
        return null;
    }

}

