package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.colony.permissions.ColonyPermissionEventHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ColonyPermissionEventHandler.class, remap = false)
public class ColonyPermissionEventHandlerMixin {
    @Redirect(method = "cancelEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"), remap = true)
    public boolean cancelLevitation(LivingEntity instance, MobEffectInstance pEffectInstance){
        if(PathingConfig.CANCEL_LEVITATION.get()) {
            return false;
        }
        return instance.addEffect(pEffectInstance);
    }
}
