package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractPathJob.class)
public interface AbstractPathJobAccessor {
    @Accessor(value = "start",remap = false) BlockPos getStartPos();
}
