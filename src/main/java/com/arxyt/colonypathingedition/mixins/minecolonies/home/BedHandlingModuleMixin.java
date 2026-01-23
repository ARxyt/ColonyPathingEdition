package com.arxyt.colonypathingedition.mixins.minecolonies.home;

import com.arxyt.colonypathingedition.api.BedHandlingModuleExtra;
import com.minecolonies.core.colony.buildings.modules.BedHandlingModule;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Set;

@Mixin(value = BedHandlingModule.class, remap = false)
public class BedHandlingModuleMixin implements BedHandlingModuleExtra {

    @Final @Shadow(remap = false) private @NotNull Set<BlockPos> bedList;

    @Unique public int getBedSize(){
        return bedList.size();
    }
}
