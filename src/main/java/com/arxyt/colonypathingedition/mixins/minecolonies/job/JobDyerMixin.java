package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkDyer;
import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkGlassblower;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobDyer;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkDyer;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.SMELTERY_AI_MODULE;

@Mixin(value = JobDyer.class, remap = false)
public abstract class JobDyerMixin extends AbstractJobCrafter<EntityAIWorkDyer, JobDyer> {
    public JobDyerMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(SMELTERY_AI_MODULE.get()){
            final NewEntityAIWorkDyer tempAI = new NewEntityAIWorkDyer((JobDyer)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
