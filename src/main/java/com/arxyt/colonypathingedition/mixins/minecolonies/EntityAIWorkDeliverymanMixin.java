package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractAISkeletonAccessor;
import com.arxyt.colonypathingedition.mixins.minecolonies.accessor.AbstractEntityAIBasicAccessor;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.entity.ai.workers.service.EntityAIWorkDeliveryman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value = EntityAIWorkDeliveryman.class, remap = false)
public abstract class EntityAIWorkDeliverymanMixin implements AbstractAISkeletonAccessor<JobDeliveryman>, AbstractEntityAIBasicAccessor<BuildingDeliveryman> {

    /**
     * @author ARxyt
     * @reason Additional slot to use.
     */
    @Overwrite(remap = false)
    private boolean cannotHoldMoreItems() {
        if(getBuilding().getBuildingLevel() >= getBuilding().getMaxBuildingLevel()){
            return false;
        }
        return InventoryUtils.getAmountOfStacksInItemHandler(getWorker().getInventoryCitizen()) > getWorker().getInventoryCitizen().getSlots() * 0.2 * getBuilding().getBuildingLevel();
    }

    /**
     * @author ARxyt
     * @reason Additional quest to manage.
     */
    @Redirect(
            method = "prepareDelivery",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/minecolonies/core/entity/ai/workers/service/EntityAIWorkDeliveryman;getSecondarySkillLevel()I",
                    remap = false
            ),
            remap = false
    )
    private int redirectGetSecondarySkillLevel(EntityAIWorkDeliveryman instance) {
        final int originalSkillLevel = invokeGetSecondarySkillLevel();
        final int buildingLevel = getBuilding().getBuildingLevel();
        return  (2 * buildingLevel + (int)Math.sqrt(originalSkillLevel * 9));
    }


}

