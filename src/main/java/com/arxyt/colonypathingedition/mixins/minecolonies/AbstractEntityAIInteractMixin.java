package com.arxyt.colonypathingedition.mixins.minecolonies;

import com.arxyt.colonypathingedition.api.AbstractEntityAIInteractExtra;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(AbstractEntityAIInteract.class)
public abstract class AbstractEntityAIInteractMixin implements AbstractEntityAIInteractExtra {
    @Shadow(remap = false) private int stillTicks = 0;
    @Shadow(remap = false) private int previousIndex = 0;
    @Shadow(remap = false) private List<BlockPos> items;

    public boolean isStillTicksExceeded(int limit){
        return  ++stillTicks > limit;
    }

    public void resetStillTick(){
        stillTicks = 0;
    }

    public boolean tryMoveForward(int currentIndex){
        if (currentIndex != previousIndex)
        {
            resetStillTick();
            previousIndex = currentIndex;
            return true;
        }
        return false;
    }

    public boolean checkPuckUpItems(){
        return items != null && items.isEmpty();
    }

    public void  resetPickUpItems(){
        items = null;
    }
}
