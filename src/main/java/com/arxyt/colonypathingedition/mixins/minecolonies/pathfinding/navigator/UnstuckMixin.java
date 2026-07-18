package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.navigator;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.MinecoloniesAdvancedPathNavigateAccessor;
import com.minecolonies.api.entity.pathfinding.IMinecoloniesNavigator;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import com.minecolonies.core.entity.pathfinding.navigation.PathingStuckHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.Random;

@Mixin(value = PathingStuckHandler.class, remap = false)
public abstract class UnstuckMixin<NAV extends PathNavigation & IMinecoloniesNavigator> {

    @Shadow (remap = false) private int delayToNextUnstuckAction;
    @Shadow (remap = false) private int stuckLevel;
    @Shadow (remap = false) private Random rand;
    @Shadow (remap = false) private BlockPos moveAwayStartPos;
    @Shadow (remap = false) private boolean hadPath;
    @Shadow (remap = false) private boolean canBreakBlocks;
    @Shadow (remap = false) private boolean canPlaceLadders;
    @Shadow (remap = false) private boolean canBuildLeafBridges;
    @Shadow (remap = false) private Direction movingAwayDir;
    @Shadow (remap = false) private BlockPos prevDestination;
    @Shadow (remap = false) private boolean canTeleportGoal;

    @Shadow (remap = false) protected abstract void placeLadders(NAV navigator);
    @Shadow (remap = false) protected abstract void placeLeaves(NAV navigator);
    @Shadow (remap = false) protected abstract void breakBlocks(NAV navigator);
    @Shadow (remap = false) protected abstract void resetStuckTimers();
    @Shadow (remap = false) public abstract void resetGlobalStuckTimers();

    @Unique private int stuckLevelRecorder = 0;
    @Unique private boolean needReset = false;

    /**
     * @author ARxyt
     * @reason It's awful.
     */
    @Overwrite(remap = false)
    private void tryUnstuck(final NAV navigator)
    {
        if (delayToNextUnstuckAction > 0)
        {
            return;
        }

        // (Patch) Attempt to prevent random wandering at the workstation
        if ((prevDestination == null || prevDestination.equals(BlockPos.ZERO)) && stuckLevel == 0 && stuckLevelRecorder != 0 ){
            completeStuckAction(navigator);
            return;
        }

        delayToNextUnstuckAction = 50;

        // Small forward teleport
        boolean teleported = false;
        if (hadPath)
        {
            int index = Math.min(Objects.requireNonNull(navigator.getPath()).getNextNodeIndex() + 5, navigator.getPath().getNodeCount() - 1);
            final Node togo = navigator.getPath().getNode(index);
            navigator.getOurEntity().teleportTo(togo.x + 0.5d, togo.y, togo.z + 0.5d);
            delayToNextUnstuckAction = 20;
            teleported = true;
        }
        if (teleported || needReset) {
            navigator.getOurEntity().stopRiding();
            navigator.recalc();
            if(needReset){
                needReset = false;
                return;
            }
        }
        else{
            stuckLevel = Math.max(stuckLevelRecorder,stuckLevel);
            stuckLevel++;
        }

        // New detour algorithm
        if ((stuckLevel <= 4 && !teleported) && prevDestination != null && !prevDestination.equals(BlockPos.ZERO)) {
            if (navigator.getPath() != null) {
                moveAwayStartPos = navigator.getPath().getNodePos(navigator.getPath().getNextNodeIndex());
            } else {
                moveAwayStartPos = navigator.getOurEntity().blockPosition().above();
            }
            int range = 20 * stuckLevel;
            BlockPos startPos = navigator.getOurEntity().blockPosition();
            // Obtain a possibly passable direction.
            // Since same-direction traversal is already approximately breadth-first, detouring to the other side can maximize benefit.
            BlockPos dPos = new BlockPos(prevDestination.getX() - startPos.getX(),0,prevDestination.getZ() - startPos.getZ());
            double distance = BlockPosUtil.dist(dPos,BlockPos.ZERO);
            if(distance == 0){
                dPos = dPos.relative(movingAwayDir,range);
                movingAwayDir = movingAwayDir.getClockWise();
            }
            else{
                if(stuckLevel >= 2){
                    if(Math.abs(dPos.getX()) > Math.abs(dPos.getZ())) {
                        dPos = dPos.relative(Direction.Axis.Z,stuckLevel % 2 == 0 ? -20:  20);
                    }
                    else{
                        dPos = dPos.relative(Direction.Axis.X,stuckLevel % 2 == 0 ? -20:  20);
                    }
                }
            }
            navigator.setPauseTicks(0);
            ((MinecoloniesAdvancedPathNavigateAccessor) navigator).invokeWalkTowards(prevDestination.offset(dPos), 100, 1.0f + 0.25f * stuckLevel);
            stuckLevelRecorder = stuckLevel;
            navigator.setPauseTicks(300);
            delayToNextUnstuckAction = 350;
            needReset = true;
            return;
        }

        // 下面这两个放置和破坏基本上只有袭击者会用，后面需要调整一下算法，目前的放置和破坏都非常蠢，但是由于现在卫兵AI更蠢所以先不改。
        // Place ladders & leaves
        if (stuckLevel >= 5 && stuckLevel <= 6)
        {
            if (canPlaceLadders && rand.nextBoolean())
            {
                delayToNextUnstuckAction = 50;
                placeLadders(navigator);
                stuckLevelRecorder = stuckLevel;
                return;
            }
            else if (canBuildLeafBridges)
            {
                delayToNextUnstuckAction = 30;
                placeLeaves(navigator);
                stuckLevelRecorder = stuckLevel;
                return;
            }
        }
        // break blocks
        if (stuckLevel >= 7 && canBreakBlocks)
        {
            delayToNextUnstuckAction = 100;
            breakBlocks(navigator);
            stuckLevelRecorder = 5;
            stuckLevel = 5;
            return;
        }

        // Directly teleport to the target location after the path is completely stuck.
        if (stuckLevel >= 5)
        {
            if(!PathingConfig.CANCEL_TELEPORT.get()) {
                completeStuckAction(navigator);
            }
            else {
                navigator.stop();
                resetGlobalStuckTimers();
            }
            resetStuckTimers();
            stuckLevelRecorder = 0;
            stuckLevel = 0;
        }
    }

    private void completeStuckAction(final NAV navigator)
    {
        final BlockPos desired = navigator.getSafeDestination();
        final Level world = navigator.getOurEntity().level();
        final Mob entity = navigator.getOurEntity();

        if (!FMLEnvironment.production)
        {
            Log.getLogger()
                    .warn("Entity complete stuck action stuck:" + navigator.getOurEntity() + " desired:" + navigator.getSafeDestination() + " stuckLevel:" + stuckLevel + " teleport:"
                            + canTeleportGoal);
        }

        if (canTeleportGoal && desired != null && desired != BlockPos.ZERO)
        {
            final BlockPos tpPos = BlockPosUtil.findAround(world, desired, 10, 10,
                    (posworld, pos) -> SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.below()), pos.below()) == SurfaceType.WALKABLE
                            && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos), pos) == SurfaceType.DROPABLE
                            && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.above()), pos.above()) == SurfaceType.DROPABLE);
            if (tpPos != null)
            {
                entity.teleportTo(tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5);
            }
        }

        navigator.stop();
        resetGlobalStuckTimers();
    }
}
