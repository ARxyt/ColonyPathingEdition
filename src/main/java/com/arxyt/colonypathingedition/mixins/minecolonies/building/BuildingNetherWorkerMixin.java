package com.arxyt.colonypathingedition.mixins.minecolonies.building;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingNetherWorker.getMaxPerPeriod;
import static net.minecraft.world.level.Level.TICKS_PER_DAY;

@Mixin(value = BuildingNetherWorker.class, remap = false)
public abstract class BuildingNetherWorkerMixin extends AbstractBuilding {

    @Shadow(remap = false) private long snapTime;
    @Shadow(remap = false) private int currentTrips;

    //此存储的意义改变，为不占用额外存储空间仍借用此变量
    @Shadow(remap = false) private int currentPeriodDay;

    public BuildingNetherWorkerMixin(@NotNull IColony colony, BlockPos pos)
    {
        super(colony, pos);
    }

    /**
     * @author ARxyt
     * @reason 取消醒来时计时，冗余且容易被跳过
     */
    @Inject(method = "onWakeUp", at = @At("HEAD"), remap = false, cancellable = true)
    public void backToOnWakeUp(CallbackInfo ci) {
        super.onWakeUp();
        ci.cancel();
    }

    /**
     * @author ARxyt
     * @reason 计时将用于无昼夜节律时的判定
     */
    @Overwrite(remap = false)
    public boolean isReadyForTrip() {
        //前面这一段是用来检测昼夜节律的
        if (snapTime == 0) {
            snapTime = colony.getWorld().getDayTime();
        }
        if (Math.abs(colony.getWorld().getDayTime() - snapTime) >= TICKS_PER_DAY) {
            this.currentTrips = 0;
            snapTime = colony.getWorld().getDayTime();
            this.currentPeriodDay = colony.getDay();
        }
        //此处为正常逻辑
        if (this.currentPeriodDay < colony.getDay()) {
            this.currentPeriodDay = colony.getDay();
            snapTime = 0;
            this.currentTrips = 0;
        }
        else {
            this.currentPeriodDay = colony.getDay();
        }
        return this.currentTrips < getMaxPerPeriod();
    }
}
