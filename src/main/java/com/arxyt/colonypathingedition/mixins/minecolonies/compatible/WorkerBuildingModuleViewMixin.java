package com.arxyt.colonypathingedition.mixins.minecolonies.compatible;

import com.arxyt.colonypathingedition.core.message.compatible.CompatibleBuildingHiringModeMessage;
import com.arxyt.colonypathingedition.core.message.compatible.CompatibleHireFireMessage;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.colony.buildings.modules.IAssignmentModuleView;
import com.minecolonies.core.colony.buildings.moduleviews.WorkerBuildingModuleView;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;
import java.util.Set;

@Mixin(value = WorkerBuildingModuleView.class, remap = false)
public abstract class WorkerBuildingModuleViewMixin extends AbstractBuildingModuleView implements IAssignmentModuleView {

    @Final @Shadow(remap = false) private Set<Integer> workerIDs;

    @Shadow(remap = false) private HiringMode hiringMode;

    /**
     * @author ARxyt
     * @reason Compatible with hire mode added by addons.
     */
    @Overwrite
    public void setHiringMode(final HiringMode hiringMode)
    {
        this.hiringMode = hiringMode;
        new CompatibleBuildingHiringModeMessage(buildingView, hiringMode, getProducer().getRuntimeID(), getProducer().key).sendToServer();
    }

    /**
     * @author ARxyt
     * @reason Compatible with hire mode added by addons.
     */
    @Overwrite
    public void addCitizen(final @NotNull ICitizenDataView citizen)
    {
        workerIDs.add(citizen.getId());
        new CompatibleHireFireMessage(buildingView, true, citizen.getId(), getProducer().getRuntimeID(), getProducer().key).sendToServer();
        citizen.setWorkBuilding(buildingView.getPosition());
        citizen.setJobView(getJobEntry().getJobViewProducer().get().apply(buildingView.getColony(), citizen));
        Objects.requireNonNull(citizen.getJobView()).setEntry(getJobEntry());
    }

    /**
     * @author ARxyt
     * @reason Compatible with hire mode added by addons.
     */
    @Overwrite
    public void removeCitizen(final @NotNull ICitizenDataView citizen)
    {
        workerIDs.remove(citizen.getId());
        new CompatibleHireFireMessage(buildingView, false, citizen.getId(), getProducer().getRuntimeID(), getProducer().key).sendToServer();
        citizen.setWorkBuilding(null);
    }
}
