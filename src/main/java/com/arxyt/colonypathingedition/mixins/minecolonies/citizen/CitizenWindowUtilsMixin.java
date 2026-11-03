package com.arxyt.colonypathingedition.mixins.minecolonies.citizen;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.ldtteam.blockui.views.View;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.core.client.gui.citizen.CitizenWindowUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(CitizenWindowUtils.class)
public abstract class CitizenWindowUtilsMixin {

    /**
     * @author ARxyt
     * @reason Some adjustments.
     */
    @Overwrite(remap = false)
    public static void createSaturationBar(final ICitizenDataView citizen, final View view)
    {
        // TODO: 临时用着，别忘了改
        double factor = 60 / PathingConfig.MAX_SATURATION.get();
        CitizenWindowUtils.createSaturationBar(citizen.getSaturation() * factor, view);
    }
}
