package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding.navigator;

import com.minecolonies.api.util.ShapeUtil;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.navigation.MovementHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.*;

@Mixin(value = MovementHandler.class, remap = false)
public abstract class MovementHandlerMixin extends MoveControl {

    @Final @Shadow(remap = false) AttributeInstance speedAtr;
    @Shadow(remap = false) private float stepHeight;
    @Shadow(remap = false) private float speedValue;

    @Unique private int jumpCoolDown = 0;
    @Unique private static int FORCE_JUMP_LIMIT = 10;

    public MovementHandlerMixin(Mob mob)
    {
        super(mob);
    }

    /**
     * @author ARxyt
     * @reason Further jump handler.
     */
    @Overwrite(remap = false)
    public void tick()
    {
        if (mob.tickCount % 20 == 0)
        {
            stepHeight = this.mob.getStepHeight();
            speedValue = (float) speedAtr.getValue();
        }

        if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.STRAFE)
        {
            final float speedAtt = speedValue;
            float speed = (float) this.speedModifier * speedAtt;
            float forward = this.strafeForwards;
            float strafe = this.strafeRight;
            float totalMovement = Mth.sqrt(forward * forward + strafe * strafe);
            if (totalMovement < 1.0F)
            {
                totalMovement = 1.0F;
            }

            totalMovement = speed / totalMovement;
            forward = forward * totalMovement;
            strafe = strafe * totalMovement;
            final float sinRotation = Mth.sin(this.mob.getYRot() * ((float) Math.PI / 180F));
            final float cosRotation = Mth.cos(this.mob.getYRot() * ((float) Math.PI / 180F));
            final float rot1 = forward * cosRotation - strafe * sinRotation;
            final float rot2 = strafe * cosRotation + forward * sinRotation;
            final PathNavigation pathnavigator = this.mob.getNavigation();

            final NodeEvaluator nodeprocessor = pathnavigator.getNodeEvaluator();
            if (nodeprocessor.getBlockPathType(this.mob.level(),
                    Mth.floor(this.mob.getX() + (double) rot1),
                    Mth.floor(this.mob.getY()),
                    Mth.floor(this.mob.getZ() + (double) rot2)) != BlockPathTypes.WALKABLE)
            {
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;
                speed = speedAtt;
            }

            this.mob.setSpeed(speed);
            this.mob.setZza(this.strafeForwards);
            this.mob.setXxa(this.strafeRight);
            this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
        }
        else if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.MOVE_TO)
        {
            this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
            final double xDif = this.wantedX - this.mob.getX();
            final double zDif = this.wantedZ - this.mob.getZ();
            final double yDif = this.wantedY - this.mob.getY();
            final double dist = xDif * xDif + yDif * yDif + zDif * zDif;
            if (dist < (double) 2.5000003E-7F)
            {
                this.mob.setZza(0.0F);
                return;
            }

            final float range = (float) (Mth.atan2(zDif, xDif) * (double) (180F / (float) Math.PI)) - 90.0F;
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), range, 90.0F));
            this.mob.setSpeed((float) ((yDif < -1.5D && mob.getVehicle() == null) ? Math.min(this.speedModifier, 1.5 / Math.max(2, -0.5 - yDif)) : this.speedModifier) * speedValue);
            final BlockPos blockpos = this.mob.blockPosition();
            final BlockState blockstate = this.mob.level().getBlockState(blockpos);

            if (PathfindingUtils.isWater(mob.level(), mob.blockPosition(), blockstate, blockstate.getFluidState())
                    && PathfindingUtils.isWater(mob.level(), mob.blockPosition().above(), null, null))
            {
                if (yDif != 0.0D)
                {
                    double d3 = Math.sqrt(xDif * xDif + yDif * yDif + zDif * zDif);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0, (double) this.mob.getSpeed() * ((yDif + 0.3) / d3) * 0.1D, 0));
                }

                return;
            }

            if (mob.getVehicle() == null) {
                final Block block = blockstate.getBlock();
                final VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
                if (yDif > (double) stepHeight) {
                    if (xDif * xDif + zDif * zDif < (double) Math.max(1.0F, this.mob.getBbWidth()) || jumpCoolDown > FORCE_JUMP_LIMIT) {
                        this.mob.getJumpControl().jump();
                        this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING;
                        jumpCoolDown = 0;
                    } else {
                        jumpCoolDown++;
                    }
                } else if ((!ShapeUtil.isEmpty(voxelshape) && this.mob.getY() < ShapeUtil.max(voxelshape, Direction.Axis.Y) + (double) blockpos.getY() && !blockstate.is(BlockTags.DOORS)
                        && !blockstate.is(BlockTags.FENCES) && !blockstate.is(BlockTags.FENCE_GATES))
                        && !block.isLadder(blockstate, this.mob.level(), blockpos, this.mob)) {
                    boolean canPass = true;
                    if (xDif > zDif){
                        canPass = ShapeUtil.min(voxelshape, Direction.Axis.Z) != 0 || ShapeUtil.max(voxelshape, Direction.Axis.Z) != 1;
                    }
                    if (zDif > xDif){
                        canPass = ShapeUtil.min(voxelshape, Direction.Axis.X) != 0 || ShapeUtil.max(voxelshape, Direction.Axis.X) != 1;
                    }
                    if (canPass) {
                        jumpCoolDown = 0;
                    }
                    if (jumpCoolDown > FORCE_JUMP_LIMIT) {
                        this.mob.getJumpControl().jump();
                        this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING;
                    } else {
                        jumpCoolDown++;
                    }
                } else {
                    jumpCoolDown = 0;
                }
            }
        }
        else if (this.operation == net.minecraft.world.entity.ai.control.MoveControl.Operation.JUMPING)
        {
            this.mob.setSpeed((float) (this.speedModifier * speedValue));

            // Avoid beeing stuck in jumping while in liquids
            final BlockPos blockpos = this.mob.blockPosition();
            final BlockState blockstate = this.mob.level().getBlockState(blockpos);
            if (this.mob.onGround() || blockstate.liquid())
            {
                this.operation = net.minecraft.world.entity.ai.control.MoveControl.Operation.WAIT;
            }
        }
        else
        {
            this.mob.setZza(0.0F);
        }
    }
}
