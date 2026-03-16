package com.arxyt.colonypathingedition.mixins.minecolonies.netherworker;

import com.arxyt.colonypathingedition.api.AbstractEntityAIBasicExtra;
import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.mixins.minecraft.DamageSourcesAccessor;
import com.google.common.collect.ImmutableList;
import com.minecolonies.api.colony.buildings.modules.ICraftingBuildingModule;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Tool;
import com.minecolonies.api.compatibility.tinkers.TinkersToolHelper;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.*;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.colony.buildings.modules.ExpeditionLogModule;
import com.minecolonies.core.colony.buildings.modules.expedition.ExpeditionLog;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import com.minecolonies.core.items.ItemAdventureToken;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.*;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.GuardConstants.BASE_PHYSICAL_DAMAGE;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_XP_DROPPED;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_DISCOVERED;
import static com.minecolonies.api.util.constant.StatisticsConstants.MINER_DEATHS;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.NETHERMINER_MENU;
import static net.minecraft.world.level.block.BrushableBlock.TICK_DELAY;

@Mixin(value = EntityAIWorkNether.class, remap = false)
public abstract class EntityAIWorkNetherMixin extends AbstractEntityAICrafting<JobNetherWorker, BuildingNetherWorker> implements AbstractEntityAIBasicExtra, DamageSourcesAccessor {
    @Shadow(remap = false) protected abstract void checkAndRequestArmor();
    @Shadow(remap = false) protected abstract IAIState checkAndRequestFood();
    @Shadow(remap = false) protected abstract void goToVault();
    @Shadow(remap = false) protected abstract void logAllEquipment(@NotNull final ExpeditionLog expeditionLog, final boolean alreadyEquipped);
    @Shadow(remap = false) protected abstract void equipArmor(final boolean equip);
    @Shadow(remap = false) protected abstract ItemStack findTool(@NotNull final EquipmentTypeEntry tool);
    @Shadow(remap = false) protected abstract ItemStack findTool(@NotNull final BlockState target, final BlockPos pos);
    @Shadow(remap = false) protected abstract void attemptToEat();
    @Shadow(remap = false) protected abstract int xpOnDrop(Block block);

    @Final @Shadow(remap = false) public List<List<GuardGear>> itemsNeeded ;
    @Final @Shadow(remap = false) private Map<EquipmentSlot, ItemStack> virtualEquipmentSlots;
    @Final @Shadow(remap = false) private static float SECONDARY_DAMAGE_REDUCTION;
    @Final @Shadow(remap = false) List<ItemStack> netherEdible;

    @Unique int timeOutCounter = 0;
    @Unique boolean hasEaten = false;

    public EntityAIWorkNetherMixin(@NotNull JobNetherWorker job) {
        super(job);
        throw new RuntimeException("EntityAIWorkNetherMixin 类不应被实例化！");
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
        List<IRequest<?>> requests = getRequestCannotBeDone().stream().filter(r ->
                          r.getRequester().getLocation().equals(building.getLocation())
                ).toList();
        return checkEmptyEquipmentAvailable(requests);
    }

    /**
     * @author ARxyt
     * @reason Rewrite to ensure it can properly equip armor / pick up food before going to the Nether.
     */
    @Overwrite(remap = false)
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
     * @author ARxyt
     * @reason Reset some data.
     */
    @Overwrite(remap = false)
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
                extraRound = ((JobNetherWorkerExtra)job).setExtraRounds(false);
                return NETHER_AWAY;
            }
            return NETHER_OPENPORTAL;
        }
        worker.getCitizenData().setJobStatus(JobStatus.STUCK);
        return IDLE;
    }

    // Is it an extra round in nether?
    boolean extraRound;

    /**
     * @author ARxyt
     * @reason Rewrite combat with monster; add some extra travels，
     */
    @Overwrite(remap = false)
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

                                // Figure out who gets to hit who this round
                                boolean takeDamage = MineColonies.getConfig().getServer().netherWorkerTakesDamage.get();

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
                                if (takeDamage && !worker.hurt(source, incomingDamage))
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

                                if (worker.getCitizenData().getSaturation() < LOW_SATURATION + 0.2 || worker.getHealth() < worker.getMaxHealth() * 0.2){
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

    /**
     * @author ARxyt
     * @reason WTF?
     */
    @Overwrite(remap = false)
    private List<ItemStack> getEdiblesList()
    {
        final Set<ItemStorage> allowedItems = building.getModule(NETHERMINER_MENU).getMenu();
        netherEdible.removeIf(item -> !allowedItems.contains(new ItemStorage(item)));
        return netherEdible;
    }
}
