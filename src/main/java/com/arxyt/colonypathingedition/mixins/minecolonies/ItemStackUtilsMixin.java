package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.util.ItemStackUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
}
