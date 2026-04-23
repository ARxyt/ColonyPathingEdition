package com.arxyt.colonypathingedition.mixins.minecolonies.herder;

import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.compatibility.Compatibility;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.modules.ItemListModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBeekeeper;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobBeekeeper;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkBeekeeper;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Set;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.BuildingConstants.BUILDING_FLOWER_LIST;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_COLLECTED;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkBeekeeper.searchForAnimals;

@Mixin(value = EntityAIWorkBeekeeper.class, remap = false)
public abstract class EntityAIWorkBeekeeperMixin extends AbstractEntityAIInteract<JobBeekeeper, BuildingBeekeeper> {
    @Final @Shadow(remap = false) private static int DECIDING_DELAY;
    @Final @Shadow(remap = false) private static int NO_ANIMALS_DELAY;
    @Final @Shadow(remap = false) private static int NO_HIVES_DELAY;
    @Final @Shadow(remap = false) private static int NO_FLOWERS_DELAY;
    @Final @Shadow(remap = false) private static double EXP_PER_HARVEST;

    @Shadow(remap = false) protected abstract BlockPos getHiveToHarvest();
    @Shadow(remap = false) protected abstract int getBeesInHives();
    @Shadow(remap = false) protected abstract boolean isReadyForBreeding();
    @Shadow(remap = false) public abstract boolean equipTool(InteractionHand hand, EquipmentTypeEntry toolType);
    @Shadow(remap = false) public abstract boolean equipItem(InteractionHand hand, ItemStack itemStack);
    @Shadow(remap = false) protected abstract int getHoneycombsPerHarvest();
    @Shadow(remap = false) protected abstract int getHoneyBottlesPerHarvest();

    @Shadow(remap = false) private boolean lastHarvestedBottle;

    public EntityAIWorkBeekeeperMixin(@NotNull JobBeekeeper job)
    {
        super(job);
    }

    /**
     * @author ARxyt
     * @reason Add penner first.
     */
    @Overwrite(remap = false)
    private IAIState decideWhatToDo()
    {
        setDelay(DECIDING_DELAY + (99 / getSecondarySkillLevel() - 1));

        final Set<BlockPos> hives = building.getHives();

        if (hives.isEmpty())
        {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(NO_HIVES), ChatPriority.BLOCKING));
            setDelay(NO_HIVES_DELAY);
            return DECIDE;
        }

        ItemListModule flowersModule = building.getModuleMatching(ItemListModule.class, m -> m.getId().equals(BUILDING_FLOWER_LIST));
        if (flowersModule.getList().isEmpty() && building.getSetting(BuildingBeekeeper.BREEDING).getValue())
        {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_BEEKEEPER_NOFLOWERS), ChatPriority.BLOCKING));
            setDelay(NO_FLOWERS_DELAY);
            return DECIDE;
        }

        BlockPos hive = getHiveToHarvest();

        if (hive != null)
        {
            return BEEKEEPER_HARVEST;
        }

        final List<Bee> bees = searchForAnimals(world, building);
        for(Bee bee : bees){
            CompoundTag tag = bee.getPersistentData();
            if (!tag.getBoolean("ColonyPathingEdition_Penned")) {
                tag.putBoolean("ColonyPathingEdition_Penned", true);
            }
        }

        final JobBeekeeper job = worker.getCitizenJobHandler().getColonyJob(JobBeekeeper.class);
        assert job != null;
        if (bees.isEmpty())
        {
            if (getBeesInHives() <= 0)
            {
                job.tickNoBees();
                if (job.checkForBeeInteraction())
                {
                    worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(NO_BEES), ChatPriority.BLOCKING));
                }
            }
            else
            {
                job.resetCounter();
            }
            setDelay(NO_ANIMALS_DELAY);
            return DECIDE;
        }
        else
        {
            job.resetCounter();
        }

        if (isReadyForBreeding())
        {
            return HERDER_BREED;
        }

        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason Rework interact with hive.
     */
    @Overwrite(remap = false)
    private IAIState harvestHoney()
    {
        if (building.getHarvestTypes().equals(BuildingBeekeeper.HONEYCOMB) || (building.getHarvestTypes().equals(BuildingBeekeeper.BOTH) && lastHarvestedBottle))
        {
            if (!equipTool(InteractionHand.MAIN_HAND, ModEquipmentTypes.shears.get()))
            {
                return PREPARING;
            }
        }
        else
        {
            if (!equipItem(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE)))
            {
                return PREPARING;
            }
        }
        final BlockPos hive = getHiveToHarvest();

        if (hive == null)
        {
            return DECIDE;
        }

        if (!walkToWorkPos(hive))
        {
            return getState();
        }

        worker.swing(InteractionHand.MAIN_HAND);
        final ItemStack itemStack = worker.getMainHandItem();
        if (!building.getHarvestTypes().equals(BuildingBeekeeper.HONEY) && ModEquipmentTypes.shears.get().checkIsEquipment(itemStack))
        {
            CitizenItemUtils.damageItemInHand(worker, InteractionHand.MAIN_HAND, 1);

            for (ItemStack stackItem : Compatibility.getCombsFromHive(hive, world, getHoneycombsPerHarvest()))
            {
                StatsUtil.trackStatByStack(building, ITEMS_COLLECTED, stackItem, stackItem.getCount());
                InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(stackItem, worker.getItemHandlerCitizen());
            }
            world.setBlockAndUpdate(hive, world.getBlockState(hive).setValue(BlockStateProperties.LEVEL_HONEY, 0));
            worker.getCitizenExperienceHandler().addExperience(EXP_PER_HARVEST);
            lastHarvestedBottle = false;
        }
        else if (!building.getHarvestTypes().equals(BuildingBeekeeper.HONEYCOMB) && itemStack.getItem() == Items.GLASS_BOTTLE)
        {
            int i;
            for (i = 0; i < getHoneyBottlesPerHarvest() && !itemStack.isEmpty(); i++)
            {
                itemStack.shrink(1);
            }
            ItemStack honeyStack = new ItemStack(Items.HONEY_BOTTLE, i);
            StatsUtil.trackStatByStack(building, ITEMS_COLLECTED, honeyStack, honeyStack.getCount());
            InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(honeyStack, worker.getItemHandlerCitizen());
            world.setBlockAndUpdate(hive, world.getBlockState(hive).setValue(BlockStateProperties.LEVEL_HONEY, 0));
            worker.getCitizenExperienceHandler().addExperience(EXP_PER_HARVEST);
            lastHarvestedBottle = true;
        }

        incrementActionsDoneAndDecSaturation();

        return START_WORKING;
    }
}
