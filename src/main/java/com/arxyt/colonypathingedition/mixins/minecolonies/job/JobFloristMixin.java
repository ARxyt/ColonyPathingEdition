package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkFlorist;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.colony.jobs.JobFlorist;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFlorist;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.FARMER_AI_MODULE;

@Mixin(value = JobFlorist.class, remap = false)
public abstract class JobFloristMixin extends AbstractJob<EntityAIWorkFlorist, JobFlorist> {
    public JobFloristMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(FARMER_AI_MODULE.get()){
            final NewEntityAIWorkFlorist tempAI = new NewEntityAIWorkFlorist((JobFlorist)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
