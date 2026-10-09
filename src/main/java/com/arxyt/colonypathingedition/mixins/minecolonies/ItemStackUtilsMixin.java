package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.ItemStackUtils;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.ItemStackUtils.getMaxEnchantmentLevel;

@Mixin(value = ItemStackUtils.class, remap = false)
public class ItemStackUtilsMixin {
    @Unique private final static int levelScale = PathingConfig.ENCHANT_LEVEL_SCALE.get();
    @Unique private final static boolean allowZero = PathingConfig.EARLY_ENCHANT.get();
    @Unique private final static int maxLevel = PathingConfig.MAX_ADDITIONAL_LEVEL_ENCHANT.get();

    @Inject(remap = false,method = "getMaxEnchantmentLevel",at = @At("RETURN"),cancellable = true)
    private static void resetMaxEnchantmentLevel(CallbackInfoReturnable<Integer> cir){
        if(cir.getReturnValue() > 0){
            int additonalLevel = allowZero? cir.getReturnValue() : cir.getReturnValue() - 1;
            int levelRange = allowZero? maxLevel + 1 : maxLevel;
            additonalLevel = Math.min((additonalLevel + levelScale) / levelScale, levelRange);
            cir.setReturnValue(allowZero? additonalLevel - 1 : additonalLevel);
        }
    }

    /**
     * @author ARxyt
     * @reason Wrong explain the tool minLevel
     */
    @Overwrite(remap = false)
    public static boolean hasEquipmentLevel(@Nullable final ItemStack stack, final EquipmentTypeEntry equipmentType, final int minimalLevel, final int maximumLevel)
    {
        if (stack == null || stack.isEmpty() || !equipmentType.checkIsEquipment(stack))
        {
            return false;
        }

        int equipmentLevel = equipmentType.getMiningLevel(stack);
        return  equipmentLevel >= minimalLevel && equipmentLevel + getMaxEnchantmentLevel(stack) <= maximumLevel;
    }
}
