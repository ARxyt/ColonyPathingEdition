package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.api.extras.JobNetherWorkerExtra;
import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkNetherWorker;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobNetherWorker;
import com.minecolonies.core.entity.ai.workers.production.EntityAIWorkNether;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.NETHER_WORKER_AI_MODULE;

@Mixin(value = JobNetherWorker.class, remap = false)
public abstract class JobNetherWorkerMixin extends AbstractJobCrafter<EntityAIWorkNether, JobNetherWorker> implements JobNetherWorkerExtra {
    @Unique public boolean eatBeforeLeave = false;
    @Unique public int extraRounds = 0;
    @Unique public Set<EquipmentTypeEntry> hasOrdered = new HashSet<>();

    public JobNetherWorkerMixin(ICitizenData entity)
    {
        super(entity);
    }

    @Inject(method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"), remap = false)
    public void additionalDeserializeNBT(CompoundTag compound, CallbackInfo ci){
        if(compound.contains("extra_round_times")){
            extraRounds = compound.getInt("extra_round_times");
        }
        for(NewEntityAIWorkNetherWorker.Tools tool: NewEntityAIWorkNetherWorker.Tools.values()) {
            String key = "has_ordered_" + tool.getType().toString();
            if(compound.contains(key)) {
                hasOrdered.add(tool.getType());
            }
        }
    }

    @Inject(method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"), remap = false, cancellable = true)
    public void additionalSerializeNBT(CallbackInfoReturnable<CompoundTag> cir){
        CompoundTag tag = cir.getReturnValue();
        tag.putInt("extra_round_times", extraRounds);
        for(EquipmentTypeEntry equipmentTypeEntry : hasOrdered) {
            tag.putBoolean("has_ordered_" + equipmentTypeEntry.toString(), true);
        }
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
        this.extraRounds = extraRounds ? this.extraRounds + 1 : 0;
        return extraRounds;
    }

    public boolean getExtraRounds(){
        return this.extraRounds > 0;
    }

    public boolean canExtraRounds(int limit){
        return this.extraRounds < limit;
    }

    public int remainExtraRounds(int limit){
        return limit - this.extraRounds - 1;
    }

    public void setShouldEat(boolean shouldEat){
        this.eatBeforeLeave = shouldEat;
    }

    public boolean getShouldEat(){
        return this.eatBeforeLeave;
    }

    public Set<EquipmentTypeEntry> getHasOrdered() {
        return hasOrdered;
    }
}
