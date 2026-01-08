package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkStoneSmeltery;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobStoneSmeltery;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkStoneSmeltery;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.SMELTERY_AI_MODULE;

@Mixin(JobStoneSmeltery.class)
public abstract class JobStoneSmelteryMixin extends AbstractJobCrafter<EntityAIWorkStoneSmeltery, JobStoneSmeltery> {
    public JobStoneSmelteryMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(SMELTERY_AI_MODULE.get()){
            final NewEntityAIWorkStoneSmeltery tempAI = new NewEntityAIWorkStoneSmeltery((JobStoneSmeltery)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
