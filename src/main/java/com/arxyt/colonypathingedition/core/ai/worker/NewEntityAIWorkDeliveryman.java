package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.JobDeliveryExtra;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.interactionhandling.PosBasedInteraction;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.colony.requestsystem.requests.StandardRequests;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.StatisticsConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.*;

public class NewEntityAIWorkDeliveryman extends AbstractEntityAIInteract<JobDeliveryman, BuildingDeliveryman> {
    /**
     * Min distance the worker should have to the warehouse to make any decisions.
     */
    private static final int MIN_DISTANCE_TO_WAREHOUSE = 5;

    /**
     * Wait 5 seconds for the worker to decide what to do.
     */
    private static final int DECISION_DELAY = TICKS_SECOND * 5;

    /**
     * Wait a few ticks for the worker to decide what to pick up.
     */
    private static final int PICKUP_DELAY = 5;

    /**
     * The inventory's slot which is held in hand.
     */
    private static final int SLOT_HAND = 0;

    /**
     * Completing a request with a priority of at least PRIORITY_FORCING_DUMP will force a dump.
     */
    private static final int PRIORITY_FORCING_DUMP = 10;

    /**
     * Delivery icon
     */
    private final static VisibleCitizenStatus DELIVERING =
            new VisibleCitizenStatus(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/work/delivery.png"), "com.minecolonies.gui.visiblestatus.delivery");

    /**
     * Render meta backpack.
     */
    public static final String RENDER_META_BACKPACK = "backpack";

    /**
     * Amount of stacks left to gather from the inventory at the gathering step.
     */
    private int currentSlot = 0;

    /**
     * Amount of stacks the worker already kept in the current gathering process.
     */
    private List<ItemStorage> alreadyKept = new ArrayList<>();

    /**
     * Amount of stacks the worker already kept in the delivery preparing process.
     */
    final private Map<IToken<?>, ItemStack> alreadyInInv = new HashMap<>();

    /**
     * Initialize the deliveryman and add all his tasks.
     *
     * @param deliveryman the job he has.
     */
    public NewEntityAIWorkDeliveryman(@NotNull final JobDeliveryman deliveryman)
    {
        super(deliveryman);
        super.registerTargets(
                /*
                 * Check if tasks should be executed.
                 */
                new AITarget(IDLE, () -> START_WORKING, 1),
                new AITarget(START_WORKING, this::checkIfExecute, this::decide, DECISION_DELAY),
                new AITarget(PREPARE_DELIVERY, this::prepareDelivery, STANDARD_DELAY),
                new AITarget(DELIVERY, this::deliver, STANDARD_DELAY),
                new AITarget(PICKUP, this::pickup, PICKUP_DELAY),
                new AITarget(DUMPING, this::dump, TICKS_SECOND)

        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata(worker.getInventoryCitizen().isEmpty() ? "" : RENDER_META_BACKPACK);
    }

    @Override
    public Class<BuildingDeliveryman> getExpectedBuildingClass()
    {
        return BuildingDeliveryman.class;
    }

    /**
     * Pickup items from a hut that has requested a pickup.
     *
     * @return the next state to go to.
     */
    private IAIState pickup()
    {
        setDelay(WALK_DELAY);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof StandardRequests.PickupRequest))
        {
            // The current task has changed since the Decision-state. Restart.
            return START_WORKING;
        }

        if (cannotHoldMoreItems())
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final BlockPos pickupTarget = currentTask.getRequester().getLocation().getInDimensionLocation();
        final IBuilding pickupBuilding = building.getColony().getServerBuildingManager().getBuilding(pickupTarget);
        if (pickupBuilding == null)
        {
            job.finishRequest(false);
            return START_WORKING;
        }

        if (!walkToBuilding(pickupBuilding))
        {
            return PICKUP;
        }

        if (pickupFromBuilding(pickupBuilding))
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            job.finishRequest(true);
            StatsUtil.trackStatByName(this.building, PICKUPS_MADE, pickupBuilding.getBuildingDisplayName(), 1);

            worker.decreaseSaturationForContinuousAction();
            worker.getCitizenExperienceHandler().addExperience(0.05D);

            if (currentTask.getRequest().getPriority() >= PRIORITY_FORCING_DUMP)
            {
                return DUMPING;
            }
            else
            {
                return START_WORKING;
            }
        }
        else if (InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        setDelay(5);
        currentSlot++;
        return PICKUP;
    }

    /**
     * Gather not needed Items from building.
     *
     * @param targetBuilding building to gather it from.
     * @return true when finished.
     */
    private boolean pickupFromBuilding(@NotNull final IBuilding targetBuilding)
    {
        if (cannotHoldMoreItems() || InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            return false;
        }

        final IItemHandler handler = targetBuilding.getItemHandlerCap();
        if (handler == null)
        {
            return false;
        }

        if (currentSlot >= handler.getSlots())
        {
            return true;
        }

        ItemStack stack = handler.getStackInSlot(currentSlot);

        while (stack.isEmpty())
        {
            currentSlot++;
            if (currentSlot >= handler.getSlots())
            {
                return true;
            }
            stack = handler.getStackInSlot(currentSlot);
        }

        final int amount = workerRequiresItem(targetBuilding, stack, alreadyKept);
        if (amount <= 0)
        {
            return false;
        }

        if (ItemStackUtils.isEmpty(handler.getStackInSlot(currentSlot)))
        {
            return false;
        }

        final ItemStack activeStack = handler.extractItem(currentSlot, amount, false);
        InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(activeStack, worker.getInventoryCitizen());
        targetBuilding.markDirty();
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);

        return false;
    }

    /**
     * Check if the worker can hold that much items. It depends on his building level. Level 1: 1 stack Level 2: 2 stacks, 4 stacks, 8, unlimited. That's 2^buildingLevel-1.
     *
     * @return whether this deliveryman can hold more items
     */
    private boolean cannotHoldMoreItems()
    {
        if (building.getBuildingLevel() >= building.getMaxBuildingLevel())
        {
            return false;
        }
        return InventoryUtils.getAmountOfStacksInItemHandler(worker.getInventoryCitizen()) > worker.getInventoryCitizen().getSlots() * (0.1 + 0.18 * building.getBuildingLevel());
    }

    /**
     * Check if worker of a certain building requires the item now. Or the builder for the current task.
     *
     * @param building         the building to check for.
     * @param stack            the stack to stack with.
     * @param localAlreadyKept already kept resources.
     * @return the amount which can get dumped.
     */
    public static int workerRequiresItem(final IBuilding building, final ItemStack stack, final List<ItemStorage> localAlreadyKept)
    {
        return building.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, false);
    }

    /**
     * Dump the inventory into the warehouse.
     *
     * @return the next state to go to.
     */
    private IAIState dump()
    {
        final @Nullable IWareHouse warehouse = getAndCheckWareHouse();
        if (warehouse == null)
        {
            return START_WORKING;
        }

        if (!walkToBuilding(warehouse))
        {
            setDelay(WALK_DELAY);
            return DUMPING;
        }

        warehouse.getTileEntity().dumpInventoryIntoWareHouse(worker.getInventoryCitizen());
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);

        return START_WORKING;
    }

    /**
     * Gets the colony's warehouse for the Deliveryman.
     *
     * @return the warehouse. null if no warehouse registered.
     */
    @Nullable
    private IWareHouse getAndCheckWareHouse()
    {
        return job.findWareHouse();
    }

    /**
     * Deliver the items to the hut.
     *
     * @return the next state.
     */
    private IAIState deliver()
    {
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof StandardRequests.DeliveryRequest))
        {
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final ILocation targetBuildingLocation = ((Delivery) currentTask.getRequest()).getTarget();
        if (!targetBuildingLocation.isReachableFromLocation(worker.getLocation()))
        {
            return START_WORKING;
        }

        final IBuilding targetBuilding = worker.getCitizenColonyHandler().getColony().getServerBuildingManager().getBuilding(targetBuildingLocation.getInDimensionLocation());
        if (targetBuilding == null)
        {
            job.finishRequest(true);
            return START_WORKING;
        }

        if (!walkToBuilding(targetBuilding))
        {
            setDelay(WALK_DELAY);
            return DELIVERY;
        }

        boolean success = true;
        boolean extracted = false;

        final IItemHandler workerInventory = worker.getInventoryCitizen();

        final Map<ItemStorage, Integer> remainingRequests = new HashMap<>();

        for (IRequest<? extends Delivery> req :
                job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask))
        {
            final ItemStack reqStack = req.getRequest().getStack();
            final ItemStorage key = new ItemStorage(reqStack);
            remainingRequests.merge(key, reqStack.getCount(), Integer::sum);
        }

        for (int i = 0; i < workerInventory.getSlots(); i++)
        {
            final ItemStack slotStack = workerInventory.getStackInSlot(i);

            if (slotStack.isEmpty())
            {
                continue;
            }

            final ItemStorage key = new ItemStorage(slotStack);

            if (!remainingRequests.containsKey(key))
            {
                continue;
            }

            int remaining = remainingRequests.get(key);
            if (remaining <= 0)
            {
                continue;
            }

            /* get amounts a building requested */
            final int extractAmount = Math.min(slotStack.getCount(), remaining);
            final ItemStack stack = workerInventory.extractItem(i, extractAmount, false);

            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }

            extracted = true;
            final int count = stack.getCount();

            final ItemStack insertionResultStack;

            if (targetBuilding instanceof AbstractBuilding)
            {
                insertionResultStack = InventoryUtils.forceItemStackToItemHandler(
                        targetBuilding.getItemHandlerCap(), stack, ((IBuilding) targetBuilding)::isItemStackInRequest);
            }
            else
            {
                insertionResultStack =
                        InventoryUtils.forceItemStackToItemHandler(targetBuilding.getItemHandlerCap(),
                                stack,
                                itemStack -> true);
            }

            int delivered = count;

            if (!ItemStackUtils.isEmpty(insertionResultStack))
            {
                delivered = count - insertionResultStack.getCount();

                if (ItemStack.matches(insertionResultStack, stack) && worker.getCitizenData() != null)
                {
                    success = false;

                    if (targetBuilding.hasModule(WorkerBuildingModule.class))
                    {
                        worker.getCitizenData().triggerInteraction(
                                new PosBasedInteraction(
                                        Component.translatable(
                                                COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NAMEDCHESTFULL,
                                                targetBuilding.getFirstModuleOccurance(WorkerBuildingModule.class)
                                                        .getFirstCitizen()
                                                        .getName()
                                        ),
                                        ChatPriority.IMPORTANT,
                                        Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NAMEDCHESTFULL),
                                        targetBuilding.getID()
                                )
                        );
                    }
                    else
                    {
                        worker.getCitizenData().triggerInteraction(
                                new PosBasedInteraction(
                                        Component.translatable(
                                                COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL,
                                                Component.literal(" :" + targetBuilding.getSchematicName())
                                        ),
                                        ChatPriority.IMPORTANT,
                                        Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL),
                                        targetBuildingLocation.getInDimensionLocation()
                                )
                        );
                    }
                }

                workerInventory.insertItem(i, insertionResultStack, false);
            }

            /* refresh remaining requests */
            remainingRequests.put(key, remaining - delivered);

            worker.getCitizenColonyHandler()
                    .getColonyOrRegister()
                    .getStatisticsManager()
                    .incrementBy(
                            ITEMS_DELIVERED,
                            delivered,
                            worker.getCitizenColonyHandler().getColonyOrRegister().getDay()
                    );

            StatsUtil.trackStatByName(building, DELIVERIES_MADE, targetBuilding.getBuildingDisplayName(), 1);
        }

        if (!extracted)
        {
            ((JobDeliveryExtra)job).setOngoingDeliveries(0);
            worker.decreaseSaturationForContinuousAction();
            CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);
            job.finishRequest(false);
            return START_WORKING;
        }

        ((JobDeliveryExtra)job).setOngoingDeliveries(job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask).size());
        worker.getCitizenExperienceHandler().addExperience(1.5D);
        worker.decreaseSaturationForContinuousAction();
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);
        job.finishRequest(true);

        if(!((JobDeliveryExtra)job).checkDeliveryFinished()) {
            return DELIVERY;
        }
        return success ? START_WORKING : DUMPING;
    }

    /**
     * Prepare deliveryman for delivery. Check if the building still needs the item and if the required items are still in the warehouse.
     *
     * @return the next state to go to.
     */
    private IAIState prepareDelivery()
    {
        final IRequest<? extends IRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof StandardRequests.DeliveryRequest))
        {
            // The current task has changed since the Decision-state.
            // Restart.
            return START_WORKING;
        }

        final List<IRequest<? extends Delivery>> taskList = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask)
                .stream().filter(iRequest -> !alreadyInInv.containsKey(iRequest.getId())).toList();
        IRequest<? extends Delivery> nextPickUp = null;

        int maxParallelCount = (2 * building.getBuildingLevel() + (int)Math.sqrt(getSecondarySkillLevel() * 9));
        for (final IRequest<? extends Delivery> task : taskList)
        {
            int totalCount = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                    itemStack -> ItemStackUtils.compareItemStacksIgnoreStackSize(task.getRequest().getStack(), itemStack));
            int hasCount = 0;
            for (final ItemStack stack : alreadyInInv.values())
            {
                if (ItemStackUtils.compareItemStacksIgnoreStackSize(stack, task.getRequest().getStack()))
                {
                    hasCount += stack.getCount();
                }
            }

            if (totalCount < hasCount + task.getRequest().getStack().getCount())
            {
                nextPickUp = task;
                break;
            }
            else
            {
                alreadyInInv.put(task.getId(), task.getRequest().getStack());
            }
        }

        if (alreadyInInv.size() > maxParallelCount || cannotHoldMoreItems() || getInventory().isFull())
        {
            return DELIVERY;
        }

        if (nextPickUp == null) {
            if(((JobDeliveryExtra)job).pickMoreDeliveryTask() == null) {
                return DELIVERY;
            }
            return PREPARE_DELIVERY;
        }

        final ILocation location = nextPickUp.getRequest().getStart();

        if (!location.isReachableFromLocation(worker.getLocation()))
        {
            ((JobDeliveryExtra)job).setOngoingDeliveries(0);
            job.finishRequest(false);
            return START_WORKING;
        }

        if (!walkToSafePos(location.getInDimensionLocation()))
        {
            return PREPARE_DELIVERY;
        }

        final BlockEntity tileEntity = world.getBlockEntity(location.getInDimensionLocation());
        job.addConcurrentDelivery(nextPickUp.getId());
        if (gatherIfInTileEntity(tileEntity, nextPickUp.getRequest().getStack()))
        {
            return PREPARE_DELIVERY;
        }

        if (alreadyInInv.size() > 1)
        {
            job.removeConcurrentDelivery(nextPickUp.getId());
            return DELIVERY;
        }

        ((JobDeliveryExtra)job).setOngoingDeliveries(0);
        job.finishRequest(false);
        job.removeConcurrentDelivery(nextPickUp.getId());
        return START_WORKING;
    }

    /**
     * Finds the first @see ItemStack the type of {@code is}. It will be taken from the chest and placed in the worker inventory. Make sure that the worker stands next the chest to
     * not break immersion. Also make sure to have inventory space for the stack.
     *
     * @param entity the tileEntity chest or building or rack.
     * @param is     the itemStack.
     * @return true if found the stack.
     */
    public boolean gatherIfInTileEntity(final BlockEntity entity, final ItemStack is)
    {
        if (ItemStackUtils.isEmpty(is))
        {
            return false;
        }

        IItemHandler handler = null;
        if (entity instanceof final TileEntityColonyBuilding hut && InventoryUtils.hasBuildingEnoughElseCount(hut.getBuilding(), new ItemStorage(is), is.getCount()) >= is.getCount())
        {
            handler = hut.getItemHandlerCap();
        }
        else if (entity instanceof final TileEntityRack rack && rack.getCount(new ItemStorage(is)) >= is.getCount())
        {
            handler = rack.getItemHandlerCap();
        }

        if (handler != null)
        {
            return InventoryUtils.transferItemStackIntoNextFreeSlotFromItemHandler(handler,
                    stack -> !ItemStackUtils.isEmpty(stack) && ItemStackUtils.compareItemStacksIgnoreStackSize(is, stack, true, true),
                    is.getCount(),
                    worker.getInventoryCitizen());
        }

        return false;
    }

    /**
     * Check the wareHouse for the next task.
     *
     * @return the next AiState to go to.
     */
    private IAIState decide()
    {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();
        if (currentTask == null)
        {
            // If there are no deliveries/pickups pending, just loiter around the warehouse.
            if (!walkToBuilding(getAndCheckWareHouse()))
            {
                setDelay(WALK_DELAY);
                return START_WORKING;
            }
            else
            {
                if (!worker.getInventoryCitizen().isEmpty())
                {
                    return DUMPING;
                }
                else
                {
                    return START_WORKING;
                }
            }
        }
        if (currentTask instanceof StandardRequests.DeliveryRequest)
        {
            // Before a delivery can be made, the inventory first needs to be dumped.
            if (!worker.getInventoryCitizen().isEmpty())
            {
                return DUMPING;
            }
            else
            {
                alreadyInInv.clear();
                return PREPARE_DELIVERY;
            }
        }
        else
        {
            return PICKUP;
        }
    }

    /**
     * Check if the deliveryman code should be executed. More concretely if he has a warehouse to work at.
     *
     * @return false if should continue as planned.
     */
    private boolean checkIfExecute()
    {
        final IWareHouse wareHouse = getAndCheckWareHouse();
        if (wareHouse != null)
        {
            worker.getCitizenData().setWorking(true);
            if (wareHouse.getTileEntity() == null)
            {
                return false;
            }
            {
                return true;
            }
        }

        worker.getCitizenData().setWorking(false);
        if (worker.getCitizenData() != null)
        {
            worker.getCitizenData()
                    .triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NOWAREHOUSE),
                            ChatPriority.BLOCKING));
        }
        return false;
    }
}
