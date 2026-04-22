package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.navigator;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Objects;

@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class MinecoloniesAdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate
{

    @Shadow(remap = false) public abstract double getSpeedFactor();
    @Shadow(remap = false) protected abstract Path convertPath(Path path);
    @Shadow(remap = false) protected abstract void onPathFinish();

    @Final @Shadow(remap = false) public static double MIN_Y_DISTANCE;

    @Shadow(remap = false) private @Nullable PathResult<? extends AbstractPathJob> pathResult;

    public MinecoloniesAdvancedPathNavigateMixin(@NotNull final Mob entity, final Level world) {
        super(entity, world);
    }

    /**
     * 将 900 * 900 替换为 (maxDistance)^2
     */
    @ModifyConstant(
            method = "setPathJob",
            constant = @Constant(doubleValue = 900 * 900),
            remap = false
    )
    private double modifyMaxDistanceSqr(double original)
    {
        return PathingConfig.MAX_PATHING_DISTANCE.get() * PathingConfig.MAX_PATHING_DISTANCE.get();
    }

    @Inject(method = "handleRails",at = @At("RETURN"),remap = false)
    private void getOffRailsAndTpToNextNode(CallbackInfoReturnable<Boolean> cir) {
        if (ourEntity.getVehicle() == null) {
            return;
        }

        final Entity entity = ourEntity.getVehicle();
        if(!(entity instanceof MinecoloniesMinecart minecoloniesMinecart)){
            return;
        }
        if ( this.getPath() == null || this.getPath().isDone() ){
            ourEntity.stopRiding();
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }
        //路径修正，市民下车后一定会tp到下一个路径点处，阻止因下车随机性导致路径重算
        int nodeIndex = this.getPath().getNextNodeIndex() + 1;
        if(nodeIndex < this.getPath().getNodeCount()) {
            final PathPointExtended pEx = (PathPointExtended) (this.getPath().getNode(nodeIndex));
            if (!pEx.isOnRails()) {
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
                ourEntity.teleportTo(pEx.x + 0.5, pEx.y, pEx.z + 0.5);
                return;
            }
        }
        //增加脱轨补偿，一旦拐弯处脱轨将会tp到更远的位置，取决于当前车速
        if(!minecoloniesMinecart.isOnRails()) {
            Vec3 movement = minecoloniesMinecart.getDeltaMovement();
            double speed = movement.length();
            nodeIndex = Math.min(this.getPath().getNodeCount() - 1, this.getPath().getNextNodeIndex() + (int)Math.ceil(speed / 0.4F));
            @NotNull final PathPointExtended tpPlace = (PathPointExtended) (Objects.requireNonNull(this.getPath())).getNode(nodeIndex);
            if(!tpPlace.isOnRails()){
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
                ourEntity.teleportTo(tpPlace.x + 0.5, tpPlace.y, tpPlace.z + 0.5);
                return;
            }
            BlockPos tpPos = tpPlace.asBlockPos();
            if (entity.level().getBlockState(tpPos.below()).is(BlockTags.RAILS)) {
                tpPos = tpPos.below();
            }
            BlockState blockstate = entity.level().getBlockState(tpPos);
            double yOffset = 0.0D;
            RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
                    ? ((BaseRailBlock) blockstate.getBlock()).getRailDirection(blockstate, level, tpPos, null)
                    : RailShape.NORTH_SOUTH;
            if (railshape.isAscending()) {
                yOffset = 0.5D;
            }
            final double x = tpPlace.x + 0.5D;
            final double y = tpPlace.y + 0.625D + yOffset;
            final double z = tpPlace.z + 0.5D;
            minecoloniesMinecart.setPos(x, y, z);
            minecoloniesMinecart.xo = x;
            minecoloniesMinecart.yo = y;
            minecoloniesMinecart.zo = z;
            mob.startRiding(minecoloniesMinecart, true);
        }
    }

    /**
     * @author ARxyt
     * @reason Try to rework, as minecolonies has said to rework this.
     */
    @Overwrite(remap = false)
    protected void followThePath()
    {
        getSpeedFactor();

        final int curNode = path.getNextNodeIndex();
        final int curNodeNext = curNode + 1;
        if (curNodeNext < path.getNodeCount())
        {
            if (!(path.getNode(curNode) instanceof PathPointExtended))
            {
                path = convertPath(path);
            }

            final PathPointExtended pEx = (PathPointExtended) path.getNode(curNode);
            final PathPointExtended pExNext = (PathPointExtended) path.getNode(curNodeNext);

            //  If current node is bottom of a ladder, then stay on this node until
            //  the ourEntity reaches the bottom, otherwise they will try to head out early
            if (pEx.isOnLadder() && pEx.getLadderFacing() == Direction.DOWN
                    && !pExNext.isOnLadder())
            {
                final Vec3 vec3 = getTempMobPos();
                if ((vec3.y - (double) pEx.y) < MIN_Y_DISTANCE)
                {
                    this.path.setNextNodeIndex(curNodeNext);
                }
                return;
            }

            if (!pEx.isOnRails() && ourEntity.getVehicle() != null && !(ourEntity.getVehicle() instanceof CavalryHorseEntity))
            {
                final Entity entity = ourEntity.getVehicle();
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        this.maxDistanceToWaypoint = 0.5F;
        boolean isTracking = PathfindingUtils.trackingMap.containsValue(ourEntity.getUUID());

        HashSet<BlockPos> reached = null;
        if (isTracking)
        {
            reached = new HashSet<>();
        }

        if (path.isDone())
        {
            onPathFinish();
            return;
        }

        // Look at multiple points, in case we're too fast
        final int thisIndex = Math.max(this.path.getNextNodeIndex(), 0);
        final Node thisNode = path.getNode(thisIndex);
        final Vec3 thisPos = thisNode.asBlockPos().getCenter();
        double minDist = DistanceUtils.dist(ourEntity.getX(), ourEntity.getY() + 0.5D, ourEntity.getZ(), thisPos);
        double dy = Math.abs(ourEntity.getY() + 0.5D - thisPos.y);
        boolean skipOnce = false;
        if(minDist < 0.5D || (minDist - dy < 0.25D && dy < mob.maxUpStep())){
            this.path.advance();
            skipOnce = true;
            if (isTracking)
            {
                reached.add(new BlockPos(thisNode.x, thisNode.y, thisNode.z));
            }
        }
        for (int i = thisIndex + 1; i < Math.min(this.path.getNodeCount() - 1, thisIndex + 5); i++)
        {
            final BlockPos pos = path.getNode(i).asBlockPos();
            double thisDist = DistanceUtils.dist(ourEntity.getX(), ourEntity.getY() + 0.5D, ourEntity.getZ(), pos.getCenter());
            if(!skipOnce && thisDist < minDist) {
                minDist = thisDist;
                this.path.advance();
                if (isTracking)
                {
                    final Node point = path.getNode(i - 1);
                    reached.add(new BlockPos(point.x, point.y, point.z));
                }
                continue;
            }
            dy = Math.abs(ourEntity.getY() - pos.getY());
            if(thisDist < 0.5D || (thisDist - dy < 0.25D && dy < ourEntity.maxUpStep())) {
                this.path.advance();
                if (isTracking)
                {
                    final Node point = path.getNode(i);
                    reached.add(new BlockPos(point.x, point.y, point.z));
                }
            }
            break;
        }

        if (isTracking)
        {
            PathfindingUtils.syncDebugReachedPositions(reached, pathResult.getDebugWatchers());
            reached.clear();
        }

        if (path.isDone())
        {
            onPathFinish();
        }
    }

}
