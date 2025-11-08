package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.crafting.PublicCrafting;
import com.minecolonies.api.crafting.IRecipeStorage;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAICrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractEntityAICrafting.class)
public interface AbstractEntityAICraftingAccessor {
    @Accessor(value = "currentRecipeStorage",remap = false) IRecipeStorage getCurrentRecipeStorage();
    @Accessor(value = "currentRequest",remap = false) IRequest<? extends PublicCrafting> getCurrentRequest();

    @Invoker(value = "resetValues",remap = false) void invokeResetValues();
}
