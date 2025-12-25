package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.colony.managers.RegisteredStructureManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.world.level.Level.TICKS_PER_DAY;


@Mixin(value = RegisteredStructureManager.class, remap = false)
public class RegisteredStructureManagerMixin {
    @Unique int trueDate = -1;

    @Inject(method = "onColonyTick",at = @At("HEAD"),remap = false)
    void onFieldDateChange(IColony colony, CallbackInfo ci){
        Level colonyWorld = colony.getWorld();
        if(colonyWorld.getDayTime() / TICKS_PER_DAY != trueDate){
            trueDate = (int)(colonyWorld.getDayTime() / TICKS_PER_DAY);
            ((RegisteredStructureManager)(Object)this).getBuildingExtensions(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()))
                    .stream()
                    .map(m -> (FarmFieldExtra) m)
                    .forEach(field -> field.advanceDay(trueDate));
        }
    }

    @Inject(method = "write", at = @At("TAIL"),remap = false)
    public void additionalSerializeNBT(HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        compound.putInt("trueDate", trueDate);
    }


    @Inject(method = "read", at = @At("TAIL"),remap = false)
    public void additionalDeserializeNBT(HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("trueDate")) {
            trueDate = compound.getInt("trueDate");
        }
    }
}
