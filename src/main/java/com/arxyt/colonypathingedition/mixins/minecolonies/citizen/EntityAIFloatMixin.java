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
     * @reason Fix NPC swimming and drowning. Restores jumping to stay afloat and escape water.
     */
    @Overwrite(remap = false)
    public void tick() {
        if (!owner.getEyeInFluidType().isAir() && owner.getEyeInFluidType().canSwim(owner)) {
            // Submerged in water: jump to swim towards the surface
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

            // Floating on surface: jump periodically (or when starting to sink) to stay afloat
            if (owner.tickCount % 3 == 0 || owner.getDeltaMovement().y < -0.05) {
                owner.getJumpControl().jump();
            }
        }
    }
}
