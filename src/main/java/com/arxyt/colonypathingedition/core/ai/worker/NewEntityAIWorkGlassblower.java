package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingGlassblower;
import com.minecolonies.core.colony.jobs.JobGlassblower;
import com.minecolonies.core.entity.ai.workers.crafting.AbstractEntityAIRequestSmelter;
import org.jetbrains.annotations.NotNull;

/**
 * Crafts glass relates things, crafts and smelts.
 */
public class NewEntityAIWorkGlassblower extends AbstractEntityAIRequestSmelter<JobGlassblower, BuildingGlassblower>
{
    /**
     * Initialize the glassblower AI.
     *
     * @param glassBlower the job he has.
     */
    public NewEntityAIWorkGlassblower(@NotNull final JobGlassblower glassBlower)
    {
        super(glassBlower);
    }

    @Override
    public Class<BuildingGlassblower> getExpectedBuildingClass()
    {
        return BuildingGlassblower.class;
    }
}
