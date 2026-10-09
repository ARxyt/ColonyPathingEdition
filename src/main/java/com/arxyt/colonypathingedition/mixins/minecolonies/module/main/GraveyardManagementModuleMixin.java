package com.arxyt.colonypathingedition.mixins.minecolonies.module.main;

import com.arxyt.colonypathingedition.core.easycolony.extension.IGraveDataExtension;
import com.minecolonies.api.colony.GraveData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.colony.buildings.modules.GraveyardManagementModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingGraveyard;
import com.minecolonies.core.tileentities.TileEntityNamedGrave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.checkerframework.common.reflection.qual.Invoke;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GraveyardManagementModule.class, remap = false)
public abstract class GraveyardManagementModuleMixin extends AbstractBuildingModule {
    @Shadow(remap = false) private @Nullable GraveData lastGraveData;

    @Redirect(
            method = "buryCitizenHere",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            remap = false
    )
    private boolean onBuryCitizenHereBeforeMarkDirty(
            List<Object> list,
            Object element,
            Tuple<BlockPos, Direction> positionAndDirection,
            AbstractEntityCitizen worker
    ) {
        boolean returnValue = list.add(element);
        if (lastGraveData == null) return returnValue;
        final IColony colony = building.getColony();
        if (positionAndDirection.getA() == null) return returnValue;
        BlockEntity tileEntity = colony.getWorld().getBlockEntity(positionAndDirection.getA());
        if (!(tileEntity instanceof TileEntityNamedGrave tileEntityNamedGrave)) return returnValue;
        ((IGraveDataExtension) tileEntityNamedGrave).setGraveData(lastGraveData);
        return returnValue;
    }
}
