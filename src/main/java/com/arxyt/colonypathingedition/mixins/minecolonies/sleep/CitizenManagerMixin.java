package com.arxyt.colonypathingedition.mixins.minecolonies.sleep;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.core.colony.Colony;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.colony.managers.CitizenManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.JOBS_MAY_NOT_SLEEP;
import static com.minecolonies.api.util.constant.TranslationConstants.ALL_CITIZENS_ARE_SLEEPING;

@Mixin(value = CitizenManager.class, remap = false)
public class CitizenManagerMixin {

    @Final @Shadow(remap = false) private Map<Integer, ICitizenData> citizens = new HashMap<>();
    @Final @Shadow(remap = false) private Colony colony;

    @Shadow(remap = false) private boolean areCitizensSleeping;

    /**
     * @author ARxyt
     * @reason 做一点优化处理
     */
    @Overwrite(remap = false)
    public void onCitizenSleep()
    {
        for (final ICitizenData citizenData : citizens.values())
        {
            if (!(citizenData.isAsleep() || citizenData.getJob() instanceof AbstractJobGuard || (citizenData.getJob() != null && JOBS_MAY_NOT_SLEEP.contains(citizenData.getJob().getClass()))))
            {
                return;
            }
        }

        if (!this.areCitizensSleeping)
        {
            MessageUtils.format(ALL_CITIZENS_ARE_SLEEPING).sendTo(colony).forAllPlayers();
        }

        this.areCitizensSleeping = true;
    }
}
