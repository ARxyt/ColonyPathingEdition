package com.arxyt.colonypathingedition.core.costants.states;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;

public enum NewAIWorkerState implements IAIState {
    PLANTATION_PRECHECK_FIELD(false);

    /**
     * Is it okay to eat.
     */
    private boolean isOkayToEat;

    /**
     * Create a new one.
     *
     * @param okayToEat if okay.
     */
    NewAIWorkerState(final boolean okayToEat)
    {
        this.isOkayToEat = okayToEat;
    }

    /**
     * Method to check if it is okay.
     *
     * @return true if so.
     */
    public boolean isOkayToEat()
    {
        return isOkayToEat;
    }
}
