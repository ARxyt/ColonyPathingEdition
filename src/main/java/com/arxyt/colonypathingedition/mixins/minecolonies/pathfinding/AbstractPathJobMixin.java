package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.api.IMNodeExtras;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.ldtteam.domumornamentum.block.decorative.PanelBlock;
import com.ldtteam.domumornamentum.block.decorative.PostBlock;
import com.ldtteam.domumornamentum.block.decorative.ShingleBlock;
import com.ldtteam.domumornamentum.block.decorative.ShingleSlabBlock;
import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.PathingOptions;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import com.minecolonies.core.entity.pathfinding.world.CachingBlockLookup;
import com.minecolonies.core.util.WorkerUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static com.minecolonies.api.util.BlockPosUtil.directionFromDelta;
import static com.minecolonies.api.util.constant.PathingConstants.HALF_A_BLOCK;
import static com.minecolonies.api.util.constant.PathingConstants.MAX_JUMP_HEIGHT;
import static com.minecolonies.core.entity.pathfinding.PathingOptions.MAX_COST;

@Mixin(value = AbstractPathJob.class, remap = false)
public abstract class AbstractPathJobMixin{

    @Final @Shadow(remap = false) private Level actualWorld;
    @Final @Shadow(remap = false) public static int MAX_NODES;
    @Final @Shadow(remap = false) private Int2ObjectOpenHashMap<MNode> nodes;
    @Final @Shadow(remap = false) protected LevelReader world;
    @Final @Shadow(remap = false) @NotNull protected BlockPos start;
    @Final @Shadow(remap = false) protected PathResult result;

    @Shadow(remap = false) protected int totalNodesVisited;
    @Shadow(remap = false) private boolean reachesDestination;
    @Shadow(remap = false) public int extraNodes;
    @Shadow(remap = false) private PathingOptions pathingOptions;
    @Shadow(remap = false) protected CachingBlockLookup cachedBlockLookup;
    @Shadow(remap = false) protected BlockPos.MutableBlockPos tempWorldPos;
    @Shadow(remap = false) protected int maxNodes;
    @Shadow(remap = false) private double maxCost;
    @Shadow(remap = false) private int visitedLevel;
    @Shadow(remap = false) private Queue<MNode> nodesToVisit;
    @Shadow(remap = false) private MNode bestNode;

    @Shadow(remap = false) public abstract Mob getEntity();
    @Shadow(remap = false) protected abstract boolean isPassable(int x, int y, int z, boolean head, MNode parent);
    @Shadow(remap = false) public abstract PathingOptions getPathingOptions();
    @Shadow(remap = false) protected abstract boolean canLeaveBlock(int x, int y, int z, int parentX, int parentY, int parentZ, boolean head);
    @Shadow(remap = false) protected abstract boolean canLeaveBlock(final int x, final int y, final int z, final MNode parent, final boolean head);
    @Shadow(remap = false) protected abstract MNode getAndSetupStartNode();
    @Shadow(remap = false) protected abstract double getEndNodeScore(MNode n);
    @Shadow(remap = false) protected abstract void handleDebugExtraNode(MNode node);
    @Shadow(remap = false) protected abstract void handleDebugPathReach(MNode bestNode);
    @Shadow(remap = false) protected abstract void handleDebugOptions(MNode node);
    @Shadow(remap = false) protected abstract boolean isAtDestination(MNode n);
    @Shadow(remap = false) protected abstract boolean stopOnNodeLimit(int totalNodesVisited, MNode bestNode, int nodesSinceEndNode);
    @Shadow(remap = false) protected abstract void visitNode(MNode node);
    @Shadow(remap = false) @NotNull protected abstract Path finalizePath(MNode targetNode);


    @Invoker(value="getGroundHeight",remap = false)
    public abstract int invokeGetGroundHeight(final MNode node, final int x, final int y, final int z);

    @Invoker(value="createNode",remap = false)
    public abstract MNode invokeCreateNode(final MNode parent, final int x, final int y, final int z, final double heuristic, final double cost);

    @Invoker(value="calculateSwimming",remap = false)
    public abstract boolean invokeCalculateSwimming(final BlockState below, final BlockState state, final BlockState above, @Nullable final MNode node);

    @Invoker(value="modifyCost",remap = false)
    public abstract double invokeModifyCost(
            final double cost,
            final MNode parent,
            final boolean swimstart,
            final boolean swimming,
            final int x,
            final int y,
            final int z,
            final BlockState state, final BlockState below);

    @Invoker(value="computeHeuristic",remap = false)
    protected abstract double computeHeuristic(final int x, final int y, final int z);

    @Unique final private int callbackTimesTolerance =  PathingConfig.CALLBACK_TIMES_TOLERANCE.get();
    @Unique final private int extendCount =  PathingConfig.NODE_EXTEND_COUNT.get();
    @Unique final protected double onRailPreference = PathingConfig.ONRAIL_PREFERENCE.get();
    @Unique final protected double onRoadPreference = PathingConfig.ONROAD_PREFERENCE.get();
    @Unique final private double swimmingPreference = PathingConfig.SWIMMING_PREFERENCE.get();
    @Unique final private double onRailCallbackMultiplier = PathingConfig.ONRAIL_CALLBACK_MULTIPLIER.get();
    @Unique final private double onRoadCallbackMultiplier = PathingConfig.ONROAD_CALLBACK_MULTIPLIER.get();
    @Unique public double ladderSwitchCost = PathingConfig.LADDER_SWITCH_COST_DEFINER.get();
    @Unique public double shingleCost = PathingConfig.SHINGLE_COST_DEFINER.get();
    @Unique public double destroyingFarmlandCost = PathingConfig.FARMLAND_COST_DEFINER.get();
    @Unique public double leafCost = PathingConfig.LEAF_COST_DEFINER.get();
    @Unique public double sweetBerryCost = PathingConfig.WATER_COST_DEFINER.get();
    @Unique private BlockEntity townhall;
    @Unique protected int actualMaxNodes;
    @Unique private Queue<MNode> pathNodesToVisit;

    /**
     * @author ARxyt
     * @reason Add some change reported in issue but rejected.
     */
    @Overwrite(remap = false)
    protected double computeCost(
            final MNode parent, final int dX, final int dY, final int dZ,
            final boolean isSwimming,
            final boolean onPath,
            final boolean isDiving,
            final boolean onRails,
            final boolean railsExit,
            final boolean swimStart,
            final boolean ladder,
            final BlockState state, final BlockState below,
            final int x, final int y, final int z) {

        double cost = 1;

        IMNodeExtras extras = (IMNodeExtras) parent;
        if (onRails) {
            cost *= pathingOptions.onRailCost;
            if (state.getBlock() instanceof PoweredRailBlock && !(state.getValue(PoweredRailBlock.POWERED)))
            {
                return 25.0;
            }
            return cost ;
        }

        if (railsExit && !extras.isStation())
        {
            cost += pathingOptions.railsExitCost;
        }

        // 原逻辑：随机性因子
        if (pathingOptions.randomnessFactor > 0.0d) {
            cost += ColonyConstants.rand.nextDouble() * pathingOptions.randomnessFactor;
        }

        // 原逻辑：洞穴空气成本
        if (state.getBlock() == Blocks.CAVE_AIR) {
            cost += pathingOptions.caveAirCost;
        }

        if (state.hasProperty(BlockStateProperties.OPEN) && !(state.getBlock() instanceof PanelBlock))
        {
            cost += pathingOptions.traverseToggleAbleCost;
        }
        else
        {
            if (!onPath && ShapeUtil.hasCollision(cachedBlockLookup, tempWorldPos.set(x, y, z), state))
            {
                cost += pathingOptions.walkInShapesCost;
            }
        }

        if (!isSwimming) {
            if (onPath) {
                cost *= pathingOptions.onPathCost;
            }
        }

        boolean nextOnSlab = (below.getBlock() instanceof SlabBlock) && below.getValue(SlabBlock.TYPE)== SlabType.BOTTOM;
        double halfY = (nextOnSlab ? -0.5 : 0.0) + (extras.getOnSlab() ? 0.5 : 0.0);
        double dYDouble = (double)dY + halfY;

        if (!isDiving)
        {
            if (Math.abs(dYDouble) > 0.6 && !ladder && !(dY == 1 && (below.getBlock() instanceof StairBlock) && below.getValue(StairBlock.FACING) == directionFromDelta(dX,0,dZ) && below.getValue(StairBlock.HALF) == Half.BOTTOM))
            {
                if (dYDouble > 0.0)
                {
                    double basicJumpCost = pathingOptions.jumpCost;
                    if (onPath){
                        basicJumpCost *= pathingOptions.onPathCost;
                    }
                    if (extras.getOnFarmland()){
                        basicJumpCost += destroyingFarmlandCost;
                    }
                    cost += basicJumpCost;
                }
                else if ( pathingOptions.dropCost != 0)
                {
                    if (!(dY==1 && below.getBlock() instanceof StairBlock)) {
                        double basicDropCost = Math.abs(Math.pow((dYDouble + 2. / 5) , 3))- 8. / 125;
                        if (dYDouble > -3.0 && onPath){
                            basicDropCost *= pathingOptions.onPathCost;
                        }
                        else {
                            basicDropCost *= Math.abs(dYDouble);
                        }
                        cost += pathingOptions.dropCost * basicDropCost;
                    }
                    if (below.getBlock() instanceof FarmBlock){
                        cost += destroyingFarmlandCost;
                    }
                }
            }
            else if (ladder && parent.isLadder() && dY == 0){
                cost += ladderSwitchCost;
            }
        }

        if (below.getBlock() instanceof PanelBlock){
            cost += 0.2;
        }

        if (below.getBlock() instanceof ShingleBlock || below.getBlock() instanceof ShingleSlabBlock)
        {
            cost += shingleCost;
        }

        if (below.getBlock() instanceof LeavesBlock)
        {
            cost += leafCost;
        }

        if (state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof WebBlock)
        {
            cost += sweetBerryCost;
        }

        if (!isDiving && ladder && !parent.isLadder() && !(state.getBlock() instanceof LadderBlock))
        {
            cost += pathingOptions.nonLadderClimbableCost;
        }

        if (isSwimming){
            if (swimStart) {
                cost += pathingOptions.swimCostEnter;
            } else {
                cost += pathingOptions.swimCost;
            }
            if (isDiving) {
                cost += pathingOptions.divingCost;
            }
        }
        return cost;
    }

    /**
     * @author ARxyt
     * @reason Explore stretgies reworked, to explore more nodes that "cheap".
     */
    @Nullable
    @Overwrite(remap = false)
    protected Path search()
    {
        this.actualMaxNodes = this.maxNodes;
        this.pathNodesToVisit = new PriorityQueue<>();
        bestNode = getAndSetupStartNode();
        double bestNodeEndScore = getEndNodeScore(bestNode);
        // Node count since we found a better end node than the current one
        int nodesSinceEndNode = 0;

        boolean shouldSkip = false;
        while (!nodesToVisit.isEmpty())
        {
            if (Thread.currentThread().isInterrupted())
            {
                return null;
            }

            Queue<MNode> cheapestNodelist = new ArrayDeque<>();
            if(nodesToVisit.peek() != null){
                pathNodesToVisit.remove(nodesToVisit.peek());
                cheapestNodelist.add(nodesToVisit.poll());
            }

            for (int i = 0; i < extendCount - 1; i++) {
                if(pathNodesToVisit.peek() != null) {
                    nodesToVisit.remove(pathNodesToVisit.peek());
                    cheapestNodelist.add(pathNodesToVisit.poll());
                }
                else break;
            }

            while (!cheapestNodelist.isEmpty()) {
                final MNode node = cheapestNodelist.poll();

                if (node == null){
                    continue;
                }

                if (node.isVisited()) {
                    // Revisiting is used to update neighbours to an updated cost
                    visitNode(node);
                    node.increaseVisited();
                    continue;
                }

                nodesSinceEndNode++;
                totalNodesVisited++;

                // Limiting max amount of nodes mapped, encountering a high cost node increases the limit
                if (totalNodesVisited > Math.min(MAX_NODES, maxNodes + node.getHeuristic() * 2)) {
                    if (stopOnNodeLimit(totalNodesVisited, bestNode, nodesSinceEndNode)) {
                        shouldSkip = true;
                        break;
                    }
                }

                if (!reachesDestination && isAtDestination(node)) {
                    bestNode = node;
                    bestNodeEndScore = getEndNodeScore(node);
                    result.setPathReachesDestination(true);
                    handleDebugPathReach(bestNode);
                    reachesDestination = true;
                    shouldSkip = true;
                    break;
                }

                if (!node.isCornerNode()) {
                    // Calculates a score for a possible end node, defaults to heuristic(closest)
                    final double nodeEndSCore = getEndNodeScore(node);
                    if (nodeEndSCore < bestNodeEndScore) {
                        if (!reachesDestination || isAtDestination(node)) {
                            nodesSinceEndNode = 0;
                            bestNode = node;
                            bestNodeEndScore = nodeEndSCore;
                        }
                    }
                }

                // Don't keep searching more costly nodes when there is a destination
                if (reachesDestination && node.getScore() > bestNode.getScore()) {
                    shouldSkip = true;
                    break;
                }

                handleDebugOptions(node);
                visitNode(node);
                node.increaseVisited();
            }
            if(shouldSkip) break;
        }

        // Explore additional possible endnodes after reaching, if we got extra nodes to search
        if (extraNodes > 0 && reachesDestination)
        {
            // Make sure to expand from the final node
            visitNode(bestNode);

            if (!nodesToVisit.isEmpty())
            {
                // Search only closest nodes to the goal
                final Queue<MNode> original = nodesToVisit;
                nodesToVisit = new PriorityQueue<>(nodesToVisit.size(), (a, b) -> {
                    if ((a.getHeuristic()) < (b.getHeuristic()))
                    {
                        return -1;
                    }
                    else if (a.getHeuristic() > b.getHeuristic())
                    {
                        return 1;
                    }
                    else
                    {
                        return a.getCounterAdded() - b.getCounterAdded();
                    }
                });
                nodesToVisit.addAll(original);

                while (!nodesToVisit.isEmpty())
                {
                    if (Thread.currentThread().isInterrupted())
                    {
                        return null;
                    }

                    final MNode node = nodesToVisit.poll();
                    if (node.isVisited())
                    {
                        visitNode(node);
                        continue;
                    }

                    handleDebugExtraNode(node);

                    final double nodeEndSCore = getEndNodeScore(node);
                    if (nodeEndSCore < bestNodeEndScore && (!reachesDestination || isAtDestination(node)))
                    {
                        bestNode = node;
                        bestNodeEndScore = nodeEndSCore;
                    }

                    if (extraNodes > 0)
                    {
                        extraNodes--;
                        if (extraNodes == 0)
                        {
                            break;
                        }
                    }
                    visitNode(node);
                }
            }
        }

        return finalizePath(bestNode);
    }


    @Unique
    private int recheckGroundHeight(int x, int y, int z){
        final BlockState state = cachedBlockLookup.getBlockState(x, y , z);
        if (ShapeUtil.max(state.getCollisionShape(world, new BlockPos(x, y, z)), Direction.Axis.Y) != 0){
            return y;
        }
        final BlockState belowState = cachedBlockLookup.getBlockState(x, y - 1, z);
        boolean belowIsWater = PathfindingUtils.isWater(cachedBlockLookup, null, belowState, null);
        if(!belowIsWater && (ShapeUtil.getEndY(belowState.getCollisionShape(world, tempWorldPos.set(x, y - 1, z)), 0) < 0.125) )
        {
            return y - 1;
        }
        return y;
    }

    @Unique
    private boolean checkConerCollision(int x, int y, int z) {
        final BlockState above = cachedBlockLookup.getBlockState(x,y+1,z);
        return !(ShapeUtil.getStartY(above.getCollisionShape(world, tempWorldPos.set(x, y + 1, z)), 1) < 0.875) || above.hasProperty(BlockStateProperties.OPEN);
    }

    @Unique
    private boolean checkPossiblyPassing(MNode node, int nextX, int nextY, int nextZ, MNode cornerNode, int dX, int dY, int dZ){
        BlockPos cornerPos = new BlockPos(cornerNode.x,cornerNode.y,cornerNode.z).above();
        BlockState cornerState = cachedBlockLookup.getBlockState(cornerPos);
        if(cornerState.getBlock() instanceof PanelBlock){
            if(!cornerState.getValue(BlockStateProperties.OPEN)){
                return false;
            }
            else{
                boolean possiblyPassing = true;
                if(dX != 0){
                    possiblyPassing = dX > 0 ? ShapeUtil.min(cornerState.getCollisionShape(world, cornerPos), Direction.Axis.X) != 0 : ShapeUtil.max(cornerState.getCollisionShape(world, cornerPos), Direction.Axis.X) != 1;
                }
                if(dZ != 0){
                    possiblyPassing = possiblyPassing && dZ > 0 ? ShapeUtil.min(cornerState.getCollisionShape(world, cornerPos), Direction.Axis.Z) != 0 : ShapeUtil.max(cornerState.getCollisionShape(world, cornerPos), Direction.Axis.Z) != 1;
                }
                return possiblyPassing;
            }
        }
        BlockPos checkPos;
        if(dY > 0){
            checkPos = new BlockPos(nextX,nextY,nextZ).below();
        }
        else{
            checkPos = new BlockPos(node.x,node.y,node.z).below();
        }
        BlockState checkState = cachedBlockLookup.getBlockState(checkPos);
        double rawDY = 2 + ShapeUtil.getStartY(cornerState.getCollisionShape(world, tempWorldPos.set(cornerPos)), 1) - ShapeUtil.getEndY(checkState.getCollisionShape(world, tempWorldPos.set(checkPos)),0);
        //目前先用着rawDY，这是对楼梯间缝隙的粗略估计，精细估计需要对缝隙进行更详细的刻画，之后再补，现在先这么用着
        return !(rawDY > 1.8);
    }

    @Unique
    private boolean checkConnection(MNode node, int dX, int dZ){
        if (node.isOnRails()){
            BlockState railState = cachedBlockLookup.getBlockState(node.x, node.y, node.z);
            RailShape railShape;
            if (railState.hasProperty(BlockStateProperties.RAIL_SHAPE))
            {
                railShape = railState.getValue(BlockStateProperties.RAIL_SHAPE);
            }
            else if(railState.hasProperty(BlockStateProperties.RAIL_SHAPE_STRAIGHT))
            {
                railShape = railState.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT);
            }
            else{
                return true;
            }
            return switch (railShape) {
                case NORTH_SOUTH, ASCENDING_SOUTH, ASCENDING_NORTH -> dZ != 0;
                case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> dX != 0;
                case NORTH_EAST -> dX > 0 || dZ < 0;
                case NORTH_WEST -> dX < 0 || dZ < 0;
                case SOUTH_EAST -> dX > 0 || dZ > 0;
                case SOUTH_WEST -> dX < 0 || dZ > 0;
            };
        }
        return true;
    }

    /**
     * @author ARxyt
     * @reason Some not valid drop change.
     */
    @Overwrite(remap = false)
    private int checkDrop(@Nullable final MNode parent, final int x, final int y, final int z, final boolean isSwimming) {
        final boolean canDrop = parent != null && !parent.isLadder();
        //  Nothing to stand on
        if (!canDrop || ((parent.x != x || parent.z != z) && isPassable(parent.x, parent.y - 1, parent.z, false, parent)
                &&
                SurfaceType.getSurfaceType(world,
                        cachedBlockLookup.getBlockState(parent.x, parent.y - 1, parent.z),
                        tempWorldPos.set(parent.x, parent.y - 1, parent.z),
                        getPathingOptions())
                        == SurfaceType.DROPABLE
                &&
                SurfaceType.getSurfaceType(world,
                        cachedBlockLookup.getBlockState(parent.x, parent.y, parent.z),
                        tempWorldPos.set(parent.x, parent.y, parent.z),
                        getPathingOptions())
                        == SurfaceType.DROPABLE)) {
            return Integer.MIN_VALUE;
        }

        for (int i = 1; i <= (pathingOptions.canDrop ? 10 : 2); i++) {
            final BlockState below = cachedBlockLookup.getBlockState(x, y - i, z);
            if (below.getBlock() instanceof BaseRailBlock){
                return y - i + 1;
            }
            if (!canLeaveBlock(x, y - 1, z, x, y, z, false)) {
                return Integer.MIN_VALUE;
            }
            if (SurfaceType.getSurfaceType(world, below, tempWorldPos.set(x, y - i, z), getPathingOptions()) == SurfaceType.WALKABLE) {
                //  Level path
                return y - i + 1;
            } else if (!(below.isAir() || below.getCollisionShape(world, new BlockPos(x, y-1, z)).isEmpty())) {
                if (PathfindingUtils.isLadder(below, pathingOptions)) {
                    return y - i + 1;
                } else {
                    return Integer.MIN_VALUE;
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * @author ARxyt
     * @reason Delete corner node, rewrite corner check method.
     */
    @Overwrite( remap = false)
    protected final void exploreInDirection(final MNode node, int dX, int dY, int dZ) {
        int nextX = node.x + dX;
        int nextY = node.y + dY;
        int nextZ = node.z + dZ;

        //  Can we traverse into this node?  Fix the y up, skip on already explored nodes
        final int firstY = invokeGetGroundHeight(node, nextX, nextY, nextZ);
        if (firstY < world.getMinBuildHeight())
        {
            return;
        }

        final int newY = recheckGroundHeight(nextX, firstY, nextZ);
        if (nextY != newY)
        {
            int conerX,conerY,conerZ;
            // if the new position is above the current node, we're taking the node directly above
            if (newY - node.y > 0 )
            {
                if (newY - node.y > 1){
                    return;
                }
                conerX = node.x;
                conerY = newY;
                conerZ = node.z;
            }
            // If we're going down, take the air-corner before going to the lower node
            else
            {
                conerX = nextX;
                conerY = node.y;
                conerZ = nextZ;
            }
            final int nodeKey = MNode.computeNodeKey(conerX, conerY, conerZ);
            MNode conerNode = nodes.get(nodeKey);
            if (conerNode == null){
                boolean isPassable = checkConerCollision(conerX, conerY, conerZ);
                conerNode = invokeCreateNode(null, conerX, conerY, conerZ, node.getHeuristic(), node.getCost());
                conerNode.setCornerNode(isPassable);
                conerNode.increaseVisited();
                if(!isPassable && checkPossiblyPassing(node, nextX, newY, nextZ, conerNode, dX, newY - node.y, dZ)) {
                    return;
                }
            }
            else if(!conerNode.isCornerNode() && checkPossiblyPassing(node, nextX, newY, nextZ, conerNode, dX, newY - node.y, dZ)){
                return;
            }
            dY = newY - node.y;
        }

        nextY = newY;
        final int nodeKey = MNode.computeNodeKey(nextX, nextY, nextZ);
        MNode nextNode = nodes.get(nodeKey);

        // Current node is already visited, only update nearby costs do not create new nodes
        if (node.isVisited())
        {
            if (nextNode == null || nextNode == node.parent || nextNode == node)
            {
                return;
            }
        }

        final BlockState aboveState = cachedBlockLookup.getBlockState(nextX, nextY + 1, nextZ);
        final BlockPos pos = new BlockPos(nextX, nextY, nextZ);
        final BlockState state = cachedBlockLookup.getBlockState(nextX, nextY, nextZ);
        final BlockPos below = new BlockPos(nextX, nextY - 1, nextZ);
        final BlockState belowState = cachedBlockLookup.getBlockState(nextX, nextY - 1, nextZ);

        if(HasBlockedTag(pos)||HasBlockedTag(below)){
            return;
        }

        final boolean isSwimming = invokeCalculateSwimming(belowState, state, aboveState, nextNode) && !(state.getBlock() instanceof WaterlilyBlock);
        if (isSwimming && !pathingOptions.canSwim()) {
            return;
        }

        if(belowState.getBlock() instanceof AbstractCauldronBlock){
            return;
        }
        if(state.getBlock() instanceof PostBlock){
            return;
        }

        final boolean swimStart = isSwimming && !node.isSwimming();
        final boolean onRails = pathingOptions.canUseRails() && state.getBlock() instanceof BaseRailBlock && checkConnection(node,dX,dZ);
        final boolean ladder = PathfindingUtils.isLadder(state, pathingOptions);
        final boolean onRoad = HasPathTag(below)||HasPathTag(pos)||(ladder||PathfindingUtils.isLadder(belowState, pathingOptions)||WorkerUtil.isPathBlock(belowState.getBlock())||WorkerUtil.isPathBlock(state.getBlock()))&&!(HasNotPathTag(below)||HasNotPathTag(pos));
        final boolean isDiving = isSwimming && PathfindingUtils.isWater(world, null, aboveState, null);


        final boolean railsExit = !onRails && node.isOnRails();
        double nextCost;
        nextCost = computeCost(node, dX, dY, dZ, isSwimming, onRoad, isDiving, onRails, railsExit, swimStart, ladder, state, belowState, nextX, nextY, nextZ);
        nextCost = invokeModifyCost(nextCost, node, swimStart, isSwimming, nextX, nextY, nextZ, state, belowState);

        if (nextCost > maxCost)
        {
            maxCost = Math.min(MAX_COST, Math.ceil(nextCost));
        }

        // since there may be a heuristic mod bug in Minecolonies, we abandon it.
        final double heuristic = computeHeuristic(nextX, nextY, nextZ);
        final double cost = node.getCost() + nextCost;

//        // fix to distant horizon, compatibility fix.
//        if(node.getHeuristic() == 0 && node.parent != null && node.parent.getHeuristic() == 0){
//            return;
//        }

        if (nextNode == null)
        {
            nextNode = invokeCreateNode(node, nextX, nextY, nextZ, heuristic, cost);
            nextNode.setOnRails(onRails);
            nextNode.setCornerNode(false);

            if (isSwimming)
            {
                nextNode.setSwimming();
            }
            if (ladder)
            {
                nextNode.setLadder();
            }

            MNode extraNextNode = extraNodeState(nextNode);
            if ((onRoad || onRails) && Math.abs(dY) <= 1 ){
                extraNextNode.setHeuristic(modifyHeuristic(node, extraNextNode, nextNode.getHeuristic(), onRoad, onRails));
            }
            else {
                extraNextNode.setHeuristic(modifyHeuristic(extraNextNode, nextNode.getHeuristic(), state));
            }
            nodesToVisit.offer(extraNextNode);
            if((onRoad || onRails) && Math.abs(dY) <= 2){
                pathNodesToVisit.offer(extraNextNode);
            }
        }
        else
        {
            if ((onRoad || onRails) && Math.abs(dY) <= 1){
                nextNode.setHeuristic(modifyHeuristic(node, nextNode, nextNode.getHeuristic(), onRoad, onRails));
            }
            else {
                nextNode.setHeuristic(modifyHeuristic(nextNode, nextNode.getHeuristic(), state));
            }
            updateNode(node, nextNode, heuristic, cost, onRails, onRoad, Math.abs(dY) <= 2);
        }
    }


    /**
     * @author ARxyt
     * @reason Y axis modification on canJump tester.
     */
    @Overwrite(remap = false)
    private int handleTargetNotPassable(@Nullable final MNode parent, final int x, final int y, final int z, @NotNull final BlockState target)
    {
        final boolean canJump = parent != null && !parent.isLadder() && !parent.isSwimming();
        //  Need to try jumping up one, if we can
        if (!canJump || SurfaceType.getSurfaceType(world, target, tempWorldPos.set(x, y, z), getPathingOptions()) != SurfaceType.WALKABLE)
        {
            return Integer.MIN_VALUE;
        }

        //  Check for headroom in the target space
        if (!isPassable(x, y + 2, z, true, parent))
        {
            final VoxelShape bb1 = cachedBlockLookup.getBlockState(x, y, z).getCollisionShape(world, tempWorldPos.set(x, y, z));
            final VoxelShape bb2 = cachedBlockLookup.getBlockState(x, y + 2, z).getCollisionShape(world, tempWorldPos.set(x, y + 2, z));
            if ((y + 2 + ShapeUtil.getStartY(bb2, 1)) - (y + ShapeUtil.getEndY(bb1, 0)) < 2)
            {
                return Integer.MIN_VALUE;
            }
        }

        if (!canLeaveBlock(x, y + 2, z, parent, true))
        {
            return Integer.MIN_VALUE;
        }

        //  Check for jump room from the origin space
        if (!isPassable(parent.x, parent.y + 2, parent.z, true, parent) && !cachedBlockLookup.getBlockState(parent.x, parent.y + 2, parent.z).hasProperty(BlockStateProperties.OPEN))
        {
            final VoxelShape bb1 = cachedBlockLookup.getBlockState(x, y, z).getCollisionShape(world, tempWorldPos.set(x, y, z));
            final VoxelShape bb2 = cachedBlockLookup.getBlockState(parent.x, parent.y + 2, parent.z).getCollisionShape(world, tempWorldPos.set(parent.x, parent.y + 2, parent.z));
            if ((parent.y + 2 + ShapeUtil.getStartY(bb2, 1)) - (y + ShapeUtil.getEndY(bb1, 0)) < 2)
            {
                return Integer.MIN_VALUE;
            }
        }

        final BlockState parentBelow = cachedBlockLookup.getBlockState(parent.x, parent.y - 1, parent.z);
        final VoxelShape parentBB = parentBelow.getCollisionShape(world, tempWorldPos.set(parent.x, parent.y - 1, parent.z));

        double parentY = ShapeUtil.max(parentBB, Direction.Axis.Y);
        double parentMaxY = parentY + parent.y - 1;
        final double targetMaxY = ShapeUtil.max(target.getCollisionShape(world, tempWorldPos.set(x, y, z)), Direction.Axis.Y) + y;
        if (targetMaxY - parentMaxY < MAX_JUMP_HEIGHT)
        {
            return y + 1;
        }
        if (target.is(BlockTags.STAIRS)
                && parentY - HALF_A_BLOCK < MAX_JUMP_HEIGHT
                && target.getValue(StairBlock.HALF) == Half.BOTTOM
                && BlockPosUtil.getXZFacing(parent.x, parent.z, x, z) == target.getValue(StairBlock.FACING))
        {
            return y + 1;
        }
        return Integer.MIN_VALUE;
    }

    /**
     * @author ARxyt
     * @reason Just useless and makes no sense.
     */
    @Overwrite(remap = false)
    private boolean reevaluteHeuristic(final MNode node, final boolean reaches)
    {
        return false;
    }

    @Unique
    private MNode extraNodeState(final MNode nextNode)
    {
        IMNodeExtras extras = (IMNodeExtras) nextNode;
        if (nextNode.isCornerNode() && nextNode.parent!=null) {
            nextNode.setHeuristic(nextNode.parent.getHeuristic());
            IMNodeExtras extrasPre = (IMNodeExtras) nextNode.parent;
            if (extrasPre.isCallbackNode()){
                extras.setCallbackNode();
            }
        }

        BlockState below = cachedBlockLookup.getBlockState(nextNode.x, nextNode.y - 1, nextNode.z);
        BlockState state = cachedBlockLookup.getBlockState(nextNode.x, nextNode.y, nextNode.z);

        if (below.getBlock() instanceof FarmBlock)
        {
            extras.setOnFarmland();
        }
        if (below.getBlock() instanceof SlabBlock && below.getValue(SlabBlock.TYPE)== SlabType.BOTTOM)
        {
            extras.setOnSlab();
        }
        if (state.getBlock() instanceof DetectorRailBlock)
        {
            extras.setStation();
        }
        return nextNode;
    }

    /**
     * Heuristic correction function, making punishments on not reliable node, onWater or inCave.
     */
    @Unique
    private double modifyHeuristic(MNode nextNode, double heuristic, final BlockState state) {
        double newHeuristic = heuristic;
        if(state.getBlock() == Blocks.CAVE_AIR){
            if(world.getLightEngine() != null) {
                newHeuristic *= 1 + 0.15 * Math.max(5 - world.getBrightness(LightLayer.BLOCK, new BlockPos(nextNode.x, nextNode.y, nextNode.z)), 0);
            }
            else{
                newHeuristic *= 1 + 0.75;
            }
        }
        if (nextNode.isSwimming()){
            newHeuristic *= swimmingPreference;
        }
        return newHeuristic;
    }

    /**
     * Heuristic correction function, making “detour exemptions” or other adjustments based on node, onRoad, and onRails.
     */
    @Unique
    protected double modifyHeuristic(MNode node, MNode nextNode, double heuristic, boolean onRoad, boolean onRails)
    {
        double newHeuristic = heuristic;
        double lastHeuristic = node.getHeuristic();
        IMNodeExtras extras = (IMNodeExtras) node;
        IMNodeExtras extrasNext = (IMNodeExtras) nextNode;
        double callbackAddon = 0.0;
        if (onRails){
            heuristic *= onRailPreference;
            callbackAddon = pathingOptions.onRailCost * onRailCallbackMultiplier;
        }
        else if (onRoad && (!node.isOnRails() || extras.isStation()))
        {
            heuristic *= onRoadPreference;
            callbackAddon = pathingOptions.onPathCost * onRoadCallbackMultiplier;
        }
        if (lastHeuristic + callbackAddon <= heuristic ){
            if (callbackAddon != 0.0){
                newHeuristic = lastHeuristic + callbackAddon;
                extrasNext.setCallbackNode();
            }
        }
        else{
            newHeuristic = heuristic;
        }
        return newHeuristic;
    }

    @Unique
    private void updateNode(@NotNull final MNode node, @NotNull final MNode nextNode, final double heuristic, final double cost, boolean onRails, boolean onRoad, boolean noDrop)
    {
        IMNodeExtras extras = (IMNodeExtras) node;
        //  This node already exists
        if ((cost >= nextNode.getCost() || nextNode.getVisitedCount() > visitedLevel) && !(extras.isCallbackNode() && nextNode.getVisitedCount() <= visitedLevel * callbackTimesTolerance))
        {
            return;
        }
        nodesToVisit.remove(nextNode);
        pathNodesToVisit.remove(nextNode);
        if (cost < nextNode.getCost()) {
            nextNode.parent = node;
            nextNode.setCost(cost);
            nextNode.setOnRails(onRails);
        }
        else if ( extras.isCallbackNode() && nextNode.isVisited() ){
            IMNodeExtras extrasNext = (IMNodeExtras) nextNode;
            if (extrasNext.isCallbackNode() && nextNode.getHeuristic() <= heuristic){
                return;
            }
            if (nextNode.parent != null && Math.abs(nextNode.parent.y - nextNode.y) > 1){
                return;
            }
        }
        nextNode.setHeuristic(heuristic);
        nodesToVisit.offer(nextNode);
        if((onRails || onRoad) && noDrop) {
            pathNodesToVisit.offer(nextNode);
        }
    }

    // special tag support
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onConstructorReturn(CallbackInfo ci) {
        townhall = computeInitialValue();
    }

    @Unique
    private BlockEntity computeInitialValue() {
        Mob entity = getEntity();
        if(entity instanceof AbstractEntityCitizen citizen) {
            ITownHall building = citizen.getCitizenData().getColony().getBuildingManager().getTownHall();
            if(building == null){
                return null;
            }
            BlockEntity townHall = actualWorld.getBlockEntity(building.getPosition());
            if (!(townHall instanceof IBlueprintDataProviderBE)) {
                if (townHall == null){
                    Log.getLogger()
                            .warn("Town hall invalid!");
                }
                return null;
            }
            return townHall;
        }
        return null;
    }

    @Unique
    private boolean HasNotPathTag (BlockPos pos){
        if(townhall != null) {
            Map<BlockPos, List<String>> tagPosMap = ((IBlueprintDataProviderBE) townhall).getPositionedTags();
            BlockPos relativePos = pos.subtract(townhall.getBlockPos());
            return tagPosMap.containsKey(relativePos) && tagPosMap.get(relativePos).contains("not_path");
        }
        return false;
    }

    @Unique
    private boolean HasPathTag (BlockPos pos){
        if(townhall != null) {
            Map<BlockPos, List<String>> tagPosMap = ((IBlueprintDataProviderBE) townhall).getPositionedTags();
            BlockPos relativePos = pos.subtract(townhall.getBlockPos());
            return tagPosMap.containsKey(relativePos) && tagPosMap.get(relativePos).contains("path");
        }
        return false;
    }

    @Unique
    private boolean HasBlockedTag (BlockPos pos){
        if(townhall != null) {
            Map<BlockPos, List<String>> tagPosMap = ((IBlueprintDataProviderBE) townhall).getPositionedTags();
            BlockPos relativePos = pos.subtract(townhall.getBlockPos());
            return tagPosMap.containsKey(relativePos) && tagPosMap.get(relativePos).contains("blocked");
        }
        return false;
    }
}


