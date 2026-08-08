package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.arxyt.colonypathingedition.core.util.SwitchUtils;
import com.arxyt.colonypathingedition.mixins.minecraft.DamageSourcesAccessor;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.modules.ICraftingBuildingModule;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.requestable.StackList;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.compatibility.tinkers.TinkersToolHelper;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.entity.ai.workers.util.GuardGearBuilder;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.*;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.items.ItemAdventureToken;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.*;
import static com.minecolonies.api.util.constant.GuardConstants.*;
import static com.minecolonies.api.util.constant.GuardConstants.BASE_PHYSICAL_DAMAGE;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_XP_DROPPED;
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
     * Multiplier for damage reduction.
     */
    private static final float SECONDARY_DAMAGE_REDUCTION = .005f;

    /**
     * Virtual slots for equipment, so we can track what is "equipped" without having it visible when the citizen is invisible.
     */
    private final Map<EquipmentSlot, ItemStack> virtualEquipmentSlots = new HashMap<>();

    boolean extraRound;
    int timeOutCounter = 0;
    boolean hasEaten = false;

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

    public NewEntityAIWorkNetherWorker(@NotNull JobNetherWorker job)
    {
        super(job);
        super.registerTargets(
                new AITarget(NETHER_LEAVE, this::leaveForNether, TICK_DELAY),
                new AITarget(NETHER_AWAY, this::stayInNether, TICK_DELAY),
                new AITarget(NETHER_RETURN, this::returnFromNether, TICK_DELAY),
                new AITarget(NETHER_OPENPORTAL, this::openPortal, TICK_DELAY),
                new AITarget(NETHER_CLOSEPORTAL, this::closePortal, TICK_DELAY)
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

    private void goToVault()
    {
        worker.playSound(SoundEvents.PORTAL_TRIGGER, worker.getRandom().nextFloat() * 0.5F + 0.25F, 0.25F);
        worker.getCitizenData().getColony().getTravellingManager().startTravellingTo(
                worker.getCitizenData(),
                building.getPortalLocation(),
                job.getCraftedResults().size() * 400 //Twenty seconds of travelling time per item, task or adventure that we complete, maybe parameterize in the config.
        );

        worker.remove(Entity.RemovalReason.DISCARDED);
    }

    @Override
    protected IAIState decide()
    {
        //Check if we are traveling, we don't spawn an entity if we are traveling.
        if (worker.getCitizenData().getColony().getTravellingManager().isTravelling(worker.getCitizenData()) || job.isInNether())
        {
            extraRound = ((JobNetherWorkerExtra)job).getExtraRounds();
            return NETHER_AWAY;
        }

        //Now check if travelling finished.
        final Optional<BlockPos> travelingTarget = worker.getCitizenData().getColony().getTravellingManager().getTravellingTargetFor(worker.getCitizenData());
        if (travelingTarget.isPresent())
        {
            worker.getCitizenData().setNextRespawnPosition(EntityUtils.getSpawnPoint(job.getColony().getWorld(), travelingTarget.get()));
            worker.getCitizenData().updateEntityIfNecessary();
        }

        job.setInNether(false);

        IAIState crafterState = super.decide();

        if (crafterState != IDLE && crafterState != START_WORKING)
        {
            return crafterState;
        }

        // Get Armor if available.
        // This is async, but we'll wait extra time for it if it's craftable.
        boolean isArmorCraftable = checkAndRequestArmorWithAvailableCheck();

        // Get food if available. We just ignore extra time waiting for it as armor is much more complex to craft.
        final IAIState tempState = checkAndRequestFood();
        if (tempState != getState())
        {
            return tempState;
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

        if (!hasItemsAvailable)
        {
            setDelay(60);
            return IDLE;
        }

        final BlockPos portal = building.getPortalLocation();
        if (portal == null)
        {
            Log.getLogger().warn("--- Missing Portal Tag In Nether Worker Building! Aborting Operation! ---");
            setDelay(120);
            return IDLE;
        }

        // Get other adventuring supplies. These are required.
        // Done this way to get all the requests in parallel
        boolean missingAxe = checkForToolOrWeapon(ModEquipmentTypes.axe.get());
        boolean missingPick = checkForToolOrWeapon(ModEquipmentTypes.pickaxe.get());
        boolean missingShovel = checkForToolOrWeapon(ModEquipmentTypes.shovel.get());
        boolean missingSword = checkForToolOrWeapon(ModEquipmentTypes.sword.get());
        boolean missingLighter = checkForToolOrWeapon(ModEquipmentTypes.flint_and_steel.get());
        if (missingAxe || missingPick || missingShovel || missingSword || missingLighter)
        {
            worker.getCitizenData().setJobStatus(JobStatus.STUCK);
            setDelay(60);
            return IDLE;
        }

        if(!hasEaten && worker.getCitizenData().getSaturation() < FULL_SATURATION){
            if(worker.getCitizenJobHandler().getColonyJob() instanceof JobNetherWorker job){
                ((JobNetherWorkerExtra) job).setShouldEat(true);
                hasEaten = true;
            }
        }

        // We should wait for armor for extra 2 minutes if it's craftable.
        if(isArmorCraftable){
            if(timeOutCounter++ < 12){
                setDelay(200);
                return getState();
            }
        }
        else{
            timeOutCounter = 0;
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
            setDelay(200);
            return getState();
        }
        else
        {
            if (!building.isReadyForTrip())
            {
                worker.getCitizenData().setJobStatus(JobStatus.IDLE);
                setDelay(120);
                return IDLE;
            }
            if (walkTo != null || !walkToBuilding())
            {
                setDelay(200);
                return getState();
            }
            if (!worker.getInventoryCitizen().hasSpace())
            {
                return INVENTORY_FULL;
            }

            IAIState checkResult = checkForItems(currentRecipeStorage);

            if (checkResult == GET_RECIPE)
            {
                currentRecipeStorage = null;
                worker.getCitizenData().setJobStatus(JobStatus.STUCK);
                setDelay(60);
                return IDLE;
            }
            if (checkResult != CRAFT)
            {
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
                logAllEquipment(expeditionLog, false);

                List<ItemStack> result = currentRecipeStorage.fullfillRecipeAndCopy(getLootContext(), ImmutableList.of(worker.getItemHandlerCitizen()), false);
                if (result != null)
                {
                    // by default all the adventure tokens are at the end (due to loot tables); space them better
                    result = new ArrayList<>(result);
                    Collections.shuffle(result, worker.getCitizenData().getRandom());
                    job.addCraftedResultsList(result);
                }

                goToVault();
                worker.getCitizenData().setJobStatus(JobStatus.WORKING);
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

        // Decide whether nether worker should escape.
        boolean escaped = false;

        equipArmor(true);

        //This is the adventure loop.
        if (!job.getCraftedResults().isEmpty())
        {
            for (ItemStack currStack : job.getCraftedResults())
            {
                if(extraRound && InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) < 8){
                    escaped = true;
                    break;
                }
                if (currStack.getItem() instanceof ItemAdventureToken)
                {
                    if (currStack.hasTag())
                    {
                        CompoundTag tag = currStack.getTag();
                        if (tag != null && tag.contains(TAG_DAMAGE))
                        {
                            worker.setItemSlot(EquipmentSlot.MAINHAND, findTool(ModEquipmentTypes.sword.get()));

                            DamageSource source = ((DamageSourcesAccessor)world.damageSources()).invokerSource(DamageSourceKeys.NETHER);

                            //Set up the mob to do battle with
                            EntityType<?> mobType = EntityType.ZOMBIE;
                            if (tag.contains(TAG_ENTITY_TYPE))
                            {
                                mobType = EntityType.byString(tag.getString(TAG_ENTITY_TYPE)).orElse(EntityType.ZOMBIE);
                            }
                            LivingEntity mob = (LivingEntity) mobType.create(world);
                            assert mob != null;
                            float mobHealth = mob.getHealth();

                            // Calculate how much damage the mob will do if it lands a hit (Before armor)
                            float incomingDamage = tag.getFloat(TAG_DAMAGE);
                            incomingDamage -= incomingDamage * (getSecondarySkillLevel() * SECONDARY_DAMAGE_REDUCTION);


                            while (mobHealth > 0 && !worker.isDeadOrDying() && !escaped) {
                                // Clear anti-hurt timers.
                                worker.hurtTime = 0;
                                worker.invulnerableTime = 0;
                                float damageToDo = BASE_PHYSICAL_DAMAGE;

                                // Calculate if the sword still exists, how much damage will be done to the mob
                                final ItemStack sword = worker.getItemBySlot(EquipmentSlot.MAINHAND);
                                if (!sword.isEmpty())
                                {
                                    if (sword.getItem() instanceof SwordItem)
                                    {
                                        damageToDo += ((SwordItem) sword.getItem()).getDamage();
                                    }
                                    else
                                    {
                                        damageToDo += (float) TinkersToolHelper.getDamage(sword);
                                    }
                                    damageToDo += (float) (EnchantmentHelper.getDamageBonus(sword, mob.getMobType()) / 2.5);
                                    sword.hurtAndBreak(1, worker, entity -> {
                                        // the sword broke; try to find another sword
                                        worker.setItemSlot(EquipmentSlot.MAINHAND, findTool(ModEquipmentTypes.sword.get()));
                                    });
                                }

                                // Hit the mob
                                mobHealth -= damageToDo;

                                // Get hit by the mob
                                if (!worker.hurt(source, incomingDamage))
                                {
                                    //Shouldn't get here, but if we do we can force the damage.
                                    incomingDamage = worker.calculateDamageAfterAbsorbs(source, incomingDamage);
                                    worker.setHealth(worker.getHealth() - incomingDamage);
                                }

                                // Every round, heal up if possible, to compensate for all of this happening in a single tick.
                                final float saturationFactor = 0.25f;
                                if(worker.getCitizenData().getSaturation() > LOW_SATURATION) {
                                    float healAmount = (float) Math.min((worker.getCitizenData().getSaturation() - LOW_SATURATION) / saturationFactor, worker.getMaxHealth() - worker.getHealth());
                                    worker.heal(healAmount);
                                    worker.getCitizenData().decreaseSaturation(healAmount * saturationFactor);
                                }

                                if (worker.getCitizenData().getSaturation() < AVERAGE_SATURATION)
                                {
                                    attemptToEat();
                                }

                                if (!worker.isDeadOrDying() && (worker.getCitizenData().getSaturation() < LOW_SATURATION + 0.2 || worker.getHealth() < worker.getMaxHealth() * 0.2))
                                {
                                    escaped = worker.getRandom().nextFloat() < getPrimarySkillLevel() / 200.0F;
                                }
                            }
                            expeditionLog.setCitizen(worker);
                            logAllEquipment(expeditionLog, true);

                            if (worker.isDeadOrDying())
                            {
                                expeditionLog.setKilled();

                                StatsUtil.trackStat(building, MINER_DEATHS, 1);

                                // Stop processing loot table data, as the worker died before finishing the trip.
                                InventoryUtils.clearItemHandler(worker.getItemHandlerCitizen());
                                job.getCraftedResults().clear();
                                job.getProcessedResults().clear();
                                return IDLE;
                            }
                            else if(!escaped)
                            {
                                // Generate loot for this mob, with all the right modifiers
                                LootParams context = this.getLootContext();
                                LootTable loot = Objects.requireNonNull(world.getServer()).getLootData().getLootTable(mob.getLootTable());
                                List<ItemStack> mobLoot = loot.getRandomItems(context);
                                if(mob instanceof MagmaCube){
                                    mobLoot.add(new ItemStack(Items.MAGMA_CREAM,1));
                                }
                                else if(mob instanceof Slime){
                                    mobLoot.add(new ItemStack(Items.SLIME_BALL,1));
                                }
                                job.addProcessedResultsList(mobLoot);
                                expeditionLog.addMob(mobType);
                                expeditionLog.addLoot(mobLoot);
                            }

                            worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                        }

                        if(escaped){
                            break;
                        }

                        if (currStack.getTag().contains(TAG_XP_DROPPED))
                        {
                            worker.getCitizenExperienceHandler().addExperience(CitizenItemUtils.applyMending(worker, currStack.getTag().getInt(TAG_XP_DROPPED)));
                        }
                    }
                }
                else if (!currStack.isEmpty())
                {
                    int itemDelay = 0;
                    if (currStack.getItem() instanceof BlockItem bi)
                    {
                        final Block block = bi.getBlock();

                        ItemStack tool = findTool(block.defaultBlockState(), worker.blockPosition());
                        if (tool.getItem() instanceof TieredItem)
                        {
                            worker.setItemSlot(EquipmentSlot.MAINHAND, tool);

                            for (int i = 0; i < currStack.getCount() && !tool.isEmpty(); i++)
                            {
                                LootParams context = this.getLootContext();
                                LootTable loot = Objects.requireNonNull(world.getServer()).getLootData().getLootTable(block.getLootTable());
                                List<ItemStack> mobLoot = loot.getRandomItems(context);

                                job.addProcessedResultsList(mobLoot);
                                expeditionLog.addLoot(mobLoot);
                                worker.getCitizenExperienceHandler().addExperience(CitizenItemUtils.applyMending(worker, xpOnDrop(block)));

                                itemDelay += TICK_DELAY;
                            }

                            worker.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                            logAllEquipment(expeditionLog, false);
                        }
                        else
                        {
                            //we didn't have a tool to use.
                            itemDelay = TICK_DELAY;
                        }
                    }
                    else
                    {
                        job.addProcessedResultsList(ImmutableList.of(currStack));
                        expeditionLog.addLoot(Collections.singletonList(currStack));
                        itemDelay = TICK_DELAY * currStack.getCount();
                    }
                    setDelay(itemDelay);
                }
            }
            job.getCraftedResults().clear();
            if(!escaped) {
                return getState();
            }
        }

        if (!job.getProcessedResults().isEmpty())
        {
            if (!worker.isDeadOrDying())
            {
                expeditionLog.setStatus(ExpeditionLog.Status.RETURNING_HOME);
                for (ItemStack item : job.getProcessedResults())
                {
                    if (InventoryUtils.addItemStackToItemHandler(worker.getItemHandlerCitizen(), item))
                    {
                        worker.decreaseSaturationForContinuousAction();
                        worker.getCitizenExperienceHandler().addExperience(0.2);
                        StatsUtil.trackStatByName(building, ITEMS_DISCOVERED, item.getHoverName(), item.getCount());
                    }
                }

                job.getProcessedResults().clear();
                if(!escaped) {
                    return getState();
                }
            }
            else
            {
                job.getProcessedResults().clear();
            }
        }
        else{
            if(worker.getHealth() >= worker.getMaxHealth() && InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack))) >= 10){
                final ICraftingBuildingModule module = building.getFirstModuleOccurance(BuildingNetherWorker.CraftingModule.class);
                currentRecipeStorage = module.getFirstFulfillableRecipe(ItemStackUtils::isEmpty, 1, false);
                if(currentRecipeStorage != null) {
                    List<ItemStack> result = currentRecipeStorage.fullfillRecipeAndCopy(getLootContext(), ImmutableList.of(worker.getItemHandlerCitizen()), false);
                    if (result != null)
                    {
                        // by default all the adventure tokens are at the end (due to loot tables); space them better
                        result = new ArrayList<>(result);
                        Collections.shuffle(result, worker.getCitizenData().getRandom());
                        job.addCraftedResultsList(result);
                        goToVault();
                        worker.getCitizenData().setJobStatus(JobStatus.WORKING);
                        extraRound = ((JobNetherWorkerExtra)job).setExtraRounds(true);
                        return getState();
                    }
                }
            }
        }

        expeditionLog.setStatus(ExpeditionLog.Status.COMPLETED);
        return NETHER_RETURN;
    }

    // calculate the XP coming from certain ores
    private int xpOnDrop(Block block)
    {
        RandomSource rnd = worker.getRandom();
        if (block == Blocks.COAL_ORE)
        {
            return rnd.nextInt(0, 2);
        }
        else if (block == Blocks.DIAMOND_ORE)
        {
            return rnd.nextInt(3, 7);
        }
        else if (block == Blocks.EMERALD_ORE)
        {
            return rnd.nextInt(3, 7);
        }
        else if (block == Blocks.LAPIS_ORE)
        {
            return rnd.nextInt(2, 5);
        }
        else if (block == Blocks.NETHER_QUARTZ_ORE)
        {
            return rnd.nextInt(2, 5);
        }
        else
        {
            return block == Blocks.NETHER_GOLD_ORE ? rnd.nextInt(0, 1) : 0;
        }
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

        worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        worker.setInvisible(false);
        job.setInNether(false);

        currentRecipeStorage = null;
        StatsUtil.trackStat(building, TRIPS_COMPLETED, 1);

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
            final Optional<PortalShape> ps = PortalShape.findPortalShape(world, portal, p -> p.isValid(), Direction.Axis.X);

            if (!ps.isPresent())
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

    private ItemStack findTool(@NotNull final BlockState target, final BlockPos pos)
    {
        int slotOfStack = getMostEfficientTool(target, pos);
        return slotOfStack < 0 ? ItemStack.EMPTY : worker.getInventoryCitizen().getStackInSlot(slotOfStack);
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

    private void logAllEquipment(@NotNull final ExpeditionLog expeditionLog, final boolean alreadyEquipped)
    {
        if (!alreadyEquipped)
        {
            equipArmor(true);
        }

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

        if (!alreadyEquipped)
        {
            equipArmor(false);
        }
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
     * Attempt to eat to restore some saturation
     */
    protected void attemptToEat()
    {
        final IDeliverable edible = new StackList(getEdiblesList(), "Edible Food", 1);
        final int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(worker, edible::matches);
        if (slot > -1)
        {
            final ItemStack stack = worker.getInventoryCitizen().getStackInSlot(slot);
            SwitchUtils.consumeFoodSwitcher(stack, worker, null);
        }
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
                    ItemStack invItem = findItem(item::test);
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

                final Map<IItemHandler, List<Integer>> items = InventoryUtils.findAllSlotsInProviderWith(building, item::test);
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
            needsCurrently = new Tuple<>(stack -> building.getModule(NETHERMINER_MENU).getMenu().contains(new ItemStorage(stack)), 16);
            return GATHERING_REQUIRED_MATERIALS;
        }
        return getState();
    }
}
