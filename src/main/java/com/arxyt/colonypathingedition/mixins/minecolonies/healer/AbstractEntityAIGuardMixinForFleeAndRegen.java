package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IGuardBuilding;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.FLEEING_SPEED;

@Mixin(value = AbstractEntityAIGuard.class, remap = false)
public abstract class AbstractEntityAIGuardMixinForFleeAndRegen implements AbstractAISkeletonAccessor<AbstractJobGuard<?>>,AbstractEntityAIBasicAccessor<AbstractBuildingGuards> {
    @Final @Shadow (remap = false) protected IGuardBuilding buildingGuards;

    @Unique private BlockPos nearestHospital = null;

    /**
     * @author ARxyt
     * @reason 修改逃跑的目标地点
     */
    @Overwrite(remap = false)
    private IAIState flee()
    {
        if (!getWorker().hasEffect(MobEffects.MOVEMENT_SPEED))
        {
            final double effect = Objects.requireNonNull(getWorker().getCitizenColonyHandler().getColonyOrRegister()).getResearchManager().getResearchEffects().getEffectStrength(FLEEING_SPEED);
            if (effect > 0)
            {
                getWorker().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, (int) (0 + effect)));
            }
        }
        final IColony colony = getWorker().getCitizenData().getColony();
        if (nearestHospital == null){
            nearestHospital = colony.getServerBuildingManager().getBestBuilding(getWorker(), BuildingHospital.class);
        }

        if (nearestHospital != null){
            if( !(getWorker().getHealth() > getWorker().getMaxHealth() / 2) ) {
                if (!EntityNavigationUtils.walkToPos(getWorker(), nearestHospital, 3, true)) {
                    return GUARD_FLEE;
                }
            }
            else{
                nearestHospital = null;
            }
        }
        else if (!invokeWalkToBuilding())
        {
            return GUARD_FLEE;
        }

        return GUARD_REGEN;
    }


    /**
     * @author ARxyt
     * @reason 修改进入START_WORKING的条件和一系列生命回复条件
     */
    @Overwrite(remap = false)
    private IAIState regen()
    {
        if (((EntityCitizen) getWorker()).getThreatTable().getTargetMob() != null && ((EntityCitizen) getWorker()).getThreatTable().getTargetMob().distanceTo(getWorker()) < 10)
        {
            return CombatAIStates.ATTACKING;
        }

        if (buildingGuards.shallRetrieveOnLowHealth())
        {
            if (!getWorker().hasEffect(MobEffects.REGENERATION))
            {
                getWorker().addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
            }
        }

        final IColony colony = getWorker().getCitizenData().getColony();
        if( nearestHospital != null && colony.getServerBuildingManager().getBuilding(nearestHospital) instanceof BuildingHospital hospital){
            hospital.checkOrCreatePatientFile(getWorker().getCivilianID());
        }

        if(getWorker().getHealth() < ((int) getWorker().getMaxHealth() * 0.75D)){
            return GUARD_REGEN;
        }

        if(Objects.requireNonNull(getWorker().getCitizenColonyHandler().getColonyOrRegister()).getRaiderManager().isRaided()){
            return CombatAIStates.ATTACKING;
        }

        return START_WORKING;
    }

}
