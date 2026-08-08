package com.arxyt.colonypathingedition.core.initializer;

import com.arxyt.colonypathingedition.core.util.SwitchUtils;
import com.minecolonies.api.colony.interactionhandling.InteractionValidatorRegistry;
import net.minecraft.network.chat.Component;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.EATING_MODULE_;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.FOOD_DIVERSITY;

public class InteractionInitializer {
    public static void init() {
        InteractionValidatorRegistry.registerStandardPredicate(Component.translatable(EATING_MODULE_ + FOOD_QUALITY + URGENT),
                citizen -> {
                    if (!citizen.getCitizenFoodHandler().hasFullFoodHistory())
                    {
                        return false;
                    }
                    return citizen.getCitizenFoodHandler().getFoodHappinessStats().quality() < SwitchUtils.qualityDemandSwitcher(citizen.getHomeBuilding()) / 2.0;
                });

        InteractionValidatorRegistry.registerStandardPredicate(Component.translatable(EATING_MODULE_ + FOOD_DIVERSITY + URGENT),
                citizen -> {
                    if (!citizen.getCitizenFoodHandler().hasFullFoodHistory())
                    {
                        return false;
                    }
                    return citizen.getCitizenFoodHandler().getFoodHappinessStats().diversity() < SwitchUtils.diversityDemandSwitcher(citizen.getHomeBuilding()) / 2.0;
                });

        InteractionValidatorRegistry.registerStandardPredicate(Component.translatable(EATING_MODULE_ + FOOD_QUALITY),
                citizen -> {
                    if (!citizen.getCitizenFoodHandler().hasFullFoodHistory())
                    {
                        return false;
                    }
                    final double quality = citizen.getCitizenFoodHandler().getFoodHappinessStats().quality();
                    final double demandQuality = SwitchUtils.qualityDemandSwitcher(citizen.getHomeBuilding());
                    return quality >= demandQuality / 2.0 && quality < quality / 1.05;

                });

        InteractionValidatorRegistry.registerStandardPredicate(Component.translatable(EATING_MODULE_ + FOOD_DIVERSITY),
                citizen -> {
                    if (!citizen.getCitizenFoodHandler().hasFullFoodHistory())
                    {
                        return false;
                    }
                    final double diversity = citizen.getCitizenFoodHandler().getFoodHappinessStats().diversity();
                    final double demandDiversity = SwitchUtils.diversityDemandSwitcher(citizen.getHomeBuilding());
                    return diversity >= demandDiversity / 2.0 && diversity < diversity / 1.05;
                });
    }
}
