package com.arxyt.colonypathingedition.mixins.minecolonies.food;

import com.arxyt.colonypathingedition.api.workersetting.BuildingCookExtra;
import com.arxyt.colonypathingedition.core.minecolonies.FoodUtilExtra;
import com.arxyt.colonypathingedition.mixins.minecolonies.AbstractEntityAIBasicMixin;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.permissions.Action;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.colony.buildings.modules.RestaurantMenuModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Predicate;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.FULL_SATURATION;
import static com.minecolonies.api.util.constant.Constants.STACKSIZE;
import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_SERVED;
import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_SERVED_DETAIL;
import static com.minecolonies.api.util.constant.TranslationConstants.POOR_MENU_INTERACTION;
import static com.minecolonies.api.util.constant.TranslationConstants.POOR_RESTAURANT_INTERACTION;
import static com.minecolonies.core.colony.buildings.modules.BuildingModules.RESTAURANT_MENU;

@Mixin(EntityAIWorkCook.class)
public abstract class EntityAIWorkCookMixin extends AbstractEntityAIBasicMixin<BuildingCook,JobCook> implements AbstractEntityAIBasicAccessor<BuildingCook> {

    @Final @Shadow(remap = false) private static VisibleCitizenStatus COOK;
    @Final @Shadow(remap = false) private static int LEVEL_TO_FEED_PLAYER;
    @Final @Shadow(remap = false) private Queue<Player> playerToServe;
    @Final @Shadow(remap = false) private Queue<AbstractEntityCitizen> citizenToServe;

    @Unique private Queue<Integer> initailCitizenToServe = new ArrayDeque<>();
    @Unique private static final double BASE_XP_GAIN = 2;
    @Unique boolean checkCustomer = true;
    @Unique boolean withSpecialReturn = false;

    @Unique private int canServeInRow(){
        return invokeGetPrimarySkillLevel() / 15 + 1;
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        if(withSpecialReturn){
            withSpecialReturn = false;
            return COOK_SERVE_FOOD_TO_CITIZEN;
        }
        return START_WORKING;
    }

    /**
     * @author ARxyt
     * @reason 修改监测内容的同时允许厨师单次服务多个村民
     */
    @Overwrite(remap = false)
    private IAIState serveFoodToCitizen(){
        getWorker().getCitizenData().setVisibleStatus(COOK);

        //检查顾客格式，以请求提出顺序拿取村民所点的菜(目前为最优的单个菜系，后期可能进一步修改)
        if(checkCustomer) {
            final RestaurantMenuModule module = building.getModule(RESTAURANT_MENU);
            while (!initailCitizenToServe.isEmpty()) {
                int citizenID = initailCitizenToServe.poll();
                ICitizenData citizenData = building.getColony().getCitizenManager().getCivilian(citizenID);
                if(citizenData.getEntity().isEmpty()){
                    ((BuildingCookExtra)building).deleteCustomer(citizenID);
                    continue;
                }
                AbstractEntityCitizen citizen = citizenData.getEntity().get();
                if (building.isInBuilding(citizen.blockPosition())) {
                    if (FoodUtils.hasBestOptionInInv(getWorker().getInventoryCitizen(), citizenData, module.getMenu(), building))
                    {
                        citizenToServe.add(citizen);
                    }
                    else
                    {
                        final ItemStorage storage = FoodUtils.checkForFoodInBuilding(citizenData, module.getMenu(), building);
                        if (storage != null)
                        {
                            citizenToServe.add(citizen);
                            needsCurrently = new Tuple<>(stack -> new ItemStorage(stack).equals(storage), 16);
                            withSpecialReturn = true;
                            return GATHERING_REQUIRED_MATERIALS;
                        }
                    }
                }
                else{
                    ((BuildingCookExtra)building).deleteCustomer(citizenID);
                }
            }
            checkCustomer = false;
        }

        if (citizenToServe.isEmpty()) {
            initailCitizenToServe.clear();
            getWorker().getNavigation().stop();
            checkCustomer = true;
            return START_WORKING;
        }

        if (!walkToWorkPos(citizenToServe.peek().blockPosition())) {
            return invokeGetState();
        }

        final AbstractEntityCitizen citizen = citizenToServe.poll();
        assert citizen != null;
        final InventoryCitizen handler = citizen.getInventoryCitizen();
        final RestaurantMenuModule module = Objects.requireNonNull(getWorker().getCitizenData().getWorkBuilding()).getModule(RESTAURANT_MENU);
        final Predicate<ItemStack> canEatPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
        final ICitizenData citizenData = citizen.getCitizenData();

        if (!handler.hasSpace()) {
            for (int feedingAttempts = 0; feedingAttempts < 10; feedingAttempts++) {
                final int foodSlot = FoodUtils.getBestFoodForCitizen(getWorker().getInventoryCitizen(), citizenData, module.getMenu());
                if (foodSlot != -1) {
                    final ItemStack stack = getWorker().getInventoryCitizen().extractItem(foodSlot, 1, false);
                    citizenData.increaseSaturation(FoodUtils.getFoodValue(stack, getWorker()));
                    Objects.requireNonNull(getWorker().getCitizenColonyHandler().getColonyOrRegister()).getStatisticsManager().increment(FOOD_SERVED, getWorker().getCitizenColonyHandler().getColonyOrRegister().getDay());
                    StatsUtil.trackStatByStack(building, FOOD_SERVED_DETAIL, stack, 1);
                } else {
                    break;
                }

                if (citizenData.getSaturation() >= CitizenConstants.FULL_SATURATION) {
                    break;
                }
            }

            return invokeGetState();
        } else if (InventoryUtils.hasItemInItemHandler(handler, canEatPredicate)) {
            return invokeGetState();
        }

        final int foodSlot = FoodUtilExtra.getBestFoodForCitizenWithRestaurantCheck(getWorker().getInventoryCitizen(), citizenData, module.getMenu(),false);
        if (foodSlot == -1) {
            if (InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), canEatPredicate) <= 0) {
                return invokeGetState();
            }
            return invokeGetState();
        }

        if (citizenData.getHomeBuilding() != null && citizenData.getHomeBuilding().getBuildingLevel() > building.getBuildingLevel() + 1) {
            getWorker().getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(POOR_RESTAURANT_INTERACTION), ChatPriority.BLOCKING));
        }

        String foodName = getWorker().getInventoryCitizen().getStackInSlot(foodSlot).getDescriptionId();
        int qty = (int) (Math.max(1.0, (FULL_SATURATION - citizen.getCitizenData().getSaturation()) / FoodUtils.getFoodValue(getWorker().getInventoryCitizen().getStackInSlot(foodSlot), citizen)));
        if (InventoryUtils.transferXOfItemStackIntoNextFreeSlotInItemHandler(getWorker().getInventoryCitizen(), foodSlot, qty, citizenData.getInventory())) {
            Objects.requireNonNull(getWorker().getCitizenColonyHandler().getColonyOrRegister()).getStatisticsManager().incrementBy(FOOD_SERVED, qty, getWorker().getCitizenColonyHandler().getColonyOrRegister().getDay());
            StatsUtil.trackStatByName(building, FOOD_SERVED_DETAIL, foodName, qty);
            getWorker().getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
            getWorker().decreaseSaturationForAction();
        }

        return invokeGetState();
    }

    /**
     * @author ARxyt
     * @reason 餐厅方块增加记忆，修改对应监测内容；优先对玩家的服务；取消在此函数内对顾客点菜进行处理；修改警告条件。
     */
    @Inject(method = "checkForImportantJobs", at=@At("HEAD"), remap = false, cancellable = true)
    protected void checkForImportantJobs(CallbackInfoReturnable<IAIState> cir) {
        final List<? extends Player> playerList = WorldUtil.getEntitiesWithinBuilding(getWorld(), Player.class,
                building, player -> player != null
                        && player.getFoodData().getFoodLevel() < LEVEL_TO_FEED_PLAYER
                        && building.getColony().getPermissions().hasPermission(player, Action.MANAGE_HUTS)
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
            getWorker().getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(POOR_MENU_INTERACTION), ChatPriority.BLOCKING));
        }


        if (!playerToServe.isEmpty())
        {
            final Predicate<ItemStack> foodPredicate = stack -> module.getMenu().contains(new ItemStorage(stack));
            if (!InventoryUtils.hasItemInItemHandler(getWorker().getInventoryCitizen(), foodPredicate))
            {
                if (InventoryUtils.hasItemInProvider(building, foodPredicate))
                {
                    needsCurrently = new Tuple<>(foodPredicate, STACKSIZE);
                    cir.setReturnValue(GATHERING_REQUIRED_MATERIALS);
                    return;
                }
            }
            cir.setReturnValue(COOK_SERVE_FOOD_TO_PLAYER);
            return;
        }

        final BuildingCookExtra cookExtra = (BuildingCookExtra) building;
        final int customerSize = cookExtra.checkSize();
        if (initailCitizenToServe.isEmpty() && citizenToServe.isEmpty() && customerSize > 0) {
            final int canServeInRow = canServeInRow();
            final int workerSize = building.getAllAssignedCitizen().size();
            int shouldServeInRow = (customerSize - 1) / workerSize + 1;
            // customerSize <= 3 的判断是防止有一个厨师因故未能上工影响分配效率，同时能够保证有断断续续的客流时有厨师能够有空余时间处理烧制任务，此方案相对检测上工厨师来说更低耗。
            if (customerSize <= 3){
                shouldServeInRow = Math.min( canServeInRow() , customerSize);
            }
            else if (canServeInRow < shouldServeInRow) {
                shouldServeInRow = canServeInRow;
            }
            initailCitizenToServe = new ArrayDeque<>(cookExtra.getCustomers(shouldServeInRow));
        }

        if (!initailCitizenToServe.isEmpty() || !citizenToServe.isEmpty())
        {
            cir.setReturnValue(COOK_SERVE_FOOD_TO_CITIZEN);
            return;
        }

        cir.setReturnValue(START_WORKING);
    }
}
