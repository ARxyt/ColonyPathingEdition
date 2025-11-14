package com.arxyt.colonypathingedition.mixins.minecolonies.smelt;

import com.arxyt.colonypathingedition.api.FurnaceBlockEntityExtras;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAIRequestSmelter;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.ItemStackUtils.*;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.BAKER_HAS_NO_FURNACES_MESSAGE;

@Mixin(value = AbstractEntityAIRequestSmelter.class, remap = false)
public abstract class AbstractEntityAIRequestSmelterMixin<J extends AbstractJobCrafter<?, J>, B extends AbstractBuilding> extends AbstractEntityAICrafting<J, B> {
    @Shadow(remap = false) protected abstract int getMaxUsableFurnaces();

    @Unique private static final int STANDARD_DELAY = 5;
    @Unique private static final int BASE_XP_GAIN = 5;
    @Unique private int randomFurnace = -1;

    @Unique
    private boolean isFurnaceNotOccupied(FurnaceBlockEntity furnace){
        FurnaceBlockEntityExtras furnaceExtra = (FurnaceBlockEntityExtras) furnace;
        return furnaceExtra.getFurnaceWorker() < 0 || furnaceExtra.getFurnaceWorker() == worker.getCivilianID();
    }

    @Unique
    private boolean isFurnaceOccupiedBy(FurnaceBlockEntity furnace, int civilianID){
        return ((FurnaceBlockEntityExtras)furnace).getFurnaceWorker() == civilianID;
    }

    @Unique
    private void resetFurnaceOccupy(FurnaceBlockEntity furnace){
        ((FurnaceBlockEntityExtras)furnace).setFurnaceWorker(-1);
    }

    @Unique
    private boolean isFurnaceCanReoccupied(FurnaceBlockEntity furnace){
        return !(((FurnaceBlockEntityExtras) furnace).atProtectTime());
    }

    @Unique
    private void setFurnaceOccupy(FurnaceBlockEntity furnace,int civilianID){
        ((FurnaceBlockEntityExtras)furnace).setFurnaceWorker(civilianID);
    }

    @Unique
    private int countOfUsableFurnaces()
    {
        int count = 0;
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace)
                {
                    if (furnace.getItem(SMELTABLE_SLOT).isEmpty() && (isFurnaceNotOccupied(furnace) || isFurnaceCanReoccupied(furnace)))
                    {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    @Unique
    private void accelerateRandomFurnaces(FurnaceUserModule module) {
        final int size = module.getFurnaces().size();
        if (randomFurnace < 0 || randomFurnace >= size) {
            randomFurnace = worker.getRandom().nextInt(size);
        }
        final Level world = building.getColony().getWorld();
        final BlockPos pos = module.getFurnaces().get(randomFurnace);
        if (WorldUtil.isBlockLoaded(world, pos)) {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace && furnace.getBlockState().getValue(BlockStateProperties.LIT)) {
                FurnaceBlockEntityExtras extrasFurnace = (FurnaceBlockEntityExtras) furnace;
                if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty())) {
                    int addProgress = worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getPrimarySkill()) / 2;
                    while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()) {
                        addProgress = extrasFurnace.addProgress(addProgress);
                        AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                    }
                }
            }
        }
    }

    /**
     * Initialize the stone smeltery and add all his tasks.
     *
     * @param smelteryJob the job he has.
     */
    public AbstractEntityAIRequestSmelterMixin(@NotNull final J smelteryJob)
    {
        super(smelteryJob);
    }

    @Override
    public boolean hasWorkToDo()
    {
        return  countOfBurningFurnaces() > 0 || super.hasWorkToDo();
    }

    /**
     * @author ARxyt
     * @reason Workers calculated separately
     */
    @Overwrite(remap = false)
    protected int getExtendedCount(final ItemStack stack)
    {
        if (currentRecipeStorage != null && currentRecipeStorage.getIntermediate() == Blocks.FURNACE)
        {
            int count = 0;
            for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
            {
                if (WorldUtil.isBlockLoaded(world, pos))
                {
                    final BlockEntity entity = world.getBlockEntity(pos);
                    if (entity instanceof final FurnaceBlockEntity furnace && isFurnaceNotOccupied(furnace))
                    {
                        final ItemStack smeltableSlot = furnace.getItem(SMELTABLE_SLOT);
                        final ItemStack resultSlot = furnace.getItem(RESULT_SLOT);
                        if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, smeltableSlot))
                        {
                            count += smeltableSlot.getCount();
                        }
                        else if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, resultSlot))
                        {
                            count += resultSlot.getCount();
                        }
                    }
                }
            }
            return count;
        }
        return 0;
    }

    /**
     * @author ARxyt
     * @reason isEmpty && !isLit -> isEmpty || !isLit
     */
    @Overwrite(remap = false)
    private BlockPos getPositionOfOvenToRetrieveFrom()
    {
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace)
            {
                int countInResultSlot = 0;
                boolean fullResult = false;
                if (!isEmpty(furnace.getItem(RESULT_SLOT)))
                {
                    countInResultSlot = furnace.getItem(RESULT_SLOT).getCount();
                    fullResult = countInResultSlot >= furnace.getItem(RESULT_SLOT).getMaxStackSize();
                }

                if (fullResult || (countInResultSlot > 0 && (furnace.getItem(SMELTABLE_SLOT).isEmpty() || !furnace.getBlockState().getValue(BlockStateProperties.LIT))))
                {
                    FurnaceBlockEntityExtras furnaceExtra = (FurnaceBlockEntityExtras) furnace;
                    if(furnaceExtra.getFurnacePicker() == worker.getCivilianID() || !(furnaceExtra.atProtectTime())){
                        return pos;
                    }
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
    private boolean accelerateFurnaces()
    {
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getModule(BuildingModules.FURNACE).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace && furnace.getBlockState().getValue(BlockStateProperties.LIT))
                {
                    if(!isFurnaceOccupiedBy(furnace,worker.getCivilianID())){
                        continue;
                    }
                    FurnaceBlockEntityExtras extrasFurnace = (FurnaceBlockEntityExtras) furnace;
                    extrasFurnace.addLitTime((int)Math.ceil(Math.sqrt(worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getSecondarySkill())) * 1.71));
                    if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty()))
                    {
                        int addProgress = (int) (worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getPrimarySkill()) * 1.8);
                        while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                            addProgress = extrasFurnace.addProgress(addProgress);
                            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * @author ARxyt
     * @reason Remastered，Workers calculated separately.
     */
    @Overwrite(remap = false)
    private int countOfBurningFurnaces()
    {
        int count = 0;
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getFirstModuleOccurance(FurnaceUserModule.class).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof final FurnaceBlockEntity furnace)
                {
                    if (isFurnaceOccupiedBy(furnace,worker.getCivilianID()) && !furnace.getItem(SMELTABLE_SLOT).isEmpty())
                    {
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    /**
     * @author ARxyt
     * @reason When picking up, first attempt to give directly to the furnace occupant; if there is no occupant, then attempt to pick up directly.
     */
    @Overwrite(remap = false)
    private void extractFromFurnaceSlot(final FurnaceBlockEntity furnace, final int slot)
    {
        if(isFurnaceNotOccupied(furnace) || slot != RESULT_SLOT){
            InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    worker.getInventoryCitizen());
        }
        else {
            if(!InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    worker.getCitizenColonyHandler().getColony().getCitizen(((FurnaceBlockEntityExtras)furnace).getFurnaceWorker()).getInventory())
            ){
                InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                        new InvWrapper(furnace), slot,
                        worker.getInventoryCitizen());
            }
        }

        if (slot == RESULT_SLOT)
        {
            furnace.getRecipesToAwardAndPopExperience((ServerLevel) Objects.requireNonNull(furnace.getLevel()),  Vec3.atCenterOf(furnace.getBlockPos()));
            if(furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                resetFurnaceOccupy(furnace);
            }
            worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        }
    }

    @Redirect(
            method = "isFuelNeeded",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/FurnaceBlockEntity;isLit()Z"
            ),
            remap = false
    )
    private boolean redirectIsLitForIsFuelNeeded(FurnaceBlockEntity furnace)
    {
        return false;
    }

    @Redirect(
            method = "checkFurnaceFuel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/FurnaceBlockEntity;isLit()Z"
            ),
            remap = false
    )
    private boolean redirectIsLitForCheckFurnaceFuel(FurnaceBlockEntity furnace)
    {
        return false;
    }

    @Unique
    boolean checkRecipeFinish = false;

    /**
     * @author ARxyt
     * @reason Involves result checking after replacement pickup; at this point walkTo is null, requiring extensive code modifications.
     */
    @Overwrite(remap = false)
    private IAIState retrieveSmeltableFromFurnace() {
        if ((walkTo == null && !checkRecipeFinish) || currentRequest == null) {
            return START_WORKING;
        }
        checkRecipeFinish = false;

        int preExtractCount = 0;
        if (walkTo != null){
            final BlockEntity entity = world.getBlockEntity(walkTo);
            if (!(entity instanceof FurnaceBlockEntity) || (isEmpty(((FurnaceBlockEntity) entity).getItem(RESULT_SLOT)))) {
                walkTo = null;
                return START_WORKING;
            }

            FurnaceBlockEntityExtras furnace = (FurnaceBlockEntityExtras) entity;
            if(furnace.getFurnacePicker() != worker.getCivilianID() && furnace.atProtectTime()){
                return START_WORKING;
            }

            if (!walkToWorkPos(walkTo)) {
                furnace.setFurnacePicker(worker.getCivilianID());
                return getState();
            }
            walkTo = null;

            preExtractCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                    stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(currentRequest.getRequest().getStack(), stack));

            extractFromFurnaceSlot((FurnaceBlockEntity) entity, RESULT_SLOT);
            furnace.setFurnacePicker(-1);
        }
        //Do we have the requested item in the inventory now?
        final int resultCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(currentRequest.getRequest().getStack(), stack)) - preExtractCount;
        if (resultCount > 0)
        {
            final ItemStack stack = currentRequest.getRequest().getStack().copy();
            stack.setCount(resultCount);
            currentRequest.addDelivery(stack);

            job.setCraftCounter(job.getCraftCounter() + resultCount);
            job.setProgress(job.getProgress() - resultCount);
            if (job.getMaxCraftingCount() == 0)
            {
                job.setMaxCraftingCount(currentRequest.getRequest().getCount());
            }
            if (job.getCraftCounter() >= job.getMaxCraftingCount() && job.getProgress() <= 0)
            {
                job.finishRequest(true);
                resetValues();
                currentRecipeStorage = null;
                incrementActionsDoneAndDecSaturation();
                return INVENTORY_FULL;
            }
        }

        setDelay(STANDARD_DELAY);
        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason Involves modifications to the distribution strategy / furnace usage strategy, as well as additions to the furnace occupation strategy.
     */
    @Overwrite(remap = false)
    private IAIState fillUpFurnace()
    {
        final FurnaceUserModule module = building.getFirstModuleOccurance(FurnaceUserModule.class);
        if (module.getFurnaces().isEmpty())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(BAKER_HAS_NO_FURNACES_MESSAGE), ChatPriority.BLOCKING));
            }
            setDelay(STANDARD_DELAY);
            return START_WORKING;
        }

        if (walkTo == null || world.getBlockState(walkTo).getBlock() != Blocks.FURNACE)
        {
            walkTo = null;
            setDelay(STANDARD_DELAY);
            return START_WORKING;
        }

        final int burningCount = countOfBurningFurnaces();
        final BlockEntity entity = world.getBlockEntity(walkTo);
        if (entity instanceof final FurnaceBlockEntity furnace && currentRecipeStorage != null)
        {
            if(!isFurnaceNotOccupied(furnace) && !isFurnaceCanReoccupied(furnace))
            {
                return START_WORKING;
            }
            final int maxFurnaces = getMaxUsableFurnaces();
            final int maxUsableFurnaces = Math.min(maxFurnaces,countOfUsableFurnaces());
            final Predicate<ItemStack> smeltable = stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(currentRecipeStorage.getCleanedInput().getFirst().getItemStack(), stack);
            final int smeltableInFurnaces = getExtendedCount(currentRecipeStorage.getCleanedInput().getFirst().getItemStack());
            final int resultInFurnaces = getExtendedCount(currentRecipeStorage.getPrimaryOutput());
            final int resultInCitizenInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                    stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, currentRecipeStorage.getPrimaryOutput()));

            final int targetCount = currentRequest.getRequest().getCount() - smeltableInFurnaces - resultInFurnaces - resultInCitizenInv;

            if (targetCount <= 0)
            {
                if(smeltableInFurnaces + resultInFurnaces == 0){
                    walkTo = null;
                    checkRecipeFinish = true;
                    return RETRIEVING_END_PRODUCT_FROM_FURNACE;
                }
                return START_WORKING;
            }
            final int amountOfSmeltableInBuilding = InventoryUtils.getCountFromBuilding(building, smeltable);
            final int amountOfSmeltableInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), smeltable);

            if (worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty())
            {
                worker.setItemInHand(InteractionHand.MAIN_HAND, currentRecipeStorage.getCleanedInput().getFirst().getItemStack().copy());
            }
            if (amountOfSmeltableInInv > 0)
            {
                if (hasFuelInFurnaceAndNoSmeltable(furnace) || hasNeitherFuelNorSmeltAble(furnace))
                {
                    int toTransfer = 0;
                    if (burningCount < maxFurnaces)
                    {
                        int availableFurnaces = maxUsableFurnaces - burningCount;

                        if (targetCount > STACKSIZE * availableFurnaces)
                        {
                            toTransfer = STACKSIZE;
                        }
                        else
                        {
                            if(targetCount <= 8 * (availableFurnaces - 1)){
                                availableFurnaces =(targetCount - 1) / 8 + 1;
                            }
                            toTransfer = (targetCount / availableFurnaces);
                            if(targetCount % availableFurnaces > 0){
                                toTransfer ++;
                            }
                        }
                    }
                    if (toTransfer > 0) {
                        setFurnaceOccupy(furnace,worker.getCivilianID());
                        if (!walkToSafePos(walkTo)) {
                            return getState();
                        }
                        CitizenItemUtils.hitBlockWithToolInHand(worker, walkTo);
                        InventoryUtils.transferXInItemHandlerIntoSlotInItemHandler(
                                worker.getInventoryCitizen(),
                                smeltable,
                                toTransfer,
                                new InvWrapper(furnace),
                                SMELTABLE_SLOT);
                        if(!furnace.getItem(RESULT_SLOT).isEmpty()){
                            extractFromFurnaceSlot(furnace, RESULT_SLOT);
                        }
                    }
                }
            }
            else if (amountOfSmeltableInBuilding >= targetCount - amountOfSmeltableInInv
                    && currentRecipeStorage.getIntermediate() == Blocks.FURNACE)
            {
                needsCurrently = new Tuple<>(smeltable, targetCount);
                resetFurnaceOccupy(furnace);
                return GATHERING_REQUIRED_MATERIALS;
            }
            else
            {
                job.finishRequest(false);
                resetValues();
                walkTo = null;
                return IDLE;
            }
        }
        else if (!(world.getBlockState(walkTo).getBlock() instanceof FurnaceBlock))
        {
            module.removeFromFurnaces(walkTo);
        }
        walkTo = null;
        setDelay(STANDARD_DELAY);
        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason Currently, only the entry conditions have been modified.
     */
    @Overwrite(remap = false)
    private IAIState checkIfAbleToSmelt()
    {
        // We're fully committed currently, try again later.
        final int burning = countOfBurningFurnaces();
        final int canUse = countOfUsableFurnaces();
        if (canUse == 0 || (burning > 0 && (burning >= getMaxUsableFurnaces() || (job.getCraftCounter() + job.getProgress()) >= job.getMaxCraftingCount())))
        {
            if(canUse == 0){
                walkTo = null;
                checkRecipeFinish = true;
                return RETRIEVING_END_PRODUCT_FROM_FURNACE;
            }
            setDelay(TICKS_SECOND);
            return getState();
        }

        final FurnaceUserModule module = building.getFirstModuleOccurance(FurnaceUserModule.class);
        for (final BlockPos pos : module.getFurnaces())
        {
            final BlockEntity entity = world.getBlockEntity(pos);

            if (entity instanceof FurnaceBlockEntity furnace)
            {
                if (furnace.getItem(SMELTABLE_SLOT).isEmpty() && (isFurnaceNotOccupied(furnace) || isFurnaceCanReoccupied(furnace)))
                {
                    randomFurnace = -1;
                    walkTo = pos;
                    return START_USING_FURNACE;
                }
            }
            else
            {
                if (!(world.getBlockState(pos).getBlock() instanceof FurnaceBlock))
                {
                    module.removeFromFurnaces(pos);
                }
            }
        }

        if (burning > 0)
        {
            setDelay(TICKS_SECOND);
        }
        else{
            accelerateRandomFurnaces(module);
        }

        return getState();
    }

}