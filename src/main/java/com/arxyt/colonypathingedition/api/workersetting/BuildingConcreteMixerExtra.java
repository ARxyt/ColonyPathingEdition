package com.arxyt.colonypathingedition.api.workersetting;

import net.minecraft.core.BlockPos;

public interface BuildingConcreteMixerExtra {
    void addSimulatedBlock(BlockPos pos, int num);
    int getSimulatedTimes(BlockPos pos);
    void deleteSimulateBlock(BlockPos pos);
}