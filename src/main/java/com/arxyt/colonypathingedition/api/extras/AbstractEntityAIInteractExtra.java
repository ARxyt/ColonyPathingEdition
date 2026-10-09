package com.arxyt.colonypathingedition.api.extras;

public interface AbstractEntityAIInteractExtra {
    boolean isStillTicksExceeded(int limit);
    void resetStillTick();
    boolean tryMoveForward(int currentIndex);
    boolean checkPuckUpItems();
    void resetPickUpItems();
}
