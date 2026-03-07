package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.entity.other.SittingEntity;
import com.minecolonies.core.entity.pathfinding.PathPointExtended;
import com.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(value = MinecoloniesAdvancedPathNavigate.class, remap = false)
public abstract class MinecoloniesAdvancedPathNavigateMixin extends AbstractAdvancedPathNavigate
{
    @Unique private MinecoloniesAdvancedPathNavigate asNavigator() {
        return (MinecoloniesAdvancedPathNavigate)(Object)this;
    }

    public MinecoloniesAdvancedPathNavigateMixin(@NotNull final Mob entity, final Level world) {
        super(entity, world);
    }

    /**
     * Change 900 * 900 to (maxDistance)^2
     */
    @ModifyConstant(
            method = "setPathJob",
            constant = @Constant(doubleValue = 900 * 900),
            remap = false
    )
    private double modifyMaxDistanceSqr(double original)
    {
        return PathingConfig.MAX_PATHING_DISTANCE.get() * PathingConfig.MAX_PATHING_DISTANCE.get();
    }

    @Inject(method = "handleRails",at = @At("RETURN"),remap = false)
    private void getOffRailsAndTpToNextNode(CallbackInfoReturnable<Boolean> cir) {
        if (ourEntity.getVehicle() == null) {
            return;
        }

        final Entity entity = ourEntity.getVehicle();
        if(entity instanceof SittingEntity){
            return;
        }

        if ( this.getPath() == null || this.getPath().isDone() ){
            ourEntity.stopRiding();
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        // Path correction: after dismounting, citizens will always teleport to the next path point, preventing path recalculation caused by random dismount positions.
        int nodeIndex = this.getPath().getNextNodeIndex() + 1;
        if(nodeIndex < this.getPath().getNodeCount()) {
            final PathPointExtended pEx = (PathPointExtended) (this.getPath().getNode(nodeIndex));
            if (!pEx.isOnRails()) {
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
                ourEntity.teleportTo(pEx.x + 0.5, pEx.y, pEx.z + 0.5);
                return;
            }
        }
        // Added derailment compensation: if derailed at a turn, they will teleport farther ahead, depending on the current speed.
        if(entity instanceof MinecoloniesMinecart minecoloniesMinecart && !minecoloniesMinecart.isOnRails()) {
            Vec3 movement = minecoloniesMinecart.getDeltaMovement();
            double speed = movement.length();
            nodeIndex = Math.min(this.getPath().getNodeCount() - 1, this.getPath().getNextNodeIndex() + (int)Math.ceil(speed / 0.4F));
            @NotNull final PathPointExtended tpPlace = (PathPointExtended) (Objects.requireNonNull(this.getPath())).getNode(nodeIndex);
            if(!tpPlace.isOnRails()){
                ourEntity.stopRiding();
                entity.remove(Entity.RemovalReason.DISCARDED);
                ourEntity.teleportTo(tpPlace.x + 0.5, tpPlace.y, tpPlace.z + 0.5);
                return;
            }
            BlockPos tpPos = tpPlace.asBlockPos();
            if (entity.level().getBlockState(tpPos.below()).is(BlockTags.RAILS)) {
                tpPos = tpPos.below();
            }
            BlockState blockstate = entity.level().getBlockState(tpPos);
            double yOffset = 0.0D;
            RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock
                    ? ((BaseRailBlock) blockstate.getBlock()).getRailDirection(blockstate, level, tpPos, null)
                    : RailShape.NORTH_SOUTH;
            if (railshape.isAscending()) {
                yOffset = 0.5D;
            }
            final double x = tpPlace.x + 0.5D;
            final double y = tpPlace.y + 0.625D + yOffset;
            final double z = tpPlace.z + 0.5D;
            minecoloniesMinecart.setPos(x, y, z);
            minecoloniesMinecart.xo = x;
            minecoloniesMinecart.yo = y;
            minecoloniesMinecart.zo = z;
            mob.startRiding(minecoloniesMinecart, true);
        }
    }
}
