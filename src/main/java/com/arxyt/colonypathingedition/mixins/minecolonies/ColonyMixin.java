package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.minecolonies.api.colony.colonyEvents.IColonyEvent;
import com.minecolonies.api.colony.managers.interfaces.IEventManager;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.events.raid.HordeRaidEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.minecolonies.api.colony.colonyEvents.EventStatus.*;

@Mixin(value = Colony.class, remap = false)
public class ColonyMixin {
    @Final @Shadow(remap = false) private IEventManager eventManager;

    @Inject(method = "checkDayTime", at = @At("HEAD"), remap = false)
    public void onCheckDayTimeAdditional(CallbackInfoReturnable<Boolean> cir) {
        eventManager.getEvents().forEach(this::updateHordeRaidEvent);
    }

    @Unique
    private void updateHordeRaidEvent(Integer id, IColonyEvent event) {
        if(event instanceof HordeRaidEvent hordeRaidEvent) {
            if (hordeRaidEvent.getStatus() != DONE && hordeRaidEvent.getStatus() != CANCELED && hordeRaidEvent.getStatus() != STARTING)
            {
                hordeRaidEvent.onUpdate();
            }
        }
    }
}
