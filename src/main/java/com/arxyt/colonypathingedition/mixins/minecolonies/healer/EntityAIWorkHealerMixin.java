package com.arxyt.colonypathingedition.mixins.minecolonies.healer;

import com.arxyt.colonypathingedition.api.workersetting.BuildingHospitalExtra;
import com.arxyt.colonypathingedition.api.PatientExtras;
import com.arxyt.colonypathingedition.mixins.minecolonies.AbstractEntityAIBasicMixin;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.arxyt.colonypathingedition.core.util.DistanceUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.Stack;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.datalistener.model.Disease;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkHealer;
import com.minecolonies.core.entity.ai.workers.util.Patient;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.network.messages.client.CircleParticleEffectMessage;
import com.minecolonies.core.network.messages.client.StreamParticleEffectMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.CitizenConstants.NOON;
import static com.minecolonies.api.util.constant.TranslationConstants.PATIENT_FULL_INVENTORY;

@Mixin(value = EntityAIWorkHealer.class, remap = false)
public abstract class EntityAIWorkHealerMixin extends AbstractEntityAIBasicMixin<BuildingHospital, IJob<?>> implements AbstractEntityAIBasicAccessor<BuildingHospital>{
    @Final @Shadow(remap = false) private static double BASE_XP_GAIN;
    @Final @Shadow(remap = false) private static int MAX_PROGRESS_TICKS;
    @Shadow(remap = false) private Patient currentPatient;
    @Shadow(remap = false) private int progressTicks;
    @Shadow(remap = false) private ICitizenData remotePatient;
    @Shadow(remap = false) private Player playerToHeal;

    @Shadow(remap = false) protected abstract boolean hasCureInInventory(final Disease disease, final IItemHandler handler);
    @Shadow(remap = false) protected abstract void recordTreatmentStats(EntityCitizen citizen);

    @Unique
    private boolean testRandomCureChance()
    {
        return getWorker().getRandom().nextInt(1200 ) <= 1 + invokeGetSecondarySkillLevel() / 20;
    }

    @Unique
    private boolean notFullHealth(AbstractEntityCitizen citizen){
        return citizen.getMaxHealth() > citizen.getHealth();
    }

    /**
     * @author ARxyt
     * @reason 后期会调整请求物资的方案，由于过于费时先搁置
     */
    @Overwrite(remap = false)
    private IAIState decide()
    {
        if (!invokeWalkToBuilding())
        {
            return DECIDE;
        }

        BuildingHospital hospital = this.building;
        BuildingHospitalExtra hospitalExtra = (BuildingHospitalExtra) hospital;

        if(WorldUtil.isPastTime(getWorker().level(),NOON)){
            hospitalExtra.setCitizenInactive();
        }


        for (final Player player : WorldUtil.getEntitiesWithinBuilding(getWorld(),
                Player.class,
                getBuilding(),
                player -> player.getHealth() < player.getMaxHealth() - 10 ))
        {
            playerToHeal = player;
            if(hospitalExtra.noHealerCuringPlayer(getWorker().getId())){
                return CURE_PLAYER;
            }
        }


        for (Patient patient : hospital.getPatients())
        {
            ICitizenData data = hospital.getColony().getCitizenManager().getCivilian(patient.getId());
            if (data == null || data.getEntity().isEmpty())
            {
                hospital.removePatientFile(patient);
                continue;
            }
            EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
            Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
            if ( disease == null && !notFullHealth(citizen) ){
                hospital.removePatientFile(patient);
                continue;
            }
            else if (!building.isInBuilding(citizen.blockPosition())){
                continue;
            }

            PatientExtras patientExtras = (PatientExtras)patient;
            int doctorID = patientExtras.getEmployed();
            if ( doctorID != getWorker().getCivilianID()){
                ICitizenData thisCitizen = hospital.getColony().getCitizenManager().getCivilian(doctorID);
                if ( thisCitizen == null || thisCitizen.getWorkBuilding() == null || !thisCitizen.getWorkBuilding().getPosition().equals(hospital.getPosition())){
                    patientExtras.setEmployed(getWorker().getCivilianID());
                }
                else{
                    continue;
                }
            }
            if (patient.getState() == Patient.PatientState.NEW)
            {
                if(disease != null){
                    this.currentPatient = patient;
                    return REQUEST_CURE;
                }
                else{
                    patient.setState(Patient.PatientState.REQUESTED);
                }
            }

            if (patient.getState() == Patient.PatientState.REQUESTED)
            {
                if (testRandomCureChance())
                {
                    this.currentPatient = patient;
                    patientExtras.setEmployed(getWorker().getCivilianID());
                    return FREE_CURE;
                }

                if (disease == null)
                {
                    this.currentPatient = patient;
                    patientExtras.setEmployed(getWorker().getCivilianID());
                    return CURE;
                }

                if (citizen.getInventoryCitizen().hasSpace())
                {
                    if (hasCureInInventory(disease, getWorker().getInventoryCitizen()) ||
                            hasCureInInventory(disease, building.getItemHandlerCap()))
                    {
                        this.currentPatient = patient;
                        patientExtras.setEmployed(getWorker().getCivilianID());
                        return CURE;
                    }

                    final ImmutableList<IRequest<? extends Stack>> list = building.getOpenRequestsOfType(getWorker().getCitizenData().getId(), TypeToken.of(Stack.class));
                    final ImmutableList<IRequest<? extends Stack>> completed = building.getCompletedRequestsOfType(getWorker().getCitizenData(), TypeToken.of(Stack.class));
                    for (final ItemStorage cure : disease.cureItems())
                    {
                        if (!InventoryUtils.hasItemInItemHandler(getWorker().getInventoryCitizen(), Disease.hasCureItem(cure)))
                        {
                            if (InventoryUtils.getItemCountInItemHandler(building.getItemHandlerCap(),
                                    Disease.hasCureItem(cure)) >= cure.getAmount())
                            {
                                needsCurrently = new Tuple<>(Disease.hasCureItem(cure), cure.getAmount());
                                return GATHERING_REQUIRED_MATERIALS;
                            }
                            boolean hasCureRequested = false;
                            for (final IRequest<? extends Stack> request : list)
                            {
                                if (Disease.isCureItem(request.getRequest().getStack(), cure))
                                {
                                    hasCureRequested = true;
                                    break;
                                }
                            }
                            for (final IRequest<? extends Stack> request : completed)
                            {
                                if (Disease.isCureItem(request.getRequest().getStack(), cure))
                                {
                                    hasCureRequested = true;
                                    break;
                                }
                            }
                            if (!hasCureRequested)
                            {
                                patient.setState(Patient.PatientState.NEW);
                                break;
                            }
                        }
                    }
                }
                else
                {
                    data.triggerInteraction(new StandardInteraction(Component.translatable(PATIENT_FULL_INVENTORY), ChatPriority.BLOCKING));
                }
            }

            if (patient.getState() == Patient.PatientState.TREATED)
            {
                if (disease == null)
                {
                    this.currentPatient = patient;
                    patientExtras.setEmployed(getWorker().getCivilianID());
                    return CURE;
                }

                if (!hasCureInInventory(disease, citizen.getInventoryCitizen()))
                {
                    patient.setState(Patient.PatientState.NEW);
                    return DECIDE;
                }
            }
        }

        if(Objects.requireNonNull(getWorker().getCitizenColonyHandler().getColonyOrRegister()).getRaiderManager().isRaided()){
            return DECIDE;
        }

        if(!WorldUtil.isDayTime(getWorker().level())){
            ((BuildingHospitalExtra)building).citizenShouldNotWork();
            return DECIDE;
        }

        if(hospitalExtra.noHealerCuringPlayer(getWorker().getId())) {
            final ICitizenData data = getBuilding().getColony().getCitizenManager().getRandomCitizen();
            if (data.getEntity().isPresent() && data.getCitizenDiseaseHandler().isHurt()
                    && BlockPosUtil.getDistance2D(data.getEntity().get().blockPosition(), getBuilding().getPosition()) < getBuilding().getBuildingLevel() * 40L)
            {
                remotePatient = data;
                return WANDER;
            }
        }

        return DECIDE;
    }

    /**
     * @author ARxyt
     * @reason 修改治疗方案和能力
     */
    @Overwrite(remap = false)
    private IAIState cure()
    {
        if (currentPatient == null)
        {
            return DECIDE;
        }
        PatientExtras patientExtras = (PatientExtras) currentPatient;
        final ICitizenData data = building.getColony().getCitizenManager().getCivilian(currentPatient.getId());
        if (data == null || data.getEntity().isEmpty() || !(data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().isSick() || notFullHealth(data.getEntity().get())))
        {
            patientExtras.setEmployed(-1);
            currentPatient = null;
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
        if (!invokeWalkToSafePos(citizen.blockPosition()))
        {
            return CURE;
        }

        final Disease disease = citizen.getCitizenData().getCitizenDiseaseHandler().getDisease();
        if (disease == null)
        {
            currentPatient = null;
            citizen.heal(10 + invokeGetPrimarySkillLevel() / 4.0F );
            citizen.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20 + invokeGetSecondarySkillLevel() * 2,getBuilding().getBuildingLevel()));
            getWorker().getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
            patientExtras.setEmployed(-1);
            return DECIDE;
        }

        if (!hasCureInInventory(disease, getWorker().getInventoryCitizen()))
        {
            if (hasCureInInventory(disease, building.getItemHandlerCap()))
            {
                for (final ItemStorage cure : disease.cureItems())
                {
                    if (InventoryUtils.getItemCountInItemHandler(getWorker().getInventoryCitizen(), Disease.hasCureItem(cure)) < cure.getAmount())
                    {
                        needsCurrently = new Tuple<>(Disease.hasCureItem(cure), 1);
                        return GATHERING_REQUIRED_MATERIALS;
                    }
                }
            }
            patientExtras.setEmployed(-1);
            currentPatient = null;
            return DECIDE;
        }

        if (!hasCureInInventory(disease, citizen.getInventoryCitizen()))
        {
            for (final ItemStorage cure : disease.cureItems())
            {
                if (InventoryUtils.getItemCountInItemHandler(citizen.getInventoryCitizen(), Disease.hasCureItem(cure)) < cure.getAmount())
                {
                    if (!citizen.getInventoryCitizen().hasSpace())
                    {
                        data.triggerInteraction(new StandardInteraction(Component.translatable(PATIENT_FULL_INVENTORY), ChatPriority.BLOCKING));
                        currentPatient = null;
                        return DECIDE;
                    }
                    InventoryUtils.transferXOfFirstSlotInItemHandlerWithIntoNextFreeSlotInItemHandler(
                            getWorker().getInventoryCitizen(),
                            Disease.hasCureItem(cure),
                            cure.getAmount(), citizen.getInventoryCitizen()
                    );
                }
            }
        }

        recordTreatmentStats(citizen);
        getWorker().getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        currentPatient.setState(Patient.PatientState.TREATED);
        currentPatient = null;
        return DECIDE;
    }

    /**
     * @author ARxyt
     * @reason 修改治疗方案和能力
     */
    @Overwrite(remap = false)
    private IAIState freeCure()
    {
        if (currentPatient == null)
        {
            return DECIDE;
        }
        PatientExtras patientExtras = (PatientExtras) currentPatient;
        final ICitizenData data = building.getColony().getCitizenManager().getCivilian(currentPatient.getId());
        if (data == null || data.getEntity().isEmpty() || !(data.getEntity().get().getCitizenData().getCitizenDiseaseHandler().isSick() || notFullHealth(data.getEntity().get())))
        {
            patientExtras.setEmployed(-1);
            currentPatient = null;
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) data.getEntity().get();
        if (!invokeWalkToSafePos(citizen.blockPosition()))
        {
            progressTicks = 0;
            return FREE_CURE;
        }

        progressTicks++;
        if (progressTicks < MAX_PROGRESS_TICKS)
        {
            new StreamParticleEffectMessage(getWorker().position().add(0, 2, 0), citizen.position(), ParticleTypes.HEART, progressTicks % MAX_PROGRESS_TICKS, MAX_PROGRESS_TICKS)
                    .sendToTrackingEntity(getWorker());

            new CircleParticleEffectMessage(getWorker().position().add(0, 2, 0), ParticleTypes.HEART, progressTicks)
                    .sendToTrackingEntity(getWorker());

            return invokeGetState();
        }

        progressTicks = 0;
        citizen.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20 + invokeGetSecondarySkillLevel() * 2,getBuilding().getBuildingLevel()));
        citizen.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,20 + invokeGetSecondarySkillLevel() * 2,2));
        citizen.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,20 + invokeGetSecondarySkillLevel() * 2,1 + invokeGetPrimarySkillLevel() / 8));
        recordTreatmentStats(citizen);
        getWorker().getCitizenExperienceHandler().addExperience(BASE_XP_GAIN);
        citizen.getCitizenData().getCitizenDiseaseHandler().cure();
        currentPatient.setState(Patient.PatientState.TREATED);
        currentPatient = null;
        patientExtras.setEmployed(-1);
        return DECIDE;
    }

    /**
     * @author ARxyt
     * @reason 修改治疗方案和能力
     */
    @Overwrite(remap = false)
    private IAIState curePlayer()
    {
        if (playerToHeal == null)
        {
            ((BuildingHospitalExtra)building).resetHealerCuringPlayer();
            return DECIDE;
        }

        if (!walkToUnSafePos(playerToHeal.blockPosition()))
        {
            return invokeGetState();
        }

        playerToHeal.heal(2 + 2 * building.getBuildingLevel());
        playerToHeal.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20 + invokeGetSecondarySkillLevel() * 2,getBuilding().getBuildingLevel()));
        playerToHeal.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,20 + invokeGetSecondarySkillLevel() * 2,2));
        getWorker().getCitizenExperienceHandler().addExperience(1);
        ((BuildingHospitalExtra)building).resetHealerCuringPlayer();
        return DECIDE;
    }

    /**
     * @author ARxyt
     * @reason 修改治疗方案和能力
     */
    @Overwrite(remap = false)
    private IAIState wander()
    {
        if (remotePatient == null || remotePatient.getEntity().isEmpty())
        {
            ((BuildingHospitalExtra)building).resetHealerWandering();
            return DECIDE;
        }

        final EntityCitizen citizen = (EntityCitizen) remotePatient.getEntity().get();
        if(citizen.getMaxHealth() <= citizen.getHealth()){
            ((BuildingHospitalExtra)building).resetHealerWandering();
            return START_WORKING;
        }
        BlockPos nowPlace = remotePatient.getEntity().get().blockPosition();
        if (!walkToUnSafePos(nowPlace) && DistanceUtils.dist(nowPlace,getWorker().blockPosition()) > 5)
        {
            return invokeGetState();
        }

        new CircleParticleEffectMessage(remotePatient.getEntity().get().position(), ParticleTypes.HEART, 1)
                .sendToTrackingEntity(getWorker());

        citizen.heal(10 + invokeGetPrimarySkillLevel() / 4.0F );
        citizen.addEffect(new MobEffectInstance(MobEffects.REGENERATION,20 + invokeGetSecondarySkillLevel() * 2, getBuilding().getBuildingLevel()));
        getWorker().getCitizenExperienceHandler().addExperience(1);

        remotePatient = null;
        ((BuildingHospitalExtra)building).resetHealerWandering();
        return START_WORKING;
    }
}
