package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.navigator;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.ldtteam.domumornamentum.block.decorative.PanelBlock;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.api.entity.pathfinding.IStuckHandler;
import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.api.util.Vec3Mutable;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.entity.other.cavalry.CavalryHorseEntity;
import com.minecolonies.core.entity.pathfinding.PathFindingStatus;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import com.minecolonies.core.util.WorkerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
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

import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class MinecoloniesAdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate
{
    @Shadow(remap = false) public abstract double getSpeedFactor();
    @Shadow(remap = false) protected abstract Path convertPath(Path path);
    @Shadow(remap = false) protected abstract void onPathFinish();
    @Shadow(remap = false) protected abstract void processCompletedCalculationResult();
    @Shadow(remap = false) protected abstract boolean handleRails();
    @Shadow(remap = false) protected abstract BlockPos findBlockUnderEntity(@NotNull Entity parEntity);

    @Final @Shadow(remap = false) public static double MIN_Y_DISTANCE;
    @Final @Shadow(remap = false) private static double ON_PATH_SPEED_MULTIPLIER;

    @Shadow(remap = false) private @Nullable PathResult<? extends AbstractPathJob> pathResult;
    @Shadow(remap = false) private int checkStuckDelay;
    @Shadow(remap = false) private int pauseTicks;
    @Shadow(remap = false) private Vec3Mutable wantedPosition;
    @Shadow(remap = false) private boolean isSneaking;
    @Shadow(remap = false) private BlockPos.MutableBlockPos tempPos;
    @Shadow(remap = false) private long finishTime;
    @Shadow(remap = false) private int pauseTickBackupAmount;
    @Shadow(remap = false) private IStuckHandler<MinecoloniesAdvancedPathNavigate> stuckHandler;

    @Unique private int randomTimer = 15 + level.random.nextInt(5);

    @Unique private MinecoloniesAdvancedPathNavigate asNavigator() {
        return (MinecoloniesAdvancedPathNavigate)(Object)this;
    }

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
            final PathPointExtended pExPre = getPreviousNodeI();

            //  If current node is bottom of a ladder, then stay on this node until
            //  the ourEntity reaches the bottom, otherwise they will try to head out early
            if (pEx.isOnLadder() && !pExNext.isOnLadder() && pEx.getLadderFacing() == Direction.DOWN)
            {
                final Vec3 vec3 = getTempMobPos();
                if ((vec3.y - (double) pEx.y) < MIN_Y_DISTANCE) {
                    this.path.setNextNodeIndex(curNodeNext);
                }
                return;
            }

            if (pExPre != null && pExPre.isOnLadder() && !pEx.isOnLadder() && pExNext.y > pEx.y) {
                final Vec3 vec3 = getTempMobPos();
                if (((double) pEx.y - vec3.y) <= 0 && readyForJump(pEx.asBlockPos().getCenter(), vec3)) {
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
        final int thisIndex = this.path.getNextNodeIndex();
        final Node thisNode = path.getNode(thisIndex);
        final Vec3 thisPos = thisNode.asBlockPos().getCenter();
        double minDist = DistanceUtils.manhattanDistanceVWithYWeight(ourEntity.getX(), ourEntity.getY() + 0.5D, ourEntity.getZ(), thisPos, 0.5);
        boolean skipOnce = false;
        if(minDist < 0.75D){
            this.path.advance();
            skipOnce = true;
            if (isTracking)
            {
                reached.add(new BlockPos(thisNode.x, thisNode.y, thisNode.z));
            }
        }

        for (int i = thisIndex + 1; i < Math.min(this.path.getNodeCount() - 1, thisIndex + 5); i++)
        {
            final Vec3 pos = path.getNode(i).asBlockPos().getCenter();
            final double thisDist = DistanceUtils.manhattanDistanceVWithYWeight(ourEntity.getX(), ourEntity.getY() + 0.5D, ourEntity.getZ(), pos, 0.5);
            if(!skipOnce && thisDist < minDist) {
                if(DistanceUtils.dist2D(ourEntity.getX(), ourEntity.getZ(), pos) < 1.5 && ourEntity.getY() - pos.y > 1.5) {
                    break;
                }
                minDist = thisDist;
                this.path.advance();
                if (isTracking)
                {
                    final Node point = path.getNode(i - 1);
                    reached.add(new BlockPos(point.x, point.y, point.z));
                }
                continue;
            }
            if(thisDist < 0.5D) {
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

    /**
     * @author ARxyt
     * @reason Try to rework panel pathing.
     */
    @Overwrite(remap = false)
    public static double getSmartGroundY(final BlockGetter world, final BlockPos.MutableBlockPos pos, final double orgY)
    {
        BlockState state = world.getBlockState(pos);

        if (!state.isAir())
        {
            if (state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof PanelBlock)
            {
                return orgY;
            }

            final VoxelShape voxelshape = state.getCollisionShape(world, pos);
            if (!ShapeUtil.isEmpty(voxelshape))
            {
                return pos.getY() + ShapeUtil.max(voxelshape, Direction.Axis.Y);
            }
        }

        pos.set(pos.getX(), pos.getY() - 1, pos.getZ());

        state = world.getBlockState(pos);
        if (!state.isAir())
        {
            final VoxelShape voxelshape = state.getCollisionShape(world, pos);
            if (!ShapeUtil.isEmpty(voxelshape))
            {
                return pos.getY() + ShapeUtil.max(voxelshape, Direction.Axis.Y);
            }
        }

        return orgY;
    }

    @Override
    public void tick()
    {
        if (checkStuckDelay-- < 0)
        {
            checkStuckDelay = 10;
            stuckHandler.checkStuck(asNavigator());
        }

        if (pauseTicks > 0)
        {
            pauseTicks--;
        }

        if (pathResult != null)
        {
            if (!pathResult.isDone())
            {
                return;
            }
            else if (pathResult.getStatus() == PathFindingStatus.CALCULATION_COMPLETE)
            {
                processCompletedCalculationResult();
                wantedPosition.setEmpty();
            }
        }

        if (isDone())
        {
            if (pathResult != null)
            {
                pathResult.setStatus(PathFindingStatus.COMPLETE);

                // Cleanup pathresult if the entity forgot about it
                if (ourEntity.level().getGameTime() - finishTime > TICKS_SECOND * 20 + pauseTickBackupAmount)
                {
                    pathResult = null;
                }
            }

            if (!wantedPosition.empty())
            {
                mob.getMoveControl().setWantedPosition(wantedPosition.getX(), wantedPosition.getY(), wantedPosition.getZ(), speedModifier);
                wantedPosition.setEmpty();
            }
            return;
        }

        this.ourEntity.setYya(0);
        if (handleLadders())
        {
            followThePath();
            return;
        }

        // we don't know if this will lower tps, so leave it as usual;
        if (--randomTimer < 0)
        {
            randomTimer = 15 + level.random.nextInt(5);
            if (WorkerUtil.isPathBlock(level.getBlockState(findBlockUnderEntity(ourEntity)).getBlock()))
            {
                speedModifier = ON_PATH_SPEED_MULTIPLIER * getSpeedFactor();
            }
            else
            {
                speedModifier = getSpeedFactor();
            }
        }

        if (isSneaking)
        {
            isSneaking = false;
            mob.setShiftKeyDown(false);
        }

        if (handleRails())
        {
            return;
        }

        ++this.tick;
        if (this.hasDelayedRecomputation)
        {
            this.recomputePath();
        }

        // The following block replaces mojangs super.tick(). Why you may ask? Because it's broken, that's why.
        // The moveHelper won't move up if standing in a block with an empty bounding box (put grass, 1 layer snow, mushroom in front of a solid block and have them try jump up).
        if (!this.isDone())
        {
            final int currentPathIndex = path.getNextNodeIndex();
            this.followThePath();

            if (this.path != null && !this.path.isDone())
            {
                if ((wantedPosition.empty() || currentPathIndex != path.getNextNodeIndex() && path.getNextNodeIndex() < path.getNodeCount()))
                {
                    Vec3 vector3d2 = path.getNextEntityPos(mob);
                    tempPos.set(Mth.floor(vector3d2.x), Mth.floor(vector3d2.y), Mth.floor(vector3d2.z));
                    if (ChunkPos.asLong(tempPos) == mob.chunkPosition().toLong() || WorldUtil.isEntityBlockLoaded(level, tempPos))
                    {
                        double xOffset = 0;
                        double zOffset = 0;
                        BlockState blockstate = this.mob.getBlockStateOn();
                        if(!(blockstate.getBlock() instanceof LadderBlock)) {
                            blockstate = level.getBlockState(this.mob.blockPosition().below());
                        }
                        if(blockstate.getBlock() instanceof LadderBlock) {
                            switch (blockstate.getValue(HorizontalDirectionalBlock.FACING)) {
                                case EAST -> xOffset = -0.2;
                                case WEST -> xOffset = 0.2;
                                case NORTH -> zOffset = 0.2;
                                case SOUTH -> zOffset = -0.2;
                            }
                            PathPointExtended previousNode = this.getPreviousNodeI();
                            if(previousNode != null && previousNode.isOnLadder() && previousNode.getLadderFacing() == Direction.DOWN) {
                                xOffset = -xOffset;
                                zOffset = -zOffset;
                            }
                        }
                        wantedPosition.set(vector3d2.x + xOffset,
                                getSmartGroundY(this.level, tempPos, vector3d2.y),
                                vector3d2.z + zOffset);
                    }
                }
            }

            if (!wantedPosition.empty())
            {
                mob.getMoveControl().setWantedPosition(wantedPosition.getX(), wantedPosition.getY(), wantedPosition.getZ(), speedModifier);
            }
        }
        // End of super.tick.

        if (pathResult != null && isDone())
        {
            pathResult.setStatus(PathFindingStatus.COMPLETE);

            // Cleanup pathresult if the entity forgot about it
            if (ourEntity.level().getGameTime() - finishTime > TICKS_SECOND * 20 + pauseTickBackupAmount)
            {
                pathResult = null;
            }
        }
    }

    private boolean handleLadders()
    {
        // we have tested !this.path.isDone();
        assert this.path != null;
        final PathPointExtended thisNode = (PathPointExtended)(this.path.getNextNode());
        final PathPointExtended previousNode = getPreviousNodeI();

        if (!thisNode.isOnLadder() && (previousNode == null || !previousNode.isOnLadder()))
        {
            return false;
        }

        final PathPointExtended nextNode = getNextNodeI();
        if(!thisNode.isOnLadder() && previousNode != null) {
            wantedPosition.setEmpty();
            return false;
        }

        final Entity entity;
        if ((entity = ourEntity.getVehicle()) != null)
        {
            ourEntity.stopRiding();
            if (!(entity instanceof CavalryHorseEntity))
            {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        // Ladder path follow
        if (path.getNextNodeIndex() < path.getNodeCount())
        {
            HashSet<BlockPos> reached = null;
            if (PathfindingUtils.trackingMap.containsValue(ourEntity.getUUID()))
            {
                reached = new HashSet<>();
            }

            final double nextX = (double) thisNode.x + ((int) (this.mob.getBbWidth() + 1.0F)) * 0.5D;
            final double nextY = thisNode.y;
            final double nextZ = (double) thisNode.z + ((int) (this.mob.getBbWidth() + 1.0F)) * 0.5D;

            final double diffX = Math.abs(this.mob.getX() - nextX);
            final double diffY = this.mob.getY() - nextY;
            final double diffZ = Math.abs(this.mob.getZ() - nextZ);
            // Ladder entry needs more exact position tracking, we want to center the citizen before doing movement in another axis
            if (thisNode.isOnLadder() && (previousNode == null || !previousNode.isOnLadder()))
            {
                if (diffX < 0.2 && diffZ < 0.2 && Math.abs(diffY) < 0.1)
                {
                    if (reached != null)
                    {
                        reached.add(thisNode.asBlockPos());
                        PathfindingUtils.syncDebugReachedPositions(reached, pathResult.getDebugWatchers());
                    }
                    this.path.advance();
                }
                double xOffset = 0;
                double zOffset = 0;
                BlockState blockstate = level.getBlockState(thisNode.asBlockPos());
                // Magic number 0.1, just sufficient.
                if(blockstate.getBlock() instanceof LadderBlock) {
                    switch (blockstate.getValue(HorizontalDirectionalBlock.FACING)) {
                        case EAST -> xOffset = 0.1;
                        case WEST -> xOffset = -0.1;
                        case NORTH -> zOffset = -0.1;
                        case SOUTH -> zOffset = 0.1;
                    }
                }
                ourEntity.xxa = 0;
                ourEntity.zza = 0;
                wantedPosition.set(nextX + xOffset, nextY, nextZ + zOffset);
                this.ourEntity.getMoveControl().setWantedPosition(nextX + xOffset, nextY, nextZ + zOffset, 0.6);
            }
            // Scaling ladder, move
            else
            {
                if (diffX < 0.5 && diffZ < 0.5 && Math.abs(diffY - 0.2) < 0.2)
                {
                    if (reached != null)
                    {
                        reached.add(thisNode.asBlockPos());
                        PathfindingUtils.syncDebugReachedPositions(reached, pathResult.getDebugWatchers());
                    }
                    this.path.setNextNodeIndex(path.getNextNodeIndex() + 1);
                }

                if (isDone())
                {
                    return true;
                }

                //  Ladder Workaround
                if (thisNode.isOnLadder() && nextNode != null && nextNode.isOnLadder() && (thisNode.y != nextNode.y || mob.getY() > thisNode.y))
                {
                    return doLadderMovement();
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Handles movement on a ladder
     *
     * @return true if a ladder is being handled
     */
    private boolean doLadderMovement()
    {
        //This way he is less nervous and gets up the ladder
        boolean newSpeed = true;
        assert this.path != null;
        final PathPointExtended thisNode = (PathPointExtended)(this.path.getNextNode());
        final PathPointExtended previousNode = getPreviousNodeI();
        Vec3 vec3 = thisNode.asBlockPos().getCenter();

        Direction toSelect = previousNode != null ? previousNode.getLadderFacing() : thisNode.getLadderFacing();

        //  Going down
        if (toSelect == Direction.DOWN) {
            newSpeed = false;

        }
        //  Any other value is the same
        else {
            vec3 = vec3.add(0, 1, 0);
        }

        if (newSpeed)
        {
            wantedPosition.set(vec3.x, vec3.y, vec3.z);
            this.ourEntity.getMoveControl().setWantedPosition(vec3.x, vec3.y, vec3.z,1);
        }
        else
        {
            vec3 = this.getPath().getNextEntityPos(this.ourEntity);
            this.ourEntity.getMoveControl().setWantedPosition(vec3.x, vec3.y, vec3.z, 0.2);
            wantedPosition.set(vec3.x, vec3.y, vec3.z);
            return !(ourEntity.getY() <= thisNode.y + 0.2);
        }
        return true;
    }

    @Nullable
    private PathPointExtended getPreviousNodeI()
    {
        assert path != null;
        if (path.getNextNodeIndex() > 0)
        {
            return (PathPointExtended) path.getNode(path.getNextNodeIndex() - 1);
        }

        return null;
    }

    @Nullable
    private PathPointExtended getNextNodeI()
    {
        assert path != null;
        if (path.getNextNodeIndex() + 1 < path.getNodeCount())
        {
            return (PathPointExtended) path.getNode(path.getNextNodeIndex() + 1);
        }

        return null;
    }

    private boolean readyForJump(Vec3 thisPosCenter, Vec3 entityPos){
        final BlockState blockstate = this.mob.getBlockStateOn();
        if(blockstate.getBlock() instanceof LadderBlock) {
            switch (blockstate.getValue(HorizontalDirectionalBlock.FACING)) {
                case EAST -> {
                    return Math.abs(thisPosCenter.x - 0.2 - entityPos.x) < 0.1;
                }
                case WEST -> {
                    return Math.abs(thisPosCenter.x + 0.2 - entityPos.x) < 0.1;
                }
                case NORTH -> {
                    return Math.abs(thisPosCenter.z + 0.2 - entityPos.z) < 0.1;
                }
                case SOUTH -> {
                    return Math.abs(thisPosCenter.z - 0.2 - entityPos.z) < 0.1;
                }
            }
        }
        return true;
    }
}
