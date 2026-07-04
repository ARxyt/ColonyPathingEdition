package com.arxyt.colonypathingedition.core.ai.worker;

import com.arxyt.colonypathingedition.api.workersetting.BuildingCookExtra;
import com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIEatTask;
import com.arxyt.colonypathingedition.core.util.ExtraFoodUtils;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.colony.requestsystem.requestable.Food;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIUsesFurnace;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.FULL_SATURATION;
import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_SERVED;
import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_SERVED_DETAIL;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.RESTAURANT_MENU;

public class NewEntityAIWorkCook extends AbstractEntityAIUsesFurnace<JobCook, BuildingCook>
{
    /**
     * The amount of food which should be served to the worker.
     */
    public static final int SATURATION_TO_SERVE = 16;

    /**
     * Delay between each serving.
     */
    private static final int SERVE_DELAY = 30;

    /**
     * The citizen the worker is currently trying to serve.
     */
    private final Queue<AbstractEntityCitizen> citizenToServe = new ArrayDeque<>();

    /**
     * The citizen the worker is currently trying to serve.
     */
    private final Queue<Player> playerToServe = new ArrayDeque<>();

    /**
     * Cooking icon
     */
    private final static VisibleCitizenStatus COOK =
            new VisibleCitizenStatus(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/work/cook.png"), "com.minecolonies.gui.visiblestatus.cook");

    /**
     * Initial all the citizen may should be served
     */
    private Queue<Integer> initailCitizenToServe = new ArrayDeque<>();

    /**
     * Xp give per action
     */
    private static final double BASE_XP_GAIN = 2;

    /**
     * If cook check customer
     */
    boolean checkCustomer = true;

    /**
     * A directly return to serve citizens after pick up
     */
    AIWorkerState withSpecialReturn = START_WORKING;

    /**
     * Constructor for the Cook. Defines the tasks the cook executes.
     *
     * @param job a cook job to use.
     */
    public NewEntityAIWorkCook(@NotNull final JobCook job)
    {
        super(job);
        super.registerTargets(
                new AITarget(COOK_SERVE_FOOD_TO_CITIZEN, this::serveFoodToCitizen, SERVE_DELAY),
                new AITarget(COOK_SERVE_FOOD_TO_PLAYER, this::serveFoodToPlayer, SERVE_DELAY)
        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        AIWorkerState returnState = withSpecialReturn;
        withSpecialReturn = START_WORKING;
        return returnState;
    }

    @Override
    public Class<BuildingCook> getExpectedBuildingClass()
    {
        return BuildingCook.class;
    }

    /**
     * Very simple action, cook straightly extract it from the furnace.
     *
     * @param furnace the furnace to retrieve from.
     */
    @Override
    protected void extractFromFurnace(final FurnaceBlockEntity furnace)
    {
        InventoryUtils.transferItemStackIntoNextFreeSlotInItemHandler(
                new InvWrapper(furnace), RESULT_SLOT,
                worker.getInventoryCitizen());
        worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        this.incrementActionsDoneAndDecSaturation();
    }

    @Override
    public IAIState startWorking()
    {
        return super.startWorking();
    }

    @Override
    protected boolean isSmeltable(final ItemStack stack)
    {
        //Only return true if the item isn't queued for a recipe.
        return ItemStackUtils.ISCOOKABLE.test(stack) && building.getModule(RESTAURANT_MENU).getMenu().contains(new ItemStorage(MinecoloniesAPIProxy.getInstance().getFurnaceRecipes().getSmeltingResult(stack)));
    }

    @Override
    protected boolean reachedMaxToKeep()
    {
        if (super.reachedMaxToKeep())
        {
            return true;
        }
        final int buildingLimit = Math.max(1, building.getBuildingLevel() * building.getBuildingLevel()) * SLOT_PER_LINE;
        return InventoryUtils.getCountFromBuildingWithLimit(building,
                FoodUtils.EDIBLE.and(stack -> FoodUtils.canEatLevel(stack, building.getBuildingLevel() - 1)),
                stack -> stack.getMaxStackSize() * 6) > buildingLimit;
    }

    @Override
    public void requestSmeltable()
    {
        final RestaurantMenuModule menuModule = building.getModule(RESTAURANT_MENU);
        if (menuModule.getMenu().isEmpty() && worker.getCitizenData() != null)
        {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(FURNACE_USER_NO_FOOD), ChatPriority.BLOCKING));
        }
    }

    private int canServeInRow(){
        return getPrimarySkillLevel() / 15 + 1;
    }

    /**
     * Serve food to citizen.
     * @return next IAIState
     */
    private IAIState serveFoodToCitizen() {
        worker.getCitizenData().setVisibleStatus(COOK);

        //检查顾客格式，以请求提出顺序拿取村民所点的菜(目前为最优的单个菜系，后期可能进一步修改)
        if (checkCustomer) {
            final RestaurantMenuModule module = building.getModule(RESTAURANT_MENU);
            while (!initailCitizenToServe.isEmpty()) {
                int citizenID = initailCitizenToServe.poll();
                ICitizenData citizenData = building.getColony().getCitizenManager().getCivilian(citizenID);
                if (citizenData.getEntity().isEmpty()) {
                    ((BuildingCookExtra) building).deleteCustomer(citizenID);
                    continue;
                }
                AbstractEntityCitizen citizen = citizenData.getEntity().get();
                if (building.isInBuilding(citizen.blockPosition())) {
                    if (FoodUtils.hasBestOptionInInv(worker.getInventoryCitizen(), citizenData, module.getMenu(), building)) {
                        citizenToServe.add(citizen);
                    } else {
                        final ItemStorage storage = FoodUtils.checkForFoodInBuilding(citizenData, module.getMenu(), building);
                        if (storage != null) {
                            citizenToServe.add(citizen);
                            needsCurrently = new Tuple<>(stack -> new ItemStorage(stack).equals(storage), 16);
                            withSpecialReturn = COOK_SERVE_FOOD_TO_CITIZEN;
                            return GATHERING_REQUIRED_MATERIALS;
                        }
                    }
                } else {
                    ((BuildingCookExtra) building).deleteCustomer(citizenID);
                }
            }
            checkCustomer = false;
        }

        // Check overtime customer.
        while (!citizenToServe.isEmpty()) {
            if (!((BuildingCookExtra) building).checkCustomerRegistry(citizenToServe.peek().getCivilianID())) {
                citizenToServe.poll();
                continue;
            }
            break;
        }

        if (citizenToServe.isEmpty()) {
            initailCitizenToServe.clear();
            worker.getNavigation().stop();
            checkCustomer = true;
            return START_WORKING;
        }

        if (!walkToWorkPos(citizenToServe.peek().blockPosition())) {
            return getState();
        }

        final AbstractEntityCitizen citizen = citizenToServe.poll();
        assert citizen != null;
        final InventoryCitizen handler = citizen.getInventoryCitizen();
        final RestaurantMenuModule module = Objects.requireNonNull(worker.getCitizenData().getWorkBuilding()).getModule(RESTAURANT_MENU);
        final Predicate<ItemStack> canEatPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
        final ICitizenData citizenData = citizen.getCitizenData();

        if (!handler.hasSpace()) {
            for (int feedingAttempts = 0; feedingAttempts < 10; feedingAttempts++) {
                final int foodSlot = ExtraFoodUtils.getBestFoodForCitizenWithRestaurantCheck(worker.getInventoryCitizen(), citizenData, module.getMenu(),false);
                if (foodSlot != -1) {
                    final ItemStack stack = worker.getInventoryCitizen().extractItem(foodSlot, 1, false);
                    citizenData.increaseSaturation(FoodUtils.getFoodValue(stack, worker));
                    Objects.requireNonNull(worker.getCitizenColonyHandler().getColonyOrRegister()).getStatisticsManager().increment(FOOD_SERVED, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
                    StatsUtil.trackStatByStack(building, FOOD_SERVED_DETAIL, stack, 1);
                } else {
                    break;
                }

                if (citizenData.getSaturation() >= CitizenConstants.FULL_SATURATION) {
                    ((BuildingCookExtra)building).deleteCustomer(citizen.getCivilianID());
                    break;
                }
            }
            return getState();
        } else if (InventoryUtils.hasItemInItemHandler(handler, canEatPredicate)) {
            return getState();
        }

        final int foodSlot = ExtraFoodUtils.getBestFoodForCitizenWithRestaurantCheck(worker.getInventoryCitizen(), citizenData, module.getMenu(),false);
        if (foodSlot == -1) {
            if (InventoryUtils.getItemCountInItemHandler(worker.getInventoryCitizen(), canEatPredicate) <= 0) {
                return getState();
            }
            return getState();
        }

        if (citizenData.getHomeBuilding() != null && citizenData.getHomeBuilding().getBuildingLevel() > building.getBuildingLevel() + 1) {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(POOR_RESTAURANT_INTERACTION), ChatPriority.BLOCKING));
        }

        String foodName = worker.getInventoryCitizen().getStackInSlot(foodSlot).getDescriptionId();
        int qty = (int) (Math.max(1.0, (FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(worker.getInventoryCitizen().getStackInSlot(foodSlot), citizen)));
        if (InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(worker.getInventoryCitizen(), foodSlot, qty, citizenData.getInventory())) {
            ((BuildingCookExtra)building).deleteCustomer(citizen.getCivilianID());
            Objects.requireNonNull(worker.getCitizenColonyHandler().getColonyOrRegister()).getStatisticsManager().incrementBy(FOOD_SERVED, qty, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
            StatsUtil.trackStatByName(building, FOOD_SERVED_DETAIL, foodName, qty);
            worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
            worker.decreaseSaturationForAction();
        }

        return getState();
    }

    /**
     * Serve food to player.
     * @return next IAIState
     */
    private IAIState serveFoodToPlayer()
    {
        if (playerToServe.isEmpty())
        {
            return START_WORKING;
        }

        BuildingCookExtra extra = (BuildingCookExtra) building;
        worker.getCitizenData().setVisibleStatus(COOK);
        if (playerToServe.peek() == null || !building.isInBuilding(playerToServe.peek().blockPosition()) || playerToServe.peek().getFoodData().getFoodLevel() >= 18)
        {
            worker.getNavigation().stop();
            Player player = playerToServe.poll();
            if(player != null) {
                extra.removePlayerServing(player.getUUID());
            }
            return COOK_SERVE_FOOD_TO_PLAYER;
        }

        assert playerToServe.peek() != null;
        if (!walkToWorkPos(playerToServe.peek().blockPosition()))
        {
            return getState();
        }

        final Player player = playerToServe.poll();
        assert player != null;
        final IItemHandler handler = new InvWrapper(player.getInventory());
        final RestaurantMenuModule module = worker.getCitizenData().getWorkBuilding().getModule(RESTAURANT_MENU);
        final Predicate<ItemStack> canEatPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
        if (InventoryUtils.isItemHandlerFull(handler))
        {
            worker.getNavigation().stop();
            extra.removePlayerServing(player.getUUID());
            return COOK_SERVE_FOOD_TO_PLAYER;
        }

        final Object2IntMap<ItemStack> transferredItemMap = InventoryUtils.transferFoodUpToSaturation(worker, handler, building.getBuildingLevel() * SATURATION_TO_SERVE, canEatPredicate);
        int count = 0;
        for (int v : transferredItemMap.values()) count += v;

        if (count <= 0)
        {
            extra.removePlayerListServing(playerToServe);
            playerToServe.clear();
            return START_WORKING;
        }

        extra.setPlayerServed(player.getUUID());
        worker.getCitizenColonyHandler().getColonyOrRegister().getStatisticsManager().incrementBy(FOOD_SERVED, count, worker.getCitizenColonyHandler().getColonyOrRegister().getDay());
        StatsUtil.trackStatByStackMap(building, FOOD_SERVED_DETAIL, transferredItemMap);
        MessageUtils.format(MESSAGE_INFO_CITIZEN_COOK_SERVE_PLAYER, worker.getName().getString()).sendTo(player);

        worker.getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        this.worker.decreaseSaturationForContinuousAction();

        return playerToServe.isEmpty() ? START_WORKING : COOK_SERVE_FOOD_TO_PLAYER;
    }

    /**
     * Checks if the cook has anything important to do before going to the default furnace user jobs. First calculate the building range if not cached yet. Then check for citizens
     * around the building. If no citizen around switch to default jobs. If citizens around check if food in inventory, if not, switch to gather job. If food in inventory switch to
     * serve job.
     *
     * @return the next IAIState to transfer to.
     */
    @Override
    protected IAIState checkForImportantJobs()
    {
        final List<? extends Player> playerList = WorldUtil.getEntitiesWithinBuilding(world, Player.class,
                building, player -> player != null
                        && player.getFoodData().getFoodLevel() < 18
                        && building.getColony().getPermissions().hasPermission(player, Action.MANAGE_HUTS)
                        && ((BuildingCookExtra)building).getPlayCanServe(player.getUUID())
        );

        playerToServe.addAll(playerList);

        //修改警告条件至餐厅菜单中菜品数量
        final RestaurantMenuModule module = building.getModule(RESTAURANT_MENU);
        int menuDiversity = 0;
        for (ItemStorage menuItem : module.getMenu())
        {
            if(FoodUtils.canEatLevel(menuItem.getItemStack(),building.getBuildingLevel())){
                menuDiversity ++;
            }
            if(menuDiversity >= building.getBuildingLevel()){
                break;
            }
        }
        if (menuDiversity < building.getBuildingLevel())
        {
            worker.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(POOR_MENU_INTERACTION), ChatPriority.BLOCKING));
        }

        final BuildingCookExtra cookExtra = (BuildingCookExtra) building;
        //检查在路上的客人是否已经取消预订或已到店
        for(int customerId : cookExtra.getOrders()) {
            ICitizenData citizenData = building.getColony().getCitizenManager().getCivilian(customerId);
            if(citizenData.getEntity().isPresent() &&  citizenData.getEntity().get() instanceof EntityCitizen citizen){
                IState state = citizen.getCitizenAI().getState();
                if( state != NewEntityAIEatTask.NewEatingState.CHECK_FOOD && state != NewEntityAIEatTask.NewEatingState.GO_TO_RESTAURANT) {
                    cookExtra.reached(customerId);
                }
            }
        }

        if (!playerToServe.isEmpty())
        {
            ((BuildingCookExtra)building).setPlayerServing(playerToServe);
            final Predicate<ItemStack> foodPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
            if (!InventoryUtils.hasItemInItemHandler(worker.getInventoryCitizen(), foodPredicate))
            {
                if (InventoryUtils.hasItemInProvider(building, foodPredicate))
                {
                    needsCurrently = new Tuple<>(foodPredicate, STACKSIZE);
                    withSpecialReturn = COOK_SERVE_FOOD_TO_PLAYER;
                    return GATHERING_REQUIRED_MATERIALS;
                }
            }
            return COOK_SERVE_FOOD_TO_PLAYER;
        }


        final int customerSize = cookExtra.checkSize();
        if (initailCitizenToServe.isEmpty() && citizenToServe.isEmpty() && customerSize > 0) {
            final int canServeInRow = canServeInRow();
            int shouldServeInRow = getShouldServeInRow(customerSize, canServeInRow);
            initailCitizenToServe = new ArrayDeque<>(cookExtra.getCustomers(shouldServeInRow));
        }

        if (!initailCitizenToServe.isEmpty() || !citizenToServe.isEmpty())
        {
            return COOK_SERVE_FOOD_TO_CITIZEN;
        }

        return START_WORKING;
    }

    private int getShouldServeInRow(int customerSize, int canServeInRow) {
        final int workerSize = building.getAllAssignedCitizen().size();
        int shouldServeInRow = (customerSize - 1) / workerSize + 1;
        // The check customerSize <= 3 is to prevent the allocation efficiency from being affected when a cook fails to start work for some reason; this approach is relatively less costly compared to detecting which cook is on duty.
        if (customerSize <= 3){
            shouldServeInRow = Math.min(canServeInRow, customerSize);
        }
        else if (canServeInRow < shouldServeInRow) {
            shouldServeInRow = canServeInRow;
        }
        return shouldServeInRow;
    }

    @Override
    protected int getActionsDoneUntilDumping()
    {
        return 1;
    }

    @Override
    protected IRequestable getSmeltAbleClass()
    {
        return new Food(STACKSIZE, building.getBuildingLevel());
    }
}
