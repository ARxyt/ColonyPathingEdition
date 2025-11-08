package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.MinecoloniesAdvancedPathNavigateAccessor;
import com.minecolonies.api.entity.pathfinding.IMinecoloniesNavigator;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.entity.pathfinding.navigation.PathingStuckHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.Random;

@Mixin(PathingStuckHandler.class)
public abstract class UnstuckMixin<NAV extends PathNavigation & IMinecoloniesNavigator> implements MinecoloniesAdvancedPathNavigateAccessor {

    @Shadow (remap = false) private int delayToNextUnstuckAction;
    @Shadow (remap = false) private int stuckLevel;
    @Shadow (remap = false) private Random rand;
    @Shadow (remap = false) private BlockPos moveAwayStartPos;
    @Shadow (remap = false) private boolean hadPath;
    @Shadow (remap = false) private boolean canBreakBlocks;
    @Shadow (remap = false) private boolean canPlaceLadders;
    @Shadow (remap = false) private boolean canBuildLeafBridges;
    @Shadow (remap = false) private int teleportRange;
    @Shadow (remap = false) private Direction movingAwayDir;
    @Shadow (remap = false) private BlockPos prevDestination;

    @Shadow (remap = false) protected abstract void placeLadders(NAV navigator);
    @Shadow (remap = false) protected abstract void placeLeaves(NAV navigator);
    @Shadow (remap = false) protected abstract void breakBlocks(NAV navigator);
    @Shadow (remap = false) protected abstract void resetStuckTimers();
    @Shadow (remap = false) protected abstract void completeStuckAction(NAV navigator);

    @Unique private static final int TICKS_PER_BLOCK = 20;

    @Unique private int stuckLevelRecorder = 0;
    @Unique private boolean needReset = false;

    /**
     * @author ARxyt
     * @reason 我不太想写啥原因，我已经气笑了
     */
    @Overwrite(remap = false)
    private void tryUnstuck(final NAV navigator)
    {
        if (delayToNextUnstuckAction > 0)
        {
            return;
        }

        // (补丁)试图阻止一下在工位的随机游走
        if ((prevDestination == null || prevDestination.equals(BlockPos.ZERO)) && stuckLevel == 0 && stuckLevelRecorder != 0 ){
            stuckLevelRecorder = 5;
            return;
        }

        delayToNextUnstuckAction = 50;

        // 向前小距离传送
        boolean teleported = false;
        if (teleportRange > 0 && hadPath)
        {
            int index = Math.min(Objects.requireNonNull(navigator.getPath()).getNextNodeIndex() + teleportRange, navigator.getPath().getNodeCount() - 1);
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
            stuckLevel=Math.max(stuckLevelRecorder,stuckLevel);
            stuckLevel++;
        }

        // 新绕路算法
        if ((stuckLevel <= 4 && !teleported) && prevDestination != null && !prevDestination.equals(BlockPos.ZERO)) {
            if (navigator.getPath() != null) {
                moveAwayStartPos = navigator.getPath().getNodePos(navigator.getPath().getNextNodeIndex());
            } else {
                moveAwayStartPos = navigator.getOurEntity().blockPosition().above();
            }
            int range = 20 * stuckLevel;
            BlockPos startPos = navigator.getOurEntity().blockPosition();
            //获取一个可能是通路的方向(因为同向遍历已近似广度优先，绕道到另一侧可以最大化收益)
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
            ((MinecoloniesAdvancedPathNavigateAccessor) navigator).invokeWalkTowards(prevDestination.offset(dPos), 100, 1.0f + 0.4f * stuckLevel);
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

        // 这是调用路径完全卡住之后直接传送到目标地点的算法的地方
        if (stuckLevel >= 5)
        {
            completeStuckAction(navigator);
            resetStuckTimers();
            stuckLevelRecorder = 0;
        }
    }
}
