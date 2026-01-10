package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.google.common.base.Objects;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * The purpose of this Mixin is to modify the pickUp mechanism.
 * 1. Locally store pickUpDay to prevent all hut clearance timers from resetting
 *    every time the save is loaded.
 * 2. After attempting to issue a pickUp process, do not require the next pickUp
 *    request to solely resolve the process blockage caused by the "attempted pickUp".
 */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class AbstractBuildingMixin extends AbstractBuildingContainer {

    @Shadow(remap = false) public int pickUpDay;

    @Shadow(remap = false) public abstract boolean hasModule(BuildingEntry.ModuleProducer<?, ?> producer);

    protected AbstractBuildingMixin(@NotNull final IColony colony, final BlockPos pos) {
        super(pos, colony);
        throw new RuntimeException("AbstractBuildingMixin 类不应被实例化！");
    }

    @Override
    public boolean canEat(final ItemStack stack)
    {
        FoodBlackListMenuModule module = getModule(FoodBlackListMenuModule.class);
        if(module != null) {
            if (module.getBlackList().contains(new ItemStorage(stack))) return false;
        }
        else{
            if (PathingConfig.food_black_list.contains(new ItemStorage(stack))) return false;
        }
        return super.canEat(stack);
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",at = @At("RETURN"),remap = false)
    private void deserializeNBTAddition (CompoundTag compound, CallbackInfo cir) {
        if(compound.contains("pick_up_day")) {
            pickUpDay = compound.getInt("pick_up_day");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",at = @At("RETURN"),remap = false,cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("pick_up_day", pickUpDay);
        cir.setReturnValue(tag);
    }

    @Inject(method = "createPickupRequest",at = @At("RETURN"),remap = false)
    private void resetPickUpDay (final int pickUpPrio, CallbackInfoReturnable<Boolean> cir) {
        if(pickUpDay == -1){
            int daysToPickup = 10 - pickUpPrio;
            pickUpDay = colony.getDay() + daysToPickup;
        }
    }
}
