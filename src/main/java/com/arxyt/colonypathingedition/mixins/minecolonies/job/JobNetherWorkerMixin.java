package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.api.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkNetherWorker;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.NETHER_WORKER_AI_MODULE;

@Mixin(value = JobNetherWorker.class, remap = false)
public abstract class JobNetherWorkerMixin  extends AbstractJobCrafter<EntityAIWorkNether, JobNetherWorker> implements JobNetherWorkerExtra {
    @Unique public boolean eatBeforeLeave = false;
    @Unique public boolean extraRounds = false;

    public JobNetherWorkerMixin(ICitizenData entity)
    {
        super(entity);
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    public void additionalDeserializeNBT(HolderLookup.Provider provider, CompoundTag compound, CallbackInfo ci){
        if(compound.contains("extra_rounds")){
            extraRounds = compound.getBoolean("extra_rounds");
        }
    }

    @Inject(method = "serializeNBT(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"), remap = false, cancellable = true)
    public void additionalSerializeNBT(CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        tag.putBoolean("extra_rounds",extraRounds);
        cir.setReturnValue(tag);
    }

    @Override
    public void createAI(){
        if(NETHER_WORKER_AI_MODULE.get()){
            final NewEntityAIWorkNetherWorker tempAI = new NewEntityAIWorkNetherWorker((JobNetherWorker)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
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
