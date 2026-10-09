package com.arxyt.colonypathingedition.api.pathfinder;

import com.arxyt.colonypathingedition.core.ai.pathfinding.structure.SpecialInfoNode;

import java.nio.file.Path;

public interface IPathfinder {
    Path search();
    int computeCost(SpecialInfoNode n);
    int computeHeuristic(SpecialInfoNode n);
}
