package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.heuristic;

import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;

// Mixin for PathJobMoveAwayFromLocation
@Mixin(PathJobMoveCloseToXNearY.class)
public abstract class PathJobMoveCloseToXNearYMixin {
    @Final @Shadow(remap = false) public BlockPos desiredPosition;
    @Final @Shadow(remap = false) public BlockPos nearbyPosition;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double heuristic = (DistanceUtils.dist(x, y, z,desiredPosition) * 2 + DistanceUtils.dist(x, y, z,nearbyPosition)) / 3;
        cir.setReturnValue((heuristic * heuristic + 99 * heuristic) / 100);
    }
}
