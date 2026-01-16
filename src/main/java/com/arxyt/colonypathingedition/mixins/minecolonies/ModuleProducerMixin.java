package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BuildingEntry.ModuleProducer.class, remap = false)
public abstract class ModuleProducerMixin {
    @Final @Shadow(remap = false)
    private int id;
    @Final @Shadow(remap = false)
    public String key;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void init(CallbackInfo cir) {
        System.out.println("<init>: " + key + ":" + id);
    }
}