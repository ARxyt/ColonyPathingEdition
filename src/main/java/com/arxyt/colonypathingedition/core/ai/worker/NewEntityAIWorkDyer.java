package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDyer;
import com.minecolonies.core.colony.jobs.JobDyer;
import org.jetbrains.annotations.NotNull;

/**
 * Crafts dye related things.
 */
public class NewEntityAIWorkDyer extends NewAbstractEntityRequestSmelter<JobDyer, BuildingDyer>
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
