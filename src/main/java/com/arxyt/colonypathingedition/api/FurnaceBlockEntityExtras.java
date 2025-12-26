package com.arxyt.colonypathingedition.api;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public interface FurnaceBlockEntityExtras{
    /**
     * An adder on target furnace's Progress
     * @return remain adder.
     */
    int addProgress(int adder);

    /**
     *  An adder on target furnace's Lit Time
     */
    void addLitTime(int adder);

    /**
     * @return citizen Civilian ID
     */
    int getFurnaceWorker();

    /**
     * @param workerID: citizen Civilian ID
     */
    void setFurnaceWorker(int workerID);

    /**
     * @return citizen Civilian ID
     */
    int getFurnacePicker();

    /**
     * @param workerID: Civilian ID
     */
    void setFurnacePicker(int workerID);

    /**
     * @return furnace protect time
     */
    boolean atProtectTime();

    /**
     * Furnace serverTick() injects for protect time
     */
    void tickProtect();

    /**
     * Furnace serverTick() injects for protect time
     */
    void setProtectTime(int protectTime);

    /**
     * Initialize furnace protect time
     * @param pBlockEntity: furnace's BlockEntity
     */
    void setPickup(AbstractFurnaceBlockEntity pBlockEntity);
}
