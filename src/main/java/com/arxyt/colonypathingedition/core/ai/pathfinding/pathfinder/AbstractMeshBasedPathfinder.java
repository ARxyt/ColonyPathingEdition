package com.arxyt.colonypathingedition.core.ai.pathfinding.pathfinder;

import com.arxyt.colonypathingedition.api.pathfinder.IPathfinder;
import com.arxyt.colonypathingedition.core.ai.pathfinding.structure.PathfinderOptions;
import com.arxyt.colonypathingedition.core.ai.pathfinding.structure.SpecialInfoNode;
import com.arxyt.colonypathingedition.core.util.PathfinderUtils;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.PathingOptions;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public abstract class AbstractMeshBasedPathfinder implements IPathfinder {
    /**
     * Start position to path from.
     */
    @NotNull
    protected final BlockPos start;

    /**
     * The pathing cache.
     */
    @NotNull
    protected final LevelReader world;

    /**
     * The entity this job belongs to, can be none
     */
    @Nullable
    protected Mob entity = null;

    /**
     * Maximum nodes we can visit.
     */
    protected int maxNodes;

    /**
     * Queue of all open nodes.
     */
    private Queue<SpecialInfoNode> nodesToVisit = new PriorityQueue<>();
    private final Queue<SpecialInfoNode> pathNodesToVisit = new PriorityQueue<>();

    /**
     * Queue of all the visited nodes.
     */
    private final Long2ObjectOpenHashMap<SpecialInfoNode> nodes = new Long2ObjectOpenHashMap<>();

    @NotNull
    private final PathfinderOptions pathfinderOptions;

    private BlockEntity townhall;

    /**
     * First node
     */
    private SpecialInfoNode startNode = null;

    /**
     * Current best node
     */
    private SpecialInfoNode bestNode = null;

    public boolean reachesDestination = false;

    public AbstractMeshBasedPathfinder(@NotNull PathfinderOptions pathfinderOptions,@NotNull BlockPos start, @NotNull LevelReader world) {
        this.pathfinderOptions = pathfinderOptions;
        this.start = start;
        this.world = world;
    }

    public AbstractMeshBasedPathfinder(@NotNull PathingOptions pathingOptions, @NotNull BlockPos start, @NotNull LevelReader world) {
        this.pathfinderOptions = new PathfinderOptions(pathingOptions);
        this.start = start;
        this.world = world;
    }

    protected abstract boolean isAtDestination(SpecialInfoNode n);

    public Path search() {
        bestNode = setupStartNode();
        double bestNodeEndScore = bestNode.h;

        boolean shouldSkip = false;
        while (!nodesToVisit.isEmpty())
        {
            if (Thread.currentThread().isInterrupted())
            {
                return null;
            }

            Queue<SpecialInfoNode> cheapestNodelist = new ArrayDeque<>();
            if(nodesToVisit.peek() != null){
                pathNodesToVisit.remove(nodesToVisit.peek());
                cheapestNodelist.add(nodesToVisit.poll());
            }

            for (int i = 0; i < pathfinderOptions.extendCount - 1; i++) {
                if(pathNodesToVisit.peek() != null) {
                    nodesToVisit.remove(pathNodesToVisit.peek());
                    cheapestNodelist.add(pathNodesToVisit.poll());
                }
                else break;
            }

            while (!cheapestNodelist.isEmpty()) {
                final SpecialInfoNode node = cheapestNodelist.poll();

                if (node == null){
                    continue;
                }

                if (node.visitTimes > 0) {
                    // Revisiting is used to update neighbours to an updated cost
                    //visitNode(node);
                    node.visitTimes++;
                    continue;
                }

                // Limiting max amount of nodes mapped, encountering a high-cost node increases the limit
                if (nodes.size() > Math.min(5000, maxNodes + node.h * 2)) {
                    shouldSkip = true;
                    break;
                }

                if (isAtDestination(node)) {
                    bestNode = node;
                    bestNodeEndScore = computeHeuristic(bestNode);
                    //handleDebugPathReach(bestNode);
                    reachesDestination = true;
                    shouldSkip = true;
                    break;
                }

                if (node.cameFrom != null) {
                    // Calculates a score for a possible end node, defaults to heuristic(closest)
                    final double nodeEndSCore = computeHeuristic(node);
                    if (nodeEndSCore < bestNodeEndScore) {
                        bestNode = node;
                        bestNodeEndScore = nodeEndSCore;
                    }
                }

                // TODO: handleDebugOptions(node);
                //visitNode(node);
                node.visitTimes ++;
            }
            if(shouldSkip) break;
        }

        int extraNodes = Math.max(15 + (int)(15 * bestNodeEndScore * bestNodeEndScore), 100);
        // Explore additional possible endnodes after reaching, if we got extra nodes to search
        if (extraNodes > 0 && reachesDestination)
        {
            // Make sure to expand from the final node
            //visitNode(bestNode);

            if (!nodesToVisit.isEmpty())
            {
                // Search only closest nodes to the goal
                final Queue<SpecialInfoNode> original = nodesToVisit;
                nodesToVisit = new PriorityQueue<>(nodesToVisit.size(), Comparator.comparingDouble(node -> node.h));
                nodesToVisit.addAll(original);

                while (!nodesToVisit.isEmpty())
                {
                    if (Thread.currentThread().isInterrupted())
                    {
                        return null;
                    }

                    final SpecialInfoNode node = nodesToVisit.poll();

                    // reset score
                    final double nodeEndSCore = computeHeuristic(node);
                    if (nodeEndSCore < bestNodeEndScore && isAtDestination(node))
                    {
                        bestNode = node;
                        bestNodeEndScore = nodeEndSCore;
                    }

                    // we already at best score, return.
                    if(bestNodeEndScore <= 1) break;

                    // no need to count, directly visit.
                    if (node.visitTimes > 0)
                    {
                        //visitNode(node);
                        node.visitTimes ++;
                        continue;
                    }

                    // counter on actual extra nodes.
                    // TODO: handleDebugOptions(node);
                    if (--extraNodes <= 0) break;
                    //visitNode(node);
                }
            }
        }

        return null;
        //return finalizePath(bestNode);
    }

    private SpecialInfoNode setupStartNode() {
        final SpecialInfoNode startNode = new SpecialInfoNode(start);
        startNode.h = computeHeuristic(startNode);
        startNode.g = 0;
        startNode.f = startNode.h;
        final BlockState aboveState = world.getBlockState(start.above());
        final BlockState thisState = world.getBlockState(start);
        final BlockState belowState = world.getBlockState(start.below());
        startNode.inDanger = PathfinderUtils.isDangerous(thisState) || PathfinderUtils.isDangerous(belowState);
        if(!(startNode.inDanger)) {
            // slight calculate on voxels, only check if head is stuck in blocks.
            final VoxelShape aboveShape = aboveState.getCollisionShape(world, start.above());
            final VoxelShape belowShape = belowState.getCollisionShape(world, start.below());
            double offsetY = PathfinderUtils.refinedVoxelShapeMin(aboveShape, Direction.Axis.Y) - PathfinderUtils.refinedVoxelShapeMax(belowShape, Direction.Axis.Y);
            if (offsetY > 0) {
                double positiveOffsetX = PathfinderUtils.refinedVoxelShapeMax(aboveShape, Direction.Axis.X);
                double negativeOffsetX = 1 - PathfinderUtils.refinedVoxelShapeMin(aboveShape, Direction.Axis.X);
                final double offsetX = positiveOffsetX > negativeOffsetX ? positiveOffsetX : -negativeOffsetX;
                double positiveOffsetZ = PathfinderUtils.refinedVoxelShapeMax(aboveShape, Direction.Axis.Z);
                double negativeOffsetZ = 1 - PathfinderUtils.refinedVoxelShapeMin(aboveShape, Direction.Axis.Z);
                final double offsetZ = positiveOffsetZ > negativeOffsetZ ? positiveOffsetZ : -negativeOffsetZ;

                startNode.inDanger = !(Math.min(offsetX, offsetZ) < 0.25);
            }
        }

        startNode.onLadder = PathfinderUtils.canClimb(thisState, pathfinderOptions);

        if(PathfinderUtils.isLiquid(thisState) || PathfinderUtils.isLiquid(aboveState)) {
            startNode.diving = true;
            startNode.waterEntrance = true;
        }
        else if(PathfinderUtils.isLiquid(belowState)) {
            startNode.swimming = true;
            startNode.waterEntrance = true;
        }
        if (pathfinderOptions.canUseRails) {
            startNode.onRails = thisState.getBlock() instanceof BaseRailBlock;
            startNode.railsEntrance = startNode.onRails;
        }
        this.startNode = startNode;
        nodes.put(startNode.hashLong, startNode);
        nodesToVisit.offer(startNode);
        return startNode;
    }

    private SpecialInfoNode setupNode(BlockPos pos, @Nullable SpecialInfoNode cameFrom)
    {
        final SpecialInfoNode thisNode = new SpecialInfoNode(pos);
        thisNode.cameFrom = cameFrom;
        final BlockState thisState = world.getBlockState(pos);
        final BlockState belowState = world.getBlockState(pos.below());
        final BlockState aboveState = world.getBlockState(pos.above());
//        if(cameFrom != null && !isValidPlaceToGo(thisState, belowState, aboveState, pos, cameFrom)) {
//            ;
//        }
        thisNode.onLadder = PathfinderUtils.canClimb(thisState, pathfinderOptions);

        if(PathfinderUtils.isLiquid(thisState) || PathfinderUtils.isLiquid(aboveState)) {
            thisNode.diving = true;
        }
        else if(PathfinderUtils.isLiquid(belowState)) {
            thisNode.swimming = true;
        }
        if (pathfinderOptions.canUseRails) {
            thisNode.onRails = thisState.getBlock() instanceof BaseRailBlock;
        }
        return thisNode;
    }
}
