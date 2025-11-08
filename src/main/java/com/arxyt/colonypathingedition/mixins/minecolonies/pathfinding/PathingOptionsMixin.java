package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.core.entity.pathfinding.PathingOptions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 修改 Minecolonies 的路径计算逻辑
 */
@Mixin(PathingOptions.class)
public abstract class PathingOptionsMixin {
    // Shadow 目标字段
    @Shadow(remap = false) public double railsExitCost;
    @Shadow(remap = false) public double swimCost;
    @Shadow(remap = false) public double onRailCost;
    @Shadow(remap = false) public double onPathCost;
    @Shadow(remap = false) public double caveAirCost;
    @Shadow(remap = false) public double jumpCost;
    @Shadow(remap = false) public double dropCost;
    @Shadow(remap = false) public double traverseToggleAbleCost;
    @Shadow(remap = false) public double walkInShapesCost;
    @Shadow(remap = false) public double divingCost;

    // 在构造函数中注入，覆盖默认值
    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void onConstructed(CallbackInfo ci) {
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
    }
}