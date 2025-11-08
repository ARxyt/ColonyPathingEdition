package com.arxyt.colonypathingedition.mixins.minecolonies.entity;

import com.arxyt.colonypathingedition.api.AbstractMinecartAccessor;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.other.MinecoloniesMinecart;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static com.minecolonies.api.research.util.ResearchConstants.WALKING;
import static net.minecraft.world.entity.Entity.RemovalReason.DISCARDED;


@Mixin(MinecoloniesMinecart.class)
public abstract class MinecoloniesMinecartMixin extends Minecart implements AbstractMinecartAccessor {
    public MinecoloniesMinecartMixin(final EntityType<?> type, final Level world)
    {
        super(type, world);
    }

    @Override
    public double getMaxSpeedWithRail() { //Non-default because getMaximumSpeed is protected
        if (!canUseRail()) return getMaxSpeed();
        BlockPos pos = this.getCurrentRailPosition();
        BlockState state = this.level().getBlockState(pos);
        if (!state.is(BlockTags.RAILS)) return getMaxSpeed();

        float railMaxSpeed = ((BaseRailBlock)state.getBlock()).getRailMaxSpeed(state, this.level(), pos, this);

        double speedFactor = 1;
        if(!this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof AbstractEntityCitizen citizen){
            MobEffectInstance effect = citizen.getEffect(MobEffects.MOVEMENT_SPEED);
            if (effect != null) {
                speedFactor += effect.getAmplifier() * 0.3;
            }
            if(citizen.getCitizenColonyHandler() != null && citizen.getCitizenColonyHandler().getColony() != null && citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) > 0){
                speedFactor += citizen.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(WALKING) * 2;
            }
            if (citizen.getCitizenData() != null && citizen.getCitizenData().getJob() instanceof JobDeliveryman){
                speedFactor += citizen.getCitizenData().getCitizenSkillHandler().getLevel(Skill.Agility) * 0.03;
            }
        }

        return Math.min(railMaxSpeed * speedFactor, getCurrentCartSpeedCapOnRail());
    }

    /**
     * @author ARxyt
     * @reason 重新获取矿车在铁轨上的数据，预备同步 1.21 最新的矿车 tick 事件 (可以防止矿车脱轨的算法)
     */
    @Overwrite(remap = false)
    public void tick()
    {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkBelowWorld();
        if (this.level().isClientSide) {
            if (getLSteps() > 0) {
                this.lerpPositionAndRotationStep(getLSteps(), getLx(), getLy(), getLz(), getLyr(), getLxr());
                lStepMinus();
            } else {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
            }
        } else {
            if (!this.isNoGravity()) {
                double d0 = this.isInWater() ? -0.005D : -0.04D;
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, d0, 0.0D));
            }

            int k = Mth.floor(this.getX());
            int i = Mth.floor(this.getY());
            int j = Mth.floor(this.getZ());
            if (this.level().getBlockState(new BlockPos(k, i - 1, j)).is(BlockTags.RAILS)) {
                --i;
            }

            BlockPos blockpos = new BlockPos(k, i, j);
            BlockState blockstate = this.level().getBlockState(blockpos);
            setOnRails(BaseRailBlock.isRail(blockstate));
            if (this.canUseRail() && isOnRails()) {
                this.moveAlongTrack(blockpos, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock)blockstate.getBlock()).isActivatorRail()) {
                    this.activateMinecart(k, i, j, (Boolean)blockstate.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.comeOffTrack();
            }

            this.checkInsideBlocks();
            this.setXRot(0.0F);
            double d1 = this.xo - this.getX();
            double d3 = this.zo - this.getZ();
            if (d1 * d1 + d3 * d3 > 0.001D) {
                this.setYRot((float)(Mth.atan2(d3, d1) * 180.0D / 3.141592653589793D));
                if (getFlipped()) {
                    this.setYRot(this.getYRot() + 180.0F);
                }
            }

            double d4 = (double)Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (d4 < -170.0D || d4 >= 170.0D) {
                this.setYRot(this.getYRot() + 180.0F);
                filpReverse();
            }

            this.setRot(this.getYRot(), this.getXRot());
            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }

        if (this.tickCount % 20 == 19) {
            if (this.getPassengers().isEmpty()) {
                this.remove(DISCARDED);
            } else {
                //Deal with ghost minecart
                for (Entity passenger : this.getPassengers()) {
                    //Ghost minecarts’ passengers mostly are ghost EntityCitizens with citizenId = 0.
                    if (passenger instanceof EntityCitizen citizen && citizen.getCivilianID() == 0) {
                        citizen.stopRiding();
                        this.remove(DISCARDED);
                        break;
                    }
                }
            }
            if (this.getPassengers().isEmpty()) {
                this.remove(DISCARDED);
            }
        }
    }
}
