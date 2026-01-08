package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.core.colony.buildings.workerbuildings.BuildingKitchen;
import com.minecolonies.core.colony.jobs.JobChef;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.StatisticsConstants.FOOD_COOKED_DETAIL;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_BAKED_DETAIL;

public class NewEntityAIWorkChef extends NewAbstractEntityRequestSmelter<JobChef, BuildingKitchen>
{
    /**
     * Initialize the Chef.
     *
     * @param jobChef the job he has.
     */
    public NewEntityAIWorkChef(@NotNull final JobChef jobChef)
    {
        super(jobChef);
    }

    @Override
    public Class<BuildingKitchen> getExpectedBuildingClass()
    {
        return BuildingKitchen.class;
    }

    /**
     * Returns the name of the smelting stat that is used in the building's statistics.
     * @return the name of the smelting stat.
     */
    protected String getSmeltingStatName()
    {
        return ITEMS_BAKED_DETAIL;
    }

    /**
     * Returns the name of the crafting stat that is used in the building's statistics.
     * Override this in your subclass to change the description of the smelting stat.
     * @return The name of the crafting statistic.
     */
    protected String getCraftingStatName()
    {
        return FOOD_COOKED_DETAIL;
    }
}
