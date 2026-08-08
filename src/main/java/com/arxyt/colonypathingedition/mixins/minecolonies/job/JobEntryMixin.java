package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.arxyt.colonypathingedition.core.job.NewJobDeliveryman;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(value = JobEntry.class, remap = false)
public class JobEntryMixin {
    @Final @Shadow(remap = false) private ResourceLocation key;
    @Final @Shadow(remap = false) private Function<ICitizenData, IJob<?>> jobProducer;

    /**
     * @author ARxyt
     * @reason Try a job switch.
     */
    @Overwrite(remap = false)
    public IJob<?> produceJob(final ICitizenData data) {
        final IJob<?> job;
        switch (key.getPath()) {
            case "deliveryman" : {
                if(PathingConfig.DELIVERYMAN_AI_MODULE.get()){
                    job = new NewJobDeliveryman(data);
                    break;
                }
            }
            default : {
                job = jobProducer.apply(data);
            }
        }
        job.setRegistryEntry((JobEntry) (Object)this);
        return job;
    }

    @Inject(method = "getHandlerProducer", at = @At("HEAD"), cancellable = true)
    private void getHandlerProducer(CallbackInfoReturnable<Function<ICitizenData, IJob<?>>> cir)
    {
        switch (key.getPath()) {
            case "deliveryman" -> {
                if(PathingConfig.DELIVERYMAN_AI_MODULE.get()) cir.setReturnValue(NewJobDeliveryman::new);
            }
        }
    }
}
