package com.arxyt.colonypathingedition.core.util;

import com.arxyt.colonypathingedition.core.ai.pathfinding.structure.PathfinderOptions;
import com.minecolonies.api.items.ModTags;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.world.level.block.CampfireBlock.LIT;

public class PathfinderUtils {
    public static boolean canClimb(final BlockState blockState, @NotNull final PathfinderOptions options)
    {
        return (blockState.is(BlockTags.CLIMBABLE) && options.canClimbAdvanced)
                || blockState.getBlock() instanceof LadderBlock || blockState.getBlock() instanceof ScaffoldingBlock;
    }

    public static boolean isLiquid(final BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    public static double refinedVoxelShapeMax(final VoxelShape shape, final Direction.Axis axis)
    {
        // Note: in vanilla this is -infinity
        if (shape == Shapes.empty())
        {
            return 0;
        }

        return shape.max(axis);
    }

    public static double refinedVoxelShapeMin(final VoxelShape shape, final Direction.Axis axis)
    {
        // Note: in vanilla this is +infinity
        if (shape == Shapes.empty())
        {
            return 1;
        }

        return shape.min(axis);
    }

    public static boolean isDangerous(final BlockState blockState)
    {
        final Block block = blockState.getBlock();

        return blockState.is(ModTags.dangerousBlocks) ||
                blockState.getFluidState().is(Fluids.LAVA) ||
                blockState.getFluidState().is(Fluids.FLOWING_LAVA) ||
                block instanceof FireBlock ||
                (block instanceof CampfireBlock && blockState.getValue(LIT)) ||
                block instanceof MagmaBlock ||
                block instanceof PowderSnowBlock ||
                block == Blocks.LAVA_CAULDRON;
    }
}
