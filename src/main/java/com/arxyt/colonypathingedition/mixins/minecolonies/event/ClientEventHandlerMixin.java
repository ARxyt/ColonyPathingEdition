package com.arxyt.colonypathingedition.mixins.minecolonies.event;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.event.ClientEventHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

@Mixin(value = ClientEventHandler.class, remap = false)
public class ClientEventHandlerMixin {
    @Redirect(
            method = "onItemTooltipEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z",
                    remap = false
            ),
            remap = false
    )
    private static boolean redirectIsFood(Predicate<ItemStack> instance, Object t) {
        return !PathingConfig.EATING_AI_MODULE.get() && instance.test((ItemStack) t);
    }
}
