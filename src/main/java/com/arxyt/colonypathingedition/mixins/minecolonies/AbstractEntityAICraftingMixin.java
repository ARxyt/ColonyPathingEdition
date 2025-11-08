package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAICraftingAccessor;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.jobs.AbstractJobCrafter;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractEntityAICrafting.class)
public abstract class AbstractEntityAICraftingMixin<B extends AbstractBuilding, J extends AbstractJobCrafter<?, J>> extends AbstractEntityAIBasicMixin<B,J> implements AbstractEntityAICraftingAccessor {
    @Shadow(remap = false) protected IRecipeStorage currentRecipeStorage;

    @Unique public void resetCurrentRecipeStorage(){
        currentRecipeStorage = null;
    }
}
