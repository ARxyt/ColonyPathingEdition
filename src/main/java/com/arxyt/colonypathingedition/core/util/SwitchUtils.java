package com.arxyt.colonypathingedition.core.util;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class SwitchUtils {
    public static boolean canEatSwitcher(final ItemStack stack, final IBuilding homeBuilding, final IBuilding workBuilding, ICitizenData citizenData) {
        if(PathingConfig.EATING_AI_MODULE.get()) {
            return NewFoodUtils.canEat(stack, homeBuilding, workBuilding, citizenData);
        }
        return FoodUtils.canEat(stack, homeBuilding, workBuilding);
    }

    public static double qualityDemandSwitcher(final IBuilding homeBuilding) {
        if(PathingConfig.EATING_AI_MODULE.get()) {
            return NewFoodUtils.getMinFoodQualityRequirement(homeBuilding);
        }
        return homeBuilding == null ? 0 : homeBuilding.getBuildingLevelEquivalent() <= 2 ? 0 : FoodUtils.getMinFoodQualityRequirement(homeBuilding.getBuildingLevelEquivalent());
    }

    public static double diversityDemandSwitcher(final IBuilding homeBuilding) {
        if(PathingConfig.EATING_AI_MODULE.get()) {
            return NewFoodUtils.getMinFoodDiversityRequirement(homeBuilding);
        }
        return homeBuilding == null ? 0 : homeBuilding.getBuildingLevelEquivalent() <= 1 ? 0 : FoodUtils.getMinFoodDiversityRequirement(homeBuilding.getBuildingLevelEquivalent());
    }

    public static void consumeFoodSwitcher(final ItemStack foodStack, final AbstractEntityCitizen citizen, @org.jetbrains.annotations.Nullable final Player player){
        if(PathingConfig.EATING_AI_MODULE.get()) {
            NewFoodUtils.consumeFood(foodStack, citizen, player);
            return;
        }
        ItemStackUtils.consumeFood(foodStack, citizen, player);
    }
}
