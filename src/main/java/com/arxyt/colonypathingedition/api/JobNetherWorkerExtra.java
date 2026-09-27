package com.arxyt.colonypathingedition.api;

import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;

import java.util.Set;

public interface JobNetherWorkerExtra {
    void setShouldEat(boolean shouldEat);
    boolean setExtraRounds(boolean extraRounds);
    boolean getShouldEat();
    boolean getExtraRounds();
    boolean canExtraRounds(int limit);
    int remainExtraRounds(int limit);
    Set<EquipmentTypeEntry> getHasOrdered();
}
