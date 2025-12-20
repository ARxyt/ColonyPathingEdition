package com.arxyt.colonypathingedition.mixins.minecolonies.tavern;

import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModels.TAVERN_RECRUIT;

import com.minecolonies.api.colony.ICitizen;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.core.colony.interactionhandling.RecruitmentInteraction;
import com.minecolonies.core.colony.interactionhandling.ServerCitizenInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = RecruitmentInteraction.class, remap = false)
public abstract class RecruitmentInteractionMixin extends ServerCitizenInteraction {
    public RecruitmentInteractionMixin(ICitizen data) {
        super(data);
    }

    @Inject(method = "onServerResponseTriggered", at = @At(value = "INVOKE", target = "Lcom/minecolonies/api/util/StatsUtil;trackStat(Lcom/minecolonies/api/colony/buildings/IBuilding;Ljava/lang/String;I)V", shift=At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    protected void afterTrackStat(int responseId, Player player, ICitizenData data, CallbackInfo ci,
                                  Component response, IColony colony, IBuilding tavern) {
        if (tavern.hasModule(TAVERN_RECRUIT)) {
            tavern.getModule(TAVERN_RECRUIT).markDirty();
        }
    }
}
