package com.arxyt.colonypathingedition.api.extras;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public interface FurnaceBlockEntityExtra {
    /**
     * An adder on target furnace's Progress
     * @return remain adder.
     */
    int pathFindEdition$addProgress(int adder);

    /**
     *  An adder on target furnace's Lit Time
     */
    void pathFindEdition$addLitTime(int adder);

    /**
     * @return citizen Civilian ID
     */
    int pathFindEdition$getFurnaceWorker();

    /**
     * @param workerID: citizen Civilian ID
     */
    void pathFindEdition$setFurnaceWorker(int workerID);

    /**
     * @return citizen Civilian ID
     */
    int pathFindEdition$getFurnacePicker();

    /**
     * @param workerID: Civilian ID
     */
    void pathFindEdition$setFurnacePicker(int workerID);

    /**
     * @return citizen Civilian ID
     */
    int pathFindEdition$getFurnaceFueler();

    /**
     * @param workerID: Civilian ID
     */
    void pathFindEdition$setFurnaceFueler(int workerID);

    /**
     * @return furnace protect time
     */
    boolean pathFindEdition$atProtectTime();

    /**
     * Furnace serverTick() injects for protect time
     */
    void pathFindEdition$tickProtect();

    /**
     * Furnace serverTick() injects for protect time
     */
    void pathFindEdition$setProtectTime(int protectTime);

    /**
     * Initialize furnace protect time
     * @param pBlockEntity: furnace's BlockEntity
     */
    void pathFindEdition$setPickup(AbstractFurnaceBlockEntity pBlockEntity);

    /**
     * Pop experience in furnace, no need to record.
     * @param worker: the entity trigger experience drop.
     */
    void pathFindEdition$clearUsedRecipesAndPopExperience(ServerLevel world, Entity worker);
}
