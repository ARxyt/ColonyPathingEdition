package com.arxyt.colonypathingedition.api;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public interface FurnaceBlockEntityExtras{
    /**
     * An adder on target furnace's Progress
     * @return remain adder.
     */
    int colonyPathingEdition$addProgress(int adder);

    /**
     *  An adder on target furnace's Lit Time
     */
    void colonyPathingEdition$addLitTime(int adder);

    /**
     * @return citizen Civilian ID
     */
    int colonyPathingEdition$getFurnaceWorker();

    /**
     * @param workerID: citizen Civilian ID
     */
    void colonyPathingEdition$setFurnaceWorker(int workerID);

    /**
     * @return citizen Civilian ID
     */
    int colonyPathingEdition$getFurnacePicker();

    /**
     * @param workerID: Civilian ID
     */
    void colonyPathingEdition$setFurnacePicker(int workerID);

    /**
     * @return citizen Civilian ID
     */
    int colonyPathingEdition$getFurnaceFueler();

    /**
     * @param workerID: Civilian ID
     */
    void colonyPathingEdition$setFurnaceFueler(int workerID);

    /**
     * @return furnace protect time
     */
    boolean colonyPathingEdition$atProtectTime();

    /**
     * Furnace serverTick() injects for protect time
     */
    void colonyPathingEdition$tickProtect();

    /**
     * Furnace serverTick() injects for protect time
     */
    void colonyPathingEdition$setProtectTime(int protectTime);

    /**
     * Initialize furnace protect time
     * @param pBlockEntity: furnace's BlockEntity
     */
    void colonyPathingEdition$setPickup(AbstractFurnaceBlockEntity pBlockEntity);
}
