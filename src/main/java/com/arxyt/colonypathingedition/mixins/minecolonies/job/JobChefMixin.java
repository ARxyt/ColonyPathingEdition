package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkChef;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobChef;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkChef;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.SMELTERY_AI_MODULE;

@Mixin(value = JobChef.class, remap = false)
public abstract class JobChefMixin extends AbstractJobCrafter<EntityAIWorkChef, JobChef> {
    public JobChefMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(SMELTERY_AI_MODULE.get()){
            final NewEntityAIWorkChef tempAI = new NewEntityAIWorkChef((JobChef)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
