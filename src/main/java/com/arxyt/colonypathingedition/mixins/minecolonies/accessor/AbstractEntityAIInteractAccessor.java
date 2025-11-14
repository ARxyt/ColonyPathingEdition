package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(value = AbstractEntityAIInteract.class, remap = false)
public interface AbstractEntityAIInteractAccessor {
    @Invoker(value = "searchForItems",remap = false) void invokeSearchForItems(final AABB boundingBox);
    @Invoker(value = "getItemsForPickUp",remap = false) List<BlockPos> invokeGetItemsForPickUp();
    @Invoker(value = "getAndRemoveClosestItemPosition",remap = false) BlockPos invokeGetAndRemoveClosestItemPosition();
}
