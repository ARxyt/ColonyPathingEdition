package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkBaker;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobBaker;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkBaker;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.SMELTERY_AI_MODULE;

@Mixin(value = JobBaker.class, remap = false)
public abstract class JobBakerMixin extends AbstractJobCrafter<EntityAIWorkBaker, JobBaker> {
    public JobBakerMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(SMELTERY_AI_MODULE.get()){
            final NewEntityAIWorkBaker tempAI = new NewEntityAIWorkBaker((JobBaker)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
