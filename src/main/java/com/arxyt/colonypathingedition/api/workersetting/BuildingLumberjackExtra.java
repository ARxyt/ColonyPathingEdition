package com.arxyt.colonypathingedition.api.workersetting;

import net.minecraft.core.BlockPos;

public interface BuildingLumberjackExtra {
    BlockPos getLastTree();
    void thisTreeToLast();
    BlockPos getThisTree();
    void setThisTree(BlockPos treePos);
}
