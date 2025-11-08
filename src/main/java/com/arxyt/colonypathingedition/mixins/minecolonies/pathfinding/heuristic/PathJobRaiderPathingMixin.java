package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.heuristic;

import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobRaiderPathing;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin( PathJobRaiderPathing.class)
public class PathJobRaiderPathingMixin {
    @Final @Shadow(remap = false) private BlockPos direction;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double heuristic = DistanceUtils.chebyshevDistance(x, y, z, direction) ;
        cir.setReturnValue((heuristic * heuristic + 99 * heuristic) / 100);
    }
}

