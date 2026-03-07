package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.CitizenAIState;
import com.minecolonies.api.entity.ai.statemachine.states.IState;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.constant.CitizenConstants;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobMiner;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.ai.minimal.EntityAISickTask;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.entity.pathfinding.navigation.EntityNavigationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.util.constant.TranslationConstants.NO_HOSPITAL;
import static com.minecolonies.api.util.constant.TranslationConstants.WAITING_FOR_CURE;
import static com.minecolonies.core.entity.ai.minimal.EntityAISickTask.DiseaseState.*;

@Mixin(value = EntityAISickTask.class, remap = false)
abstract public class EntityAISickTaskMixin {
    @Final @Shadow(remap = false) private EntityCitizen citizen;
    @Final @Shadow(remap = false) private ICitizenData citizenData;
    @Shadow(remap = false) private BlockPos bestHospital;

    @Shadow (remap = false) protected abstract void reset();

    @Unique private Disease guessDisease = null;

    /**
     * @author ARxyt
     * @reason Look for heal modification.
     */
    @Overwrite(remap = false)
    private IState checkForCure()
    {
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            if(citizen.getHealth() >= Math.min(citizen.getMaxHealth(),100)){
                reset();
                return CitizenAIState.IDLE;
            }
            if(citizenData.getJob() instanceof JobMiner){
                return GO_TO_HUT;
            }
            return SEARCH_HOSPITAL;
        }
        for (final ItemStorage cure : disease.cureItems())
        {
            final int slot = InventoryUtils.findFirstSlotInProviderNotEmptyWith(citizen, Disease.hasCureItem(cure));
            if (slot == -1)
            {
                if (citizen.getCitizenData().getCitizenDiseaseHandler().isSick())
                {
                    guessDisease = disease;
                    return SEARCH_HOSPITAL;
                }
                reset();
                return CitizenAIState.IDLE;
            }
        }
        return APPLY_CURE;
    }

    // Reserved code
//    /**
//     * @author ARxyt
//     * @reason
//     */
//    @Overwrite(remap = false)
//    private IState goToHut()

    /**
     * @author ARxyt
     * @reason Check if cured.
     */
    @Inject(
            method = "wander",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    public void wanderWithRecoveryCheck(CallbackInfoReturnable <IState> cir)
    {
        if(citizen.getCitizenData().getCitizenDiseaseHandler().getDisease() == null && guessDisease != null){
            reset();
            cir.setReturnValue(CHECK_FOR_CURE);
        }
    }


    /**
     * @author ARxyt
     * @reason Look for heal modification.
     */
    @Overwrite(remap = false)
    private IState searchHospital()
    {
        final IColony colony = citizenData.getColony();
        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        bestHospital = colony.getServerBuildingManager().getBestBuilding(citizen, BuildingHospital.class);

        if (bestHospital == null)
        {
            if (disease == null)
            {
                if(citizenData.getSaturation() < CitizenConstants.FULL_SATURATION){
                    return CitizenAIState.EATING;
                }
                return CitizenAIState.IDLE;
            }
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(NO_HOSPITAL, disease.name(), disease.getCureString()),
                    Component.translatable(NO_HOSPITAL),
                    ChatPriority.BLOCKING));
            return WANDER;
        }
        else if (disease != null)
        {
            citizenData.triggerInteraction(new StandardInteraction(Component.translatable(WAITING_FOR_CURE, disease.name(), disease.getCureString()),
                    Component.translatable(WAITING_FOR_CURE),
                    ChatPriority.BLOCKING));
        }

        return GO_TO_HOSPITAL;
    }

    /**
     * @author ARxyt
     * @reason Check if cured.
     */
    @Inject(
            method = "goToHospital",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    public void goToHospitalWithRecoveryCheck(CallbackInfoReturnable <IState> cir)
    {
        if(citizen.getCitizenData().getCitizenDiseaseHandler().getDisease() == null && guessDisease != null){
            cir.setReturnValue(CHECK_FOR_CURE);
        }
    }

    /**
     * @author ARxyt
     * @reason Look for heal modification.
     */
    @Overwrite(remap = false)
    private IState waitForCure()
    {
        if (bestHospital == null)
        {
            return SEARCH_HOSPITAL;
        }

        final IState state = checkForCure();
        if (state == APPLY_CURE)
        {
            return APPLY_CURE;
        }
        else if (state == CitizenAIState.IDLE)
        {
            reset();
            return CitizenAIState.IDLE;
        }

        if(citizen.getCitizenData().getCitizenDiseaseHandler().getDisease() == null && guessDisease != null){
            reset();
            return CitizenAIState.IDLE;
        }

        final IColony colony = citizenData.getColony();
        IBuilding building = colony.getServerBuildingManager().getBuilding(bestHospital);
        if(!(building instanceof BuildingHospital hospital) ){
            return SEARCH_HOSPITAL;
        }
        if(!hospital.isInBuilding(citizen.blockPosition())){
            EntityNavigationUtils.walkToPos(citizen, bestHospital, 3, true);
        }
        boolean isPatient = false;
        for (Patient patient : hospital.getPatients()){
            if (patient.getId() == citizen.getCivilianID()){
                isPatient = true;
                break;
            }
        }
        if(!isPatient){
            hospital.checkOrCreatePatientFile(citizen.getCivilianID());
        }

        boolean asleep = citizen.getCitizenSleepHandler().isAsleep();
        if(guessDisease != null){
            if (asleep)
            {
                if ( !(hospital.getBedList().contains(citizen.getCitizenSleepHandler().getBedLocation())))
                {
                    citizen.getCitizenSleepHandler().onWakeUp();
                    asleep = false;
                }
            }
            else if(isPatient)
            {
                return FIND_EMPTY_BED;
            }
            if (!asleep)
            {
                return GO_TO_HOSPITAL;
            }
        }
        return WAIT_FOR_CURE;
    }

    @Inject(
            method = "findEmptyBed",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void findEmptyBedWithRecoveryCheck(CallbackInfoReturnable <IState> cir){
        if(citizen.getCitizenData().getCitizenDiseaseHandler().getDisease() == null){
            cir.setReturnValue(CHECK_FOR_CURE);
        }
    }

    @Inject(
            method = "reset",
            at = @At("RETURN"),
            remap = false
    )
    private void afterReset(CallbackInfo cir){
        guessDisease = null;
    }

}
