package com.arxyt.colonypathingedition.core.ai.pathfinding.structure;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.entity.pathfinding.PathingOptions;

/**
 * Configuration values for pathing, used by pathjobs and normally set through the navigator
 */
public class PathfinderOptions
{
    // basic costs
    public double jumpCost;
    public double dropCost;
    public double onPathCost;
    public double onRailCost;
    public double railsExitCost;
    public double swimCost;
    public double caveAirCost;
    public double traverseToggleAbleCost;
    public double walkInShapesCost;
    public double divingCost;
    public double ladderSwitchCost;
    public double shingleCost;
    public double destroyingFarmlandCost;
    public double leafCost;
    public double sweetBerryCost;

    // advanced settings.
    public int callbackTimesTolerance;
    public int extendCount;
    public double onRailPreference;
    public double onRoadPreference;
    public double swimmingPreference;
    public double onRailCallbackMultiplier;
    public double onRoadCallbackMultiplier;

    // not editable.
    public double climbCost = 3D;
    public double swimEnterCost = 24D;

    // switches.
    public boolean canUseRails = false;
    public boolean canSwim = true;
    public boolean canEnterGates  = true;
    public boolean canOpenDoors = false;
    public boolean canDive = false;
    public boolean canDrop = true;
    public boolean canPassDanger = false;

    /**
     * Whether to path through vines.
     */
    public boolean canClimbAdvanced = false;

    public PathfinderOptions()
    {
        swimCost = PathingConfig.WATER_COST_DEFINER.get();
        onPathCost = PathingConfig.ROAD_COST_MULTIPLIER.get();
        onRailCost = PathingConfig.RAIL_COST_MULTIPLIER.get();
        caveAirCost = PathingConfig.CAVE_COST_DEFINER.get();
        railsExitCost = PathingConfig.RAILEXIT_COST_DEFINER.get();
        jumpCost = PathingConfig.JUMP_COST_DEFINER.get();
        dropCost = PathingConfig.DROP_COST_MULTIPLIER.get();
        traverseToggleAbleCost = PathingConfig.DOORS_COST_DEFINER.get();
        walkInShapesCost = PathingConfig.INSHAPE_COST_DEFINER.get();
        divingCost = PathingConfig.DIVE_COST_DEFINER.get();
        ladderSwitchCost = PathingConfig.LADDER_SWITCH_COST_DEFINER.get();
        shingleCost = PathingConfig.SHINGLE_COST_DEFINER.get();
        destroyingFarmlandCost = PathingConfig.FARMLAND_COST_DEFINER.get();
        leafCost = PathingConfig.LEAF_COST_DEFINER.get();
        sweetBerryCost = PathingConfig.WATER_COST_DEFINER.get(); // TODO: add config.
        callbackTimesTolerance =  PathingConfig.CALLBACK_TIMES_TOLERANCE.get();
        extendCount =  PathingConfig.NODE_EXTEND_COUNT.get();
        onRailPreference = PathingConfig.ONRAIL_PREFERENCE.get();
        onRoadPreference = PathingConfig.ONROAD_PREFERENCE.get();
        swimmingPreference = PathingConfig.SWIMMING_PREFERENCE.get();
        onRailCallbackMultiplier = PathingConfig.ONRAIL_CALLBACK_MULTIPLIER.get();
        onRoadCallbackMultiplier = PathingConfig.ONROAD_CALLBACK_MULTIPLIER.get();
    }

    public PathfinderOptions(PathingOptions originalOption) {
        this();
        this.canUseRails = originalOption.canUseRails();
        this.canSwim = originalOption.canSwim();
        this.canEnterGates = originalOption.canEnterGates();
        this.canOpenDoors = originalOption.canOpenDoors();
        this.canDive = originalOption.canWalkUnderWater();
        this.canDrop = originalOption.canDrop;
        this.canPassDanger = originalOption.canPassDanger();
        this.canClimbAdvanced = originalOption.canClimbAdvanced();
    }
}