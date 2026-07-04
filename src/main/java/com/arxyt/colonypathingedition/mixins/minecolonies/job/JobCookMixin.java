package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkCook;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import com.minecolonies.core.colony.jobs.JobCook;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkCook;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.EATING_AI_MODULE;

@Mixin(value = JobCook.class, remap = false)
public abstract class JobCookMixin extends AbstractJob<EntityAIWorkCook, JobCook> {
    public JobCookMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(EATING_AI_MODULE.get()){
            final NewEntityAIWorkCook tempAI = new NewEntityAIWorkCook((JobCook)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
