package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingStoneSmeltery;
import com.minecolonies.core.colony.jobs.JobStoneSmeltery;
import org.jetbrains.annotations.NotNull;

public class NewEntityAIWorkStoneSmeltery extends NewAbstractEntityRequestSmelter<JobStoneSmeltery, BuildingStoneSmeltery> {
    /**
     * Initialize the stone smeltery and add all his tasks.
     *
     * @param jobStoneSmeltery the job he has.
     */
    public NewEntityAIWorkStoneSmeltery(@NotNull final JobStoneSmeltery jobStoneSmeltery)
    {
        super(jobStoneSmeltery);
    }

    @Override
    public Class<BuildingStoneSmeltery> getExpectedBuildingClass()
    {
        return BuildingStoneSmeltery.class;
    }
}
