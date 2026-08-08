package com.arxyt.colonypathingedition.mixins.minecolonies.module.main;

import com.arxyt.colonypathingedition.api.JobWithAdditionalHireCheck;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.modules.IAssignsJob;
import com.minecolonies.api.colony.buildings.modules.IBuildingEventsModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.colony.buildings.modules.ITickingModule;
import com.minecolonies.api.colony.jobs.IJob;
import com.minecolonies.api.colony.jobs.ModJobs;
import com.minecolonies.core.colony.buildings.modules.AbstractAssignedCitizenModule;
import com.minecolonies.core.colony.buildings.modules.CourierAssignmentModule;
import com.minecolonies.core.util.BuildingUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

@Mixin(value = CourierAssignmentModule.class, remap = false)
public abstract class CourierAssignmentModuleMixin extends AbstractAssignedCitizenModule implements IAssignsJob, IBuildingEventsModule, ITickingModule, IPersistentModule {
    @Unique
    final private double moduleMaxMultiplier = PathingConfig.WAREHOUSE_ASSIGN_MULTIPLIER.get();

    @Override
    public void onColonyTick(@NotNull final IColony colony)
    {
        // If we have no active worker, grab one from the Colony
        if (!isFull() && BuildingUtils.canAutoHire(building, getHiringMode(), getJobEntry()))
        {
            for (final ICitizenData data : colony.getCitizenManager().getCitizens())
            {
                final IJob<?> job = data.getJob();
                // we assume all job deliveryman implements interface JobWithAdditionalHireCheck.
                if (job != null && job.getJobRegistryEntry().equals(ModJobs.delivery.get()) && !hasAssignedCitizen(data) && job instanceof JobWithAdditionalHireCheck jobHireCheck && jobHireCheck.IsHiredByAdditionalWorkPlace())
                {
                    assignCitizen(data);
                }
            }
        }

        for (final ICitizenData citizenData : new ArrayList<>(getAssignedCitizen()))
        {
            final IJob<?> job = citizenData.getJob();
            if (job == null || !job.getJobRegistryEntry().equals(ModJobs.delivery.get()))
            {
                removeCitizen(citizenData);
            }
        }
    }

    @Inject(method = "getModuleMax",at = @At("HEAD"), remap = false, cancellable = true)
    public void newGetModuleMax(CallbackInfoReturnable<Integer> cir) {
        if(PathingConfig.NEW_WAREHOUSE_ASSIGN_MAX.get()) {
            int level = this.building.getBuildingLevel();
            cir.setReturnValue((int)Math.ceil(level * (level + 1) * moduleMaxMultiplier));
        }
    }
}
