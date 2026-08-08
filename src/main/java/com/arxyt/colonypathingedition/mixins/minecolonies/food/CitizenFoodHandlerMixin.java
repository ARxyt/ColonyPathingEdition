package com.arxyt.colonypathingedition.mixins.minecolonies.food;

import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.google.common.collect.EvictingQueue;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.citizen.citizenhandlers.ICitizenFoodHandler;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenFoodHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.EATING_MODULE_;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.TranslationConstants.*;

@Mixin(value = CitizenFoodHandler.class, remap = false)
public abstract class CitizenFoodHandlerMixin implements ICitizenFoodHandler{
    @Final @Shadow(remap = false) private static int FOOD_QUEUE_SIZE;
    @Final @Shadow(remap = false) private EvictingQueue<Item> lastEatenFoods;
    @Final @Shadow(remap = false) private ICitizenData citizenData;
    @Shadow(remap = false) private ICitizenFoodHandler.CitizenFoodStats foodStatCache;
    @Shadow(remap = false) private boolean dirty;

    @Inject(remap = false,method ="getFoodHappinessStats" ,at = @At("HEAD"), cancellable = true)
    public void rewriteGetFoodHappinessStats(CallbackInfoReturnable<ICitizenFoodHandler.CitizenFoodStats> cir)
    {
        if (foodStatCache == null || dirty)
        {
            float qualityFoodCounter = 0;
            float diversityFoodCounter = 0;
            Set<Item> uniqueFoods = new HashSet<>();
            for (final Item foodItem : lastEatenFoods)
            {
                float qualityFoodAdder = 0;
                float diversityFoodAdder = 0;
                if (foodItem instanceof IMinecoloniesFoodItem)
                {
                    qualityFoodAdder += 0.5F;
                    diversityFoodAdder += 0.5F;
                }
                FoodProperties foodProperties = foodItem.getFoodProperties(new ItemStack(foodItem),null);
                if(foodProperties != null){
                    final float nutritionDensity = foodProperties.saturation() / foodProperties.nutrition();
                    qualityFoodAdder += nutritionDensity / 2.0f;
                    diversityFoodAdder += Math.max(nutritionDensity , 1.0f);
                }
                if(!NewFoodUtils.canEatLevel(new ItemStack(foodItem), citizenData.getHomeBuilding() == null ? 0 : citizenData.getHomeBuilding().getBuildingLevel() - 1)) {
                    qualityFoodAdder /= 1.5F;
                    diversityFoodAdder /= 1.2F;
                }
                qualityFoodCounter += qualityFoodAdder;
                if(!uniqueFoods.contains(foodItem)){
                    diversityFoodCounter += diversityFoodAdder;
                }
                uniqueFoods.add(foodItem);
            }
            foodStatCache = new ICitizenFoodHandler.CitizenFoodStats(Mth.ceil(diversityFoodCounter), Mth.ceil(qualityFoodCounter));
        }
        cir.setReturnValue(foodStatCache);
    }

    @Override
    public void addLastEaten(final Item item)
    {
        lastEatenFoods.add(item);
        citizenData.markDirty(TICKS_SECOND);
        dirty = true;
        if (lastEatenFoods.size() >= FOOD_QUEUE_SIZE)
        {
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(EATING_MODULE_ + FOOD_DIVERSITY), ChatPriority.CHITCHAT));
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(EATING_MODULE_ + FOOD_QUALITY), ChatPriority.CHITCHAT));
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(EATING_MODULE_ + FOOD_DIVERSITY + URGENT), ChatPriority.IMPORTANT));
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(EATING_MODULE_ + FOOD_QUALITY + URGENT), ChatPriority.IMPORTANT));
        }
    }
}
