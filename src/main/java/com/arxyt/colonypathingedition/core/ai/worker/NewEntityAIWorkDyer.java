package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDyer;
import com.minecolonies.core.colony.jobs.JobDyer;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAIRequestSmelter;
import org.jetbrains.annotations.NotNull;

/**
 * Crafts dye related things.
 */
public class NewEntityAIWorkDyer extends AbstractEntityAIRequestSmelter<JobDyer, BuildingDyer>
{
    /**
     * Initialize the dyer.
     *
     * @param dyer the job he has.
     */
    public NewEntityAIWorkDyer(@NotNull final JobDyer dyer)
    {
        super(dyer);
    }

    @Override
    public Class<BuildingDyer> getExpectedBuildingClass()
    {
        return BuildingDyer.class;
    }
}
