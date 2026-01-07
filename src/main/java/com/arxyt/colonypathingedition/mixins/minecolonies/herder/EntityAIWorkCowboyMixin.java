package com.arxyt.colonypathingedition.mixins.minecolonies.herder;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCowboy;
import com.minecolonies.core.colony.jobs.JobCowboy;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.entity.ai.workers.production.herders.EntityAIWorkCowboy;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.Collections;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.MILKING_ATTEMPTS;

@Mixin(value = EntityAIWorkCowboy.class, remap = false)
public abstract class EntityAIWorkCowboyMixin extends AbstractEntityAIHerder<JobCowboy, BuildingCowboy> {

    @Shadow(remap = false) @Final private static VisibleCitizenStatus HERD_COW;

    @Shadow(remap = false) private int milkCoolDown;

    @Shadow(remap = false) @Final private static int MILK_COOL_DOWN;

    @Shadow(remap = false) private int stewCoolDown;

    public EntityAIWorkCowboyMixin(@NotNull final JobCowboy job)
    {
        super(job);
    }

    @Unique private int milkTimes = 0;
    @Unique private int stewTimes = 0;

    /**
     * @author ARxyt
     * @reason Weird judgement.
     */
    @Overwrite(remap = false)
    private IAIState milkCows()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);

        if (!worker.getCitizenInventoryHandler().hasItemInInventory(building.getMilkInputItem().getItem()))
        {
            if (InventoryUtils.hasBuildingEnoughElseCount(building, new ItemStorage(building.getMilkInputItem()), 1) > 0
                    && walkToBuilding())
            {
                checkAndTransferFromHut(building.getMilkInputItem());
            }
            else
            {
                milkCoolDown = MILK_COOL_DOWN;
                return DECIDE;
            }
        }

        final Cow cow = searchForAnimals(a -> a instanceof Cow && !(a instanceof MushroomCow) && !a.isBaby()).stream()
                .map(a -> (Cow) a).findFirst().orElse(null);

        if (cow == null)
        {
            milkCoolDown = MILK_COOL_DOWN;
            return DECIDE;
        }

        walkingToAnimal(cow);

        if (equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(new ItemStorage(building.getMilkInputItem().getItem(), building.getMilkInputItem().getCount()))))
        {
            incrementActionsDoneAndDecSaturation();
            StatsUtil.trackStat(building, MILKING_ATTEMPTS, 1);
            worker.getCitizenExperienceHandler().addExperience(1.0);
            if (InventoryUtils.addItemStackToItemHandler(worker.getInventoryCitizen(), building.getMilkOutputItem()))
            {
                building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).onMilked();
                CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, getItemSlot(building.getMilkOutputItem().getItem()));
                InventoryUtils.tryRemoveStackFromItemHandler(worker.getInventoryCitizen(), building.getMilkInputItem());
            }
            else{
                milkTimes = 0;
                return INVENTORY_FULL;
            }
            if(++milkTimes > getPrimarySkillLevel() / 10 || !building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToMilk()){
                milkTimes = 0;
                return INVENTORY_FULL;
            }
            else{
                setDelay(10);
                return COWBOY_MILK;
            }
        }
        return DECIDE;
    }

    /**
     * @author ARxyt
     * @reason Weird judgement.
     */
    @Overwrite(remap = false)
    private IAIState milkMooshrooms()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);

        if (!worker.getCitizenInventoryHandler().hasItemInInventory(Items.BOWL))
        {
            if (InventoryUtils.hasBuildingEnoughElseCount(building, new ItemStorage(new ItemStack(Items.BOWL, 1)), 1) > 0
                    && walkToBuilding())
            {
                checkAndTransferFromHut(new ItemStack(Items.BOWL, 1));
            }
            else
            {
                stewCoolDown = MILK_COOL_DOWN;
                return DECIDE;
            }
        }

        final MushroomCow mooshroom = searchForAnimals(a -> a instanceof MushroomCow && !a.isBaby()).stream()
                .map(a -> (MushroomCow) a).findFirst().orElse(null);

        if (mooshroom == null)
        {
            stewCoolDown = MILK_COOL_DOWN;
            return DECIDE;
        }

        walkingToAnimal(mooshroom);

        if (equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(new ItemStorage(Items.BOWL))))
        {
            final FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) worker.level());
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
            incrementActionsDoneAndDecSaturation();
            StatsUtil.trackStat(building, MILKING_ATTEMPTS, 1);
            worker.getCitizenExperienceHandler().addExperience(1.0);
            if (mooshroom.mobInteract(fakePlayer, InteractionHand.MAIN_HAND).equals(InteractionResult.CONSUME))
            {
                if (InventoryUtils.addItemStackToItemHandler(worker.getInventoryCitizen(), fakePlayer.getMainHandItem()))
                {
                    building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).onStewed();
                    CitizenItemUtils.setHeldItem(worker, InteractionHand.MAIN_HAND, getItemSlot(fakePlayer.getMainHandItem().getItem()));
                    InventoryUtils.tryRemoveStackFromItemHandler(worker.getInventoryCitizen(), new ItemStack(Items.BOWL));
                }
                else{
                    stewTimes = 0;
                    return INVENTORY_FULL;
                }
            }
            if(++ stewTimes > getPrimarySkillLevel() / 10 || !building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToStew()) {
                stewTimes = 0;
                return INVENTORY_FULL;
            }
            else{
                setDelay(10);
                return COWBOY_STEW;
            }
        }
        return DECIDE;
    }
}
