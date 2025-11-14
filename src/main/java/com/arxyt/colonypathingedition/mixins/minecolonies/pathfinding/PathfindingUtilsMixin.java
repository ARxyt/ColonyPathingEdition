package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.minecolonies.api.blocks.huts.AbstractBlockMinecoloniesDefault;
import com.minecolonies.api.entity.mobs.drownedpirate.AbstractDrownedEntityPirateRaider;
import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PathfindingUtils.class, remap = false)
public abstract class PathfindingUtilsMixin {

    @Invoker(value = "canStandInSolidBlock",remap = false)
    static boolean callCanStandInSolidBlock(BlockState state) {
        throw new AssertionError();
    }

    /**
     * @author ARxyt
     * @reason Change some details
     */
    @Overwrite(remap = false)
    public static BlockPos prepareStart(@NotNull final LivingEntity entity)
    {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(entity.getX()),
                Mth.floor(entity.getY()),
                Mth.floor(entity.getZ()));
        final Level level = entity.level();
        BlockState bs = level.getBlockState(pos);
        final Block b = bs.getBlock();

        // Check if the entity is standing ontop of another block with part of its bb
        final BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        // Regen start pos at target block if there is a collision shape.
        final double posHeight = ShapeUtil.max(bs.getCollisionShape(level, pos), Direction.Axis.Y);
        if ( posHeight != 0 )
        {
            // Additional check
            double posX = Math.min(ShapeUtil.max(bs.getCollisionShape(level, pos), Direction.Axis.X),1 - ShapeUtil.min(bs.getCollisionShape(level, pos), Direction.Axis.X));
            double posZ = Math.min(ShapeUtil.max(bs.getCollisionShape(level, pos), Direction.Axis.Z),1 - ShapeUtil.min(bs.getCollisionShape(level, pos), Direction.Axis.Z));
            boolean canStand = Math.min(posX,posZ) < 0.25;
            return posHeight > 0.2 && !canStand ? pos.above().immutable() : pos.immutable();
        }

        final BlockState belowState = level.getBlockState(below);
        if (entity.onGround() && SurfaceType.getSurfaceType(level, belowState, below) != SurfaceType.WALKABLE)
        {
            int minX = Mth.floor(entity.getBoundingBox().minX);
            int minZ = Mth.floor(entity.getBoundingBox().minZ);
            int maxX = Mth.floor(entity.getBoundingBox().maxX);
            int maxZ = Mth.floor(entity.getBoundingBox().maxZ);

            for (int x = minX; x <= maxX; x++)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    final BlockPos toCheck = new BlockPos(x, below.getY(), z);
                    // Only check other positions than the current
                    if ((x != pos.getX() || z != pos.getZ())
                            && SurfaceType.getSurfaceType(level, level.getBlockState(toCheck), toCheck) == SurfaceType.WALKABLE
                            && Math.abs(ShapeUtil.max(level.getBlockState(toCheck).getCollisionShape(level, toCheck), Direction.Axis.Y) + toCheck.getY() - entity.getY()) < 0.1)
                    {
                        pos.setX(x);
                        pos.setZ(z);
                        below.setX(x);
                        below.setZ(z);
                    }
                }
            }
        }

        // 1 Up when we're standing within this collision shape
        final VoxelShape collisionShape = bs.getCollisionShape(level, pos);
        final boolean isFineToStandIn = callCanStandInSolidBlock(bs);
        if (bs.blocksMotion() && !isFineToStandIn && collisionShape.max(Direction.Axis.Y) > 0)
        {
            final double relPosX = Math.abs(entity.getX() % 1);
            final double relPosZ = Math.abs(entity.getZ() % 1);

            for (final AABB box : collisionShape.toAabbs())
            {
                if (relPosX >= box.minX && relPosX <= box.maxX
                        && relPosZ >= box.minZ && relPosZ <= box.maxZ
                        && box.maxY > 0)
                {
                    pos.set(pos.getX(), pos.getY() + 1, pos.getZ());
                    bs = level.getBlockState(pos);
                    break;
                }
            }
        }

        BlockState down = level.getBlockState(pos.below());
        while (callCanStandInSolidBlock(bs) && callCanStandInSolidBlock(down) && !down.getBlock().isLadder(down, level, pos.below(), entity) && down.getFluidState().isEmpty())
        {
            pos.move(Direction.DOWN, 1);
            bs = down;
            down = level.getBlockState(pos.below());

            if (pos.getY() < entity.getCommandSenderWorld().getMinBuildHeight())
            {
                return entity.blockPosition();
            }
        }

        if (entity.isInWater() && !(entity instanceof AbstractDrownedEntityPirateRaider))
        {
            while (!bs.getFluidState().isEmpty())
            {
                pos.set(pos.getX(), pos.getY() + 1, pos.getZ());
                bs = level.getBlockState(pos);
            }
        }
        else if (b instanceof FenceBlock || b instanceof WallBlock || b instanceof AbstractBlockMinecoloniesDefault || (bs.blocksMotion() && !callCanStandInSolidBlock(bs)))
        {
            final VoxelShape shape = bs.getCollisionShape(level, pos);
            if (shape.isEmpty())
            {
                return pos.immutable();
            }

            final Vec3 relativePos = entity.position().subtract(shape.move(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ()).bounds().getCenter());

            //Push away from fence
            final double dX = relativePos.x;
            final double dZ = relativePos.z;

            if (Math.abs(dX) < Math.abs(dZ))
            {
                pos.set(pos.getX(), pos.getY(), dZ < 0 ? pos.getZ() - 1 : pos.getZ() + 1);
            }
            else
            {
                pos.set(dX < 0 ? pos.getX() - 1 : pos.getX() + 1, pos.getY(), pos.getZ());
            }
        }

        return pos.immutable();
    }
}
