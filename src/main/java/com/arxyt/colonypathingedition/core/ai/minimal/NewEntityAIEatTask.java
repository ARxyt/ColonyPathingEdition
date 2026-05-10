package com.arxyt.colonypathingedition.core.ai.minimal;

import com.arxyt.colonypathingedition.api.workersetting.BuildingCookExtra;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.minecolonies.FoodUtilExtra;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.TickingTransition;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.Network;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.other.SittingEntity;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.network.messages.client.ItemParticleEffectMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIEatTask.NewEatingState.*;
import static com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIEatTask.EatingCheckState.*;
import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.JOBS_EAT_IMMEDIATELY;
import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.JOBS_FORCE_EAT_AT_HUT;
import static com.arxyt.colonypathingedition.core.minecolonies.FoodUtilExtra.getShouldEatAtHut;
import static com.minecolonies.api.util.constant.CitizenConstants.FULL_SATURATION;
import static com.minecolonies.api.util.constant.CitizenConstants.NIGHT;
import static com.minecolonies.api.util.constant.GuardConstants.BASIC_VOLUME;
import static com.minecolonies.api.util.constant.TranslationConstants.NO_RESTAURANT;

public class NewEntityAIEatTask implements IStateAI {

    private static final Predicate<BuildingCook> STAFFED_RESTAURANTS = buildingCook -> buildingCook.getModule(BuildingModules.COOK_WORK).hasAssignedCitizen();
    private final double WAITING_MINUTES = PathingConfig.RESTAURANT_WAITING_TIME.get();
    private static final int REQUIRED_TIME_TO_EAT = 5;
    private static final int IMMEDIATELY_DELAY = 1;
    private static final int WAITING_DELAY = 10;
    private static final int WALKING_DELAY = 20;
    private static final int STUCK_DELAY = 100;
    private static final int MAX_SCORE_DISTANCE = 200;
    private static final int CROWD_PENALTY = 10;
    private IBuilding buildingToGo = null;

    public enum NewEatingState implements IState
    {
        CHECK_FOOD,
        GO_TO_HUT,
        GO_TO_RESTAURANT,
        WAIT_FOR_FOOD,
        GET_FOOD_YOURSELF,
        GO_TO_EAT_POS,
        EAT,
        DONE
    }

    public enum EatingCheckState {
        CHECK_INHAND,
        CHECK_HUT,
        CHECK_RESTAURANT
    }
    /**
     * The citizen assigned to this task.
     */
    private final EntityCitizen citizen;
    private EatingCheckState checkState;
    private IBuilding restaurant = null;
    private BlockPos restaurantPos = null;
    private BlockPos eatPos = null;
    private int timeOutWalking = 0;
    private int waitingTicks = 0;
    private int foodSlot = -1;
    private final Set<Item> eatenFood = new LinkedHashSet<>();

    /**
     * Instantiates this task.
     *
     * @param citizen the citizen.
     */
    public NewEntityAIEatTask(final EntityCitizen citizen)
    {
        super();
        this.citizen = citizen;

        citizen.getCitizenAI().addTransition(new TickingTransition<>(CitizenAIState.EATING, () -> true, this::startEating, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(CHECK_FOOD, () -> true, this::checkFood, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_HUT, () -> true, this::goToHut, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_RESTAURANT, () -> true, this::goToRestaurant, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(WAIT_FOR_FOOD, () -> true, this::waitForFood, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GET_FOOD_YOURSELF, () -> true, this::getFoodYourself, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(GO_TO_EAT_POS, () -> true, this::goToEatingPlace, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(EAT, () -> true, this::eat, 1));
        citizen.getCitizenAI().addTransition(new TickingTransition<>(DONE, () -> true, this::endEating, 1));
    }

    /**
     * Tool fuctions
     */

    private void reset(){
        citizen.releaseUsingItem();
        citizen.stopUsingItem();
        citizen.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        citizen.getCitizenData().setVisibleStatus(null);
        checkState = CHECK_INHAND;
        timeOutWalking = 0;
        waitingTicks = 0;
        restaurantPos = null;
        restaurant = null;
        eatPos = null;
        foodSlot = -1;
        eatenFood.clear();
        buildingToGo = null;
    }

    private boolean hasFood(boolean needRestaurantCheck){
        final int slot = FoodUtilExtra.getBestFoodForCitizenWithRestaurantCheck(citizen.getInventoryCitizen(), citizen.getCitizenData(), null ,needRestaurantCheck);
        if(slot != -1) {
            foodSlot = slot;
            return true;
        }
        return false;
    }

    private BlockPos findPlaceToEat()
    {
        if (restaurantPos != null)
        {
            final IBuilding restaurant = citizen.getCitizenData().getColony().getServerBuildingManager().getBuilding(restaurantPos);
            if (restaurant instanceof BuildingCook)
            {
                final BlockPos sitting = ((BuildingCook) restaurant).getNextSittingPosition();
                if(sitting == null || restaurant.isInBuilding(sitting)){
                    return sitting;
                }
            }
        }
        return null;
    }

    /**
     * AI transports
     */

    private NewEatingState startEating(){
        reset();
        citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.EAT);
        final ICitizenData citizenData = citizen.getCitizenData();
        final IJob<?> job = citizen.getCitizenJobHandler().getColonyJob();
        if (job != null && citizenData.isWorking())
        {
            citizenData.setWorking(false);
        }
        return CHECK_FOOD;
    }

    private NewEatingState checkFood(){
        switch (checkState) {
            case CHECK_INHAND : {
                if (hasFood(true)) {
                    citizen.getCitizenAI().setCurrentDelay(IMMEDIATELY_DELAY);
                    return EAT;
                }
                checkState = CHECK_HUT;
            }
            case CHECK_HUT : {
                final ICitizenData citizenData = citizen.getCitizenData();
                final IBuilding buildingWorker = citizenData.getWorkBuilding();
                if (buildingWorker == null) {
                    return GO_TO_RESTAURANT;
                }
                final IColony colony = citizenData.getColony();
                final BlockPos bestRestaurantPos = colony.getServerBuildingManager().getBestBuilding(citizen, BuildingCook.class);
                final BlockPos citizenPos = citizen.blockPosition();
                BlockPos buildingPos = buildingWorker.getPosition();
                IBuilding buildingToCheck = buildingWorker;
                if (PathingConfig.DELIVERY_EAT_AT_WAREHOUSE.get() && buildingWorker instanceof BuildingDeliveryman){
                    BlockPos alterBuildingPos = colony.getServerBuildingManager().getBestBuilding(citizen, BuildingWareHouse.class);
                    if(alterBuildingPos != null) {
                        buildingPos = alterBuildingPos;
                        buildingToCheck = colony.getServerBuildingManager().getBuilding(alterBuildingPos);
                    }
                }
                if(buildingToCheck == null){
                    return  GO_TO_RESTAURANT;
                }
                buildingToGo = buildingToCheck;
                // For citizens working outside their work huts, maybe more efficient to eat nearby.
                // Chefs should eat at their workplace more often, as they are producers of food.
                if ( bestRestaurantPos == null || BlockPosUtil.dist(citizenPos, buildingPos) < BlockPosUtil.dist(citizenPos, bestRestaurantPos) || (citizenData.getJob() != null && JOBS_FORCE_EAT_AT_HUT.contains(citizenData.getJob().getClass()))) {
                    final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(citizen.getCitizenData(), null, buildingToCheck);
                    if (storageToGet != null) {
                        boolean niceFood = getShouldEatAtHut(citizenData, storageToGet.getItem());
                        if (niceFood) {
                            return GO_TO_HUT;
                        }
                    }
                }
                return GO_TO_RESTAURANT;
            }
            case CHECK_RESTAURANT : {
                // There should be some complex simulation to find the best restaurant.
                // We get those restaurants that close enough (<=200 blocks) to citizen.
                // Then calculate score, for example we now uses distance + min(customer,0) * 10.
                // Citizen will go to the restaurant with minimum score.
                // If there doesn't have qualified restaurant, drop back to original.
                final ICitizenData citizenData = citizen.getCitizenData();
                final IColony colony = citizenData.getColony();
                final Map<BlockPos,BuildingCook> alteredRestaurantPos = colony.getServerBuildingManager().getBuildings().entrySet()
                        .stream()
                        .filter(e -> e.getValue() instanceof BuildingCook cook && cook.getBuildingLevel() > 0)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> (BuildingCook) e.getValue()
                        ));
                final BlockPos citizenPos = citizen.getOnPos();
                restaurantPos = alteredRestaurantPos.entrySet()
                        .stream()
                        .filter(e -> e.getKey().distManhattan(citizen.getOnPos()) <= MAX_SCORE_DISTANCE)
                        .min(Comparator.comparingInt(e -> {
                            int distance = e.getKey().distManhattan(citizenPos);
                            int people = ((BuildingCookExtra)(e.getValue())).getCustomerCount(); // 实际方法名
                            return distance + Math.max(people - 5, 0) * CROWD_PENALTY;
                        }))
                        .map(Map.Entry::getKey)
                        .orElse(
                            alteredRestaurantPos.entrySet()
                            .stream()
                            .min(Comparator.comparingInt(e -> e.getKey().distManhattan(citizen.getOnPos())))
                            .map(Map.Entry::getKey)
                            .orElse(null)
                        );
                if(restaurantPos != null){
                    final IBuilding building = Objects.requireNonNull(citizen.getCitizenColonyHandler().getColonyOrRegister()).getServerBuildingManager().getBuilding(restaurantPos);
                    if(building instanceof BuildingCook cook){
                        ((BuildingCookExtra)(cook)).preorderTable(citizen.getCivilianID());
                    }
                }
                return GO_TO_RESTAURANT;
            }
        }
        return GO_TO_RESTAURANT;
    }

    private NewEatingState goToHut(){
        restaurantPos = null;
        restaurant = null;
        if(buildingToGo == null){
            return GO_TO_RESTAURANT;
        }
        if (!EntityNavigationUtils.walkToBuilding(citizen, buildingToGo)) {
            // adding some speed if starved.
            MobEffectInstance effectInstance = citizen.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if(effectInstance != null){
                citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,effectInstance.getDuration(),3));
            }
            citizen.getCitizenAI().setCurrentDelay(WALKING_DELAY);
            return GO_TO_HUT;
        }
        final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(citizen.getCitizenData(), null, buildingToGo);
        if (storageToGet != null)
        {
            // When restaurants out of food, would trigger "Force Eat At Hut".
            // Worker would return to work hut to eat, regardless of food condition.
            // If there isn't food at hut, go back to restaurants to wait for player.
            int qty = ((int) ((FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(storageToGet.getItemStack(), citizen))) + 1;
            if(InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(buildingToGo, storageToGet, qty, citizen.getInventoryCitizen())) {
                return EAT;
            }
        }
        return GO_TO_RESTAURANT;
    }

    private NewEatingState goToRestaurant() {
        final ICitizenData citizenData = citizen.getCitizenData();
        if(restaurantPos == null){
            checkState = CHECK_RESTAURANT;
            checkFood();
            if (restaurantPos == null)
            {
                if (citizen.getCitizenData().getSaturation() >= CitizenConstants.AVERAGE_SATURATION)
                {
                    reset();
                    citizenData.setJustAte(true);
                    return DONE;
                }
                citizenData.triggerInteraction(new StandardInteraction(Component.translatable(NO_RESTAURANT), ChatPriority.BLOCKING));
                citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
                checkState = CHECK_INHAND;
                return checkFood();
            }
        }
        final IBuilding building = Objects.requireNonNull(citizen.getCitizenColonyHandler().getColonyOrRegister()).getServerBuildingManager().getBuilding(restaurantPos);
        if (building != null)
        {
            if (building.isInBuilding(citizen.blockPosition()))
            {
                ((BuildingCookExtra)building).tryRegisterCustomer(citizen.getCivilianID());
                return WAIT_FOR_FOOD;
            }
            else if (!EntityNavigationUtils.walkToBuilding(citizen, building))
            {
                // adding some speed if starved.
                MobEffectInstance effectInstance = citizen.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                if(effectInstance != null){
                    citizen.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,effectInstance.getDuration(),3));
                }
            }
        }
        citizen.getCitizenAI().setCurrentDelay(WALKING_DELAY);
        return GO_TO_RESTAURANT;
    }

    private NewEatingState waitForFood()
    {
        final ICitizenData citizenData = citizen.getCitizenData();
        final IColony colony = citizenData.getColony();
        restaurantPos = colony.getServerBuildingManager().getBestBuilding(citizen, BuildingCook.class);

        if (restaurantPos == null)
        {
            citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
            return GO_TO_RESTAURANT;
        }

        restaurant = colony.getServerBuildingManager().getBuilding(restaurantPos);
        eatPos = findPlaceToEat();
        if (restaurant != null)
        {
            timeOutWalking = 0;
            return GO_TO_EAT_POS;
        }

        if (hasFood(true))
        {
            return EAT;
        }

        citizen.getCitizenAI().setCurrentDelay(WALKING_DELAY);
        return WAIT_FOR_FOOD;
    }

    private NewEatingState getFoodYourself()
    {
        if (restaurantPos == null)
        {
            citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
            return GO_TO_RESTAURANT;
        }

        final IColony colony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        assert colony != null;
        final IBuilding cookBuilding = colony.getServerBuildingManager().getBuilding(restaurantPos);
        if (cookBuilding instanceof BuildingCook)
        {
            if (!EntityNavigationUtils.walkToBuilding(citizen, cookBuilding))
            {
                citizen.getCitizenAI().setCurrentDelay(WALKING_DELAY);
                return GET_FOOD_YOURSELF;
            }

            final ItemStorage storageToGet = FoodUtils.checkForFoodInBuilding(citizen.getCitizenData(), null, cookBuilding);
            if (storageToGet != null)
            {
                int qty = ((int) ((FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(storageToGet.getItemStack(), citizen))) + 1;
                if(!InventoryUtils.transferItemStackIntoNextBestSlotInItemHandler(cookBuilding, storageToGet, qty, citizen.getInventoryCitizen())){
                    // This caused by a fulfilled inventory, which means citizens can't eat by themselves, so reset to seek an assist.
                    BuildingCookExtra restaurantExtra = ((BuildingCookExtra)restaurant);
                    restaurantExtra.tryRegisterCustomer(citizen.getCivilianID());
                    timeOutWalking = 0;
                    waitingTicks = -2400; // Wait for one more minute.
                    citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
                    return WAIT_FOR_FOOD;
                }
                return EAT;
            }
            else{
                final ICitizenData citizenData = citizen.getCitizenData();
                if (citizenData.getJob() instanceof JobCook jobCook && jobCook.getBuildingPos().equals(restaurantPos))
                {
                    reset();
                    return DONE;
                }
                checkState = CHECK_HUT;
                checkFood();
                if(buildingToGo != null) {
                    return GO_TO_HUT;
                }
                else {
                    citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
                    return GO_TO_RESTAURANT;
                }
            }
        }
        citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
        return GO_TO_RESTAURANT;
    }

    private NewEatingState goToEatingPlace()
    {
        IJob<?> jobCitizen = citizen.getCitizenData().getJob();
        BuildingCookExtra restaurantExtra = ((BuildingCookExtra)restaurant);
        if(!restaurantExtra.checkCustomerRegistry(citizen.getCivilianID()) || !STAFFED_RESTAURANTS.test((BuildingCook)restaurant) || !WorldUtil.isPastTime(citizen.level(), NIGHT - 2100) || (jobCitizen != null && JOBS_EAT_IMMEDIATELY.contains(jobCitizen.getClass())) || eatPos == null){
            restaurantExtra.deleteCustomer(citizen.getCivilianID());
            waitingTicks = 0;
            timeOutWalking = 0;
            if (hasFood(false)) return EAT;
            else return GET_FOOD_YOURSELF;
        }

        // A state reset if they are full.
        if(citizen.getCitizenData().getSaturation() == FULL_SATURATION){
            reset();
            return DONE;
        }

        if(hasFood(true)) {
            waitingTicks = 0;
            timeOutWalking = 0;
            restaurantExtra.deleteCustomer(citizen.getCivilianID());
            return EAT;
        }

        if (timeOutWalking >= 10) // seconds
        {
            waitingTicks = 0;
            timeOutWalking = 0;
            restaurantExtra.deleteCustomer(citizen.getCivilianID());
            return GET_FOOD_YOURSELF;
        }

        if (EntityNavigationUtils.walkToPos(citizen, eatPos, 2, true))
        {
            SittingEntity.sitDown(eatPos, citizen, (int)(1200 * WAITING_MINUTES));
            if (waitingTicks >= 1200 * WAITING_MINUTES) // ticks
            {
                waitingTicks = 0;
                timeOutWalking = 0;
                restaurantExtra.deleteCustomer(citizen.getCivilianID());
                return GET_FOOD_YOURSELF;
            }
            waitingTicks += WAITING_DELAY;
            citizen.getCitizenAI().setCurrentDelay(WAITING_DELAY);
            return GO_TO_EAT_POS;
        }
        timeOutWalking++;
        citizen.getCitizenAI().setCurrentDelay(WALKING_DELAY);
        return GO_TO_EAT_POS;
    }

    private IState eat()
    {
        if (!hasFood(false))
        {
            citizen.getCitizenAI().setCurrentDelay(STUCK_DELAY);
            return CHECK_FOOD;
        }

        final ICitizenData citizenData = citizen.getCitizenData();
        final ItemStack foodStack = citizenData.getInventory().getStackInSlot(foodSlot);

        citizen.setItemInHand(InteractionHand.MAIN_HAND, foodStack);
        citizen.swing(InteractionHand.MAIN_HAND);
        citizen.playSound(SoundEvents.GENERIC_EAT, (float) BASIC_VOLUME, (float) SoundUtils.getRandomPitch(citizen.getRandom()));
        Network.getNetwork()
                .sendToTrackingEntity(new ItemParticleEffectMessage(citizen.getMainHandItem(),
                        citizen.getX(),
                        citizen.getY(),
                        citizen.getZ(),
                        citizen.getXRot(),
                        citizen.getYRot(),
                        citizen.getEyeHeight()), citizen);

        if (++waitingTicks < REQUIRED_TIME_TO_EAT)
        {
            citizen.getCitizenAI().setCurrentDelay(WAITING_DELAY);
            return EAT;
        }

        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        if (eatenFood.isEmpty())
        {
            foodHandler.addLastEaten(foodStack.getItem());
        }
        eatenFood.add(foodStack.getItem());

        ItemStackUtils.consumeFood(foodStack, citizen, null);
        citizen.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        if (citizenData.getSaturation() < FULL_SATURATION && !citizenData.getInventory().getStackInSlot(foodSlot).isEmpty())
        {
            waitingTicks = 0;
            return EAT;
        }

        for (final Item foodItem : eatenFood)
        {
            if (foodHandler.getLastEaten() != foodItem)
            {
                foodHandler.addLastEaten(foodItem);
            }
        }
        eatenFood.clear();
        citizenData.setJustAte(true);
        reset();
        return DONE;
    }

    private IState endEating(){
        if (citizen.getCitizenJobHandler().getColonyJob() != null) {
            citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
            return CitizenAIState.WORK;
        }
        citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.HOUSE);
        return CitizenAIState.IDLE;
    }
}
