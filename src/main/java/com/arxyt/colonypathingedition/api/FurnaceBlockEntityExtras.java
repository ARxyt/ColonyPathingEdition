package com.arxyt.colonypathingedition.api;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public interface FurnaceBlockEntityExtras{
    /**
     * 直接给当前熔炉的 Progress 加 adder
     * @return 如果 adder 大于实际上熔炉还未完成的部分，会返回剩余的 adder
     */
    int addProgress(int adder);

    /**
     * 直接给当前熔炉的 Lit Time 加 adder
     */
    void addLitTime(int adder);

    /**
     * @return 返回工人的 Civilian ID
     */
    int getFurnaceWorker();

    /**
     * 输入需要使用工人的 Civilian ID
     */
    void setFurnaceWorker(int workerID);

    /**
     * @return 返回工人的 Civilian ID
     */
    int getFurnacePicker();

    /**
     * 输入需要使用工人的 Civilian ID
     */
    void setFurnacePicker(int workerID);

    /**
     * @return 如函数名
     */
    boolean atProtectTime();

    // 这是 protectTime 相关的 servertick 事件
    void tickProtect();

    // 设置拿取人为当前占用熔炉的工人
    void setPickup(AbstractFurnaceBlockEntity pBlockEntity);
}
