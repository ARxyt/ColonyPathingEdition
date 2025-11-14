package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.interactionhandling.IInteractionResponseHandler;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.entity.ai.JobStatus;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.MathUtils;
import com.minecolonies.core.colony.CitizenData;
import com.minecolonies.core.entity.citizen.citizenhandlers.CitizenDiseaseHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static com.minecolonies.api.util.constant.CitizenConstants.DISABLED;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;

@Mixin(value = CitizenData.class, remap = false)
public abstract class CitizenDataMixinForLeisure implements ICitizenData {

    @Shadow(remap = false) private @Nullable IBuilding homeBuilding;
    @Shadow(remap = false) private int leisureTime;
    @Shadow(remap = false) private int interactedRecently;
    @Shadow(remap = false) private boolean isWorking;
    @Shadow(remap = false) private IJob<?> job;
    @Shadow(remap = false) private int inactivityTimer;
    @Shadow(remap = false) private JobStatus jobStatus;

    @Final @Shadow(remap = false) private Set<UUID> interactedRecentlyPlayers;
    @Final @Shadow(remap = false) protected Map<Component, IInteractionResponseHandler> citizenChatOptions;
    @Final @Shadow(remap = false) private CitizenDiseaseHandler citizenDiseaseHandler;

    @Unique final private static int MAX_PRE_LEISURE_TIME = PathingConfig.MAX_PRE_LEISURE_TIME.get();
    @Unique final private static int LEISURE_TIME = PathingConfig.LEISURE_TIME.get();
    @Unique final private static int LEISURE_RATIO = PathingConfig.LEISURE_RATIO.get();

    @Unique int coolDownTime = 0;

    // Rewrite as a preventing of setting change.
    @Inject(method = "update", at = @At("HEAD"), remap = false, cancellable = true)
    private void rewriteUpdateData(int tickRate, CallbackInfo ci){
        if (getEntity().isEmpty() || !getEntity().get().isAlive())
        {
            ci.cancel();
            return;
        }

        final int homeBuildingLevel = homeBuilding == null ? 1 : homeBuilding.getBuildingLevel();
        if (leisureTime > 0) {
            leisureTime -= tickRate;
            if (leisureTime <= 0){
                coolDownTime = TICKS_SECOND * LEISURE_RATIO / 2;
            }
        }
        else {
            if (leisureTime > -TICKS_SECOND * MAX_PRE_LEISURE_TIME * homeBuildingLevel && jobStatus == JobStatus.IDLE) {
                leisureTime -= tickRate;
            }
            if ((coolDownTime -= tickRate) < 0 && MathUtils.RANDOM.nextInt(TICKS_SECOND * LEISURE_RATIO ) <= tickRate) {
                leisureTime += TICKS_SECOND * LEISURE_TIME;
            }
        }

        if (interactedRecently > 0)
        {
            interactedRecently -= tickRate;
            if (interactedRecently <= 0)
            {
                interactedRecentlyPlayers.clear();
            }
        }

        if (!isWorking && job != null && inactivityTimer != DISABLED && ++inactivityTimer >= job.getInactivityLimit())
        {
            job.triggerActivityChangeAction(this.isWorking);
            inactivityTimer = DISABLED;
        }

        final List<IInteractionResponseHandler> toRemove = new ArrayList<>();
        for (final IInteractionResponseHandler handler : citizenChatOptions.values())
        {
            try
            {
                if (!handler.isValid(this))
                {
                    toRemove.add(handler);
                }
            }
            catch (final Exception e)
            {
                Log.getLogger().warn("Error during validation of handler: {}", handler.getInquiry(), e);
                // If anything goes wrong in checking validity, remove handler.
                toRemove.add(handler);
            }
        }

        if (!toRemove.isEmpty())
        {
            markDirty(20 * 10);
        }

        for (final IInteractionResponseHandler handler : toRemove)
        {
            citizenChatOptions.remove(handler.getId());
            for (final Component comp : handler.getPossibleResponses())
            {
                if (citizenChatOptions.containsKey(handler.getResponseResult(comp)))
                {
                    citizenChatOptions.get(handler.getResponseResult(comp)).removeParent(handler.getId());
                }
            }
        }

        citizenDiseaseHandler.update(tickRate);
        ci.cancel();
    }
}
