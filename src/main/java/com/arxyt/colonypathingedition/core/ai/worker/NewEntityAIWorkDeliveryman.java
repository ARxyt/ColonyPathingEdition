package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.core.job.NewJobDeliveryman;
import com.arxyt.colonypathingedition.core.manager.LinkageManager;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
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
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.interactionhandling.PosBasedInteraction;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import steve_gall.minecolonies_compatibility.api.common.building.module.INetworkStorageView;
import steve_gall.minecolonies_compatibility.core.common.building.module.NetworkStorageModule;
import steve_gall.minecolonies_compatibility.core.common.init.ModBuildingModules;

import java.util.*;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.*;
import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.WALKING_DELAY;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.*;

public class NewEntityAIWorkDeliveryman extends AbstractEntityAIInteract<NewJobDeliveryman, BuildingDeliveryman> {

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
     * Which WareHouse Delivery should dump to.
     */
    private int wareHouseIndex = -1;

    /**
     * Dump times.
     */
    private int dumpTimes = 0;

    /**
     * Waiting timer.
     */
    private int waitingTimer = 0;

    /**
     * Pathing Min Dist.
     */
    private double minDistToDestination = Double.MAX_VALUE;

    /**
     * Pathing Min Dist.
     */
    private int notDecentTimer = 0;

    /**
     * Initialize the deliveryman and add all his tasks.
     *
     * @param deliveryman the job he has.
     */
    public NewEntityAIWorkDeliveryman(@NotNull final NewJobDeliveryman deliveryman)
    {
        super(deliveryman);
        super.registerTargets(
                /*
                 * Check if tasks should be executed.
                 */
                new AITarget<IAIState>(IDLE, () -> START_WORKING, 1),
                new AITarget<>(START_WORKING, this::checkIfExecute, this::decide, 1),
                new AITarget<IAIState>(PREPARE_DELIVERY, this::prepareDelivery, 1),
                new AITarget<IAIState>(DELIVERY, this::deliver, 1),
                new AITarget<IAIState>(PICKUP, this::pickup, WAITING_DELAY),
                new AITarget<IAIState>(DUMPING, this::dump, WALK_DELAY)

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
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getTaskToDeliver();

        if (!(currentTask instanceof StandardRequests.PickupRequest))
        {
            setDelay(STUCK_DELAY);
            // The current task has changed since the Decision-state. Restart.
            return START_WORKING;
        }

        if (cannotHoldMoreItems())
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            setDelay(STUCK_DELAY);
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final BlockPos pickupTarget = currentTask.getRequester().getLocation().getInDimensionLocation();
        final IBuilding pickupBuilding = building.getColony().getServerBuildingManager().getBuilding(pickupTarget);
        if (pickupBuilding == null)
        {
            job.setOngoingDeliveries(0);
            job.finishRequest(false);
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        boolean walkSuccess = walkToBuilding(pickupBuilding);
        if (!walkSuccess)
        {
            double alterDistToDestination = DistanceUtils.dist2(pickupBuilding.getPosition(), worker.blockPosition());
            if(minDistToDestination >= alterDistToDestination) {
                minDistToDestination = alterDistToDestination;
                notDecentTimer = 0;
            }
            else{
                if(notDecentTimer < minDistToDestination * 20) {
                    notDecentTimer += WALKING_DELAY;
                }
                else{
                    walkSuccess = true;
                }
            }
            if(!walkSuccess) {
                setDelay(WALKING_DELAY);
                return PICKUP;
            }
        }
        minDistToDestination = Double.MAX_VALUE;
        notDecentTimer = 0;

        if (pickupFromBuilding(pickupBuilding))
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            job.setOngoingDeliveries(0);
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
                setDelay(WAITING_DELAY);
                return START_WORKING;
            }
        }
        else if (InventoryUtils.openSlotCount(worker.getInventoryCitizen()) <= 0)
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        setDelay(3);
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
        return InventoryUtils.getAmountOfStacksInItemHandler(worker.getInventoryCitizen()) >= maxHoldItemsInInv();
    }

    private double maxHoldItemsInInv()
    {
        return worker.getInventoryCitizen().getSlots() * (0.1 + 0.18 * building.getBuildingLevel());
    }

    private int maxParallelTask()
    {
        return (2 * building.getBuildingLevel() + (int)Math.sqrt(getSecondarySkillLevel() * 9)) + 1;
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
        final @Nullable IWareHouse warehouse = getDumpWareHouse();
        if (warehouse == null)
        {
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        if (!walkToBuilding(warehouse))
        {
            return DUMPING;
        }

        // For test, we use the easiest codes.
        // TODO: 检查是否能够实现多仓库倾倒，实现后将index调整移至job并存储。
        warehouse.getTileEntity().dumpInventoryIntoWareHouse(worker.getInventoryCitizen());
        if(!worker.getInventoryCitizen().isEmpty()) {
            wareHouseIndex ++;
            return DUMPING;
        }
        if(wareHouseIndex != -1 && dumpTimes++ >= 10) {
            wareHouseIndex = -1;
        }
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);

        setDelay(STUCK_DELAY);
        return START_WORKING;
    }

    /**
     * Gets the colony's warehouse for the Deliveryman to dump.
     *
     * @return the warehouse. null if no warehouse available.
     */
    @Nullable
    private IWareHouse getDumpWareHouse()
    {
        if(wareHouseIndex < 0) {
            return job.findWareHouse();
        }
        List<IWareHouse> wareHouses = job.findWareHouses();
        if(wareHouseIndex >= wareHouses.size()) {
            return null;
        }
        return wareHouses.get(wareHouseIndex);
    }

    /**
     * Deliver the items to the hut.
     *
     * @return the next state.
     */
    private IAIState deliver()
    {
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getTaskToDeliver();

        if (!(currentTask instanceof StandardRequests.DeliveryRequest))
        {
            setDelay(STUCK_DELAY);
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final ILocation targetBuildingLocation = ((Delivery) currentTask.getRequest()).getTarget();
        if (!targetBuildingLocation.isReachableFromLocation(worker.getLocation()))
        {
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        final IBuilding targetBuilding = worker.getCitizenColonyHandler().getColony().getServerBuildingManager().getBuilding(targetBuildingLocation.getInDimensionLocation());
        if (targetBuilding == null)
        {
            job.removeConcurrentDelivery(currentTask.getId());
            job.setTaskNotFinished(currentTask.getId());
            setDelay(STUCK_DELAY);
            return DELIVERY;
        }

        boolean walkSuccess = walkToBuilding(targetBuilding);
        if (!walkSuccess)
        {
            double alterDistToDestination = DistanceUtils.dist2(targetBuilding.getPosition(), worker.blockPosition());
            if(minDistToDestination >= alterDistToDestination) {
                minDistToDestination = alterDistToDestination;
                notDecentTimer = 0;
            }
            else{
                if(notDecentTimer < minDistToDestination * 20) {
                    notDecentTimer += WALKING_DELAY;
                }
                else{
                    walkSuccess = true;
                }
            }
            if(!walkSuccess) {
                setDelay(WALKING_DELAY);
                return DELIVERY;
            }
        }
        minDistToDestination = Double.MAX_VALUE;
        notDecentTimer = 0;

        boolean success = true;
        boolean extracted = false;

        final IItemHandler workerInventory = worker.getInventoryCitizen();

        final Map<ItemStorage, Integer> remainingRequests = new HashMap<>();

        final List<IRequest<? extends Delivery>> iRequestList = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask);

        for (IRequest<? extends Delivery> req : new ArrayList<>(iRequestList)) {
            ItemStack reqStack = req.getRequest().getStack();
            if (alreadyInInv != null && alreadyInInv.containsKey(req.getId())) {
                alreadyInInv.remove(req.getId());
                remainingRequests.merge(new ItemStorage(reqStack), reqStack.getCount(), Integer::sum);
            } else {
                iRequestList.remove(req);
            }
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
            job.setOngoingDeliveries(iRequestList.size());
            worker.decreaseSaturationForContinuousAction();
            CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);
            job.finishRequest(false);
            return START_WORKING;
        }

        job.setOngoingDeliveries(iRequestList.size());
        worker.getCitizenExperienceHandler().addExperience(1.5D);
        worker.decreaseSaturationForContinuousAction();
        CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, SLOT_HAND);
        job.finishRequest(true);

        boolean isFinished = job.checkDeliveryFinished();
        if(!alreadyInInv.isEmpty()) {
            if(isFinished) {
                setDelay(WAITING_DELAY);
                return START_WORKING;
            }
            else{
                return DELIVERY;
            }
        }

        if(!isFinished) {
            setDelay(STUCK_DELAY);
            return PREPARE_DELIVERY;
        }

        setDelay(WAITING_DELAY);
        return success ? START_WORKING : DUMPING;
    }

    /**
     * Prepare deliveryman for delivery. Check if the building still needs the item and if the required items are still in the warehouse.
     *
     * @return the next state to go to.
     */
    private IAIState prepareDelivery()
    {
        final IRequest<? extends IRequestable> currentTaskToDeliver = job.getTaskToDeliver();
        if (!(currentTaskToDeliver instanceof StandardRequests.DeliveryRequest))
        {
            // Restart owe to state change.
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        final IRequest<? extends IRequestable> currentTaskToCheck = job.getTaskToPickup();
        final IRequest<? extends IRequestable> currentTask = currentTaskToCheck instanceof StandardRequests.DeliveryRequest ? currentTaskToCheck : currentTaskToDeliver;

        final List<IRequest<? extends Delivery>> taskList = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask)
                .stream().filter(iRequest -> !alreadyInInv.containsKey(iRequest.getId())).toList();
        IRequest<? extends Delivery> nextPickUp = null;

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

        final int pickSize = alreadyInInv.size();
        final int maxParallelCount = maxParallelTask();
        final int invFill = InventoryUtils.getAmountOfStacksInItemHandler(worker.getInventoryCitizen());
        final int invMax = (int)Math.ceil(maxHoldItemsInInv());
        if (pickSize >= maxParallelCount || invFill >= invMax)
        {
            return DELIVERY;
        }

        if (nextPickUp == null) {
            if(job.pickMoreDeliveryTask(Math.min(maxParallelCount - pickSize, invMax - invFill)) == null) {
                return DELIVERY;
            }
            return PREPARE_DELIVERY;
        }

        final ILocation location = nextPickUp.getRequest().getStart();

        if (!location.isReachableFromLocation(worker.getLocation()))
        {
            job.removeConcurrentDelivery(nextPickUp.getId());
            job.setTaskNotFinished(nextPickUp.getId());
            setDelay(WAITING_DELAY);
            return PREPARE_DELIVERY;
        }

        boolean walkSuccess = walkToSafePos(location.getInDimensionLocation());
        if (!walkSuccess)
        {
            double alterDistToDestination = DistanceUtils.dist2(location.getInDimensionLocation(), worker.blockPosition());
            if(minDistToDestination >= alterDistToDestination) {
                minDistToDestination = alterDistToDestination;
                notDecentTimer = 0;
            }
            else{
                if(notDecentTimer < minDistToDestination * 20) {
                    notDecentTimer += WALKING_DELAY;
                }
                else{
                    walkSuccess = true;
                }
            }
            if(!walkSuccess) {
                setDelay(WALKING_DELAY);
                return PREPARE_DELIVERY;
            }
        }
        minDistToDestination = Double.MAX_VALUE;
        notDecentTimer = 0;

        final BlockEntity tileEntity = world.getBlockEntity(location.getInDimensionLocation());
        job.addConcurrentDelivery(nextPickUp.getId());
        if (gatherIfInTileEntity(tileEntity, nextPickUp.getRequest().getStack()))
        {
            return PREPARE_DELIVERY;
        }

        //There is a task with item less than require, so we delete the request and continue our preparing.
        job.removeConcurrentDelivery(nextPickUp.getId());
        job.setTaskNotFinished(nextPickUp.getId());
        setDelay(WAITING_DELAY);
        return PREPARE_DELIVERY;
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

        boolean success = false;
        if (handler != null)
        {
            success = InventoryUtils.transferItemStackIntoNextFreeSlotFromItemHandler(handler,
                    stack -> !ItemStackUtils.isEmpty(stack) && ItemStackUtils.compareItemStacksIgnoreStackSize(is, stack, true, true),
                    is.getCount(),
                    worker.getInventoryCitizen());
        }

        if(!success) {
            success = gatherAdditionalTileEntities(entity, is);
        }

        return success;
    }

    /**
     * Add more available tile entities here.
     */
    public boolean gatherAdditionalTileEntities(final BlockEntity entity, final ItemStack is)
    {
        if (LinkageManager.useCompatibilityAddon) {
            if (entity == null) {
                return false;
            }
            IWareHouse warehouse = job.getWareHouseWorkingFor();
            if (warehouse == null) {
                return false;
            }
            NetworkStorageModule module = warehouse.getModule(ModBuildingModules.NETWORK_STORAGE);
            if (module == null || entity.getLevel() == null) {
                return false;
            }
            INetworkStorageView view = NetworkStorageModule.getAllViews(entity.getLevel(), entity.getBlockPos(), (v) -> v.getLinkedModule() != null && NetworkStorageModule.canExtract(v)).findAny().orElse(null);
            if (view == null) {
                return false;
            }

            InventoryCitizen inventory = this.worker.getInventoryCitizen();
            ItemStack extracting = view.extractItem(is, true);
            if (!extracting.isEmpty()) {
                ItemStack remain = ItemHandlerHelper.insertItem(inventory, extracting, true);
                if (remain.isEmpty()) {
                    ItemStack extracted = view.extractItem(is, false).copy();
                    ItemHandlerHelper.insertItem(inventory, extracted, false);
                    return true;
                }
            }
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
        IRequest<? extends IDeliverymanRequestable> currentTask = job.generateAndGetCurrentTask(Math.min(maxParallelTask(),5));
        if (currentTask == null)
        {
            if (!worker.getInventoryCitizen().isEmpty()) return DUMPING;

            // we are now waiting or working for other warehouse, then we should not allow other deliverymen take the tasks of our warehouse.
            job.setWaitingForJob(true);
            if (waitingTimer++ < 10){
                setDelay(WAITING_DELAY);
                return START_WORKING;
            }

            //TODO:这是一个检测其他仓库任务的代码，需要仔细斟酌其开销，理论上不算大，但是需要注意与拿取任务代码的冲突问题。
            currentTask = job.generateAndGetCurrentTaskFormOtherWareHouse(Math.min(maxParallelTask(),5));

            if(currentTask == null) {
                // For we are using too much test on warehouses so we double the delay here.
                if (!walkToBuilding(job.findWareHouse())) {
                    setDelay(WALKING_DELAY * 2);
                } else {
                    setDelay(WAITING_DELAY * 2);
                }
                return START_WORKING;
            }
        }
        else {
            // we got tasks form our own warehouse, so we are not waiting, then we allow other deliverymen take the tasks of our warehouse.
            job.setWaitingForJob(false);
        }

        // Once we got a task, we clear the waiting timer, we prefer tasks of workers' own warehouse.
        waitingTimer = 0;
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
        final IWareHouse wareHouse = job.findWareHouse();
        if (wareHouse != null)
        {
            worker.getCitizenData().setWorking(true);
            return wareHouse.getTileEntity() != null;
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

    @Override
    protected boolean inventoryNeedsDump()
    {
        return false;
    }
}
