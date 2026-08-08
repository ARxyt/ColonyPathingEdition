package com.arxyt.colonypathingedition.core.util;

import com.arxyt.colonypathingedition.api.JobWithEatingLimit;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.advancements.AdvancementTriggers;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.entity.citizen.happiness.ExpirationBasedHappinessModifier;
import com.minecolonies.api.entity.citizen.happiness.StaticHappinessSupplier;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.items.ItemBowlFood;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.core.util.AdvancementUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.HoneyBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

import static com.minecolonies.api.research.util.ResearchConstants.SATURATION;
import static com.minecolonies.api.util.constant.HappinessConstants.HADGREATFOOD;

public class NewFoodUtils {
    public static final Predicate<ItemStack> EDIBLE = itemStack -> ItemStackUtils.ISFOOD.test(itemStack) && !ItemStackUtils.ISCOOKABLE.test(itemStack);
    public static final double foodPunishMinecolonies = PathingConfig.FOOD_NUTRITION_MINECOLONIES.get();
    public static final double foodPunishNormal = PathingConfig.FOOD_NUTRITION_NORMAL.get();
    public static final double foodBonusMinecolonies = PathingConfig.FOOD_BONUS_MINECOLONIES.get();
    public static final double foodBonusNormal = PathingConfig.FOOD_BONUS_NORMAL.get();

    public static boolean getShouldEatAtHut(ICitizenData citizenData, Item food){
        // 如果食物栏末尾不是当前食物，且当前食物存在于历史食物栏中，那么食用此食物可能导致生活水平下降，这里会严格检测。
        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        final Item lastFood = foodHandler.getLastEaten();
        float localScore = foodHandler.checkLastEaten(food);
        final ICitizenFoodHandler.CitizenFoodStats foodStats = foodHandler.getFoodHappinessStats();
        final double diversityRequirement = getMinFoodDiversityRequirement(citizenData.getHomeBuilding());
        final double qualityRequirement = getMinFoodQualityRequirement(citizenData.getHomeBuilding());
        if (lastFood != food){
            final boolean isMinecolfood = food instanceof IMinecoloniesFoodItem;
            final int lastLocalScore = foodHandler.checkLastEaten(food);
            FoodProperties foodProperties = food.getFoodProperties(new ItemStack(food),null);
            FoodProperties lastFoodProperties = lastFood == null ? null : lastFood.getFoodProperties(new ItemStack(lastFood),null);
            final boolean isLastMinecolfood = lastFood instanceof IMinecoloniesFoodItem;
            final float thisDensity = foodProperties == null ? 0 : foodProperties.getSaturationModifier();
            final float lastDensity = lastFoodProperties == null ? 0 : lastFoodProperties.getSaturationModifier();
            final float qualityChange = thisDensity + (isMinecolfood? 0.5F : 0) - lastDensity - (isLastMinecolfood? 0.5F : 0);
            final float diversityChange = (localScore <= 0 ? Math.min(2 * thisDensity + (isMinecolfood? 0.5F : 0), 1.0F) : 0) - (lastLocalScore == 0 ? Math.min(2 * lastDensity + (isLastMinecolfood? 0.5F : 0), 1.0F) : 0);
            return (foodStats.quality() + qualityChange > qualityRequirement && foodStats.diversity() + diversityChange > diversityRequirement) || (qualityChange > 0 && diversityChange > 0);
        }
        else return foodStats.quality() > qualityRequirement && foodStats.diversity() > diversityRequirement;
    }

    public static float getRecalLocalScore(ICitizenData citizenData, Item food){
        // 如果食物栏末尾不是当前食物，且当前食物存在于历史食物栏中，那么食用此食物可能导致生活水平下降，这里会严格检测。
        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        final Item lastFood = foodHandler.getLastEaten();
        float localScore = foodHandler.checkLastEaten(food);
        final ICitizenFoodHandler.CitizenFoodStats foodStats = foodHandler.getFoodHappinessStats();
        final double diversityRequirement = getMinFoodDiversityRequirement(citizenData.getHomeBuilding());
        final double qualityRequirement = getMinFoodQualityRequirement(citizenData.getHomeBuilding());
        if (lastFood != food){
            final boolean isMinecolfood = food instanceof IMinecoloniesFoodItem;
            final int lastLocalScore = foodHandler.checkLastEaten(food);
            FoodProperties foodProperties = food.getFoodProperties(new ItemStack(food),null);
            FoodProperties lastFoodProperties = lastFood == null ? null : lastFood.getFoodProperties(new ItemStack(lastFood),null);
            final boolean isLastMinecolfood = lastFood instanceof IMinecoloniesFoodItem;
            final float thisDensity = foodProperties == null ? 0 : foodProperties.getSaturationModifier();
            final float lastDensity = lastFoodProperties == null ? 0 : lastFoodProperties.getSaturationModifier();
            final float qualityChange = thisDensity + (isMinecolfood? 0.5F : 0) - lastDensity - (isLastMinecolfood? 0.5F : 0);
            final float diversityChange = (localScore <= 0 ? Math.min(2 * thisDensity + (isMinecolfood? 0.5F : 0), 1.0F) : 0) - (lastLocalScore == 0 ? Math.min(2 * lastDensity + (isLastMinecolfood? 0.5F : 0), 1.0F) : 0);
            if(foodStats.quality() + qualityChange > qualityRequirement && foodStats.diversity() + diversityChange > diversityRequirement){
                return Float.MIN_VALUE;
            }
            return localScore - (qualityChange / 3 + diversityChange);
        }
        else if (foodStats.quality() > qualityRequirement && foodStats.diversity() > diversityRequirement){
            return Float.MIN_VALUE;
        }
        return localScore;
    }

    public static int getBestFoodForCitizenWithRestaurantCheck(InventoryCitizen inventoryCitizen, ICitizenData citizenData, Set<ItemStorage> menu, boolean needRestaurantCheck){
        // Smaller score is better.
        float bestScore = Float.MAX_VALUE;
        int bestSlot = -1;

        IBuilding workBuilding = citizenData.getWorkBuilding();
        if (PathingConfig.DELIVERY_EAT_AT_WAREHOUSE.get() && workBuilding instanceof BuildingDeliveryman && citizenData.getEntity().isPresent()) {
            BlockPos alterBuildingPos = citizenData.getColony().getServerBuildingManager().getBestBuilding(citizenData.getEntity().get(), BuildingWareHouse.class);
            if(alterBuildingPos != null) {
                workBuilding = citizenData.getColony().getServerBuildingManager().getBuilding(alterBuildingPos);
            }
        }

        for (int i = 0; i < inventoryCitizen.getSlots(); i++)
        {
            final ItemStorage invStack = new ItemStorage(inventoryCitizen.getStackInSlot(i));
            if ((menu == null || menu.contains(invStack)) && canEat(invStack.getItemStack(), citizenData.getHomeBuilding(), workBuilding, citizenData))
            {
                final Item food = invStack.getItem();
                final float localScore = getRecalLocalScore(citizenData, food);
                if (localScore == Float.MIN_VALUE){
                    return i;
                }
                if (localScore < bestScore)
                {
                    bestScore = localScore;
                    bestSlot = i;
                }
            }
        }
        // Tried everything to maintain quality/diversity but failed, so if we have restaurants in colony, try to eat at restaurants.
        if (needRestaurantCheck && citizenData.getColony().getServerBuildingManager().getFirstBuildingMatching(building -> building instanceof BuildingCook) != null){
            return -1;
        }
        return bestSlot;
    }

    public static ItemStorage checkForFoodInBuilding(final ICitizenData citizenData, @Nullable final Set<ItemStorage> menu, final IBuilding building)
    {
        // Smaller score is better.
        float bestScore = Integer.MAX_VALUE;
        ItemStorage bestStorage = null;

        final Level world = building.getColony().getWorld();

        IBuilding workBuilding = citizenData.getWorkBuilding();
        if (PathingConfig.DELIVERY_EAT_AT_WAREHOUSE.get() && workBuilding instanceof BuildingDeliveryman && building instanceof BuildingWareHouse) {
            workBuilding = building;
        }

        for (final BlockPos pos : building.getContainers()) {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack rackEntity)
                {
                    for (final ItemStorage storage : rackEntity.getAllContent().keySet())
                    {
                        if ((menu == null || menu.contains(storage)) && canEat(storage.getItemStack(), citizenData.getHomeBuilding(), workBuilding, citizenData))
                        {
                            final Item food = storage.getItem();
                            final float localScore = getRecalLocalScore(citizenData, food);
                            if (localScore == Float.MIN_VALUE){
                                return new ItemStorage(storage.getItemStack().copy());
                            }
                            if (localScore < bestScore)
                            {
                                bestScore = localScore;
                                bestStorage = storage;
                            }
                        }
                    }
                }
            }
        }
        return bestStorage == null ? null : new ItemStorage(bestStorage.getItemStack().copy());
    }

    public static boolean hasBestOptionInInv(final InventoryCitizen inventoryCitizen, final ICitizenData citizenData, @Nullable final Set<ItemStorage> menu, final IBuilding building)
    {
        final int invSlot = getBestFoodForCitizenWithRestaurantCheck(inventoryCitizen, citizenData ,menu ,true);
        // Smaller score is better.
        float bestScore = Integer.MAX_VALUE;
        float bestInvScore = Integer.MAX_VALUE;
        if (invSlot >= 0)
        {
            final ItemStack stack = inventoryCitizen.getStackInSlot(invSlot);
            bestInvScore = getRecalLocalScore(citizenData, stack.getItem());
            if(bestInvScore == Float.MIN_VALUE){
                return true;
            }
        }

        final Level world = building.getColony().getWorld();
        for (final BlockPos pos : building.getContainers())
        {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack rackEntity)
                {
                    for (final ItemStorage storage : rackEntity.getAllContent().keySet())
                    {
                        if ((menu == null || menu.contains(storage)) && canEat(storage.getItemStack(), citizenData.getHomeBuilding(), citizenData.getWorkBuilding(), citizenData))
                        {
                            final Item food = storage.getItem();
                            final float localScore = getRecalLocalScore(citizenData, food);
                            if (localScore == Float.MIN_VALUE){
                                return false;
                            }
                            if (localScore < bestScore)
                            {
                                bestScore = localScore;
                            }
                        }
                    }
                }
            }
        }
        return bestInvScore < bestScore;
    }

    public static ItemStorage checkForForceEatingInBuilding(final ICitizenData citizenData, @Nullable final Set<ItemStorage> menu, final IBuilding building) {
        // Smaller score is better.
        float bestScore = Integer.MAX_VALUE;
        ItemStorage bestStorage = null;

        final Level world = building.getColony().getWorld();

        IBuilding workBuilding = citizenData.getWorkBuilding();
        if (PathingConfig.DELIVERY_EAT_AT_WAREHOUSE.get() && workBuilding instanceof BuildingDeliveryman && building instanceof BuildingWareHouse) {
            workBuilding = building;
        }

        for (final BlockPos pos : building.getContainers()) {
            if (WorldUtil.isBlockLoaded(world, pos))
            {
                final BlockEntity entity = world.getBlockEntity(pos);
                if (entity instanceof TileEntityRack rackEntity)
                {
                    for (final ItemStorage storage : rackEntity.getAllContent().keySet())
                    {
                        if ((menu == null || menu.contains(storage)) && canForceEat(storage.getItemStack(), workBuilding, citizenData))
                        {
                            final Item food = storage.getItem();
                            final float localScore = getRecalLocalScore(citizenData, food);
                            if (localScore == Float.MIN_VALUE){
                                return new ItemStorage(storage.getItemStack().copy());
                            }
                            if (localScore < bestScore)
                            {
                                bestScore = localScore;
                                bestStorage = storage;
                            }
                        }
                    }
                }
            }
        }
        return bestStorage == null ? null : new ItemStorage(bestStorage.getItemStack().copy());
    }

    public static int getBestFoodForceEating(InventoryCitizen inventoryCitizen, ICitizenData citizenData, Set<ItemStorage> menu){
        // Smaller score is better.
        float bestScore = Float.MAX_VALUE;
        int bestSlot = -1;

        IBuilding workBuilding = citizenData.getWorkBuilding();
        if (PathingConfig.DELIVERY_EAT_AT_WAREHOUSE.get() && workBuilding instanceof BuildingDeliveryman && citizenData.getEntity().isPresent()) {
            BlockPos alterBuildingPos = citizenData.getColony().getServerBuildingManager().getBestBuilding(citizenData.getEntity().get(), BuildingWareHouse.class);
            if(alterBuildingPos != null) {
                workBuilding = citizenData.getColony().getServerBuildingManager().getBuilding(alterBuildingPos);
            }
        }

        for (int i = 0; i < inventoryCitizen.getSlots(); i++)
        {
            final ItemStorage invStack = new ItemStorage(inventoryCitizen.getStackInSlot(i));
            if ((menu == null || menu.contains(invStack)) && canForceEat(invStack.getItemStack(), workBuilding ,citizenData))
            {
                final Item food = invStack.getItem();
                final float localScore = getRecalLocalScore(citizenData, food);
                if (localScore == Float.MIN_VALUE){
                    return i;
                }
                if (localScore < bestScore)
                {
                    bestScore = localScore;
                    bestSlot = i;
                }
            }
        }
        return bestSlot;
    }

    public static void consumeFood(final ItemStack foodStack, final AbstractEntityCitizen citizen, final Inventory inventory)
    {
        final ICitizenData citizenData = citizen.getCitizenData();
        final double satIncrease = getFoodValue(foodStack, citizen);
        citizenData.increaseSaturation(satIncrease);

        ItemStack itemUseReturn = foodStack.finishUsingItem(citizen.level(), citizen);
        // Special handling for these as those are stackable + have a return per item.
        if (foodStack.getItem() instanceof HoneyBottleItem)
        {
            itemUseReturn = new ItemStack(Items.GLASS_BOTTLE);
        }
        else if (foodStack.getItem() instanceof ItemBowlFood)
        {
            itemUseReturn = new ItemStack(Items.BOWL);
        }

        if (!itemUseReturn.isEmpty() && itemUseReturn.getItem() != foodStack.getItem())
        {
            if (citizenData.getInventory().isFull() || (inventory != null && !inventory.add(itemUseReturn)))
            {
                InventoryUtils.spawnItemStack(
                        citizen.level(),
                        citizen.getX(),
                        citizen.getY(),
                        citizen.getZ(),
                        itemUseReturn
                );
            }
            else
            {
                InventoryUtils.addItemStackToItemHandler(citizenData.getInventory(), itemUseReturn);
            }
        }

        if (foodStack.getItem() instanceof IMinecoloniesFoodItem foodItem && foodItem.getTier() >= 3)
        {
            citizen.getCitizenData().getCitizenHappinessHandler().addModifier(new ExpirationBasedHappinessModifier(HADGREATFOOD, 2.0, new StaticHappinessSupplier(2.0), 5));
        }

        IColony citizenColony = citizen.getCitizenColonyHandler().getColonyOrRegister();
        if (citizenColony != null)
        {
            AdvancementUtils.TriggerAdvancementPlayersForColony(citizenColony, playerMP -> AdvancementTriggers.CITIZEN_EAT_FOOD.trigger(playerMP, foodStack));
        }
        citizenData.markDirty(60);
    }

    /**
     * @param stack
     * @param workBuilding
     * @return If citizen can eat food in stack.
     */
    public static boolean canEat(final ItemStack stack, final IBuilding homeBuilding, final IBuilding workBuilding, ICitizenData citizenData)
    {
        if (!EDIBLE.test(stack))
        {
            return false;
        }

        final int homeBuildingLevel = homeBuilding == null ? 0 : homeBuilding.getBuildingLevelEquivalent();
        return canEatLevel(stack, homeBuildingLevel) && (workBuilding == null || workBuilding.canEat(stack)) && (!(citizenData.getJob() instanceof JobWithEatingLimit job) || job.canEat(stack));
    }

    public static boolean canEatLevel(final ItemStack stack, final int buildingLevel)
    {
        if (buildingLevel <= 1)
        {
            return stack.getItem().getFoodProperties(stack, null) != null;
        }
        final FoodProperties foodProperties = stack.getItem().getFoodProperties(stack, null);
        return foodProperties != null && foodProperties.getNutrition() >= buildingLevel + 1;
    }

    public static boolean canForceEat(final ItemStack stack, final IBuilding workBuilding, ICitizenData citizenData)
    {
        return EDIBLE.test(stack) && (workBuilding == null || workBuilding.canEat(stack)) && (!(citizenData.getJob() instanceof JobWithEatingLimit job) || job.canEat(stack));
    }

    /**
     * Calculate the actual food value for a citizen consuming a given food.
     * @param foodStack the food to consume.
     * @param itemFood the food properties of that food.
     * @param researchBonus the bonus from research (0 for no bonus).
     * @return the saturation adjustment to apply when consuming this food.
     */
    public static double getFoodValue(final ItemStack foodStack, @Nullable final FoodProperties itemFood, final double researchBonus)
    {
        if (itemFood == null)
        {
            return 0;
        }

        final double nutrition = itemFood.getNutrition() * (foodStack.getItem() instanceof IMinecoloniesFoodItem ? foodPunishMinecolonies : foodPunishNormal);
        final double bonus = itemFood.getSaturationModifier() * itemFood.getNutrition() * 2 * (foodStack.getItem() instanceof IMinecoloniesFoodItem ? foodBonusMinecolonies : foodBonusNormal) * (1.0 + researchBonus);
        return nutrition + bonus;
    }

    /**
     * Calculate the actual food value for a citizen consuming a given food.
     * @param foodStack the food to consume.
     * @param citizen the citizen consuming the food.
     * @return the saturation adjustment to apply when consuming this food.
     */
    public static double getFoodValue(final ItemStack foodStack, final AbstractEntityCitizen citizen)
    {
        final FoodProperties itemFood = foodStack.getItem().getFoodProperties(foodStack, citizen);
        final double researchBonus = citizen.getCitizenColonyHandler().getColonyOrRegister().getResearchManager().getResearchEffects().getEffectStrength(SATURATION);
        return getFoodValue(foodStack, itemFood, researchBonus);
    }

    public static double getMinFoodQualityRequirement(@Nullable final IBuilding building)
    {
        int requirement = building == null ? 0 : building.getBuildingLevelEquivalent();
        return requirement * PathingConfig.QUALITY_REQUIREMENT.get();
    }


    public static double getMinFoodDiversityRequirement(@Nullable final IBuilding building)
    {
        int requirement = building == null ? 0 : building.getBuildingLevelEquivalent();
        return requirement * PathingConfig.DIVERSITY_REQUIREMENT.get();
    }
}
