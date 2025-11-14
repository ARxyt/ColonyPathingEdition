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
    // 提升单次运输容量
    /**
     * @author ARxyt
     * @reason 略微加大一下可用slot数
     */
    @Overwrite(remap = false)
    private boolean cannotHoldMoreItems() {
        if(getBuilding().getBuildingLevel() >= getBuilding().getMaxBuildingLevel()){
            return false;
        }
        return InventoryUtils.getAmountOfStacksInItemHandler(getWorker().getInventoryCitizen()) > 2 + 5 * getBuilding().getBuildingLevel();
    }

    // 增强并发任务处理能力
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
        // 仅在 prepareDelivery() 中调用时生效
        final int originalSkillLevel = invokeGetSecondarySkillLevel();
        final int buildingLevel = getBuilding().getBuildingLevel();
        return  (2 * buildingLevel + (int)Math.sqrt(originalSkillLevel * 9));
    }


}

