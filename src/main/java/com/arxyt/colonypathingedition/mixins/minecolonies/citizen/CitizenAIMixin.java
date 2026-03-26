package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.api.workersetting.BuildingHospitalExtra;
import com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIEatTask;
import com.arxyt.colonypathingedition.core.ai.minimal.NewEntityAIFlee;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.entity.ai.IStateAI;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.CompatibilityUtils;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.jobs.JobHealer;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.colony.jobs.JobPupil;
import com.minecolonies.core.entity.ai.minimal.EntityAICitizenAvoidEntity;
import com.minecolonies.core.entity.ai.minimal.EntityAIEatTask;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.CitizenAI;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.EATING_AI_MODULE;
import static com.arxyt.colonypathingedition.core.config.PathingConfig.FLEE_AI_MODULE;
import static com.minecolonies.api.entity.citizen.VisibleCitizenStatus.*;
import static com.minecolonies.api.entity.citizen.VisibleCitizenStatus.HOUSE;
import static com.minecolonies.api.entity.citizen.VisibleCitizenStatus.MOURNING;
import static com.minecolonies.api.entity.citizen.VisibleCitizenStatus.WORKING;
import static com.minecolonies.api.util.constant.CitizenConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.COM_MINECOLONIES_COREMOD_ENTITY_CITIZEN_MOURNING;
import static com.minecolonies.api.util.constant.TranslationConstants.COM_MINECOLONIES_COREMOD_ENTITY_CITIZEN_RAID;
import static com.minecolonies.core.entity.ai.minimal.EntityAIEatTask.RESTAURANT_LIMIT;
import static com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler.SEEK_DOCTOR_HEALTH;

@Mixin(value = CitizenAI.class, remap = false)
public class CitizenAIMixin {
    @Final @Shadow(remap = false) private EntityCitizen citizen;
    @Shadow(remap = false) private IState lastState;

    @Shadow(remap = false) private List<IStateAI> minimalAI;

    @Inject(
            method = "<init>",
            at = @At("TAIL"),
            remap = false
    )
    private void initResetAI(EntityCitizen citizen, CallbackInfo ci){
        if(EATING_AI_MODULE.get()) {
            minimalAI.removeIf(iStateAI -> iStateAI instanceof EntityAIEatTask);
            minimalAI.add(new NewEntityAIEatTask(citizen));
        }
        if(FLEE_AI_MODULE.get()) {
            minimalAI.removeIf(iStateAI -> iStateAI instanceof EntityAICitizenAvoidEntity);
            minimalAI.add(new NewEntityAIFlee(citizen, Monster.class, INITIAL_RUN_SPEED_AVOID));
        }
    }

    @Inject(
            method = "calculateNextState",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void calculateNextState(CallbackInfoReturnable<IState> cir)
    {
        if (citizen.getCitizenJobHandler().getColonyJob() instanceof AbstractJobGuard<?> guardJob)
        {
            citizen.getCitizenAI().setCurrentDelay(40);
            if (newShouldEat() || (citizen.getCitizenData().getSaturation() <= 0 && citizen.getHealth() < SEEK_DOCTOR_HEALTH))
            {
                citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.EAT);
                cir.setReturnValue(CitizenAIState.EATING);
                return;
            }

            // Sick
            if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick() && !(citizen.getCitizenJobHandler().getColonyJob() instanceof JobHealer) && guardJob.canAIBeInterrupted())
            {
                citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.SICK);
                cir.setReturnValue(CitizenAIState.SICK);
                return;
            }

            citizen.setVisibleStatusIfNone(WORKING);
            cir.setReturnValue(CitizenAIState.WORK);
            return;
        }

        // Sick at hospital
        if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick() && citizen.getCitizenData().getCitizenDiseaseHandler().sleepsAtHospital())
        {
            citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.SICK);
            citizen.getCitizenAI().setCurrentDelay(40);
            cir.setReturnValue(CitizenAIState.SICK);
            return;
        }

        // Raiding
        if (citizen.getCitizenColonyHandler().getColonyOrRegister().getRaiderManager().isRaided())
        {
            citizen.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_ENTITY_CITIZEN_RAID), ChatPriority.IMPORTANT));
            citizen.setVisibleStatusIfNone(RAIDED);
            // 检查原始返回值是否是 SLEEP 且是因为殖民地被袭击
            if (citizen.getCitizenData().getWorkBuilding() instanceof BuildingHospital hospital) {
                if(((BuildingHospitalExtra)hospital).checkCitizenOnDuty(citizen.getCivilianID())) {
                    if (citizen.getCitizenSleepHandler().isAsleep()){
                        citizen.getCitizenSleepHandler().onWakeUp();
                    }
                    citizen.getCitizenAI().setCurrentDelay(20);
                    cir.setReturnValue(CitizenAIState.WORK);
                    return;
                }
            }
            citizen.getCitizenAI().setCurrentDelay(100);
            cir.setReturnValue(CitizenAIState.SLEEP);
            return;
        }

        // Sleeping
        if (!WorldUtil.isPastTime(CompatibilityUtils.getWorldFromCitizen(citizen), NIGHT - 2000))
        {
            if (lastState == CitizenAIState.SLEEP)
            {
                citizen.setVisibleStatusIfNone(SLEEP);
                if(citizen.level().getDayTime() % 24000 < 23700) {
                    citizen.getCitizenAI().setCurrentDelay(300);
                }
                else{
                    citizen.getCitizenAI().setCurrentDelay(40);
                }
                cir.setReturnValue(CitizenAIState.SLEEP);
                return;
            }

            if (citizen.getCitizenSleepHandler().shouldGoSleep())
            {
                citizen.getCitizenData().onGoSleep();
                cir.setReturnValue(CitizenAIState.SLEEP);
                return;
            }
        }
        else
        {
            if (citizen.getCitizenSleepHandler().isAsleep() || (lastState == CitizenAIState.SLEEP && citizen.getCitizenData().getHomeBuilding() != null))
            {
                if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick())
                {
                    final BlockPos bedPos = citizen.getCitizenSleepHandler().getBedLocation();
                    if (bedPos == null || bedPos.distSqr(citizen.blockPosition()) > 5)
                    {
                        citizen.getCitizenSleepHandler().onWakeUp();
                    }
                }
                else
                {
                    citizen.getCitizenSleepHandler().onWakeUp();
                }
            }
        }

        // Sick
        if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick() || citizen.getCitizenData().getCitizenDiseaseHandler().isHurt())
        {
            citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.SICK);
            citizen.getCitizenAI().setCurrentDelay(40);
            cir.setReturnValue(CitizenAIState.SICK);
            return;
        }

        // Eating
        if (newShouldEat())
        {
            citizen.getCitizenData().setVisibleStatus(VisibleCitizenStatus.EAT);
            citizen.getCitizenAI().setCurrentDelay(40);
            cir.setReturnValue(CitizenAIState.EATING);
            return;
        }

        // Mourning
        if (citizen.getCitizenData().getCitizenMournHandler().isMourning() && citizen.level().getDayTime() % 24000 < NOON)
        {
            if (lastState != CitizenAIState.MOURN)
            {
                citizen.getCitizenData().triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_ENTITY_CITIZEN_MOURNING,
                        citizen.getCitizenData().getCitizenMournHandler().getDeceasedCitizens().iterator().next()),
                        Component.translatable(COM_MINECOLONIES_COREMOD_ENTITY_CITIZEN_MOURNING),
                        ChatPriority.IMPORTANT));

                citizen.setVisibleStatusIfNone(MOURNING);
            }
            citizen.getCitizenAI().setCurrentDelay(20 * 15);
            cir.setReturnValue(CitizenAIState.MOURN);
            return;
        }

        // Work
        if (citizen.getCitizenJobHandler().getColonyJob() == null || (citizen.isBaby() && citizen.getCitizenJobHandler().getColonyJob() instanceof JobPupil && citizen.level().getDayTime() % 24000 > 9000))
        {
            citizen.setVisibleStatusIfNone(HOUSE);
            citizen.getCitizenAI().setCurrentDelay(20 * 15);
            cir.setReturnValue(CitizenAIState.IDLE);
            return;
        }

        if ( citizen.getCitizenJobHandler().getColonyJob().getWorkerAI() instanceof AbstractEntityAIBasic<?,?> abstractEntityAIBasic && !abstractEntityAIBasic.canGoIdle()
                && (citizen.getCitizenData().getLeisureTime() <= 0
                || !citizen.getCitizenData().getJob().canAIBeInterrupted()))
        {
            citizen.setVisibleStatusIfNone(WORKING);
            citizen.getCitizenAI().setCurrentDelay(20);
            cir.setReturnValue(CitizenAIState.WORK);
            return;
        }

        citizen.setVisibleStatusIfNone(HOUSE);
        cir.setReturnValue(CitizenAIState.IDLE);
    }

    @Unique
    private boolean newShouldEat() {
        if(citizen.getCitizenJobHandler().getColonyJob() instanceof JobNetherWorker job){
            if(!citizen.getCitizenData().justAte()) {
                return ((JobNetherWorkerExtra)job).getShouldEat();
            }
            ((JobNetherWorkerExtra)job).setShouldEat(false);
        }

        if (citizen.getCitizenData().justAte())
        {
            return false;
        }

        if (citizen.getCitizenData().getJob() != null && (!citizen.getCitizenData().getJob().canAIBeInterrupted()))
        {
            return false;
        }

        if (lastState == CitizenAIState.EATING)
        {
            return true;
        }

        if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick() && citizen.getCitizenSleepHandler().isAsleep())
        {
            return false;
        }

        return (citizen.getCitizenData().getSaturation() <= RESTAURANT_LIMIT ||
                (citizen.getCitizenData().getSaturation() < LOW_SATURATION && citizen.getHealth() < SEEK_DOCTOR_HEALTH));
    }

}
