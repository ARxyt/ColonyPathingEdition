package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.AIOneTimeEventTarget;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.SurfaceType;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.minecolonies.api.util.constant.CitizenConstants.INITIAL_RUN_SPEED_AVOID;
import static com.minecolonies.api.util.constant.CitizenConstants.MAX_GUARD_CALL_RANGE;

@Mixin(value = EntityCitizen.class, remap = false)
public abstract class EntityCitizenMoveAwayMixin {
    @Shadow(remap = false) private ITickRateStateMachine<IState> citizenAI;

    @Unique BlockPos nearestHospital;

    @Unique private EntityCitizen asCitizen() {
        return (EntityCitizen)(Object)this;
    }

    @Inject(method = "performMoveAway",at=@At("HEAD"),remap = false)
    private void performMoveAway(Entity attacker, CallbackInfo ci){
        EntityCitizen citizen = asCitizen();
        // Environmental damage
        if (!(attacker instanceof LivingEntity) &&
                (!(citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard) || citizen.getCitizenJobHandler().getColonyJob().canAIBeInterrupted()))
        {
            if(citizen.getCitizenData().getCitizenDiseaseHandler().isHurt()){
                final BlockPos tpPos = BlockPosUtil.findAround(citizen.level(), citizen.blockPosition(), 10, 10,
                        (posworld, pos) -> SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.below()), pos.below()) == SurfaceType.WALKABLE
                                && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos), pos) == SurfaceType.DROPABLE
                                && SurfaceType.getSurfaceType(posworld, posworld.getBlockState(pos.above()), pos.above()) == SurfaceType.DROPABLE);
                if (tpPos != null)
                {
                    citizen.teleportTo(tpPos.getX() + 0.5, tpPos.getY(), tpPos.getZ() + 0.5);
                    return;
                }
            }
            EntityNavigationUtils.walkAwayFrom(citizen, citizen.blockPosition(), 5, INITIAL_RUN_SPEED_AVOID);
            return;
        }

        if (attacker == null)
        {
            return;
        }

        if ((citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard))
        {
            // 30 Blocks range
            citizen.callForHelp(attacker, 900);
            return;
        }

        citizenAI.addTransition(new AIOneTimeEventTarget<>(CitizenAIState.FLEE));
        citizen.callForHelp(attacker, MAX_GUARD_CALL_RANGE);
        if(citizen.getCitizenData().getCitizenDiseaseHandler().isHurt() && !(citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard)){
            final IColony colony = citizen.getCitizenData().getColony();
            if (nearestHospital == null){
                nearestHospital = colony.getBuildingManager().getBestBuilding(citizen, BuildingHospital.class);
            }
            if (nearestHospital != null ){
                EntityNavigationUtils.walkToPos(citizen, nearestHospital, 3, true);
                return;
            }
        }
        EntityNavigationUtils.walkAwayFrom(citizen, attacker.blockPosition(), 15, INITIAL_RUN_SPEED_AVOID);
    }
}
