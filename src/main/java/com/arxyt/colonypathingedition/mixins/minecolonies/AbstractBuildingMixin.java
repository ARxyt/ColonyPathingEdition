package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.AbstractBuildingContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 此Mixin的目的是修改pickUp机制。
 * 1.本地存储 pickUpDay, 阻止每次进入存档即重置所有小屋清仓计时。
 * 2.当尝试发出 pick up 进程后，不要求下一次 pick up 请求仅来清理 “尝试 pick up“ 的进程堵塞问题。
 */
@Mixin(value = AbstractBuilding.class, remap = false)
public abstract class AbstractBuildingMixin extends AbstractBuildingContainer {

    @Shadow(remap = false) public int pickUpDay;

    protected AbstractBuildingMixin(@NotNull final IColony colony, final BlockPos pos) {
        super(pos, colony);
        throw new RuntimeException("AbstractBuildingMixin 类不应被实例化！");
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V",at = @At("RETURN"),remap = false)
    private void deserializeNBTAddition (HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        if(compound.contains("pick_up_day")) {
            pickUpDay = compound.getInt("pick_up_day");
        }
    }

    @Inject(method = "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;",at = @At("RETURN"),remap = false,cancellable = true)
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
