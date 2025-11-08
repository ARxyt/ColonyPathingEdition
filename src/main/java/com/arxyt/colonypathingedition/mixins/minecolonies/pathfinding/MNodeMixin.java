package com.arxyt.colonypathingedition.mixins.minecolonies.pathfinding;

import com.arxyt.colonypathingedition.api.IMNodeExtras;
import com.minecolonies.core.entity.pathfinding.MNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;

/**
 * 把 IMNodeExtras 接口的方法织入到 MNode，
 * 并用 @Unique 字段保存状态。
 */
@Mixin(MNode.class)
@Implements(@Interface(iface = IMNodeExtras.class, prefix = "extra$"))
public abstract class MNodeMixin
{
    @Unique private boolean onFarmland;
    @Unique private boolean onSlab;
    @Unique private boolean isStation;

    // 是否是绕路节点
    @Unique private boolean isCallBack;

    // prefix = "extra$"，下面方法会被当作 IMNodeExtras 的实现织入 MNode

    public boolean extra$getOnFarmland()
    {
        return onFarmland;
    }

    public void extra$setOnFarmland()
    {
        this.onFarmland = true;
    }

    public boolean extra$getOnSlab()
    {
        return onSlab;
    }

    public void extra$setOnSlab()
    {
        this.onSlab = true;
    }

    public boolean extra$isStation()
    {
        return isStation;
    }

    public void extra$setStation()
    {
        this.isStation = true;
    }

    public boolean extra$isCallbackNode(){return  isCallBack;}

    public void extra$setCallbackNode(){this.isCallBack=true;}
}
