package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.modules.EnchanterStationsModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingEnchanter;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobEnchanter;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkEnchanter;
import com.minecolonies.core.network.messages.client.CircleParticleEffectMessage;
import com.minecolonies.core.network.messages.client.StreamParticleEffectMessage;
import com.minecolonies.core.util.WorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.CITIZENS_VISITED;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_WORKERS_TO_DRAIN_SET;


@Mixin(value = EntityAIWorkEnchanter.class, remap = false)
public abstract class EntityAIWorkEnchanterMixin extends AbstractEntityAICrafting<JobEnchanter, BuildingEnchanter>
{
    @Shadow(remap = false) @Final private static int MANA_REQ_PER_LEVEL;
    @Shadow(remap = false) @Final private static Predicate<ItemStack> IS_BOOK;
    @Shadow(remap = false) @Final private static Predicate<ItemStack> IS_ANCIENT_TOME;

    @Shadow(remap = false) private int progressTicks;

    @Shadow(remap = false) private ICitizenData citizenToGatherFrom;

    @Shadow(remap = false) protected abstract void resetDraining();

    @Shadow(remap = false) @Final private static long MIN_DISTANCE_TO_DRAIN;
    @Shadow(remap = false) @Final private static int MAX_PROGRESS_TICKS;
    @Shadow(remap = false) @Final private static double XP_PER_DRAIN;
    @Unique boolean shouldGain = false;
    @Unique boolean gatherWithoutCitizen = false;

    public EntityAIWorkEnchanterMixin(@NotNull final JobEnchanter job)
    {
        super(job);
    }

    @Override
    public boolean hasWorkToDo()
    {
        return true;
    }

    /**
     * @author ARxyt
     * @reason It's really weird, thus several changes are made.
     */
    @Overwrite(remap = false)
    protected IAIState decide()
    {
        worker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (!walkToBuilding())
        {
            return START_WORKING;
        }

        final IAIState craftState = getNextCraftingState();
        // no more time limit
        if (craftState != IDLE)
        {
            return craftState;
        }

        if (wantInventoryDumped())
        {
            // Wait to dump before continuing.
            return getState();
        }

        // loosen level limit
        if (getPrimarySkillLevel() < MANA_REQ_PER_LEVEL || (shouldGain && getPrimarySkillLevel() < building.getBuildingLevel() * 15))
        {
            final EnchanterStationsModule module = building.getModule(BuildingModules.ENCHANTER_STATIONS);

            // request books first, and request more.
            final int booksInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), IS_BOOK);
            if (booksInInv <= 0)
            {
                final int numberOfBooksInBuilding = InventoryUtils.hasBuildingEnoughElseCount(building, IS_BOOK, 1);
                if (numberOfBooksInBuilding > 0)
                {
                    needsCurrently = new Tuple<>(IS_BOOK, 1);
                    return GATHERING_REQUIRED_MATERIALS;
                }
                checkIfRequestForItemExistOrCreateAsync(new ItemStack(Items.BOOK, 1), 8, 1, false);
            }

            // as "getBuildingsToGatherFrom" is broken, we gain mana at it own work place instead.
            BlockPos posToDrainFrom;
            if (module.getBuildingsToGatherFrom().isEmpty())
            {
                if (worker.getCitizenData() != null)
                {
                    worker.getCitizenData()
                            .triggerInteraction(new StandardInteraction(Component.translatable(NO_WORKERS_TO_DRAIN_SET), ChatPriority.BLOCKING));
                }
                posToDrainFrom = building.getPosition();
            }
            else{
                posToDrainFrom = module.getRandomBuildingToDrainFrom();
                if (posToDrainFrom == null)
                {
                    posToDrainFrom = building.getPosition();
                }
            }
            job.setBuildingToDrainFrom(posToDrainFrom);
            shouldGain = false;
            return ENCHANTER_DRAIN;
        }

        final BuildingEnchanter.@NotNull CraftingModule craftingModule = building.getFirstModuleOccurance(BuildingEnchanter.CraftingModule.class);
        boolean ancientTomeCraftingDisabled = false;
        for (final IToken<?> token : craftingModule.getRecipes())
        {
            final IRecipeStorage storage = IColonyManager.getInstance().getRecipeManager().getRecipes().get(token);
            if (storage != null && !storage.getInput().isEmpty() && storage.getInput().getFirst().getItem() == ModItems.ancientTome && craftingModule.isDisabled(token))
            {
                ancientTomeCraftingDisabled = true;
            }
        }

        if (!ancientTomeCraftingDisabled)
        {
            // request more.
            final int ancientTomesInInv = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), IS_ANCIENT_TOME);
            if (ancientTomesInInv <= 0)
            {
                final int amountOfAncientTomes = InventoryUtils.hasBuildingEnoughElseCount(building, IS_ANCIENT_TOME, 1);
                if (amountOfAncientTomes > 0)
                {
                    needsCurrently = new Tuple<>(IS_ANCIENT_TOME, 1);
                    return GATHERING_REQUIRED_MATERIALS;
                }
                checkIfRequestForItemExistOrCreateAsync(new ItemStack(ModItems.ancientTome, 1), 8, 1, false);
                shouldGain = true;
                return IDLE;
            }
        }

        return ENCHANT;
    }

    /**
     * @author ARxyt
     * @reason Some changes deal with no worker at work. Boost bonus.
     */
    @Overwrite(remap = false)
    private IAIState gatherAndDrain()
    {
        if (job.getPosToDrainFrom() == null)
        {
            return IDLE;
        }

        final IBuilding buildingWorker = building.getColony().getBuildingManager().getBuilding(job.getPosToDrainFrom());
        if (!walkToBuilding(buildingWorker))
        {
            return getState();
        }

        if (buildingWorker == null)
        {
            resetDraining();
            building.getFirstModuleOccurance(EnchanterStationsModule.class).removeWorker(job.getPosToDrainFrom());
            return IDLE;
        }

        if (citizenToGatherFrom == null)
        {
            final List<AbstractEntityCitizen> workers = new ArrayList<>();
            for (final Optional<AbstractEntityCitizen> citizen : Objects.requireNonNull(getModuleForJob().getAssignedEntities()))
            {
                citizen.ifPresent(workers::add);
            }

            final AbstractEntityCitizen citizen;
            if (workers.size() > 1)
            {
                citizen = workers.get(worker.getRandom().nextInt(workers.size()));
            }
            else
            {
                if (workers.isEmpty())
                {
                    resetDraining();
                    return START_WORKING;
                }
                citizen = workers.getFirst();
            }

            citizenToGatherFrom = citizen.getCitizenData();
            progressTicks = 0;
            return getState();
        }

        if (citizenToGatherFrom.getEntity().isEmpty())
        {
            citizenToGatherFrom = null;
            return getState();
        }

        if (progressTicks == 0)
        {
            gatherWithoutCitizen = false;
            // If worker is too far away wait.
            if (BlockPosUtil.getDistance2D(citizenToGatherFrom.getEntity().get().blockPosition(), worker.blockPosition()) > MIN_DISTANCE_TO_DRAIN)
            {
                if (job.incrementWaitingTicks())
                {
                    return getState();
                }
                gatherWithoutCitizen = true;
            }
        }

        progressTicks++;
        if (progressTicks < MAX_PROGRESS_TICKS)
        {
            final Vec3 start = worker.position().add(0, 2, 0);
            final Vec3 goal;
            if (gatherWithoutCitizen){
                goal = job.getPosToDrainFrom().getCenter().add(0, 2, 0);
            }
            else {
                goal = citizenToGatherFrom.getEntity().get().position().add(0, 2, 0);
            }

            new StreamParticleEffectMessage(start, goal, ParticleTypes.ENCHANT, progressTicks % MAX_PROGRESS_TICKS, MAX_PROGRESS_TICKS)
                    .sendToTrackingEntity(worker);

            new CircleParticleEffectMessage(start, ParticleTypes.HAPPY_VILLAGER, progressTicks).sendToTrackingEntity(worker);

            WorkerUtil.faceBlock(BlockPos.containing(goal), worker);

            if (worker.getRandom().nextBoolean())
            {
                worker.swing(InteractionHand.MAIN_HAND);
            }
            else
            {
                worker.swing(InteractionHand.OFF_HAND);
            }

            return getState();
        }

        final int bookSlot = InventoryUtils.findFirstSlotInItemHandlerWith(worker.getInventoryCitizen(), Items.BOOK);
        if (bookSlot != -1 && !gatherWithoutCitizen)
        {
            final int size = citizenToGatherFrom.getInventory().getSlots();
            final int attempts = (int) (getSecondarySkillLevel() / 5.0);

            for (int i = 0; i < attempts; i++)
            {
                int randomSlot = worker.getRandom().nextInt(size);
                final ItemStack stack = citizenToGatherFrom.getInventory().getStackInSlot(randomSlot);
                if (!stack.isEmpty() && stack.isEnchantable())
                {
                    EnchantmentHelper.enchantItem(worker.getRandom(), stack, getSecondarySkillLevel() > 50 ? 2 : 1, world.registryAccess(), Optional.empty());
                    worker.getInventoryCitizen().extractItem(bookSlot, 1, false);
                    break;
                }
            }

            if(job.getPosToDrainFrom() == building.getPosition()){
                worker.getCitizenData().getCitizenSkillHandler().incrementLevel(Skill.Mana, building.getBuildingLevel());
            }
            else {
                worker.getCitizenData().getCitizenSkillHandler().incrementLevel(Skill.Mana, building.getBuildingLevel() * 2);
            }
            worker.getCitizenExperienceHandler().addExperience(XP_PER_DRAIN);
            worker.getCitizenData().markDirty(80);
            StatsUtil.trackStat(building, CITIZENS_VISITED, 1);
        }
        else{
            worker.getCitizenData().getCitizenSkillHandler().incrementLevel(Skill.Mana, 1);
            worker.getCitizenData().markDirty(80);
        }
        resetDraining();
        return IDLE;
    }
}
