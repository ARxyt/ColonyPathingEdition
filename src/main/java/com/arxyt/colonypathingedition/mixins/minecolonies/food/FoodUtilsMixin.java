package com.arxyt.colonypathingedition.mixins.minecolonies.food;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.items.IMinecoloniesFoodItem;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse;
import com.minecolonies.core.tileentities.TileEntityRack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

import static com.arxyt.colonypathingedition.core.util.ExtraFoodUtils.getBestFoodForCitizenWithRestaurantCheck;
import static com.arxyt.colonypathingedition.core.util.ExtraFoodUtils.getRecalLocalScore;
import static com.minecolonies.api.util.FoodUtils.getBestFoodForCitizen;

@Mixin(value = FoodUtils.class, remap = false)
public class FoodUtilsMixin {
    @Unique private static final double foodPunisher = PathingConfig.FOOD_PUNISHER.get();
    @Unique private static final double foodBonusNormal = PathingConfig.FOOD_BONUS_NORMAL.get();
    @Unique private static final double foodBonusMinecolonies = PathingConfig.FOOD_BONUS_MINECOLONIES.get();

    @Inject(remap = false,method = "getFoodValue(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;D)D",at = @At("RETURN"), cancellable = true)
    private static void resetFoodBonus(ItemStack foodStack, FoodProperties itemFood, double researchBonus, CallbackInfoReturnable<Double> cir){
        double noPunish = foodStack.getItem() instanceof IMinecoloniesFoodItem ? 1 : 4 * foodPunisher;
        double bonus = itemFood.getSaturationModifier() * itemFood.getNutrition() * 2 * (foodStack.getItem() instanceof IMinecoloniesFoodItem ? foodBonusMinecolonies : foodBonusNormal);
        cir.setReturnValue(cir.getReturnValueD() * noPunish + bonus);
    }

    /**
     * @author ARxyt
     * @reason Have conflicts with my modification, remastered。
     */
    @Inject(method = "getBestFoodForCitizen", at = @At("HEAD"), remap = false, cancellable = true)
    private static void rewriteGetBestFoodForCitizen(InventoryCitizen inventoryCitizen, ICitizenData citizenData, Set<ItemStorage> menu, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(getBestFoodForCitizenWithRestaurantCheck(inventoryCitizen, citizenData ,menu ,true));
    }

    /**
     * @author ARxyt
     * @reason Remaster prior。
     */
    @Inject(method = "checkForFoodInBuilding", at = @At("HEAD"), remap = false, cancellable = true)
    private static void rewriteCheckForFoodInBuilding(ICitizenData citizenData, Set<ItemStorage> menu, IBuilding building, CallbackInfoReturnable<ItemStorage> cir){
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
                        if ((menu == null || menu.contains(storage)) && FoodUtils.canEat(storage.getItemStack(), citizenData.getHomeBuilding(), workBuilding))
                        {
                            final Item food = storage.getItem();
                            final float localScore = getRecalLocalScore(citizenData, food);
                            if (localScore == Float.MIN_VALUE){
                                cir.setReturnValue(new ItemStorage(storage.getItemStack().copy()));
                                return;
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
        cir.setReturnValue(bestStorage == null ? null : new ItemStorage(bestStorage.getItemStack().copy()));
    }

    /**
     * @author ARxyt
     * @reason Remaster prior。
     */
    @Inject(method = "hasBestOptionInInv", at = @At("HEAD"), remap = false, cancellable = true)
    private static void hasBestOptionInInv(InventoryCitizen inventoryCitizen, ICitizenData citizenData, Set<ItemStorage> menu, IBuilding building, CallbackInfoReturnable<Boolean> cir)
    {
        final int invSlot = getBestFoodForCitizen(inventoryCitizen, citizenData, menu);
        // Smaller score is better.
        float bestScore = Integer.MAX_VALUE;
        float bestInvScore = Integer.MAX_VALUE;
        if (invSlot >= 0)
        {
            final ItemStack stack = inventoryCitizen.getStackInSlot(invSlot);
            bestInvScore = getRecalLocalScore(citizenData, stack.getItem());
            if(bestInvScore == Float.MIN_VALUE){
                cir.setReturnValue(true);
                return;
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
                        if ((menu == null || menu.contains(storage)) && FoodUtils.canEat(storage.getItemStack(), citizenData.getHomeBuilding(), citizenData.getWorkBuilding()))
                        {
                            final Item food = storage.getItem();
                            final float localScore = getRecalLocalScore(citizenData, food);
                            if (localScore == Float.MIN_VALUE){
                                cir.setReturnValue(false);
                                return;
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
        cir.setReturnValue(bestInvScore < bestScore);
    }
}
