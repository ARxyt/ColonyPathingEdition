package com.arxyt.colonypathingedition.mixins.minecolonies.module.main;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.colony.buildings.modules.MinimumStockModule;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MinimumStockModule.class, remap = false)
public abstract class MinimumStockModuleMixin {

    /**
     * Change target in onColonyTick, no more at least maxStackSize order.
     */
    @Redirect(
            method = "onColonyTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I",
                    ordinal = 0
            ),
            remap = false
    )
    private int redirectGetMaxStackSizeForTarget(ItemStack stack) {
        if(PathingConfig.MINIMUM_STOCK_PRECISE.get()) {
            return 1;
        }
        else{
            return stack.getMaxStackSize();
        }
    }

    /**
     * Change target in alterItemsToBeKept, no more at least maxStackSize order.
     */
    @Redirect(
            method = "alterItemsToBeKept",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"
            ),
            remap = false
    )
    private int redirectGetMaxStackSizeForAlter(ItemStack stack) {
        if(PathingConfig.MINIMUM_STOCK_PRECISE.get()) {
            return 1;
        }
        else{
            return stack.getMaxStackSize();
        }
    }

}