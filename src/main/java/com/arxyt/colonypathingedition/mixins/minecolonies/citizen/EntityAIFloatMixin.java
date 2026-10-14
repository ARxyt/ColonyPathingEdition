package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.core.entity.ai.minimal.EntityAIFloat;
import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobEscapeWater;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = EntityAIFloat.class, remap = false)
public abstract class EntityAIFloatMixin extends FloatGoal {

    @Shadow(remap = false) @Final private Mob owner;
    @Shadow(remap = false) private PathResult waterPathing;

    public EntityAIFloatMixin(Mob mob) {
        super(mob);
    }

    /**
     * @author ARxyt
     * @reason Fix NPC swimming and drowning. Restores continuous upward swimming when submerged or in deep water.
     */
    @Overwrite(remap = false)
    public void tick() {
        if (!owner.getEyeInFluidType().isAir() && owner.getEyeInFluidType().canSwim(owner)) {
            // Always swim upward every tick while eyes are submerged
            owner.getJumpControl().jump();

            if (owner.level().getBlockState(BlockPos.containing(owner.getEyePosition()).above()).isAir()) {
                return;
            }

            if (waterPathing == null || !waterPathing.isInProgress()) {
                if (owner.getNavigation() instanceof MinecoloniesAdvancedPathNavigate nav) {
                    nav.setPauseTicks(0);
                    nav.stop();

                    waterPathing = nav.setPathJob(
                            new PathJobEscapeWater(CompatibilityUtils.getWorldFromEntity(owner),
                                    owner.blockPosition(),
                                    (int) owner.getAttribute(Attributes.FOLLOW_RANGE).getValue() * 5,
                                    owner),
                            null, 1.0, false);
                    nav.setPauseTicks(20 * 15);
                }
            }
        } else {
            if (waterPathing != null) {
                waterPathing = null;
                if (owner.getNavigation() instanceof MinecoloniesAdvancedPathNavigate nav) {
                    nav.setPauseTicks(0);
                }
            }

            // In deep water (above fluid jump threshold), jumpInFluid is used (smooth upward swim stroke)
            if (owner.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value()) > owner.getFluidJumpThreshold()
                    || owner.getDeltaMovement().y < -0.03
                    || owner.tickCount % 3 == 0) {
                owner.getJumpControl().jump();
            }
        }
    }
}
