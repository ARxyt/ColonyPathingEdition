package com.arxyt.colonypathingedition.core.ai.worker;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingBaker;
import com.minecolonies.core.colony.jobs.JobBaker;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_BAKED_DETAIL;

public class NewEntityAIWorkBaker extends NewAbstractEntityRequestSmelter<JobBaker, BuildingBaker> {
    /**
     * Baking icon
     */
    private final static VisibleCitizenStatus BAKING =
            new VisibleCitizenStatus(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/icons/work/baker.png"), "com.minecolonies.gui.visiblestatus.baker");

    /**
     * Constructor for the Baker. Defines the tasks the bakery executes.
     *
     * @param job a bakery job to use.
     */
    public NewEntityAIWorkBaker(@NotNull final JobBaker job)
    {
        super(job);
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingBaker> getExpectedBuildingClass()
    {
        return BuildingBaker.class;
    }

    /**
     * Returns the bakery's worker instance. Called from outside this class.
     *
     * @return citizen object.
     */
    @Nullable
    public AbstractEntityCitizen getCitizen()
    {
        return worker;
    }

    @Override
    protected IAIState craft()
    {
        worker.getCitizenData().setVisibleStatus(BAKING);
        return super.craft();
    }

    @Override
    public boolean isAfterDumpPickupAllowed()
    {
        return true;
    }

    /**
     * Returns the name of the smelting stat that is used in the building's statistics.
     * @return the name of the smelting stat.
     */
    protected String getSmeltingStatName()
    {
        return ITEMS_BAKED_DETAIL;
    }
}
