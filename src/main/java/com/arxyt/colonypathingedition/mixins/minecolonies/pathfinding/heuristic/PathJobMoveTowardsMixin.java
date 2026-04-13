package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.heuristic;

import com.arxyt.colonypathingedition.api.IMNodeExtras;
import com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.AbstractPathJobMixin;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveTowards;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PathJobMoveTowards.class, remap = false)
public abstract class PathJobMoveTowardsMixin extends AbstractPathJobMixin {
    @Final @Shadow(remap = false) protected BlockPos target;
    @Final @Shadow(remap = false) protected int minDistance;

    @Inject(method = "computeHeuristic(III)D", at = @At("HEAD"), cancellable = true,remap = false)
    protected void computeHeuristic(int x, int y, int z, CallbackInfoReturnable<Double> cir) {
        double trueDistance = DistanceUtils.dist(x, y, z, target);
        double awayDistance = DistanceUtils.dist(x, y, z, start);
        double dist = DistanceUtils.dist2(start, target);
        double multiplier = 2 * dist / Math.abs(trueDistance - awayDistance + 1.002 * dist);
        double heuristic = trueDistance * trueDistance * multiplier;
        cir.setReturnValue((heuristic));
    }

    /**
     * Heuristic correction function, making “detour exemptions” or other adjustments based on node, onRoad, and onRails.
     */
    @Override
    protected double modifyHeuristic(MNode node, MNode nextNode, double heuristic, boolean onRoad, boolean onRails)
    {
        if(heuristic < 5) {
            return heuristic;
        }
        double newHeuristic;
        IMNodeExtras extras = (IMNodeExtras) node;
        if (onRails){
            heuristic *= onRailPreference * 0.8;
        }
        else if (onRoad && (!node.isOnRails() || extras.isStation()))
        {
            heuristic *= onRoadPreference * 0.85;
        }
        newHeuristic = heuristic;
        if(newHeuristic < 10) {
            return 5 + newHeuristic / 2;
        }
        return newHeuristic;
    }

    /**
     * @author ARxyt
     * @reason Slight change.
     */
    @Overwrite(remap = false)
    protected boolean isAtDestination(@NotNull final MNode n)
    {
        return (DistanceUtils.dist(n.x, n.y, n.z, start) > minDistance && DistanceUtils.manhattanDistance(n.x, n.y, n.z,target) < minDistance / 2.0) ||
                DistanceUtils.manhattanDistance(n.x, n.y, n.z,target) < 3;
    }
}
