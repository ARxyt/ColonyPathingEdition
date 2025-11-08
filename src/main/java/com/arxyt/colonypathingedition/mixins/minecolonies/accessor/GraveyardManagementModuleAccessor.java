package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.core.colony.buildings.modules.GraveyardManagementModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = GraveyardManagementModule.class, remap = false)
public interface GraveyardManagementModuleAccessor {

    @Accessor(remap = false)
    List<String> getRestingCitizen();

}