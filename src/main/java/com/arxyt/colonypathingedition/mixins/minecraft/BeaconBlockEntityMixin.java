package com.arxyt.colonypathingedition.mixins.minecraft;

import javax.annotation.Nullable;

import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {

    @Inject( method = "applyEffects", at = @At("HEAD"))
    private static void applyMobEffectsToCitizens(Level pLevel, BlockPos pPos, int pLevels,
                                                      @Nullable MobEffect pPrimary,
                                                      @Nullable MobEffect pSecondary,
                                                      CallbackInfo cb) {
        if (!pLevel.isClientSide && pPrimary != null) {
            double d0 = (double)(pLevels * 10 + 10);
            int i = 0;
            if (pLevels >= 4 && pPrimary == pSecondary) {
                i = 1;
            }

            int j = (9 + pLevels * 2) * 20;
            AABB aabb = (new AABB(pPos)).inflate(d0).expandTowards(0.0D, (double)pLevel.getHeight(), 0.0D);
            List<AbstractEntityCitizen> list = pLevel.getEntitiesOfClass(AbstractEntityCitizen.class, aabb);

            for(AbstractEntityCitizen citizen : list) {
                citizen.addEffect(new MobEffectInstance(pPrimary, j, i, true, true));
            }

            if (pLevels >= 4 && pPrimary != pSecondary && pSecondary != null) {
                for(AbstractEntityCitizen citizen1 : list) {
                    citizen1.addEffect(new MobEffectInstance(pSecondary, j, 0, true, true));
                }
            }

        }
    }
}
