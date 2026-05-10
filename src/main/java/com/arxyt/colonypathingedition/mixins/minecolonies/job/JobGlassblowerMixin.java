package com.arxyt.colonypathingedition.mixins.minecolonies.job;

import com.arxyt.colonypathingedition.core.ai.worker.NewEntityAIWorkGlassblower;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobGlassblower;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkGlassblower;
import org.spongepowered.asm.mixin.Mixin;

import static com.arxyt.colonypathingedition.core.config.PathingConfig.SMELTERY_AI_MODULE;

@Mixin(value = JobGlassblower.class, remap = false)
public abstract class JobGlassblowerMixin extends AbstractJobCrafter<EntityAIWorkGlassblower, JobGlassblower> {
    public JobGlassblowerMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public void createAI(){
        if(SMELTERY_AI_MODULE.get()){
            final NewEntityAIWorkGlassblower tempAI = new NewEntityAIWorkGlassblower((JobGlassblower)((Object)this));
            if(tempAI != null){
                getCitizen().getEntity().get().getCitizenJobHandler().setWorkAI(tempAI);
                return;
            }
        }
        super.createAI();
    }
}
