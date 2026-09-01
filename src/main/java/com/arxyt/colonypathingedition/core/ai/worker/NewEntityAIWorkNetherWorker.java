package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerCombatAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerMiningAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerPickupAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerPiglinTradeAction;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.RecipeStorageAccessor;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.modules.ICraftingBuildingModule;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.crafting.RecipeStorage;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.entity.ai.workers.util.GuardGearBuilder;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.items.ItemAdventureToken;
import com.minecolonies.core.util.WorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.*;
import static com.arxyt.colonypathingedition.core.costants.states.NewAIWorkerState.NETHER_GATHER_REWARDS;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.*;
import static com.minecolonies.api.util.constant.GuardConstants.*;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_ENTITY_TYPE;
import static com.minecolonies.api.util.constant.StatisticsConstants.*;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.NETHERMINER_MENU;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner.*;
import static com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner.RENDER_META_SHOVEL;

public class NewEntityAIWorkNetherWorker extends AbstractEntityAICrafting<JobNetherWorker, BuildingNetherWorker>
{

    /**
     * Delay for each of the crafting operations.
     */
    private static final int TICK_DELAY = 40;

    /**
     * Virtual slots for equipment, so we can track what is "equipped" without having it visible when the citizen is invisible.
     */
    private final Map<EquipmentSlot, ItemStack> virtualEquipmentSlots = new HashMap<>();

    private boolean extraRound;
    private int timeOutCounter = 0;
    private boolean hasEaten = false;
    private final AdventureActionHandler actionHandler = new AdventureActionHandler();
    private IAIState dumpReturnState = IDLE;

    /**
     * Edibles that the worker will attempt to eat while in the nether (unfiltered)
     */
    final List<ItemStack> netherEdible = IColonyManager.getInstance()
            .getCompatibilityManager()
            .getEdibles(building.getBuildingLevel() - 1)
            .stream()
            .map(ItemStorage::getItemStack)
            .collect(Collectors.toList());

    /**
     * List of items that are required by the guard based on building level and guard level.  This array holds a pointer to the building level and then pointer to GuardGear
     */
    public final List<List<GuardGear>> itemsNeeded = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public NewEntityAIWorkNetherWorker(@NotNull JobNetherWorker job)
    {
        super(job);
        super.registerTargets(
                new AITarget<IAIState>(NETHER_LEAVE, this::leaveForNether, TICK_DELAY),
                new AITarget<IAIState>(NETHER_AWAY, this::stayInNether, 1),
                new AITarget<IAIState>(NETHER_GATHER_REWARDS, this::gatherRewards, 1),
                new AITarget<IAIState>(NETHER_RETURN, this::returnFromNether, TICK_DELAY),
                new AITarget<IAIState>(NETHER_OPENPORTAL, this::openPortal, TICK_DELAY),
                new AITarget<IAIState>(NETHER_CLOSEPORTAL, this::closePortal, TICK_DELAY)
        );
        worker.setCanPickUpLoot(true);

        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_IRON, ARMOR_LEVEL_MAX, LEATHER_BUILDING_LEVEL_RANGE, DIA_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_IRON, ARMOR_LEVEL_DIAMOND + 3, LEATHER_BUILDING_LEVEL_RANGE, DIA_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_IRON, ARMOR_LEVEL_DIAMOND + 1, LEATHER_BUILDING_LEVEL_RANGE, IRON_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_IRON, ARMOR_LEVEL_DIAMOND, LEATHER_BUILDING_LEVEL_RANGE, CHAIN_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_CHAIN, ARMOR_LEVEL_IRON, LEATHER_BUILDING_LEVEL_RANGE, GOLD_BUILDING_LEVEL_RANGE));
    }

    @Override
    public boolean hasWorkToDo()
    {
        if(getState() == DECIDE){
            return super.hasWorkToDo();
        }
        return true;
    }

    private boolean checkEmptyEquipmentAvailable(List<IRequest<?>> requests){
        for (final List<GuardGear> itemList : itemsNeeded) {
            for (final GuardGear item : itemList) {
                // 如果槽位已经有装备，跳过
                if (virtualEquipmentSlots.containsKey(item.getType())
                        && !ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                {
                    continue;
                }

                // 检查请求列表中是否包含该物品
                boolean matched = requests.stream().anyMatch(r ->
                        r.getRequest() instanceof Tool tool && tool.getEquipmentType().getDisplayName().equals(item.getItemNeeded().getDisplayName())
                );

                if (!matched) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkAndRequestArmorWithAvailableCheck(){
        checkAndRequestArmor();
        List<IRequest<?>> requests = ((AbstractEntityAIBasicExtra)this).getRequestCannotBeDone().stream().filter(r ->
                r.getRequester().getLocation().equals(building.getLocation())
        ).toList();
        return checkEmptyEquipmentAvailable(requests);
    }

    @Override
    protected void updateRenderMetaData()
    {
        StringBuilder renderData = new StringBuilder(getState() == CRAFT
                || getState() == NETHER_LEAVE
                || getState() == NETHER_RETURN
                || getState() == NETHER_OPENPORTAL
                || getState() == NETHER_CLOSEPORTAL ? RENDER_META_WORKING : "");

        for (int slot = 0; slot < worker.getInventoryCitizen().getSlots(); slot++)
        {
            final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
            if (stack.getItem() == Items.TORCH && renderData.indexOf(RENDER_META_TORCH) == -1)
            {
                renderData.append(RENDER_META_TORCH);
            }
            else if (stack.canPerformAction(ToolActions.PICKAXE_DIG) && renderData.indexOf(RENDER_META_PICKAXE) == -1)
            {
                renderData.append(RENDER_META_PICKAXE);
            }
            else if (stack.canPerformAction(ToolActions.SHOVEL_DIG) && renderData.indexOf(RENDER_META_SHOVEL) == -1)
            {
                renderData.append(RENDER_META_SHOVEL);
            }
        }

        worker.setRenderMetadata(renderData.toString());
    }

    @Override
    public Class<BuildingNetherWorker> getExpectedBuildingClass()
    {
        return BuildingNetherWorker.class;
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        return START_WORKING;
    }

    @Override
    public boolean canBeInterrupted()
    {
        return !worker.isInvisible();
    }

    @Override
    protected IAIState decide()
    {
        //Check if we are traveling.
        if (!job.getCraftedResults().isEmpty())
        {
            extraRound = ((JobNetherWorkerExtra)job).getExtraRounds();
            worker.setInvisible(true);
            setDelay(WAITING_DELAY);
            return NETHER_AWAY;
        }
        if (!job.getProcessedResults().isEmpty() || job.isInNether()) {
            setDelay(WAITING_DELAY);
            return NETHER_GATHER_REWARDS;
        }

        job.setInNether(false);

        IAIState crafterState = super.decide();

        if (crafterState != IDLE && crafterState != START_WORKING)
        {
            setDelay(WAITING_DELAY);
            return crafterState;
        }

        if (!building.isReadyForTrip())
        {
            worker.getCitizenData().setJobStatus(JobStatus.IDLE);
            setDelay(STUCK_DELAY);
            return IDLE;
        }

        if (!walkToBuilding())
        {
            setDelay(WALKING_DELAY);
            return getState();
        }

        if (!worker.getInventoryCitizen().hasSpace())
        {
            setDelay(WAITING_DELAY);
            return INVENTORY_FULL;
        }

        // Get Armor if available.
        // This is async, but we'll wait extra time for it if it's craftable.
        equipArmor(true);
        boolean isArmorCraftable = checkAndRequestArmorWithAvailableCheck();

        // Get food if available. We just ignore extra time waiting for it as armor is much more complex to craft.
        final IAIState tempState = checkAndRequestFood();
        if (tempState != getState())
        {
            setDelay(WAITING_DELAY);
            return tempState;
        }

        final BlockPos portal = building.getPortalLocation();
        if (portal == null)
        {
            Log.getLogger().warn("--- Missing Portal Tag In Nether Worker Building! Aborting Operation! ---");
            setDelay(STUCK_DELAY);
            return IDLE;
        }

        // Check for materials needed to go to the Nether:
        IRecipeStorage rs = building.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class).getFirstRecipe(ItemStack::isEmpty);
        boolean hasItemsAvailable = true;
        if (rs != null)
        {
            for (ItemStorage item : rs.getInput())
            {
                if (!checkIfRequestForItemExistOrCreateAsync(new ItemStack(item.getItem(), 1), item.getAmount(), item.getAmount()))
                {
                    hasItemsAvailable = false;
                }
            }
        }

        // Optional
        checkForToolOrWeapon(ModEquipmentTypes.axe.get());
        checkForToolOrWeapon(ModEquipmentTypes.shovel.get());
        checkForToolOrWeapon(ModEquipmentTypes.hoe.get());

        // Demand
        boolean missingPick = checkForToolOrWeapon(ModEquipmentTypes.pickaxe.get());
        boolean missingSword = checkForToolOrWeapon(ModEquipmentTypes.sword.get());
        boolean missingLighter = checkForToolOrWeapon(ModEquipmentTypes.flint_and_steel.get());
        if (!hasItemsAvailable || missingPick || missingSword || missingLighter)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            setDelay(STUCK_DELAY);
            return START_WORKING;
        }

        if(!hasEaten && worker.getCitizenData().getSaturation() < FULL_SATURATION){
            if(worker.getCitizenJobHandler().getColonyJob() instanceof JobNetherWorkerExtra jobExtra){
                jobExtra.setShouldEat(true);
                hasEaten = true;
            }
        }

        // We should wait for armor for extra 2 minutes if it's craftable.
        if(isArmorCraftable){
            if(timeOutCounter++ < 6){
                setDelay(STUCK_DELAY * 4);
                return getState();
            }
        }

        if (currentRecipeStorage == null)
        {
            final ICraftingBuildingModule module = building.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class);
            currentRecipeStorage = module.getFirstFulfillableRecipe(ItemStackUtils::isEmpty, 1, false);
            if (building.isReadyForTrip())
            {
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            }

            if (currentRecipeStorage == null && building.shallClosePortalOnReturn())
            {
                final BlockState block = world.getBlockState(portal);
                if (block.is(Blocks.NETHER_PORTAL))
                {
                    return NETHER_CLOSEPORTAL;
                }
            }
            setDelay(STUCK_DELAY);
            return getState();
        }
        else
        {
            IAIState checkResult = checkForItems(currentRecipeStorage);
            if (checkResult == GET_RECIPE)
            {
                currentRecipeStorage = null;
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                setDelay(STUCK_DELAY);
                return IDLE;
            }
            if (checkResult != CRAFT)
            {
                setDelay(WAITING_DELAY);
                return checkResult;
            }
        }

        timeOutCounter = 0;
        hasEaten = false;
        return NETHER_LEAVE;
    }

    /**
     * Leave for the Nether by walking to the portal and going invisible.
     */
    protected IAIState leaveForNether()
    {
        if (!worker.getInventoryCitizen().hasSpace())
        {
            return INVENTORY_FULL;
        }

        if (currentRecipeStorage == null)
        {
            job.setInNether(false);
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            return IDLE;
        }

        final ExpeditionLog expeditionLog = building.getFirstModuleOccurance(ExpeditionLogModule.class).getLog();
        expeditionLog.reset();
        expeditionLog.setStatus(ExpeditionLog.Status.STARTING);
        expeditionLog.setCitizen(worker);

        // Attempt to light the portal and travel
        final BlockPos portal = building.getPortalLocation();
        if (portal != null && currentRecipeStorage != null)
        {
            final BlockState block = world.getBlockState(portal);
            if (block.is(Blocks.NETHER_PORTAL))
            {
                if (!walkToWorkPos(portal))
                {
                    return getState();
                }
                building.recordTrip();
                job.setInNether(true);

                expeditionLog.setStatus(ExpeditionLog.Status.IN_PROGRESS);
                logAllEquipment(expeditionLog);

                List<ItemStack> result = currentRecipeStorage.fullfillRecipeAndCopy(getLootContext(), ImmutableList.of(worker.getItemHandlerCitizen()), false);
                if (result != null)
                {
                    // by default all the adventure tokens are at the end (due to loot tables); space them better
                    result = new ArrayList<>(result);
                    Collections.shuffle(result, worker.getCitizenData().getRandom());
                    job.addCraftedResultsList(result);
                }

                worker.setInvisible(true);
                worker.getCitizenData().setJobStatus(JobStatus.WORKING);
                worker.playSound(SoundEvents.PORTAL_TRIGGER, worker.getRandom().nextFloat() * 0.5F + 0.25F, 0.25F);
                return NETHER_AWAY;
            }
            return NETHER_OPENPORTAL;
        }
        worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        return IDLE;
    }

    /**
     * Stay "in the Nether" and process the queues
     */
    protected IAIState stayInNether()
    {
        final ExpeditionLog expeditionLog = building.getFirstModuleOccurance(ExpeditionLogModule.class).getLog();
        equipArmor(true);

        // Action Loop
        if(actionHandler.canActionTick()) {
            switch (actionHandler.doAction()) {
                case INVALID -> {
                    actionHandler.onActionFinished();
                    job.getCraftedResults().remove(actionHandler.getCurrStack());
                    setDelay(WAITING_DELAY);
                    return getState();
                }
                case FAIL -> {
                    actionHandler.onActionFinished();
                    job.getCraftedResults().clear();
                    job.getProcessedResults().clear();
                    setDelay(STUCK_DELAY);
                    return IDLE;
                }
                case ESCAPE -> {
                    actionHandler.onActionFinished();
                    onTravelFinished(expeditionLog, true);
                    StatsUtil.trackStat(building, "escaped", 1);
                    return NETHER_RETURN;
                }
                case SUCCESS -> {
                    List<ItemStack> rewards = actionHandler.onActionFinished();
                    job.addProcessedResultsList(rewards);
                    expeditionLog.addLoot(rewards);
                    logAllEquipment(expeditionLog);
                    setDelay(actionHandler.actionDelay());
                    job.getCraftedResults().remove(actionHandler.getCurrStack());
                    return getState();
                }
                case IN_PROGRESS -> {
                    logAllEquipment(expeditionLog);
                    setDelay(actionHandler.actionDelay());
                    return getState();
                }
            }
        }

        //This is the adventure loop.
        if (!job.getCraftedResults().isEmpty())
        {
            ItemStack currStack = job.getCraftedResults().peek();
            if(currStack == null) {
                job.getCraftedResults().poll();
                return getState();
            }
            if (currStack.getItem() instanceof ItemAdventureToken && currStack.hasTag())
            {
                CompoundTag tag = currStack.getTag();
                assert tag != null;
                if(tag.contains(TAG_ENTITY_TYPE)) {
                    actionHandler.setAction(new NetherWorkerCombatAction(world, worker, job, tag, extraRound), currStack);
                    return getState();
                }
                if(tag.contains("tradeLoot")) {
                    actionHandler.setAction(new NetherWorkerPiglinTradeAction(world, worker, job, tag), currStack);
                    return getState();
                }
            }
            else if (!currStack.isEmpty())
            {
                if (currStack.getItem() instanceof BlockItem bi)
                {
                    final Block block = bi.getBlock();
                    actionHandler.setAction(new NetherWorkerMiningAction(world, worker, job, currStack, getMostEfficientTool(block.defaultBlockState(), worker.blockPosition())), currStack);
                    // we got a may-not-reliable tool slot, so immediately reset.
                    setDelay(0);
                    return NETHER_AWAY;
                }
                else
                {
                    actionHandler.setAction(new NetherWorkerPickupAction(currStack), currStack);
                    return getState();
                }
            }
            job.getCraftedResults().poll();
            return getState();
        }

        return onTravelFinished(expeditionLog, false);
    }

    private IAIState onTravelFinished(ExpeditionLog expeditionLog, Boolean escaped) {
        job.getCraftedResults().clear();
        if(job.getProcessedResults().isEmpty()) {
            extraRound = ((JobNetherWorkerExtra) job).setExtraRounds(false);
            expeditionLog.setStatus(ExpeditionLog.Status.RETURNING_HOME);
            return NETHER_RETURN;
        }

        if (!escaped && ((JobNetherWorkerExtra) job).canExtraRounds(extraRoundsLimit()) && worker.getHealth() >= worker.getMaxHealth() && InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) >= 10) {
            if (currentRecipeStorage instanceof RecipeStorage recipeStorage) {
                List<ItemStack> result = ((RecipeStorageAccessor)recipeStorage).invokeInsertCraftedItems(ImmutableList.of(worker.getItemHandlerCitizen()), recipeStorage.getPrimaryOutput(), getLootContext(),false);
                if (result != null) {
                    // by default all the adventure tokens are at the end (due to loot tables); space them better
                    result = new ArrayList<>(result);
                    Collections.shuffle(result, worker.getCitizenData().getRandom());
                    job.addCraftedResultsList(result);
                    worker.getCitizenData().setJobStatus(JobStatus.WORKING);
                    extraRound = ((JobNetherWorkerExtra) job).setExtraRounds(true);
                    StatsUtil.trackStat(building, "extraRounds", 1);
                    setDelay(WAITING_DELAY);
                    return getState();
                }
            }
        }

        extraRound = ((JobNetherWorkerExtra) job).setExtraRounds(false);
        expeditionLog.setStatus(ExpeditionLog.Status.RETURNING_HOME);
        return NETHER_GATHER_REWARDS;
    }

    private int extraRoundsLimit() {
        return getSecondarySkillLevel() / 16;
    }

    protected IAIState gatherRewards() {
        final BlockPos portal = building.getPortalLocation();
        worker.setInvisible(false);
        if (!walkToWorkPos(portal))
        {
            return getState();
        }

        if(job.getProcessedResults().isEmpty()) {
            return NETHER_RETURN;
        }
        for (ItemStack item : job.getProcessedResults().stream().toList()) {
            if(!InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(), item)) {
                continue;
            }
            worker.decreaseSaturationForContinuousAction();
            worker.getCitizenExperienceHandler().addExperience(0.2);
            job.getProcessedResults().remove(item);
            StatsUtil.trackStatByName(building, ITEMS_DISCOVERED, item.getHoverName(), item.getCount());
        }

        dumpReturnState = NETHER_GATHER_REWARDS;
        return INVENTORY_FULL;
    }

    @Override
    public IAIState afterDump(){
        IAIState state = super.afterDump();
        if (state == IDLE) {
            state = dumpReturnState;
            dumpReturnState = IDLE;
        }
        return state;
    }

    /**
     * Return from the nether by going visible, walking to building and preparing to close the portal
     */
    protected IAIState returnFromNether()
    {
        //Shutdown Portal
        if (building.shallClosePortalOnReturn() && world.getBlockState(building.getPortalLocation()).is(Blocks.NETHER_PORTAL))
        {
            return NETHER_CLOSEPORTAL;
        }

        if (!walkToBuilding())
        {
            return getState();
        }

        final ExpeditionLog expeditionLog = building.getFirstModuleOccurance(ExpeditionLogModule.class).getLog();
        expeditionLog.setStatus(ExpeditionLog.Status.COMPLETED);

        job.setInNether(false);
        currentRecipeStorage = null;
        StatsUtil.trackStat(building, TRIPS_COMPLETED, 1);
        dumpReturnState = START_WORKING;
        return INVENTORY_FULL;
    }

    /**
     * Open the portal to the nether if it's not open
     */
    protected IAIState openPortal()
    {
        // Attempt to light the portal and travel
        final BlockPos portal = building.getPortalLocation();
        if (portal != null && currentRecipeStorage != null)
        {
            if (!walkToWorkPos(portal))
            {
                return getState();
            }

            final BlockState block = world.getBlockState(portal);
            final Optional<PortalShape> ps = PortalShape.findPortalShape(world, portal, PortalShape::isValid, Direction.Axis.X);

            if (ps.isEmpty())
            {
                // Can't find the portal
                return IDLE;
            }

            if (!block.is(Blocks.NETHER_PORTAL))
            {
                useFlintAndSteel();
                ps.get().createPortalBlocks();
                return NETHER_LEAVE;
            }
        }
        return START_WORKING;
    }

    /**
     * Close the nether portal while idle around the building
     */
    protected IAIState closePortal()
    {
        final BlockPos portal = building.getPortalLocation();
        final BlockState block = world.getBlockState(portal);

        if (block.is(Blocks.NETHER_PORTAL))
        {
            if (!walkToWorkPos(portal))
            {
                return getState();
            }

            useFlintAndSteel();
            world.setBlockAndUpdate(building.getPortalLocation(), Blocks.AIR.defaultBlockState());
        }

        if (job.isInNether())
        {
            return NETHER_RETURN;
        }

        currentRecipeStorage = null;
        return INVENTORY_FULL;
    }

    /**
     * Helper to 'use' the flint and steel on portal open and close
     */
    private void useFlintAndSteel()
    {
        final ItemStack tool = findTool(ModEquipmentTypes.flint_and_steel.get());
        tool.hurtAndBreak(1, worker, entity -> {});
    }

    private ItemStack findItem(@NotNull final Predicate<ItemStack> predicate)
    {
        int slotOfStack = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getItemHandlerCitizen(), predicate);
        return slotOfStack < 0 ? ItemStack.EMPTY : worker.getInventoryCitizen().getStackInSlot(slotOfStack);
    }

    private ItemStack findTool(@NotNull final EquipmentTypeEntry tool)
    {
        return findItem(stack -> ItemStackUtils.hasEquipmentLevel(stack, tool, 0, building.getMaxEquipmentLevel()));
    }

    @Override
    protected int getMostEfficientTool(@NotNull final BlockState target, final BlockPos pos)
    {
        final EquipmentTypeEntry toolType = WorkerUtil.getBestToolForBlock(target, target.getDestroySpeed(world, pos), building, world, pos);
        final int required = WorkerUtil.getCorrectHarvestLevelForBlock(target);

        @NotNull final InventoryCitizen inventory = worker.getInventoryCitizen();
        if (toolType == ModEquipmentTypes.none.get())
        {
            int bestSlot = NO_TOOL;
            int bestLevel = 0;
            // find tool with special enchantment.
            for (int i = 0; i < worker.getInventoryCitizen().getSlots(); i++)
            {
                final ItemStack item = inventory.getStackInSlot(i);
                boolean silkTouch = item.getEnchantmentLevel(Enchantments.SILK_TOUCH) > 0;
                if(silkTouch) {
                    return i;
                }
                int fortune = item.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
                if(fortune > bestLevel) {
                    bestLevel = fortune;
                    bestSlot = i;
                }
            }
            return bestSlot;
        }

        int bestSlot = -1;
        int bestLevel = Integer.MAX_VALUE;
        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding().getMaxEquipmentLevel();

        for (int i = 0; i < worker.getInventoryCitizen().getSlots(); i++)
        {
            final ItemStack item = inventory.getStackInSlot(i);
            final int level = toolType.getMiningLevel(item);

            if (level > -1 && level >= required && level < bestLevel && ItemStackUtils.verifyEquipmentLevel(item, level, required, maxToolLevel))
            {
                bestSlot = i;
                bestLevel = level;
            }
        }

        return (!target.requiresCorrectToolForDrops() && bestSlot == -1) ? NO_TOOL : bestSlot;
    }

    /**
     * Equip or Un-equip armor etc.
     *
     * @param equipSlot Slot to attempt to modify
     * @param equip     true if equipping, false if clearing
     */
    private void setEquipSlot(EquipmentSlot equipSlot, boolean equip)
    {
        if (equip)
        {
            for (final List<GuardGear> itemList : itemsNeeded)
            {
                for (final GuardGear item : itemList)
                {
                    if (item.getType().equals(equipSlot)
                            && building.getBuildingLevel() >= item.getMinBuildingLevelRequired() && building.getBuildingLevel() <= item.getMaxBuildingLevelRequired())
                    {
                        if (!item.test(worker.getInventoryCitizen().getArmorInSlot(item.getType())))
                        {
                            final int toBeEquipped = InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getItemHandlerCitizen(), item);
                            if (toBeEquipped > -1)
                            {
                                final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(toBeEquipped);
                                worker.getInventoryCitizen().transferArmorToSlot(item.getType(), toBeEquipped);
                                virtualEquipmentSlots.put(item.getType(), stack);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            worker.getInventoryCitizen().moveArmorToInventory(equipSlot);
            virtualEquipmentSlots.put(equipSlot, ItemStack.EMPTY);
        }
    }

    private void equipArmor(final boolean equip)
    {
        setEquipSlot(EquipmentSlot.HEAD, equip);
        setEquipSlot(EquipmentSlot.CHEST, equip);
        setEquipSlot(EquipmentSlot.LEGS, equip);
        setEquipSlot(EquipmentSlot.FEET, equip);
    }

    private void logAllEquipment(@NotNull final ExpeditionLog expeditionLog)
    {
        equipArmor(true);

        final IDeliverable edible = new StackList(getEdiblesList(), "Edible Food", 1);

        final List<ItemStack> equipment = new ArrayList<>();
        equipment.add(findTool(ModEquipmentTypes.sword.get()));

        equipment.add(worker.getInventoryCitizen().getArmorInSlot(EquipmentSlot.HEAD));
        equipment.add(worker.getInventoryCitizen().getArmorInSlot(EquipmentSlot.CHEST));
        equipment.add(worker.getInventoryCitizen().getArmorInSlot(EquipmentSlot.LEGS));
        equipment.add(worker.getInventoryCitizen().getArmorInSlot(EquipmentSlot.FEET));

        equipment.add(findTool(ModEquipmentTypes.pickaxe.get()));
        equipment.add(findTool(ModEquipmentTypes.axe.get()));
        equipment.add(findTool(ModEquipmentTypes.shovel.get()));
        equipment.add(findItem(edible::matches));
        expeditionLog.setEquipment(equipment);
    }

    /**
     * Put together the valid list of things to request for food
     */
    private List<ItemStack> getEdiblesList()
    {
        final Set<ItemStorage> allowedItems = building.getModule(NETHERMINER_MENU).getMenu();
        netherEdible.removeIf(item -> !allowedItems.contains(new ItemStorage(item)));
        return netherEdible;
    }

    /**
     * Make sure we have all the needed adventuring supplies This is very similar to the AbstractEntityAiFight "atBuildingActions" But doesn't handle shields, and doesn't equip or
     * leave equipped armor.
     */
    protected void checkAndRequestArmor()
    {
        for (final List<GuardGear> itemList : itemsNeeded)
        {
            for (final GuardGear item : itemList)
            {
                if (!(building.getBuildingLevel() >= item.getMinBuildingLevelRequired() && building.getBuildingLevel() <= item.getMaxBuildingLevelRequired()))
                {
                    continue;
                }

                int bestSlot = -1;
                int bestLevel = -1;
                IItemHandler bestHandler = null;

                if (virtualEquipmentSlots.containsKey(item.getType()) && !ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                {
                    bestLevel = item.getItemNeeded().getMiningLevel(virtualEquipmentSlots.get(item.getType()));
                }
                else
                {
                    ItemStack invItem = findItem(item);
                    if (!invItem.isEmpty())
                    {
                        if (!virtualEquipmentSlots.containsKey(item.getType()) || ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                        {
                            virtualEquipmentSlots.put(item.getType(), invItem);
                            bestLevel = item.getItemNeeded().getMiningLevel(invItem);
                        }
                    }
                    else
                    {
                        virtualEquipmentSlots.put(item.getType(), ItemStack.EMPTY);
                    }
                }

                final Map<IItemHandler, List<Integer>> items = InventoryUtils.findAllSlotsInProviderWith(building, item);
                if (items.isEmpty())
                {
                    // None found, check for equipped
                    if (ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                    {
                        // create request
                        checkForToolOrWeaponAsync(item.getItemNeeded(), item.getMinArmorLevel(), item.getMaxArmorLevel());
                    }
                }
                else
                {
                    // Compare levels
                    for (Map.Entry<IItemHandler, List<Integer>> entry : items.entrySet())
                    {
                        for (final Integer slot : entry.getValue())
                        {
                            final ItemStack stack = entry.getKey().getStackInSlot(slot);
                            if (ItemStackUtils.isEmpty(stack))
                            {
                                continue;
                            }

                            int currentLevel = item.getItemNeeded().getMiningLevel(stack);

                            if (currentLevel > bestLevel)
                            {
                                bestLevel = currentLevel;
                                bestSlot = slot;
                                bestHandler = entry.getKey();
                            }
                        }
                    }
                }

                // Transfer if needed
                if (bestHandler != null)
                {
                    if (!ItemStackUtils.isEmpty(virtualEquipmentSlots.get(item.getType())))
                    {
                        final int slot =
                                InventoryUtils.findFirstSlotInItemHandlerNotEmptyWith(worker.getInventoryCitizen(), stack -> stack == virtualEquipmentSlots.get(item.getType()));
                        if (slot > -1)
                        {
                            InventoryUtils.transferItemStackIntoNextFreeSlotInProvider(worker.getInventoryCitizen(), slot, building);
                        }
                    }

                    // Used for further comparisons, set to the right inventory slot afterwards
                    virtualEquipmentSlots.put(item.getType(), bestHandler.getStackInSlot(bestSlot));
                    InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(bestHandler, bestSlot, worker.getInventoryCitizen());
                }
            }
        }
    }

    protected IAIState checkAndRequestFood()
    {
        if (InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) >= 16)
        {
            // We have enough food.
            return getState();
        }

        if (InventoryUtils.hasBuildingEnoughElseCount(building, stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack)), 1) >= 1)
        {
            needsCurrently = new Tuple<>(stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack)), 32);
            return GATHERING_REQUIRED_MATERIALS;
        }
        return getState();
    }

}
