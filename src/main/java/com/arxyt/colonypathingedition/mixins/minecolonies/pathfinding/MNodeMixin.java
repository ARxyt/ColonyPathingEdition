package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.api.extras.IMNodeExtra;
import com.minecolonies.core.entity.pathfinding.MNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;

/**
 * 把 IMNodeExtras 接口的方法织入到 MNode，
 * 并用 @Unique 字段保存状态。
 */
@Mixin(value = MNode.class, remap = false)
public abstract class MNodeMixin implements IMNodeExtra
{
    @Unique private boolean onFarmland;
    @Unique private boolean onSlab;
    @Unique private boolean isStation;

    // 是否是绕路节点
    @Unique private boolean isCallBack;


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
