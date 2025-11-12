package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.api.IMNodeExtras;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.ldtteam.domumornamentum.block.decorative.*;
import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.minecolonies.api.colony.buildings.workerbuildings.ITownHall;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.PathingOptions;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.world.CachingBlockLookup;
import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.api.util.constant.ColonyConstants;
import com.minecolonies.core.util.WorkerUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import static com.minecolonies.api.util.BlockPosUtil.directionFromDelta;
import static com.minecolonies.core.entity.pathfinding.PathingOptions.MAX_COST;

@Mixin(AbstractPathJob.class)
public abstract class AbstractPathJobMixin{

    @Final @Shadow(remap = false) private Level actualWorld;
    @Final @Shadow(remap = false) public static int MAX_NODES;
    @Final @Shadow(remap = false) private Int2ObjectOpenHashMap<MNode> nodes;
    @Final @Shadow(remap = false) protected LevelReader world;
    @Final @Shadow(remap = false) @NotNull protected BlockPos start;

    @Shadow(remap = false) private int visitedLevel;
    @Shadow(remap = false) private Queue<MNode> nodesToVisit;
    @Shadow(remap = false) private double maxCost;
    @Shadow(remap = false) protected double heuristicMod;
    @Shadow(remap = false) private PathingOptions pathingOptions;
    @Shadow(remap = false) protected CachingBlockLookup cachedBlockLookup;
    @Shadow(remap = false) protected BlockPos.MutableBlockPos tempWorldPos;
    @Shadow(remap = false) protected int maxNodes;

    @Shadow(remap = false) public abstract Mob getEntity();
    @Shadow(remap = false) protected abstract void recalcHeuristic(final MNode node);
    @Shadow(remap = false) protected abstract boolean isPassable(int x, int y, int z, boolean head, MNode parent);
    @Shadow(remap = false) public abstract PathingOptions getPathingOptions();
    @Shadow(remap = false) protected abstract boolean canLeaveBlock(int x, int y, int z, int parentX, int parentY, int parentZ, boolean head);

    @Unique private BlockEntity townhall;
    @Unique protected int actualMaxNodes;
    @Unique public double ladderSwitchCost = PathingConfig.LADDER_SWITCH_COST_DEFINER.get();
    @Unique public double shingleCost = PathingConfig.SHINGLE_COST_DEFINER.get();
    @Unique public double destroyingFarmlandCost = PathingConfig.FARMLAND_COST_DEFINER.get();
    @Unique public double leafCost = PathingConfig.LEAF_COST_DEFINER.get();
    @Unique public double sweetBerryCost = PathingConfig.WATER_COST_DEFINER.get();
    @Unique final protected double onRailPreference = PathingConfig.ONRAIL_PREFERENCE.get();
    @Unique final protected double onRoadPreference = PathingConfig.ONROAD_PREFERENCE.get();
    @Unique final private double swimmingPreference = PathingConfig.SWIMMING_PREFERENCE.get();
    @Unique final private double onRailCallbackMultiplier = PathingConfig.ONRAIL_CALLBACK_MULTIPLIER.get();
    @Unique final private double onRoadCallbackMultiplier = PathingConfig.ONROAD_CALLBACK_MULTIPLIER.get();
    @Unique final private int callbackTimesTolerance =  PathingConfig.CALLBACK_TIMES_TOLERANCE.get();

    @Invoker(value="getGroundHeight",remap = false)
    public abstract int invokeGetGroundHeight(final MNode node, final int x, final int y, final int z);

    @Invoker(value="createNode",remap = false)
    public abstract MNode invokeCreateNode(final MNode parent, final int x, final int y, final int z, final int nodeKey, final double heuristic, final double cost);

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

    /**
     * 重写 computeCost 方法，修改游泳进入成本并添加自定义逻辑。
     *
     * @author YourName
     * @reason 调整路径计算中的游泳成本
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
            if (Math.abs(dYDouble) > 0.6 && !ladder && !(dY == 1 && (below.getBlock() instanceof StairBlock) && below.getValue(StairBlock.FACING)==directionFromDelta(dX,0,dZ) && below.getValue(StairBlock.HALF)== Half.BOTTOM))
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

        if (below.getBlock() instanceof ShingleBlock || below.getBlock() instanceof ShingleSlabBlock)
        {
            cost += shingleCost;
        }

        if (state.getBlock() instanceof PanelBlock)
        {
            cost += 0.2;
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
     * 在 search 方法头部插入：记录当前 maxNodes 到 savedMaxNodes
     */
    @Inject(
            method = "search",
            at     = @At("HEAD"),
            remap = false
    )
    private void onSearchHead(CallbackInfoReturnable<Path> cir)
    {
        this.actualMaxNodes = this.maxNodes;
    }



    /**
     * 用 Redirect 拦截 recalcHeuristic(bestNode) 调用，
     * 调用原方法后，bestNode 已被更新，我们可以在这里用它做计算。
     */
    @Redirect(
            method = "search()Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(
                    value   = "INVOKE",
                    target  = "Lcom/minecolonies/core/entity/pathfinding/pathjobs/AbstractPathJob;recalcHeuristic(Lcom/minecolonies/core/entity/pathfinding/MNode;)V",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private void onRecalcHeuristicAndThen1(AbstractPathJob instance, MNode bestNode)
    {
        // 1) 先执行原来的 recalcHeuristic(bestNode)
        recalcHeuristic(bestNode);

        // 2) 按 bestNode 的 heuristic 调整 maxNodes：
        double h = bestNode.getHeuristic();
        int extra = (int) Math.ceil(Math.sqrt(h) * 10);
        maxNodes = Math.max(Math.min(actualMaxNodes + extra , MAX_NODES),maxNodes);
    }

    /**
     * 用 Redirect 拦截 recalcHeuristic(bestNode) 调用，
     * 调用原方法后，bestNode 已被更新，我们可以在这里用它做计算。
     */
    @Redirect(
            method = "search()Lnet/minecraft/world/level/pathfinder/Path;",
            at = @At(
                    value   = "INVOKE",
                    target  = "Lcom/minecolonies/core/entity/pathfinding/pathjobs/AbstractPathJob;recalcHeuristic(Lcom/minecolonies/core/entity/pathfinding/MNode;)V",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private void onRecalcHeuristicAndThen2(AbstractPathJob instance, MNode bestNode)
    {
        recalcHeuristic(bestNode);

        double h = bestNode.getHeuristic();
        int extra = (int) Math.ceil(Math.sqrt(h) * 10);
        maxNodes = Math.max(Math.min(actualMaxNodes + extra , MAX_NODES),maxNodes);
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
        return !(ShapeUtil.getStartY(above.getCollisionShape(world, tempWorldPos.set(x, y + 1, z)), 1) < 0.875)||(above.hasProperty(BlockStateProperties.OPEN)&&!(above.getBlock() instanceof PanelBlock));
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
        double rawDY = 2 + ShapeUtil.getStartY(cornerState.getCollisionShape(world, tempWorldPos.set(cornerPos)), 1)-ShapeUtil.getEndY(checkState.getCollisionShape(world, tempWorldPos.set(checkPos)),0);
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
            } else if (!below.isAir()) {
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
     * @reason 重构代码，取消corner链接，使用统一规则缩减代码量，同时de奇怪的bug
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
                conerNode = invokeCreateNode(null, conerX, conerY, conerZ, nodeKey, node.getHeuristic(), node.getCost());
                conerNode.setCornerNode(isPassable);
                conerNode.increaseVisited();
                if(!isPassable && checkPossiblyPassing(node, nextX, newY, nextZ, conerNode, dX, newY - node.y, dZ)){
                    return;
                }
            }
            else{
                if(!conerNode.isCornerNode() && checkPossiblyPassing(node, nextX, newY, nextZ, conerNode, dX, newY - node.y, dZ)){
                    return;
                }
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
        final boolean onRoad =HasPathTag(below)||HasPathTag(pos)||(ladder||PathfindingUtils.isLadder(belowState, pathingOptions)||WorkerUtil.isPathBlock(belowState.getBlock())||WorkerUtil.isPathBlock(state.getBlock()))&&!(HasNotPathTag(below)||HasNotPathTag(pos));
        final boolean isDiving = isSwimming && PathfindingUtils.isWater(world, null, aboveState, null);


        final boolean railsExit = !onRails && node.isOnRails();
        double nextCost;
        nextCost = computeCost(node, dX, dY, dZ, isSwimming, onRoad, isDiving, onRails, railsExit, swimStart, ladder, state, belowState, nextX, nextY, nextZ);
        nextCost = invokeModifyCost(nextCost, node, swimStart, isSwimming, nextX, nextY, nextZ, state, belowState);

        if (nextCost > maxCost)
        {
            maxCost = Math.min(MAX_COST, Math.ceil(nextCost));
        }

        final double heuristic = computeHeuristic(nextX, nextY, nextZ) * heuristicMod;
        final double cost = node.getCost() + nextCost;

        if (nextNode == null)
        {
            nextNode = invokeCreateNode(node, nextX, nextY, nextZ, nodeKey, heuristic, cost);
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
        }
        else
        {
            if ((onRoad || onRails) && Math.abs(dY) <= 1){
                nextNode.setHeuristic(modifyHeuristic(node, nextNode, nextNode.getHeuristic(), onRoad, onRails));
            }
            else {
                nextNode.setHeuristic(modifyHeuristic(nextNode, nextNode.getHeuristic(), state));
            }
            updateNode(node, nextNode, heuristic, cost, onRails);
        }
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

    @Unique
    private double modifyHeuristic(MNode nextNode, double heuristic, final BlockState state) {
        double newHeuristic = heuristic;
        if(state.getBlock() == Blocks.CAVE_AIR){
            newHeuristic *= 1 + 0.15 * Math.max(5 - world.getBrightness(LightLayer.BLOCK, new BlockPos(nextNode.x, nextNode.y, nextNode.z)) , 0);
        }
        if (nextNode.isSwimming()){
            newHeuristic *= swimmingPreference;
        }
        return newHeuristic;
    }

    /**
     * 启发值修正函数，根据 node、onRoad、onRails 做“绕路豁免”或其他调整。
     */
    @Unique
    protected double modifyHeuristic(MNode node, MNode nextNode, double heuristic, boolean onRoad, boolean onRails)
    {
        double newHeuristic = heuristic;
        double lastHeuristic =node.getHeuristic();
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
        if (lastHeuristic + callbackAddon <= heuristic){
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
    private void updateNode(@NotNull final MNode node, @NotNull final MNode nextNode, final double heuristic, final double cost, boolean onRails)
    {
        IMNodeExtras extras = (IMNodeExtras) node;
        //  This node already exists
        if ((cost >= nextNode.getCost() || nextNode.getVisitedCount() > visitedLevel) && !(extras.isCallbackNode() && nextNode.getVisitedCount() <= visitedLevel * callbackTimesTolerance))
        {
            return;
        }
        nodesToVisit.remove(nextNode);
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
    }

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


