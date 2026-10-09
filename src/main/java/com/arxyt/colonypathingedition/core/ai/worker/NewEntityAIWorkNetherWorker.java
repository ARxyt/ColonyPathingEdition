package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.extras.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.api.extras.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerCombatAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerMiningAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerPickupAction;
import com.arxyt.colonypathingedition.core.ai.actions.netherworker.NetherWorkerPiglinTradeAction;
import com.arxyt.colonypathingedition.core.util.ToolUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.*;
import static com.arxyt.colonypathingedition.core.costants.states.NewAIWorkerState.NETHER_GATHER_REWARDS;
import static com.arxyt.colonypathingedition.core.costants.states.NewAIWorkerState.NETHER_GATHER_TOOLS;
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
    private IAIState pickupReturnState = START_WORKING;

    public enum Tools {
        SWORD(true, ModEquipmentTypes.sword.get()),
        PICKAXE(true, ModEquipmentTypes.pickaxe.get()),
        AXE(false, ModEquipmentTypes.axe.get()),
        SHOVEL(false, ModEquipmentTypes.shovel.get()),
        HOE(false, ModEquipmentTypes.hoe.get());

        /**
         * Is it demands.
         */
        private boolean required;

        /**
         * Its type.
         */
        private EquipmentTypeEntry type;

        /**
         * Create a new one.
         *
         * @param required if demands.
         */
        Tools(final boolean required, final EquipmentTypeEntry equipmentType)
        {
            this.required = required;
            this.type = equipmentType;
        }

        /**
         * Worker have to get one.
         *
         * @return true if so.
         */
        public boolean isRequired()
        {
            return required;
        }

        /**
         * The type of tools we needed.
         *
         * @return type.
         */
        public EquipmentTypeEntry getType() {
            return type;
        }
    }

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
                new AITarget<IAIState>(NETHER_GATHER_TOOLS, this::gatherTools, 1),
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
        return pickupReturnState;
    }

    @Override
    public boolean canBeInterrupted()
    {
        return !worker.isInvisible();
    }

    @Override
    protected IAIState decide()
    {
        JobNetherWorkerExtra jobExtra = (JobNetherWorkerExtra)job;
        //Check if we are traveling.
        if (!job.getCraftedResults().isEmpty())
        {
            extraRound = jobExtra.getExtraRounds();
            worker.setInvisible(true);
            setDelay(WAITING_DELAY);
            return NETHER_AWAY;
        }
        if (!job.getProcessedResults().isEmpty() || job.isInNether()) {
            setDelay(WAITING_DELAY);
            return NETHER_GATHER_REWARDS;
        }

        job.setInNether(false);
        worker.setInvisible(false);
        pickupReturnState = START_WORKING;

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
                if (!checkIfRequestForItemExistOrCreateAsync(new ItemStack(item.getItem(), 1), item.getAmount() * (1 + extraRoundsLimit()), item.getAmount()))
                {
                    hasItemsAvailable = false;
                }
            }
        }

        boolean hasGotTools = checkForToolOrWeaponNotTooBroken();
        boolean missingLighter = checkForToolOrWeapon(ModEquipmentTypes.flint_and_steel.get());
        if (!hasItemsAvailable || !hasGotTools || missingLighter)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            setDelay(STUCK_DELAY);
            return getState();
        }

        if(!hasEaten && worker.getCitizenData().getSaturation() < FULL_SATURATION){
            jobExtra.setShouldEat(true);
            hasEaten = true;
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
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);

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

                JobNetherWorkerExtra jobExtra = (JobNetherWorkerExtra)job;
                // Check for materials needed to go to the Nether
                if (currentRecipeStorage != null && jobExtra.canExtraRounds(extraRoundsLimit()))
                {
                    for (ItemStorage item : currentRecipeStorage.getInput())
                    {
                        checkIfRequestForItemExistOrCreateAsync(new ItemStack(item.getItem(), 1), item.getAmount() * jobExtra.remainExtraRounds(extraRoundsLimit()), item.getAmount());
                    }
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
        worker.setInvisible(true);

        // Action Loop
        if(actionHandler.canActionTick()) {
            switch (actionHandler.doAction()) {
                case INVALID -> {
                    actionHandler.onActionFinished();
                    job.getCraftedResults().remove(actionHandler.getCurrStack());
                    setDelay(WAITING_DELAY);
                    worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    return NETHER_AWAY;
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
                    worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    return NETHER_RETURN;
                }
                case SUCCESS -> {
                    List<ItemStack> rewards = actionHandler.onActionFinished();
                    job.addProcessedResultsList(rewards);
                    expeditionLog.addLoot(rewards);
                    logAllEquipment(expeditionLog);
                    setDelay(actionHandler.actionDelay());
                    job.getCraftedResults().remove(actionHandler.getCurrStack());
                    worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    return NETHER_AWAY;
                }
                case IN_PROGRESS -> {
                    logAllEquipment(expeditionLog);
                    setDelay(actionHandler.actionDelay());
                    worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    return NETHER_AWAY;
                }
            }
        }

        if(!requestToolOrWeaponNotTooBroken()) {
            return NETHER_GATHER_TOOLS;
        }

        //This is the adventure loop.
        if (!job.getCraftedResults().isEmpty())
        {
            ItemStack currStack = job.getCraftedResults().peek();
            if(currStack == null) {
                job.getCraftedResults().poll();
                return NETHER_AWAY;
            }
            if (currStack.getItem() instanceof ItemAdventureToken && currStack.hasTag())
            {
                CompoundTag tag = currStack.getTag();
                assert tag != null;
                if(tag.contains(TAG_ENTITY_TYPE)) {
                    actionHandler.setAction(new NetherWorkerCombatAction(world, worker, job, tag, extraRound,
                            toolSlots.get(ModEquipmentTypes.sword.get()) != null ? toolSlots.get(ModEquipmentTypes.sword.get()) : -1,
                            alterToolSlots.get(ModEquipmentTypes.sword.get()) != null ? alterToolSlots.get(ModEquipmentTypes.sword.get()) : -1,
                            mendingToolSlot(ModEquipmentTypes.sword.get())
                            ), currStack);
                    return NETHER_AWAY;
                }
                if(tag.contains("tradeLoot")) {
                    actionHandler.setAction(new NetherWorkerPiglinTradeAction(world, worker, job, tag), currStack);
                    return NETHER_AWAY;
                }
            }
            else if (!currStack.isEmpty())
            {
                if (currStack.getItem() instanceof BlockItem bi)
                {
                    final Block block = bi.getBlock();
                    final BlockState state = block.defaultBlockState();
                    final BlockPos pos = worker.blockPosition();
                    actionHandler.setAction(new NetherWorkerMiningAction(world, worker, job, currStack,
                            getMostEfficientTool(state, pos),
                            mendingToolSlot(WorkerUtil.getBestToolForBlock(state, state.getDestroySpeed(world, pos), building, world, pos))
                    ), currStack);
                    return NETHER_AWAY;
                }
                else
                {
                    actionHandler.setAction(new NetherWorkerPickupAction(currStack), currStack);
                    return NETHER_AWAY;
                }
            }
            job.getCraftedResults().poll();
            return NETHER_AWAY;
        }

        return onTravelFinished(expeditionLog, false);
    }

    private IAIState onTravelFinished(ExpeditionLog expeditionLog, Boolean escaped) {
        JobNetherWorkerExtra jobExtra = (JobNetherWorkerExtra) job;
        job.getCraftedResults().clear();
        if(job.getProcessedResults().isEmpty()) {
            extraRound = jobExtra.setExtraRounds(false);
            expeditionLog.setStatus(ExpeditionLog.Status.RETURNING_HOME);
            return NETHER_RETURN;
        }

        if (!escaped && jobExtra.canExtraRounds(extraRoundsLimit()) && worker.getHealth() >= worker.getMaxHealth() && InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) >= 10) {
            if (currentRecipeStorage instanceof RecipeStorage) {
                List<ItemStack> result = currentRecipeStorage.fullfillRecipeAndCopy(getLootContext(), ImmutableList.of(worker.getItemHandlerCitizen()), false);
                if (result != null) {
                    // by default all the adventure tokens are at the end (due to loot tables); space them better
                    result = new ArrayList<>(result);
                    Collections.shuffle(result, worker.getCitizenData().getRandom());
                    job.addCraftedResultsList(result);
                    worker.getCitizenData().setJobStatus(JobStatus.WORKING);
                    extraRound = jobExtra.setExtraRounds(true);
                    StatsUtil.trackStat(building, "extraRounds", 1);
                    if (currentRecipeStorage != null && jobExtra.canExtraRounds(extraRoundsLimit()))
                    {
                        for (ItemStorage item : currentRecipeStorage.getInput())
                        {
                            checkIfRequestForItemExistOrCreateAsync(new ItemStack(item.getItem(), 1), item.getAmount() * jobExtra.remainExtraRounds(extraRoundsLimit()), item.getAmount());
                        }
                    }
                    if(!checkForToolOrWeaponNotTooBroken()) {
                        setDelay(STUCK_DELAY);
                        return NETHER_GATHER_TOOLS;
                    }
                    setDelay(WAITING_DELAY);
                    return getState();
                }
                final IRecipeStorage recipeStorage = currentRecipeStorage;
                IAIState checkResult = checkForItems(currentRecipeStorage);
                currentRecipeStorage = recipeStorage;
                if(checkResult == GATHERING_REQUIRED_MATERIALS) {
                    pickupReturnState = NETHER_GATHER_TOOLS;
                    setDelay(STUCK_DELAY);
                    return checkResult;
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

    protected IAIState gatherTools() {
        worker.setInvisible(false);
        if (!walkToBuilding())
        {
            return NETHER_GATHER_TOOLS;
        }
        pickupReturnState = NETHER_GATHER_TOOLS;
        final IRecipeStorage recipeStorage = currentRecipeStorage;
        IAIState checkResult = checkForItems(currentRecipeStorage);
        currentRecipeStorage = recipeStorage;
        if(checkResult == GATHERING_REQUIRED_MATERIALS) {
            return checkResult;
        }
        setDelay(WAITING_DELAY);
        return checkForToolOrWeaponNotTooBroken() ? NETHER_AWAY : NETHER_GATHER_TOOLS;
    }

    protected IAIState gatherRewards() {
        final BlockPos portal = building.getPortalLocation();
        if(worker.isInvisible()) {
            worker.teleportTo(portal.getX(), portal.getY() + 0.5D, portal.getZ());
        }
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

        final int maxToolLevel = worker.getCitizenColonyHandler().getWorkBuilding() == null ?
                building.getMaxEquipmentLevel() : worker.getCitizenColonyHandler().getWorkBuilding().getMaxEquipmentLevel();
        final Predicate<ItemStack> suffcientPredicate = stack -> {
            final int miningLevel = toolType.getMiningLevel(stack);
            return miningLevel >= Math.max(0, required) && miningLevel + ItemStackUtils.getMaxEnchantmentLevel(stack) <= maxToolLevel;
        };

        // get form cached tool;
        int slot = getCachedMostEfficientTool(toolType, suffcientPredicate);
        if (slot > 0) return slot;
        if (!target.requiresCorrectToolForDrops()) return NO_TOOL;

        int bestSlot = -1;
        int bestLevel = Integer.MIN_VALUE;

        for (int i = 0; i < inventory.getSlots(); i++)
        {
            final ItemStack itemStack = inventory.getStackInSlot(i);
            final int miningLevel = toolType.getMiningLevel(itemStack);
            final int trueLevel = miningLevel + ItemStackUtils.getMaxEnchantmentLevel(itemStack);

            if (miningLevel > -1 && miningLevel >= required && trueLevel > bestLevel && trueLevel <= maxToolLevel)
            {
                bestSlot = i;
                bestLevel = trueLevel;
            }

            if(bestLevel == maxToolLevel) {
                break;
            }
        }

        if(bestSlot != -1) {
            if(!toolSlots.containsKey(toolType)) {
                toolSlots.put(toolType, bestSlot);
            }
            else{
                alterToolSlots.put(toolType, bestSlot);
            }
        }

        return bestSlot;
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


    // TODO: Move these functions to abstract AI class.
    private Map<EquipmentTypeEntry, Integer> toolSlots = new HashMap<>();
    private Map<EquipmentTypeEntry, Integer> alterToolSlots = new HashMap<>();
    private Map<EquipmentTypeEntry, Tuple<BlockPos, Integer>> hutToolCache = new HashMap<>();

    private static final Predicate<ItemStack> IS_MENDING_DAMAGED_TOOL =
            ToolUtils::isMendingDamagedTool;

    public boolean checkForToolOrWeaponNotTooBroken()
    {
        final boolean needTool = checkForToolOrWeaponNotTooBroken(TOOL_LEVEL_WOOD_OR_GOLD, true);
        worker.getCitizenData().setJobStatus(needTool? JobStatus.STUCK : JobStatus.WORKING);
        return needTool;
    }

    public boolean requestToolOrWeaponNotTooBroken()
    {
        final boolean needTool = checkForToolOrWeaponNotTooBroken(TOOL_LEVEL_WOOD_OR_GOLD, false);
        worker.getCitizenData().setJobStatus(needTool? JobStatus.STUCK : JobStatus.WORKING);
        return needTool;
    }

    protected boolean checkForToolOrWeaponNotTooBroken(final int minimalLevel, boolean shouldPickup)
    {
        // Got what type of equipment we still need.
        final Set<EquipmentTypeEntry> typeNeedCheck = checkForNeededToolsNotTooBroken(
                Arrays.stream(Tools.values()).map(Tools::getType).collect(Collectors.toList()),
                minimalLevel);

        final Set<EquipmentTypeEntry> hasOrdered = ((JobNetherWorkerExtra) job).getHasOrdered();
        if (shouldPickup) {
            hasOrdered.clear();
        }
        else{
            typeNeedCheck.removeAll(hasOrdered);
        }

        // We fulfilled our requirements.
        if (typeNeedCheck.isEmpty())
        {
            hutToolCache.clear();
            return shouldPickup || toolSlots.size() >= Tools.values().length;
        }

        // We delete what we find in our hut, they're no need to check again for almost they will stay there.
        typeNeedCheck.removeAll(hutToolCache.keySet());

        // We directly calculate which tool we have requested and delete it as requests are not completed.
        final Set<EquipmentTypeEntry> toolWeRequested = new HashSet<>();

        for (IRequest<? extends Tool> r : building.getOpenRequestsOfTypeFiltered(
                worker.getCitizenData(), TypeToken.of(Tool.class),
                req -> req.getRequest().getMinLevel() >= minimalLevel && typeNeedCheck.contains(req.getRequest().getEquipmentType())))
        {
            toolWeRequested.add(r.getRequest().getEquipmentType());
        }
        for (IRequest<? extends Tool> r : building.getCompletedRequestsOfTypeFiltered(
                worker.getCitizenData(), TypeToken.of(Tool.class),
                req -> req.getRequest().getMinLevel() >= minimalLevel && typeNeedCheck.contains(req.getRequest().getEquipmentType())))
        {
            toolWeRequested.add(r.getRequest().getEquipmentType());
        }

        typeNeedCheck.removeAll(toolWeRequested);
        if (!shouldPickup) {
            hasOrdered.addAll(toolWeRequested);
        }

        // We find tools in our hut once the request finish, here we check all the slots and cache them in hutToolCache.
        Map<EquipmentTypeEntry, Tuple<BlockPos, Integer>> toolFindInHut = findToolsInHut(typeNeedCheck, minimalLevel);
        hutToolCache.putAll(toolFindInHut);
        typeNeedCheck.removeAll(toolFindInHut.keySet());

        // We got the address, so just walk to workspace and pick up.
        if (shouldPickup && !hutToolCache.isEmpty() && walkToBuilding())
        {
            Set<EquipmentTypeEntry> failedTools = retrieveToolInHut(minimalLevel);
            // we should not request these invalid-cached tools at this time, check it out in the inventory.
            toolWeRequested.addAll(failedTools);
        }

        // Nothing found, so we request.
        for (EquipmentTypeEntry type : typeNeedCheck)
        {
            final Tool request = new Tool(type, minimalLevel, Math.max(building.getMaxEquipmentLevel(), minimalLevel));
            if(shouldPickup) {
                worker.getCitizenData().createRequest(request);
            }
            else{
                worker.getCitizenData().createRequestAsync(request);
            }
        }

        // shouldPickup -> we get everything well-prepared in worker's inventory.
        // !shouldPickup -> we have every tool we need in slot cache.
        return shouldPickup ? typeNeedCheck.isEmpty() && toolWeRequested.isEmpty() : toolSlots.size() >= Tools.values().length;
    }

    private Set<EquipmentTypeEntry> checkForNeededToolsNotTooBroken(
            @NotNull final List<EquipmentTypeEntry> toolTypes, final int minimalLevel)
    {
        final IBuilding workingBuilding = worker.getCitizenColonyHandler().getWorkBuilding();
        final int maxToolLevel = workingBuilding != null
                ? workingBuilding.getMaxEquipmentLevel()
                : building.getMaxEquipmentLevel();

        final InventoryCitizen inventory = worker.getInventoryCitizen();
        final int inventorySlots = inventory.getSlots();

        // validation check.
        final List<EquipmentTypeEntry> requiredTools = new ArrayList<>();
        final List<EquipmentTypeEntry> requireAlterTools = new ArrayList<>();
        final Set<EquipmentTypeEntry> typeNeedCheck = new HashSet<>();
        for (EquipmentTypeEntry tool : toolTypes)
        {
            boolean haveTool = false;

            final Integer mainSlot = toolSlots.get(tool);
            if (mainSlot != null && mainSlot >= 0 && mainSlot < inventorySlots)
            {
                final ItemStack thisTool = inventory.getStackInSlot(mainSlot);
                if (tool.checkIsEquipment(thisTool))
                {
                    haveTool = true;
                    if (thisTool.getDamageValue() <= thisTool.getMaxDamage() * 0.75)
                    {
                        alterToolSlots.remove(tool);
                        continue;
                    }
                }
            }
            if (!haveTool)
            {
                toolSlots.remove(tool);
            }

            final Integer alterSlot = alterToolSlots.get(tool);
            if (alterSlot != null && alterSlot >= 0 && alterSlot < inventorySlots)
            {
                final ItemStack thisTool = inventory.getStackInSlot(alterSlot);
                if (tool.checkIsEquipment(thisTool))
                {
                    if (haveTool)
                    {
                        continue;
                    }
                    alterToolSlots.remove(tool);
                    toolSlots.put(tool, alterSlot);
                    if (thisTool.getDamageValue() <= thisTool.getMaxDamage() * 0.75)
                    {
                        continue;
                    }
                    haveTool = true;
                }
            }
            alterToolSlots.remove(tool);

            if (haveTool)
            {
                requireAlterTools.add(tool);
            }
            else
            {
                requiredTools.add(tool);
            }
            typeNeedCheck.add(tool);
        }

        // further check on main tools.
        Map<EquipmentTypeEntry, Integer> furtherCheck = ToolUtils.findEquipments(inventory, requiredTools, minimalLevel, maxToolLevel);
        toolSlots.putAll(furtherCheck);
        furtherCheck.forEach((tool, slot) -> {
            ItemStack thisTool = inventory.getStackInSlot(slot);
            if (thisTool.getDamageValue() <= thisTool.getMaxDamage() * 0.75) {
                typeNeedCheck.remove(tool);
            } else {
                requireAlterTools.add(tool);
            }
        });

        // further check on alter tools.
        alterToolSlots.putAll(ToolUtils.findEquipmentsWithExceptSlots(
                inventory, requireAlterTools, minimalLevel, maxToolLevel, toolSlots));
        typeNeedCheck.removeAll(alterToolSlots.keySet());

        return typeNeedCheck;
    }

    public Map<EquipmentTypeEntry, Tuple<BlockPos, Integer>> findToolsInHut(final Set<EquipmentTypeEntry> typeNeedCheck, final int minimalLevel)
    {
        final Map<EquipmentTypeEntry, Tuple<BlockPos, Integer>> toCache = new HashMap<>();
        if (building != null) {
            for(BlockPos pos : building.getContainers()) {
                final BiFunction<ItemStack, EquipmentTypeEntry, Boolean> toolPredicate =
                        (ItemStack stack, EquipmentTypeEntry equipmentType) ->
                                ItemStackUtils.hasEquipmentLevel(stack, equipmentType, minimalLevel, building.getMaxEquipmentLevel());
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity != null)
                {
                    IItemHandler rack =  entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                    Map<EquipmentTypeEntry, Integer> foundCache = ToolUtils.checkEquipmentsInItemHandler(rack, typeNeedCheck, toolPredicate);
                    foundCache.forEach((tool, slot) -> toCache.put(tool, new Tuple<>(pos, slot)));
                }
            }
        }
        return toCache;
    }

    public Set<EquipmentTypeEntry> retrieveToolInHut(final int minimalLevel) {
        final Set<EquipmentTypeEntry> failTools = new HashSet<>();
        if (building != null) {
            final Set<EquipmentTypeEntry> toRemove = new HashSet<>();
            for (EquipmentTypeEntry toolType : hutToolCache.keySet()) {
                final Tuple<BlockPos, Integer> cache = hutToolCache.get(toolType);
                final BlockPos rackCache = cache.getA();
                if (rackCache == null || !building.getContainers().contains(rackCache) || cache.getB() == null) {
                    toRemove.add(toolType);
                    failTools.add(toolType);
                    continue;
                }
                final int rackSlot = cache.getB();
                final Predicate<ItemStack> toolPredicate = stack -> ItemStackUtils.hasEquipmentLevel(stack, toolType, minimalLevel, building.getMaxEquipmentLevel());
                final BlockEntity entity = world.getBlockEntity(rackCache);
                if (entity != null) {
                    IItemHandler rack = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
                    int citizenSlot = ToolUtils.transferItemStackOfExactSlotIntoEmptySlotInNextItemHandler(rack, rackSlot, toolPredicate, worker.getInventoryCitizen());
                    if (citizenSlot > 0) {
                        if (toolSlots.containsKey(toolType)) {
                            alterToolSlots.put(toolType, citizenSlot);
                        } else {
                            toolSlots.put(toolType, citizenSlot);
                        }
                    } else {
                        failTools.add(toolType);
                    }
                    if (citizenSlot != ToolUtils.INVENTORY_FULL) {
                        toRemove.add(toolType);
                        ;
                    }
                }
            }
            for (EquipmentTypeEntry toolType : toRemove) {
                hutToolCache.remove(toolType);
            }
        }
        return failTools;
    }

    protected int mendingToolSlot(EquipmentTypeEntry toolType) {
        Set<Integer> slotToCheck = Stream.concat(
                        alterToolSlots.entrySet().stream(),
                        toolSlots.entrySet().stream())
                .filter(e -> !e.getKey().equals(toolType))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
        final IItemHandler workerInventory = worker.getItemHandlerCitizen();
        Optional<Integer> firstSlot = slotToCheck.stream()
                .filter(i -> IS_MENDING_DAMAGED_TOOL.test(workerInventory.getStackInSlot(i)))
                .findFirst();
        return firstSlot.orElse(-1);
    }

    protected int getCachedMostEfficientTool(@NotNull final EquipmentTypeEntry toolType, final Predicate<ItemStack> suffcientPredicate) {
        final IItemHandler workerInventory = worker.getItemHandlerCitizen();
        final int slot = toolSlots.get(toolType) == null ? -1 : toolSlots.get(toolType);
        if(suffcientPredicate.test(workerInventory.getStackInSlot(slot))) {
            return slot;
        }
        final int alterSlot = alterToolSlots.get(toolType) == null ? -1 : alterToolSlots.get(toolType);
        if(suffcientPredicate.test(workerInventory.getStackInSlot(alterSlot))) {
            return alterSlot;
        }
        return -1;
    }
}
