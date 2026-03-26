package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickingTransition;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineTransition;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.entity.ai.minimal.EntityAICitizenAvoidEntity;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityAICitizenAvoidEntity.class, remap = false)
public class EntityAIRunToHospitalMixin {
    @Final @Shadow(remap = false) private EntityCitizen citizen;
    @Final @Shadow(remap = false) private double nearSpeed;
    @Shadow(remap = false) private PathResult moveAwayPath;

    @Unique private BlockPos nearestHospital;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/api/entity/ai/statemachine/tickratestatemachine/ITickRateStateMachine;addTransition(Lcom/minecolonies/api/entity/ai/statemachine/transitions/IStateMachineTransition;)V"
            ),
            remap = false
    )
    private void preventTransitions(ITickRateStateMachine<IState> instance, IStateMachineTransition<IState> iStateMachineTransition) {
        if(!PathingConfig.FLEE_AI_MODULE.get()){
            instance.addTransition((ITickingTransition<IState>) iStateMachineTransition);
        }
    }

    @Inject(
            method = "performMoveAway",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    public void performMoveAwayWithHospitalCheck(CallbackInfoReturnable<Boolean> cir)
    {
        if(citizen.getCitizenData().getCitizenDiseaseHandler().isHurt() && WorldUtil.isDayTime(this.citizen.level())){
            final IColony colony = citizen.getCitizenData().getColony();
            if (nearestHospital == null){
                nearestHospital = colony.getServerBuildingManager().getBestBuilding(citizen, BuildingHospital.class);
            }
            if (nearestHospital != null && (this.moveAwayPath == null || !this.moveAwayPath.isInProgress()) && this.citizen.getNavigation().isDone()){
                EntityNavigationUtils.walkToPos(citizen, nearestHospital, 3, true, nearSpeed);
                moveAwayPath = citizen.getNavigation().getPathResult();
                cir.setReturnValue(true);
            }
        }
    }
}
