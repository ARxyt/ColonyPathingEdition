package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate;
import com.minecolonies.core.entity.pathfinding.pathjobs.AbstractPathJob;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecoloniesAdvancedPathNavigate.class)
public interface MinecoloniesAdvancedPathNavigateAccessor {
    @Invoker(value = "walkTowards",remap = false) PathResult<AbstractPathJob> invokeWalkTowards(final BlockPos towards, final double range, final double speedFactor);
}
