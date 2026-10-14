package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.heuristic;

import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.MNode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveCloseToXNearY;

// Mixin for PathJobMoveAwayFromLocation
@Mixin(value = PathJobMoveCloseToXNearY.class, remap = false)
public abstract class PathJobMoveCloseToXNearYMixin {
    @Final @Shadow(remap = false) public BlockPos desiredPosition;
    @Final @Shadow(remap = false) public BlockPos nearbyPosition;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double heuristic = (DistanceUtils.manhattanDistanceV(x, y, z,desiredPosition) * 2 + DistanceUtils.manhattanDistanceV(x, y, z,nearbyPosition)) / 3;
        cir.setReturnValue((heuristic * heuristic + 99 * heuristic) / 100);
    }

    @Inject(method = "getEndNodeScore(Lcom/minecolonies/core/entity/pathfinding/MNode;)D", at = @At("RETURN"), cancellable = true, remap = false)
    protected void onGetEndNodeScore(MNode n, CallbackInfoReturnable<Double> cir) {
        int dy = Math.abs(desiredPosition.getY() - n.y);
        if (dy >= 2) {
            cir.setReturnValue(cir.getReturnValueD() + dy * 4.0D);
        }
    }
}
