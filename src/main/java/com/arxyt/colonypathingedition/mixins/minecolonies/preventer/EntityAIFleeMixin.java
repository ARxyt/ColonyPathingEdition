package com.arxyt.colonypathingedition.mixins.minecolonies.preventer;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickRateStateMachine;
import com.minecolonies.api.entity.ai.statemachine.tickratestatemachine.ITickingTransition;
import com.minecolonies.api.entity.ai.statemachine.transitions.IStateMachineTransition;
import com.minecolonies.core.entity.ai.minimal.EntityAICitizenAvoidEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityAICitizenAvoidEntity.class, remap = false)
public class EntityAIFleeMixin {

    // To prevent disturbing the new AI system.
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

}
