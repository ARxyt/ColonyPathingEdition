package com.arxyt.colonypathingedition.mixins.minecolonies.workersetting;

import com.arxyt.colonypathingedition.api.workersetting.BuildingHospitalExtra;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingHospital;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;


@Mixin(BuildingHospital.class)
public abstract class BuildingHospitalMixin implements BuildingHospitalExtra {

    @Unique private BuildingHospital asHospital() {
        return (BuildingHospital)(Object)this;
    }

    @Unique private int healerOnDuty = -1;
    @Unique private boolean workerActive = false;
    @Unique private boolean shouldWork = false;
    @Unique private int healerCuringPlayer = -1;
    @Unique int healerWandering = -1;

    @Unique public void setCitizenInactive(){
        workerActive = false;
    }
    @Unique public void citizenShouldWork() {
        shouldWork = true;
    }
    @Unique public void citizenShouldNotWork() {
        shouldWork = false;
    }
    @Unique public boolean checkCitizenOnDuty(int citizenId) {
        return shouldWork && citizenId == healerOnDuty;
    }
    @Unique public boolean IsThisOnDutyCitizenID(int citizenId) {
        return citizenId == healerOnDuty;
    }
    @Unique public void resetHealerCuringPlayer(){
        healerCuringPlayer = -1;
    }
    @Unique public void resetHealerWandering(){
        healerWandering = -1;
    }

    @Unique
    public boolean noHealerCuringPlayer(int workerID){
        if(healerCuringPlayer != -1 && healerCuringPlayer != workerID){
            for(ICitizenData worker : asHospital().getAllAssignedCitizen()){
                if (worker.getId() == healerCuringPlayer){
                    return false;
                }
            }
        }
        healerCuringPlayer = workerID;
        return true;
    }

    @Unique
    public boolean noHealerWandering(int workerID){
        if(healerWandering != -1 && healerWandering != workerID){
            for(ICitizenData worker : asHospital().getAllAssignedCitizen()){
                if (worker.getId() == healerWandering){
                    return false;
                }
            }
        }
        healerWandering = workerID;
        return true;
    }

    @Unique
    public int getOnDutyCitizen(int workerID){
        final BuildingHospital hospital = asHospital();
        if (!workerActive) {
            Set<ICitizenData> citizens = hospital.getAllAssignedCitizen();
            if(citizens.contains(hospital.getColony().getCitizenManager().getCivilian(workerID))){
                healerOnDuty = workerID;
                workerActive = true;
            }
        }
        return healerOnDuty;
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    private void deserializeNBTAddition (CompoundTag compound,CallbackInfo cir){
        if(compound.contains("on_duty_worker")){
            healerOnDuty = compound.getInt("on_duty_worker");
        }
        if(compound.contains("duty_can_change")){
            workerActive = compound.getBoolean("duty_can_change");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"), remap = false, cancellable = true)
    private void serializeNBTAddition (CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("on_duty_worker", healerOnDuty);
        tag.putBoolean("duty_can_change", workerActive);
        cir.setReturnValue(tag);
    }
}
