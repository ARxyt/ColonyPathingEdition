package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkPlanter;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobPlanter;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkPlanter;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.PLANTER_AI_MODULE;

@Mixin(value = JobPlanter.class, remap = false)
public abstract class JobPlanterMixin extends AbstractJobCrafter<EntityAIWorkPlanter, JobPlanter> {
    public JobPlanterMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(PLANTER_AI_MODULE.get()){
            final NewEntityAIWorkPlanter tempAI = new NewEntityAIWorkPlanter((JobPlanter)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
