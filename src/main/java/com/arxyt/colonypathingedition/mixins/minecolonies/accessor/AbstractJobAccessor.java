package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.colony.jobs.AbstractJob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractJob.class, remap = false)
public interface AbstractJobAccessor {
    @Accessor(value = "citizen",remap = false) ICitizenData getCitizen();
}
