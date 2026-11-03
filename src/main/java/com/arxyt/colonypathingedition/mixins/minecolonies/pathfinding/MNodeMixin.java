package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.api.extras.IMNodeExtras;
import com.minecolonies.core.entity.pathfinding.MNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;

@Mixin(value = MNode.class, remap = false)
public abstract class MNodeMixin implements IMNodeExtras
{
    @Unique private boolean onFarmland = false;
    @Unique private boolean onSlab = false;
    @Unique private boolean isStation = false;

    // 是否是绕路节点
    @Unique private boolean isCallBack = false;

    public boolean getOnFarmland()
    {
        return onFarmland;
    }

    public void setOnFarmland()
    {
        this.onFarmland = true;
    }

    public boolean getOnSlab()
    {
        return onSlab;
    }

    public void setOnSlab()
    {
        this.onSlab = true;
    }

    public boolean isStation()
    {
        return isStation;
    }

    public void setStation()
    {
        this.isStation = true;
    }

    public boolean isCallbackNode() {
        return isCallBack;
    }

    public void setCallbackNode() {
        this.isCallBack = true;
    }
}
