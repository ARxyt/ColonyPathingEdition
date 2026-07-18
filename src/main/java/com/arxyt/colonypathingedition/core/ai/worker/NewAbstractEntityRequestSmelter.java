package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.FurnaceBlockEntityExtras;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AIEventTarget;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIBlockingEventType;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.translation.RequestSystemTranslationConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.FurnaceUserModule;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.*;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.ItemStackUtils.*;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_SMELTED_DETAIL;
import static com.minecolonies.api.util.constant.TranslationConstants.BAKER_HAS_NO_FURNACES_MESSAGE;
import static com.minecolonies.api.util.constant.TranslationConstants.FURNACE_USER_NO_FUEL;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.FURNACE;

public abstract class NewAbstractEntityRequestSmelter <J extends AbstractJobCrafter<?, J>, B extends AbstractBuilding> extends AbstractEntityAICrafting<J, B> {
    /**
     * Base xp gain for the smelter.
     */
    private static final double BASE_XP_GAIN = 5;

    /**
     * Furnace data.
     */
    private int randomFurnace = -1;
    private BlockPos furnacePos = null;
    private AIWorkerState withSpecialReturn = START_WORKING;

    /**
     * Initialize the stone smeltery and add all his tasks.
     *
     * @param smelteryJob the job he has.
     */
    public NewAbstractEntityRequestSmelter(@NotNull final J smelteryJob)
    {
        super(smelteryJob);
        super.registerTargets(
                // Background tasks. Make sure there is enough fuel and accelerate furnaces based on speed.
                new AIEventTarget(AIBlockingEventType.EVENT, this::checkFurnaceFuel, this::checkAndAddFuelToFurnace, TICKS_SECOND * 5),
                new AIEventTarget(AIBlockingEventType.EVENT, this::accelerateFurnaces, this::getState, TICKS_SECOND),

                // Additional AI tasks compared to normal crafting AI
                new AITarget(RETRIEVING_END_PRODUCT_FROM_FURNACE, this::retrieveProductFromFurnace, 1),
                new AITarget(FILL_UP_FURNACES, this::fillUpFurnace, 1),
                new AITarget(ADD_FUEL_TO_FURNACE, this::addFuelToFurnace, 1)
        );
    }

    @Override
    public IAIState executeCraftingAction(final int toolSlot)
    {
        if (currentRecipeStorage == null)
        {
            return START_WORKING;
        }

        if (currentRecipeStorage.getIntermediate() != Blocks.FURNACE)
        {
            return super.executeCraftingAction(toolSlot);
        }

        if (building.getModule(FURNACE).getFurnaces().isEmpty())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData()
                        .triggerInteraction(new StandardInteraction(Component.translatable(BAKER_HAS_NO_FURNACES_MESSAGE), ChatPriority.BLOCKING));
            }
            setDelay(AbstractEntityAIBasic.STANDARD_DELAY);
            return START_WORKING;
        }

        final List<ItemStack> possibleFuels = getAllowedFuel();
        if (possibleFuels.isEmpty())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(FURNACE_USER_NO_FUEL), ChatPriority.BLOCKING));
            }
            return getState();
        }

        furnacePos = getFurnaceToRetrieveOutputFrom();
        if (furnacePos != null)
        {
            return RETRIEVING_END_PRODUCT_FROM_FURNACE;
        }

        if (InventoryUtils.hasBuildingEnoughElseCount(building, isCorrectFuel(possibleFuels), 1) > 1 || InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(possibleFuels)))
        {
            furnacePos = getFurnaceWithoutFuel();
            if (furnacePos != null)
            {
                return ADD_FUEL_TO_FURNACE;
            }
        }

        final int burning = countOfBurningFurnaces();
        if (burning > 0 && burning >= getMaxUsableFurnaces())
        {
            setDelay(TICKS_SECOND);
            return getState();
        }

        furnacePos = getEmptyFurnaceWithFuel();
        if (furnacePos != null)
        {
            randomFurnace = -1;
            return FILL_UP_FURNACES;
        }
        else if(burning == 0){
            setDelay(TICKS_SECOND);
            accelerateRandomFurnaces(building.getModule(FURNACE));
        }

        return START_WORKING;
    }

    /**
     * Add furnace fuel when necessary
     * @return next AI state to transfer to.
     */
    private IAIState addFuelToFurnace()
    {
        // no furnace or furnace not loaded
        if (furnacePos == null || !WorldUtil.isBlockLoaded(world, furnacePos)) {
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        // block isn't furnace
        final BlockEntity entity = world.getBlockEntity(furnacePos);
        if (!(entity instanceof FurnaceBlockEntity furnace))
        {
            furnacePos = null;
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        // check if is correct fueler
        if (!isFurnaceCorrectFueler(furnace, worker.getCivilianID()) && !isFurnaceCanReoccupied(furnace)) {
            setDelay(WAITING_DELAY);
            return CRAFT;
        }

        // a first check of fuel use
        final List<ItemStack> possibleFuels = getActivePossibleFuels();
        // alter check
        final List<ItemStack> alterPossibleFuels = getActivePossibleFuels();

        // set fueler and go to furnace
        setFurnaceFueler(furnace, worker.getCivilianID());

        // we delete fuel related to recipe storge
        if (currentRecipeStorage != null) {
            possibleFuels.removeIf(stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, currentRecipeStorage.getPrimaryOutput()));
            possibleFuels.removeIf(stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, currentRecipeStorage.getCleanedInput().get(0).getItemStack()));
        }

        // check if we have to use alter fuels
        boolean haveToUseAlterFuels = false;
        if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))) {
            if (InventoryUtils.hasBuildingEnoughElseCount(building, isCorrectFuel(possibleFuels), 1) >= 1) {
                needsCurrently = new Tuple<>(isCorrectFuel(possibleFuels), STACKSIZE);
                withSpecialReturn = ADD_FUEL_TO_FURNACE;
                return GATHERING_REQUIRED_MATERIALS;
            }
            haveToUseAlterFuels = true;
        }

        // we have to use alter fuels, so check if we have store some.
        if (haveToUseAlterFuels && !InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(alterPossibleFuels))) {
            if (currentRecipeStorage != null && InventoryUtils.hasBuildingEnoughElseCount(building, isCorrectFuel(alterPossibleFuels), 1) >= 1)
            {
                ItemStack stack = currentRecipeStorage.getCleanedInput().get(0).getItemStack();
                checkIfRequestForItemExistOrCreateAsync(stack, Math.max(currentRecipeStorage.getPrimaryOutput().getCount(),STACKSIZE), currentRecipeStorage.getPrimaryOutput().getCount());
                needsCurrently = new Tuple<>(isCorrectFuel(alterPossibleFuels), STACKSIZE);
                withSpecialReturn = ADD_FUEL_TO_FURNACE;
                return GATHERING_REQUIRED_MATERIALS;
            }
            setDelay(WAITING_DELAY);
            furnacePos = null;
            return START_WORKING;
        }

        if (!walkToWorkPos(furnacePos))
        {
            setDelay(WALKING_DELAY);
            return getState();
        }

        final List<ItemStack> chosenFuels = haveToUseAlterFuels ? alterPossibleFuels : possibleFuels;
        if (InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(chosenFuels)) && isEmpty(furnace.getItem(FUEL_SLOT)))
        {
            InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoInItemHandler(
                    worker.getInventoryCitizen(), isCorrectFuel(chosenFuels), STACKSIZE,
                    new InvWrapper(furnace), FUEL_SLOT);
            setFurnaceFueler(furnace, -1);
        }

        furnacePos = null;
        return START_WORKING;
    }

    /**
     * Retrieve ready products from the furnaces. If no position has been set return. Else navigate to the position of the furnace. On arrival execute the extract method of the
     * specialized worker.
     *
     * @return the next state to go to.
     */
    private IAIState retrieveProductFromFurnace()
    {
        if (furnacePos == null || currentRecipeStorage == null || currentRequest == null) {
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        final BlockEntity entity = world.getBlockEntity(furnacePos);
        if (!(entity instanceof FurnaceBlockEntity furnace) || isEmpty(furnace.getItem(RESULT_SLOT))) {
            furnacePos = null;
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        if(!isFurnaceCorrectPicker(furnace, worker.getCivilianID()) && !isFurnaceCanReoccupied(furnace)) {
            setDelay(WAITING_DELAY);
            return CRAFT;
        }

        if(isFurnaceCorrectPicker(furnace, worker.getCivilianID())) {
            setFurnacePicker(furnace, worker.getCivilianID());
        }

        if (!walkToWorkPos(furnacePos)) {
            setDelay(WALKING_DELAY);
            return getState();
        }
        furnacePos = null;

        final ItemStack stack = furnace.getItem(RESULT_SLOT).copy();
        if (extractFromFurnaceSlot(furnace, RESULT_SLOT))
        {
            final int count = stack.getCount();
            if (count > 0)
            {
                if (ItemStackUtils.compareItemStacksIgnoreStackSize(currentRecipeStorage.getPrimaryOutput(), stack))
                {
                    final ItemStack requestStack = currentRequest.getRequest().getStack().copy();
                    requestStack.setCount(count);
                    currentRequest.addDelivery(requestStack);

                    job.setCraftCounter(job.getCraftCounter() + count);
                    if (job.getMaxCraftingCount() == 0)
                    {
                        job.setMaxCraftingCount(currentRequest.getRequest().getCount());
                    }
                    if (job.getCraftCounter() >= job.getMaxCraftingCount())
                    {
                        return finalizeCraftingTask();
                    }
                }
                else
                {
                    job.getSecondaryOutputs().addTo(new ItemStorage(stack), stack.getCount());
                }
            }
        }
        setDelay(WAITING_DELAY);
        return START_WORKING;
    }

    /**
     * Smelt the smeltable after the required items are in the inv.
     *
     * @return the next state to go to.
     */
    private IAIState fillUpFurnace()
    {
        // no furnace or furnace not loaded
        if (furnacePos == null || currentRecipeStorage == null || !WorldUtil.isBlockLoaded(world, furnacePos))
        {
            furnacePos = null;
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        // block isn't furnace
        final BlockPos targetFurnacePos = furnacePos;
        final BlockEntity entity = world.getBlockEntity(targetFurnacePos);
        if (!(entity instanceof FurnaceBlockEntity furnace)) {
            furnacePos = null;
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        // furnace has been occupied
        if(!isFurnaceNotOccupied(furnace) && !isFurnaceCanReoccupied(furnace)) {
            setDelay(WAITING_DELAY);
            return CRAFT;
        }

        // to do check
        if (job.getMaxCraftingCount() == 0)
        {
            job.setMaxCraftingCount(currentRequest.getRequest().getCount());
        }

        final ItemStack inputStack = currentRecipeStorage.getCleanedInput().getFirst().getItemStack();
        final Predicate<ItemStack> smeltablePredicate = stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(inputStack, stack);
        final int smeltableInInventory = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                stack -> ItemStackUtils.compareItemStacksIgnoreStackSize(stack, inputStack));

        final int smeltableInFurnaces = getExtendedCount(inputStack);
        final int resultInFurnaces = getExtendedCount(currentRecipeStorage.getPrimaryOutput());
        final int targetCount = (job.getMaxCraftingCount() - job.getCraftCounter()) - smeltableInFurnaces - resultInFurnaces - smeltableInInventory;

        if ((job.getMaxCraftingCount() - job.getCraftCounter()) - smeltableInFurnaces - resultInFurnaces <= 0)
        {
            setDelay(WAITING_DELAY);
            return START_WORKING;
        }

        if (smeltableInInventory == 0)
        {
            needsCurrently = new Tuple<>(smeltablePredicate, targetCount);
            furnacePos = null;
            withSpecialReturn = FILL_UP_FURNACES;
            return GATHERING_REQUIRED_MATERIALS;
        }

        final int burningFurnaces = countOfBurningFurnaces();
        final int maxFurnaces = getMaxUsableFurnaces();
        if (burningFurnaces > maxFurnaces)
        {
            setDelay(WAITING_DELAY);
            return START_WORKING;
        }

        final int pendingCount = (job.getMaxCraftingCount() - job.getCraftCounter()) - smeltableInFurnaces - resultInFurnaces;
        if (pendingCount <= 0)
        {
            setDelay(WAITING_DELAY);
            return START_WORKING;
        }

        final int availableSmeltable = Math.min(pendingCount, smeltableInInventory);

        // we occupy the furnace
        setFurnaceOccupy(furnace, worker.getCivilianID());

        // walk to furnace
        if (!walkToWorkPos(targetFurnacePos)) {
            setDelay(WALKING_DELAY);
            return getState();
        }

        if (worker.getItemInHand(InteractionHand.MAIN_HAND).isEmpty())
        {
            worker.setItemInHand(InteractionHand.MAIN_HAND, inputStack.copy());
        }
        if (hasFuelInFurnaceAndNoSmeltable(furnace) || hasNeitherFuelNorSmeltAble(furnace))
        {
            int toTransfer = getToTransfer(maxFurnaces, burningFurnaces, availableSmeltable);
            if (toTransfer > 0)
            {
                CitizenItemUtils.hitBlockWithToolInHand(worker, furnacePos);
                InventoryUtils.transferXInItemHandlerIntoSlotInItemHandler(
                        worker.getInventoryCitizen(),
                        smeltablePredicate,
                        toTransfer,
                        new InvWrapper(furnace),
                        SMELTABLE_SLOT);
                if(!furnace.getItem(RESULT_SLOT).isEmpty()){
                    extractFromFurnaceSlot(furnace, RESULT_SLOT);
                }
            }
        }
        furnacePos = null;

        return CRAFT;
    }

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
                        addProgress = extrasFurnace.pathFindEdition$addProgress(addProgress);
                        AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                    }
                }
            }
        }
    }

    private IAIState checkAndAddFuelToFurnace() {
        final List<ItemStack> possibleFuels = getAllowedFuel();
        if (InventoryUtils.hasBuildingEnoughElseCount(building, isCorrectFuel(possibleFuels), 1) > 1 || InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(possibleFuels)))
        {
            furnacePos = getFurnaceWithoutFuel();
            if (furnacePos != null)
            {
                return ADD_FUEL_TO_FURNACE;
            }
        }
        return getState();
    }

    /**
     * Check Fuel levels in the furnace
     */
    private boolean checkFurnaceFuel()
    {
        final List<ItemStack> possibleFuels = getActivePossibleFuels();
        final FurnaceUserModule module = building.getModule(FURNACE);
        if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), isCorrectFuel(possibleFuels))
                && InventoryUtils.hasBuildingEnoughElseCount(building, isCorrectFuel(possibleFuels), 1) < 1
                && !building.hasWorkerOpenRequestsOfType(worker.getCitizenData().getId(), TypeToken.of(StackList.class)))
        {
            worker.getCitizenData().createRequestAsync(new StackList(possibleFuels, RequestSystemTranslationConstants.REQUESTS_TYPE_BURNABLE, STACKSIZE * (1 + module.getFurnaces().size() / building.getAllAssignedCitizen().size()), 1));
        }

        return getState() instanceof AIWorkerState state && state == IDLE;
    }

    /**
     * Actually accelerate the furnaces
     */
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
                    extrasFurnace.pathFindEdition$addLitTime((int)Math.ceil(Math.sqrt(worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getSecondarySkill())) * 1.71));
                    if (!(furnace.getItem(SMELTABLE_SLOT).isEmpty()))
                    {
                        int addProgress = (int) (worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getPrimarySkill()) * 1.8);
                        while (addProgress > 0 && !furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                            addProgress = extrasFurnace.pathFindEdition$addProgress(addProgress);
                            AbstractFurnaceBlockEntity.serverTick(world, pos, world.getBlockState(pos), furnace);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected int getExtendedCount(final ItemStack stack)
    {
        if (currentRecipeStorage != null && currentRecipeStorage.getIntermediate() == Blocks.FURNACE)
        {
            int count = 0;
            for (final BlockPos pos : building.getModule(FURNACE).getFurnaces())
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

    private static int getToTransfer(int maxFurnaces, int burningFurnaces, int availableSmeltable) {
        int toTransfer = 0;
        int availableFurnaces = maxFurnaces - burningFurnaces;
        if (availableFurnaces >= 1)
        {
            if (availableSmeltable > STACKSIZE * availableFurnaces)
            {
                toTransfer = STACKSIZE;
            }
            else
            {
                if(availableSmeltable <= 8 * (availableFurnaces - 1)){
                    availableFurnaces = (availableSmeltable - 1) / 8 + 1;
                }
                toTransfer = (availableSmeltable / availableFurnaces);
                if(availableSmeltable % availableFurnaces > 0){
                    toTransfer ++;
                }
            }
        }
        return toTransfer;
    }

    /**
     * Directly return to target state if citizens are on duty.
     */
    @Override
    public IAIState getStateAfterPickUp()
    {
        AIWorkerState returnState = withSpecialReturn;
        withSpecialReturn = START_WORKING;
        return returnState;
    }
    // --------------------- Utility Methods --------------------- //

    /**
     * Get a furnace with fuel but without smeltable.
     * @return the block pos of it.
     */
    private BlockPos getEmptyFurnaceWithFuel()
    {
        final FurnaceUserModule module = building.getModule(FURNACE);
        for (final BlockPos pos : module.getFurnaces())
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof FurnaceBlockEntity furnaceBlockEntity)
            {
                if (isEmpty(furnaceBlockEntity.getItem(SMELTABLE_SLOT)) && (isFurnaceNotOccupied(furnaceBlockEntity) || isFurnaceCanReoccupied(furnaceBlockEntity)))
                {
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Check for furnaces with missing fuel.
     * @return the pos of the first without fuel.
     */
    private BlockPos getFurnaceWithoutFuel()
    {
        for (final BlockPos pos : building.getModule(FURNACE).getFurnaces())
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof FurnaceBlockEntity furnace && furnace.getItem(FUEL_SLOT).isEmpty())
            {
                if(isFurnaceCorrectFueler(furnace, worker.getCivilianID()) || isFurnaceCanReoccupied(furnace)) {
                    return pos;
                }
            }
        }
        return null;
    }


    /**
     * Get the furnace which has finished smeltables. For this check each furnace which has been registered to the building. Check if the furnace is turned off and has something in
     * the result slot or check if the furnace has more than x results.
     *
     * @return the position of the furnace.
     */
    private BlockPos getFurnaceToRetrieveOutputFrom()
    {
        for (final BlockPos pos : building.getModule(FURNACE).getFurnaces())
        {
            final BlockEntity entity = world.getBlockEntity(pos);
            if (entity instanceof final FurnaceBlockEntity furnace)
            {
                int countInResultSlot = 0;
                boolean fullfill = false;
                if (!isEmpty(furnace.getItem(RESULT_SLOT)))
                {
                    countInResultSlot = furnace.getItem(RESULT_SLOT).getCount();
                    fullfill = countInResultSlot >= furnace.getItem(RESULT_SLOT).getMaxStackSize();
                }

                if (fullfill || (countInResultSlot > 0 && (furnace.getItem(SMELTABLE_SLOT).isEmpty() || !furnace.getBlockState().getValue(BlockStateProperties.LIT))))
                {
                    if(isFurnaceCorrectPicker(furnace, worker.getCivilianID())){
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Very simple action, straightly extract from a furnace slot.
     *
     * @param furnace the furnace to retrieve from.
     */
    private boolean extractFromFurnaceSlot(final FurnaceBlockEntity furnace, final int slot)
    {
        ItemStack stack = furnace.getItem(slot);
        final Component name = stack.getHoverName();
        final int count = stack.getCount();
        if (stack.isEmpty())
        {
            return false;
        }
        final boolean success;
        if(isFurnaceCorrectPicker(furnace, worker.getCivilianID()) || slot != RESULT_SLOT){
            success = InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    worker.getInventoryCitizen());
        }
        else {
            if(!InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                    new InvWrapper(furnace), slot,
                    worker.getCitizenColonyHandler().getColony().getCitizen(((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnacePicker()).getInventory())
            ){
                success = InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                        new InvWrapper(furnace), slot,
                        worker.getInventoryCitizen());
            }
            else{
                success = true;
            }
            if(success){
                ICitizen mayCitizen = worker.getCitizenColonyHandler().getColony().getCitizen(((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnacePicker());
                if(mayCitizen instanceof EntityCitizen citizen && citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobCrafter<?,?> jobCrafter) {
                    jobCrafter.setCraftCounter(jobCrafter.getCraftCounter() + count);
                }
            }
        }
        if (slot == RESULT_SLOT && success)
        {
            recordSmeltingBuildingStats(name, count);
            furnace.getRecipesToAwardAndPopExperience((ServerLevel) Objects.requireNonNull(furnace.getLevel()),  Vec3.atCenterOf(furnace.getBlockPos()));
            if(furnace.getItem(SMELTABLE_SLOT).isEmpty()){
                resetFurnaceOccupy(furnace);
            }
            worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        }
        return success;
    }

    /**
     * Check to see how many furnaces are still processing
     *
     * @return the count.
     */
    private int countOfBurningFurnaces()
    {
        int count = 0;
        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getModule(FURNACE).getFurnaces())
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
     * Max number of furnaces the worker can use in parallel.
     * @return the number.
     */
    private int getMaxUsableFurnaces()
    {
        final int maxSkillFurnaces = (worker.getCitizenData().getCitizenSkillHandler().getLevel(getModuleForJob().getPrimarySkill()) / 10) + 1;

        int count = 0;
        for (final BlockPos pos : building.getModule(FURNACE).getFurnaces())
        {
            if (WorldUtil.isBlockLoaded(world, pos)) {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof FurnaceBlockEntity furnace && !furnace.getItem(FUEL_SLOT).isEmpty() && (isFurnaceNotOccupied(furnace) || isFurnaceCanReoccupied(furnace))) {
                    count += 1;
                }
            }
        }

        return Math.min(maxSkillFurnaces, count);
    }

    /**
     * Get a copy of the list of allowed fuel.
     *
     * @return the list.
     */
    private List<ItemStack> getAllowedFuel()
    {
        final List<ItemStack> list = new ArrayList<>();
        for (final ItemStorage storage : building.getModule(BuildingModules.ITEMLIST_FUEL).getList())
        {
            final ItemStack stack = storage.getItemStack().copy();
            stack.setCount(stack.getMaxStackSize());
            list.add(stack);
        }
        return list;
    }

    /**
     * Get the list of possible fuels, adjusted for any inputs/outputs of the current recipe to avoid interference
     */
    private List<ItemStack> getActivePossibleFuels()
    {
        final List<ItemStack> possibleFuels = getAllowedFuel();
        if (possibleFuels.isEmpty())
        {
            if (worker.getCitizenData() != null)
            {
                worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(FURNACE_USER_NO_FUEL), ChatPriority.IMPORTANT));
            }
            return ImmutableList.of();
        }
        return possibleFuels;
    }

    /**
     * Predicate for checking fuel in inventories
     */
    private static Predicate<ItemStack> isCorrectFuel(final List<ItemStack> possibleFuels)
    {
        return item -> ItemStackUtils.compareItemStackListIgnoreStackSize(possibleFuels, item);
    }

    /**
     * Returns the name of the smelting stat that is used in the building's statistics.
     * Override this in your subclass to change the description of the smelting stat.
     * @return the name of the smelting stat.
     */
    protected String getSmeltingStatName()
    {
        return ITEMS_SMELTED_DETAIL;
    }

    /**
     * Records the smelting request in the building's statistics.
     * Override this in your subclass to change the description of the smelting stat.
     *
     * @param hoverName the stack name.
     * @param count the stack count.
     */
    protected void recordSmeltingBuildingStats(final Component hoverName, final int count)
    {
        StatsUtil.trackStatByName(building, getSmeltingStatName(), hoverName, count);
    }

    // --------------------- Furnace Occupier --------------------- //

    /**
     * @param furnace the target furnace entity.
     * @return If the furnace were not occupied by any worker or were occupied by exactly the worker checking.
     */
    private boolean isFurnaceNotOccupied(FurnaceBlockEntity furnace){
        int occupier = ((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnaceWorker();
        return occupier < 0 || occupier == worker.getCivilianID();
    }

    /**
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the checker or target occupier.
     * @return If the furnace occupied exactly by this worker.
     */
    private boolean isFurnaceOccupiedBy(FurnaceBlockEntity furnace, int civilianID){
        return ((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnaceWorker() == civilianID;
    }

    /**
     * Reset the worker and picker to empty.
     *
     * @param furnace the target furnace entity.
     */
    private void resetFurnaceOccupy(FurnaceBlockEntity furnace){
        FurnaceBlockEntityExtras furnaceExtra = (FurnaceBlockEntityExtras) furnace;
        furnaceExtra.pathFindEdition$setFurnaceWorker(-1);
        furnaceExtra.pathFindEdition$setFurnacePicker(-1);
    }

    /**
     * @param furnace the target furnace entity.
     * @return If the furnace can be reoccupied only by its protect time.
     */
    private boolean isFurnaceCanReoccupied(FurnaceBlockEntity furnace){
        return !(((FurnaceBlockEntityExtras) furnace).pathFindEdition$atProtectTime());
    }

    /**
     * Set the occupier of the furnace to civilian by ID.
     *
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the occupier.
     */
    private void setFurnaceOccupy(FurnaceBlockEntity furnace, int civilianID){
        ((FurnaceBlockEntityExtras)furnace).pathFindEdition$setFurnaceWorker(civilianID);
    }

    /**
     * Set the picker of the furnace to civilian by ID.
     *
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the picker.
     */
    private void setFurnacePicker(FurnaceBlockEntity furnace, int civilianID){
        ((FurnaceBlockEntityExtras)furnace).pathFindEdition$setFurnacePicker(civilianID);
    }

    /**
     * Set the fueler of the furnace to civilian by ID.
     *
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the fueler.
     */
    private void setFurnaceFueler(FurnaceBlockEntity furnace, int civilianID){
        ((FurnaceBlockEntityExtras)furnace).pathFindEdition$setFurnaceFueler(civilianID);
    }

    /**
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the checking picker.
     * @return If furnace were not occupied by any picker or target citizen were exactly the picker .
     */
    private boolean isFurnaceCorrectPicker(FurnaceBlockEntity furnace, int civilianID){
        int picker = ((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnacePicker();
        return picker == civilianID || picker < 0;
    }

    /**
     * @param furnace the target furnace entity.
     * @param civilianID the civilian ID of the checking fueler.
     * @return If furnace were not occupied by any fueler or target citizen were exactly the fueler .
     */
    private boolean isFurnaceCorrectFueler(FurnaceBlockEntity furnace, int civilianID){
        int fueler = ((FurnaceBlockEntityExtras)furnace).pathFindEdition$getFurnaceFueler();
        return fueler == civilianID || fueler < 0;
    }
}
