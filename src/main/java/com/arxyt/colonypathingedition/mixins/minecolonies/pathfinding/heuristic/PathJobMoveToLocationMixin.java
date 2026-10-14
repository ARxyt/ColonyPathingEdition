package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.heuristic;

import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PathJobMoveToLocation.class, remap = false)
public class PathJobMoveToLocationMixin {
    @Final @Shadow(remap = false) protected BlockPos destination;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double heuristic = DistanceUtils.manhattanDistanceV(x, y, z, destination) ;
        cir.setReturnValue((heuristic * heuristic + 99 * heuristic) / 100);
    }

    @Inject(method = "getEndNodeScore(Lcom/minecolonies/core/entity/pathfinding/MNode;)D", at = @At("RETURN"), cancellable = true, remap = false)
    protected void onGetEndNodeScore(MNode n, CallbackInfoReturnable<Double> cir) {
        int dy = Math.abs(destination.getY() - n.y);
        if (dy == 1) {
            int dx = Math.abs(destination.getX() - n.x);
            int dz = Math.abs(destination.getZ() - n.z);
            double penalty = (dx == 0 && dz == 0) ? 1.5D : 0.5D;
            cir.setReturnValue(cir.getReturnValueD() + penalty);
        } else if (dy >= 2) {
            cir.setReturnValue(cir.getReturnValueD() + dy * 3.0D);
        }
    }
}
