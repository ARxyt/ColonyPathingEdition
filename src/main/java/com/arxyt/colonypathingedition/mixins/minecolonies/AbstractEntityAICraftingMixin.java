package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = AbstractEntityAICrafting.class, remap = false)
public abstract class AbstractEntityAICraftingMixin<J extends AbstractJobCrafter<?, J>, B extends AbstractBuilding> extends AbstractEntityAIInteract<J, B> {
    public AbstractEntityAICraftingMixin(@NotNull final J job)
    {
        super(job);
    }
}
