package com.arxyt.colonypathingedition.mixins.minecolonies.netherworker;

import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JobNetherWorker.class)
public abstract class JobNetherWorkerMixin implements JobNetherWorkerExtra {
    public boolean eatBeforeLeave = false;
    public boolean extraRounds = false;

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    public void additionalDeserializeNBT(CompoundTag compound, CallbackInfo ci){
        if(compound.contains("extra_rounds")){
            extraRounds = compound.getBoolean("extra_rounds");
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"), remap = false, cancellable = true)
    public void additionalSerializeNBT(CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        tag.putBoolean("extra_rounds",extraRounds);
        cir.setReturnValue(tag);
    }

    public boolean setExtraRounds(boolean extraRounds) {
        this.extraRounds = extraRounds;
        return extraRounds;
    }

    public boolean getExtraRounds(){
        return this.extraRounds;
    }

    public void setShouldEat(boolean shouldEat){
        this.eatBeforeLeave = shouldEat;
    }

    public boolean getShouldEat(){
        return this.eatBeforeLeave;
    }
}
