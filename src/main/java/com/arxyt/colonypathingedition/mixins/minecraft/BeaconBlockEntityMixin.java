package com.arxyt.colonypathingedition.mixins.minecraft;

import javax.annotation.Nullable;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {

    @Inject(
            at = @At(
                    value = "INVOKE",
                    target = "net/minecraft/world/level/Level.getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"),
            method = "applyEffects",
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void citizenBeaconEffects(Level level, BlockPos pos, int beaconLevel,
                                                      @NotNull MobEffect primaryEffect,
                                                      @Nullable MobEffect secondaryEffect,
                                                      CallbackInfo cb, double range, int power,
                                                      int duration, AABB box) {
        if(PathingConfig.BEACON_EFFECT.get()) {
            List<AbstractEntityCitizen> list = level
                    .getEntitiesOfClass(AbstractEntityCitizen.class, box);

            for (AbstractEntityCitizen entity : list) {
                entity.addEffect(new MobEffectInstance(primaryEffect, duration, power, true, true));
            }

            if (beaconLevel >= 4 && primaryEffect != secondaryEffect && secondaryEffect != null) {
                for (AbstractEntityCitizen entity : list) {
                    entity.addEffect(new MobEffectInstance(secondaryEffect, duration, 0, true, true));
                }
            }
        }
    }
}
