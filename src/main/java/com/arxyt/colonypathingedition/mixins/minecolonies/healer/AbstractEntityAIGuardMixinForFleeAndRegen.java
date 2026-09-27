package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IGuardBuilding;
import com.minecolonies.api.entity.ai.combat.CombatAIStates;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIFight;
import com.minecolonies.core.entity.ai.workers.guard.AbstractEntityAIGuard;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.FLEEING_SPEED;

@Mixin(value = AbstractEntityAIGuard.class, remap = false)
public abstract class AbstractEntityAIGuardMixinForFleeAndRegen<J extends AbstractJobGuard<J>, B extends AbstractBuildingGuards> extends AbstractEntityAIFight<J, B> {
    @Final @Shadow (remap = false) protected IGuardBuilding buildingGuards;

    @Unique private BlockPos nearestHospital = null;

    public AbstractEntityAIGuardMixinForFleeAndRegen(@NotNull final J job) {
        super(job);
    }

    /**
     * @author ARxyt
     * @reason Change target to flee.
     */
    @Overwrite(remap = false)
    private IAIState flee()
    {
        if (!worker.hasEffect(MobEffects.MOVEMENT_SPEED))
        {
            final double effect = Objects.requireNonNull(worker.getCitizenColonyHandler().getColonyOrRegister()).getResearchManager().getResearchEffects().getEffectStrength(FLEEING_SPEED);
            if (effect > 0)
            {
                worker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, (int) (0 + effect)));
            }
        }
        final IColony colony = worker.getCitizenData().getColony();
        if (nearestHospital == null){
            nearestHospital = colony.getServerBuildingManager().getBestBuilding(worker, BuildingHospital.class);
        }

        if (nearestHospital != null){
            if( !(worker.getHealth() > worker.getMaxHealth() / 2) ) {
                if (!EntityNavigationUtils.walkToPos(worker, nearestHospital, 3, true)) {
                    return GUARD_FLEE;
                }
            }
            else{
                nearestHospital = null;
            }
        }
        else if (!walkToBuilding())
        {
            return GUARD_FLEE;
        }

        return GUARD_REGEN;
    }


    /**
     * @author ARxyt
     * @reason Change methods to regen.
     */
    @Overwrite(remap = false)
    private IAIState regen()
    {
        if (((EntityCitizen) worker).getThreatTable().getTargetMob() != null && ((EntityCitizen) worker).getThreatTable().getTargetMob().distanceTo(worker) < 10)
        {
            return CombatAIStates.ATTACKING;
        }

        if (buildingGuards.shallRetrieveOnLowHealth())
        {
            if (!worker.hasEffect(MobEffects.REGENERATION))
            {
                worker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
            }
        }

        final IColony colony = worker.getCitizenData().getColony();
        if( nearestHospital != null && colony.getServerBuildingManager().getBuilding(nearestHospital) instanceof BuildingHospital hospital){
            hospital.checkOrCreatePatientFile(worker.getCivilianID());
        }

        if(worker.getHealth() < ((int) worker.getMaxHealth() * 0.75D)){
            return GUARD_REGEN;
        }

        if(Objects.requireNonNull(worker.getCitizenColonyHandler().getColonyOrRegister()).getRaiderManager().isRaided()){
            return CombatAIStates.ATTACKING;
        }

        return START_WORKING;
    }

}
