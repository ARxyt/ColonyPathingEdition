package com.arxyt.colonypathingedition.core.util;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCook;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Set;

import static com.minecolonies.api.util.FoodUtils.EDIBLE;

public class ExtraFoodUtils {

    public static boolean getShouldEatAtHut(ICitizenData citizenData, Item food){
        // 如果食物栏末尾不是当前食物，且当前食物存在于历史食物栏中，那么食用此食物可能导致生活水平下降，这里会严格检测。
        final ICitizenFoodHandler foodHandler = citizenData.getCitizenFoodHandler();
        final Item lastFood = foodHandler.getLastEaten();
        float localScore = foodHandler.checkLastEaten(food);
        final ICitizenFoodHandler.CitizenFoodStats foodStats = foodHandler.getFoodHappinessStats();
        final int diversityRequirement = FoodUtils.getMinFoodDiversityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        final int qualityRequirement = FoodUtils.getMinFoodQualityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        if (lastFood != food){
            final boolean isMinecolfood = food instanceof IMinecoloniesFoodItem;
            final int lastLocalScore = foodHandler.checkLastEaten(food);
            FoodProperties foodProperties = food.getFoodProperties(new ItemStack(food),null);
            FoodProperties lastFoodProperties = lastFood == null ? null : lastFood.getFoodProperties(new ItemStack(lastFood),null);
            final boolean isLastMinecolfood = lastFood instanceof IMinecoloniesFoodItem;
            final float thisDensity = foodProperties == null ? 0 : foodProperties.saturation() / foodProperties.nutrition();
            final float lastDensity = lastFoodProperties == null ? 0 : lastFoodProperties.saturation() / lastFoodProperties.nutrition();
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
        final int diversityRequirement = FoodUtils.getMinFoodDiversityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        final int qualityRequirement = FoodUtils.getMinFoodQualityRequirement(citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevelEquivalent());
        if (lastFood != food){
            final boolean isMinecolfood = food instanceof IMinecoloniesFoodItem;
            final int lastLocalScore = foodHandler.checkLastEaten(food);
            FoodProperties foodProperties = food.getFoodProperties(new ItemStack(food),null);
            FoodProperties lastFoodProperties = lastFood == null ? null : lastFood.getFoodProperties(new ItemStack(lastFood),null);
            final boolean isLastMinecolfood = lastFood instanceof IMinecoloniesFoodItem;
            final float thisDensity = foodProperties == null ? 0 : foodProperties.saturation() / foodProperties.nutrition();
            final float lastDensity = lastFoodProperties == null ? 0 : lastFoodProperties.saturation() / lastFoodProperties.nutrition();
            final float qualityChange = thisDensity / 2 + (isMinecolfood? 0.5F : 0) - lastDensity - (isLastMinecolfood? 0.5F : 0);
            final float diversityChange = (localScore <= 0 ? Math.min(thisDensity + (isMinecolfood? 0.5F : 0), 1.0F) : 0) - (lastLocalScore == 0 ? Math.min(2 * lastDensity + (isLastMinecolfood? 0.5F : 0), 1.0F) : 0);
            if((foodStats.quality() + qualityChange > qualityRequirement && foodStats.diversity() + diversityChange > diversityRequirement) || (qualityChange > 0 && diversityChange > 0)){
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
            if ((menu == null || menu.contains(invStack)) && FoodUtils.canEat(invStack.getItemStack(), citizenData.getHomeBuilding(), workBuilding))
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
                        if ((menu == null || menu.contains(storage)) && canForceEat(storage.getItemStack(), workBuilding))
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
            if ((menu == null || menu.contains(invStack)) && canForceEat(invStack.getItemStack(), workBuilding))
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

    public static boolean canForceEat(final ItemStack stack, final IBuilding workBuilding)
    {
        return EDIBLE.test(stack) || workBuilding == null || workBuilding.canEat(stack);
    }

}
