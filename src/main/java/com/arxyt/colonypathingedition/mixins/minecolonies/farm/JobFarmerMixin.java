package com.arxyt.colonypathingedition.mixins.minecolonies.farm;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.colony.jobs.JobFarmer;
import com.minecolonies.core.entity.ai.workers.production.agriculture.EntityAIWorkFarmer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(JobFarmer.class)
public abstract class JobFarmerMixin extends AbstractJobCrafter<EntityAIWorkFarmer, JobFarmer> {
    public JobFarmerMixin(final ICitizenData entity)
    {
        super(entity);
    }

    @Override
    public boolean ignoresDamage(@NotNull final DamageSource damageSource){
        return damageSource.is(DamageTypes.SWEET_BERRY_BUSH);
    }
}
