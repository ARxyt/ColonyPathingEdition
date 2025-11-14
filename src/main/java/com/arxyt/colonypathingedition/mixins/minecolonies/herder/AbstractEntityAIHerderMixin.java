package com.arxyt.colonypathingedition.mixins.minecolonies.herder;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.costants.states.HerderCheckState;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.StatsUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AnimalHerdingModule;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.herders.AbstractEntityAIHerder;
import com.minecolonies.core.util.citizenutils.CitizenItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Predicate;

import static com.arxyt.colonypathingedition.core.costants.states.HerderCheckState.*;
import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEM_USED;

@Mixin(value = AbstractEntityAIHerder.class, remap = false)
public abstract class AbstractEntityAIHerderMixin<J extends AbstractJob<?, J>, B extends AbstractBuilding>
        extends AbstractEntityAIInteract<J, B>
{
    @Final @Shadow(remap = false) private static int NUM_OF_ANIMALS_TO_BREED;
    @Final @Shadow(remap = false) protected static double XP_PER_ACTION;
    @Final @Shadow(remap = false) private Map<UUID, Long> fedRecently;
    @Shadow(remap = false) protected AnimalHerdingModule current_module;
    @Shadow(remap = false) private int breedTimeOut;

    @Shadow(remap = false) public abstract int getMaxAnimalMultiplier();
    @Shadow(remap = false) public abstract boolean walkingToAnimal(Animal animal);
    @Shadow(remap = false) public abstract List<? extends Animal> searchForAnimals(Predicate<Animal> predicate);
    @Shadow(remap = false) public abstract boolean equipTool(InteractionHand hand, EquipmentTypeEntry toolType);
    @Shadow(remap = false) public abstract boolean equipItem(final InteractionHand hand, final List<ItemStorage> itemStacks);
    @Shadow(remap = false) public abstract List<? extends ItemEntity> searchForItemsInArea();
    @Shadow(remap = false) protected abstract boolean canBreedChildren();


    @Shadow(remap = false)
    protected static boolean isBreedAble(final Animal entity) {
        return !entity.isBaby() && (entity.isInLove() || entity.canFallInLove());
    }

    @Unique final private boolean isMaxAnimalChange = PathingConfig.MAX_ANIMAL_MODIFIER.get();
    @Unique private static final int DECIDING_DELAY = 40;
    @Unique private Animal toKill;
    @Unique private HerderCheckState checkState;
    @Unique private int pickupTimeOut;
    @Unique private List<Animal> toFeedList;
    @Unique private Animal currentFed;

    public AbstractEntityAIHerderMixin(@NotNull J job) {
        super(job);
        throw new RuntimeException("AbstractEntityAIHerderMixin 类不应被实例化！");
    }

    // 修改 registerTargets 中使用的 DECIDING_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 80))
    private static int modifyDecidingDelayInInit(int original) {
        return DECIDING_DELAY;
    }

    // 修改 registerTargets 中使用的 BREEDING_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 40))
    private static int modifyBreedingDelayInInit(int original) {
        return 10;
    }

    // 修改 registerTargets 中使用的 BUTCHER_DELAY
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 20))
    private static int modifyButcherDelayInInit(int original) {
        return 10;
    }

    // 修改 decideWhatToDo 中减少 breedTimeOut 的 DECIDING_DELAY
    @ModifyConstant(method = "decideWhatToDo", constant = @Constant(intValue = 80),remap = false)
    private static int modifyDecidingDelayInDecideWhatToDo(int original) {
        return 10;
    }

    @Inject(method = "prepareForHerding",at=@At("RETURN"),remap = false)
    private void resetStateBeforePrepareForHerding(CallbackInfoReturnable<IAIState> cir){
        if(cir.getReturnValue() == DECIDE){
            checkState = CHECK_PICKUP;
        }
    }

    /**
     * @author ARxyt
     * @reason 重新写一下计算顺序，太诡异了，所有的前置条件和状态转移做的非常奇怪。理应没有冲突，方便起见暂时使用 Overwrite.
     */
    @Overwrite(remap = false)
    public IAIState decideWhatToDo()
    {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);

        if (breedTimeOut > 0)
        {
            breedTimeOut -= (int)Math.round(DECIDING_DELAY * (1 + getPrimarySkillLevel() / 40.0D));
        }
        if (pickupTimeOut > 0)
        {
            pickupTimeOut -= DECIDING_DELAY;
        }

        for (final AnimalHerdingModule module : building.getModulesByType(AnimalHerdingModule.class))
        {
            final List<? extends Animal> animals = searchForAnimals(module::isCompatible);
            if (animals.isEmpty())
            {
                continue;
            }

            current_module = module;

            final boolean hasBreedingItem = InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(),
                    stack -> ItemStackUtils.compareItemStorageListIgnoreStackSize(module.getBreedingItems(), stack)) > 1;

            switch (checkState){
                case CHECK_PICKUP : {
                    if ( pickupTimeOut <= 0 ){
                        pickupTimeOut = 500;
                        if (!searchForItemsInArea().isEmpty()){
                            checkState = CHECK_BREED;
                            return HERDER_PICKUP;
                        }
                    }
                }
                case CHECK_BREED : {
                    if (canBreedChildren() && hasBreedingItem && breedTimeOut <= 0) {
                        int numOfBreedableAnimals = 0;
                        for (final Animal entity : animals) {
                            if (isBreedAble(entity)) {
                                numOfBreedableAnimals++;
                            }
                        }
                        if (numOfBreedableAnimals >= NUM_OF_ANIMALS_TO_BREED) {
                            checkState = CHECK_BABY;
                            return HERDER_BREED;
                        }
                    }
                }
                case CHECK_BABY : {
                    if (hasBreedingItem) {
                        for (final Animal entity : animals) {
                            if (entity.isBaby()) {
                                checkState = CHECK_BUTCHER;
                                toFeedList = null;
                                return HERDER_FEED;
                            }
                        }
                    }
                }
                case CHECK_BUTCHER : {
                    if (ColonyConstants.rand.nextDouble() < chanceToButcher(animals))
                    {
                        return HERDER_BUTCHER;
                    }
                }
                default : {
                    checkState = CHECK_PICKUP;
                }
            }
        }

        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason 写的太狗屎，重构一下
     */
    @Overwrite(remap = false)
    public double chanceToButcher(final List<? extends Animal> allAnimals)
    {
        // 用幂运算替代原先的乘法
        int maxAnimals = (int) Math.pow(getMaxAnimalMultiplier(),building.getBuildingLevel()) + 2;
        int minAnimals = getMaxAnimalMultiplier() * building.getBuildingLevel() + 1;
        if (!isMaxAnimalChange){
            maxAnimals = minAnimals + 1;
        }
        // 如果没开启繁殖设置，且动物总数未超过上限，则不屠宰
        if (!building.getSetting(AbstractBuilding.BREEDING).getValue()
                && allAnimals.size() <= maxAnimals)
        {
            return 0;
        }

        // 统计所有成年动物
        int grownUp = 0;
        for (Animal animal : allAnimals)
        {
            if (!animal.isBaby())
            {
                grownUp++;
            }
        }

        // 成年动物太少时不屠宰
        if (grownUp <= minAnimals)
        {
            return 0;
        }

        return Math.pow(grownUp - minAnimals, 4) / Math.pow(maxAnimals - minAnimals + 1, 4);
    }

    /**
     * @author ARxyt
     * @reason 屠宰的检测/击杀方式/寻路要求改变，理应没有冲突，方便起见暂时使用 Overwrite.
     */
    @Overwrite(remap = false)
    protected IAIState butcherAnimals() {
        if (this.current_module == null)
        {
            return AIWorkerState.DECIDE;
        }

        Objects.requireNonNull(this.current_module); List<? extends Animal> animals = searchForAnimals(this.current_module::isCompatible);

        if (!equipTool(InteractionHand.MAIN_HAND, ModEquipmentTypes.axe.get()))
        {
            return AIWorkerState.START_WORKING;
        }

        if (animals.isEmpty())
        {
            return AIWorkerState.DECIDE;
        }

        BlockPos center = this.worker.blockPosition();
        if (toKill == null || !toKill.isAlive()) {
            animals.sort(Comparator.<Animal>comparingDouble(an -> an.blockPosition().distSqr(center)));
            Animal decideToKill = null;
            for (Animal entity : animals) {
                if (!entity.isBaby() && !entity.isInLove() && (decideToKill == null || decideToKill.getHealth() > entity.getHealth())) {
                    decideToKill = entity;
                }
            }
            toKill = decideToKill;
        }

        if (toKill == null ) {
            return AIWorkerState.DECIDE;
        }

        walkingToAnimal(toKill);
        if (BlockPosUtil.getDistance2D(center,toKill.blockPosition()) < 4 && !ItemStackUtils.isEmpty(this.worker.getMainHandItem())) {
            this.worker.swing(InteractionHand.MAIN_HAND);
            DamageSource ds = toKill.level().damageSources().playerAttack(getFakePlayer());
            toKill.hurt(ds, PathingConfig.BUTCHER_INSTANT_KILL.get()? 999.0F : 3.0F * building.getBuildingLevel());
            CitizenItemUtils.damageItemInHand(this.worker, InteractionHand.MAIN_HAND, 1);
        }

        if (!toKill.isAlive()) {
            StatsUtil.trackStat(this.building, "animals_butchered", 1);
            this.worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
            incrementActionsDoneAndDecSaturation();
            this.fedRecently.remove(toKill.getUUID());
            return AIWorkerState.DECIDE;
        }

        return AIWorkerState.HERDER_BUTCHER;
    }


    /**
     * @author ARxyt
     * @reason 很多设置上的修改，理应没有冲突，方便起见暂时使用 Overwrite.
     */
    @Overwrite(remap = false)
    protected IAIState feedAnimal() {
        if (current_module == null) {
            return DECIDE;
        }

        if (!equipItem(InteractionHand.MAIN_HAND, current_module.getBreedingItems())) {
            return START_WORKING;
        }

        if (toFeedList == null){
            currentFed = null;
            toFeedList = new ArrayList<>();
            List<? extends Animal> animals = searchForAnimals(current_module::isCompatible);
            int canFeedInRow = 1 + getSecondarySkillLevel() / 20;
            for (final Animal animal : animals) {
                if (animal.isBaby() && worker.level().getGameTime() - fedRecently.getOrDefault(animal.getUUID(), 0L) > TICKS_SECOND * 60 * 5) {
                    toFeedList.add(animal);
                    if (toFeedList.size() >= canFeedInRow) {
                        break;
                    }
                }
            }
        }

        if (toFeedList.isEmpty())
        {
            return DECIDE;
        }

        if (currentFed == null){
            currentFed = toFeedList.removeFirst();
        }

        Animal toFeed = currentFed;
        if (!walkingToAnimal(toFeed))
        {
            if (toFeed.isBaby())
            {
                toFeed.ageUp(Math.min(2000, 100 * getPrimarySkillLevel()));
            }

            // Values taken from vanilla.
            worker.swing(InteractionHand.MAIN_HAND);
            StatsUtil.trackStatByName(building, ITEM_USED, worker.getMainHandItem().getItem().getDescriptionId(), 1);
            if (worker.getRandom().nextDouble() > getPrimarySkillLevel() / 198.0D) {
                worker.getMainHandItem().shrink(1);
            }
            worker.getCitizenExperienceHandler().addExperience(XP_PER_ACTION);
            worker.level().broadcastEntityEvent(toFeed, (byte) 18);
            toFeed.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            CitizenItemUtils.removeHeldItem(worker);
            fedRecently.put(toFeed.getUUID(), worker.level().getGameTime());

            currentFed = null;
        }

        worker.decreaseSaturationForContinuousAction();
        return getState();
    }

    /**
     * @author ARxyt
     * @reason Do some change
     */
    @Redirect(
            method = "breedTwoAnimals",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"
            ),
            remap = false
    )
    private void redirectShrink(ItemStack itemStack, int amount)
    {
        if (worker.getRandom().nextDouble() > getPrimarySkillLevel() / 198.0D) {
            itemStack.shrink(amount);
        }
    }
}
