package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractEntityAIBasic.class, remap = false)
public interface AbstractEntityAIBasicAccessor<B extends AbstractBuilding> {
    @Accessor(value = "building",remap = false) B getBuilding();
    @Accessor(value = "walkTo",remap = false) BlockPos getWalkTo();

    @Final @Invoker(value = "walkToBuilding",remap = false) boolean invokeWalkToBuilding();
    @Final @Invoker(value = "walkToBuilding",remap = false) boolean invokeWalkToBuilding(IBuilding building);
    @Final @Invoker(value = "walkToSafePos",remap = false) boolean invokeWalkToSafePos(final BlockPos blockpos);
    @Final @Invoker(value = "setDelay",remap = false) void invokeSetDelay(final int timeout);
    @Invoker(value = "getInventory",remap = false) InventoryCitizen invokeGetInventory();
    @Invoker(value = "getPrimarySkillLevel",remap = false ) int invokeGetPrimarySkillLevel();
    @Invoker(value = "getSecondarySkillLevel",remap = false ) int invokeGetSecondarySkillLevel();
    @Invoker(value = "getModuleForJob",remap = false) WorkerBuildingModule invokeGetModuleForJob();
}
