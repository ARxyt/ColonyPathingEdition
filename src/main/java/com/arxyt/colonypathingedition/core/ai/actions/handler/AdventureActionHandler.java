package com.arxyt.colonypathingedition.core.ai.actions.handler;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.arxyt.colonypathingedition.core.costants.AdditionalContants.ADVENTURE_DELAY;

public class AdventureActionHandler
{
    private ActionType currentActionType;
    private Action currentAction;
    private ItemStack currStack;

    public AdventureActionHandler() {
        currentActionType = ActionType.NONE;
        currentAction = null;
    }

    public ActionType getCurrentActionType()
    {
        return currentActionType;
    }

    public boolean canActionTick()
    {
        return currentActionType != null && currentActionType != ActionType.NONE;
    }

    public void setAction(Action action, ItemStack currStack)
    {
        this.currentAction = action;
        this.currentActionType = action.getActionType();
        this.currStack = currStack;
    }

    public ResultType doAction()
    {
        if(currentAction == null) return ResultType.INVALID;
        else return currentAction.tick();
    }

    public int actionDelay()
    {
        if(currentAction == null) return ADVENTURE_DELAY;
        else return currentAction.currentDelay();
    }

    public List<ItemStack> onActionFinished()
    {
        List<ItemStack> rewards = currentAction.getRewards();

        currentAction = null;
        currentActionType = ActionType.NONE;
        return rewards;
    }

    public ItemStack getCurrStack() {
        return currStack;
    }

    public enum ActionType
    {
        NONE,

        /**
         * Fight an entity.
         */
        COMBAT,

        /**
         * Mine blocks.
         */
        MINING,

        /**
         * Directly pick up.
         */
        PICKUP,

        /**
         * Trade with entity.
         */
        TRADE,

        /**
         * Treasure found.
         */
        TREASURE,

        /**
         * Boss fight.
         */
        BOSS
    }

    public enum ResultType
    {
        INVALID,
        IN_PROGRESS,
        FAIL,
        ESCAPE,
        SUCCESS,
        SPECIAL
    }

    /**
     * Base Action.
     */
    public static abstract class Action
    {
        protected ActionType currentActionType;

        protected List<ItemStack> rewards;

        protected int actionDelay = ADVENTURE_DELAY;

        protected Action(ActionType actionType)
        {
            this.currentActionType = actionType;
            this.rewards = new ArrayList<>();
        }

        protected int currentDelay()
        {
            return actionDelay;
        }

        protected ActionType getActionType() {
            return currentActionType;
        }

        protected abstract ResultType tick();

        public List<ItemStack> getRewards() {
            return rewards;
        }

        protected ResultType specialTick() {
            return ResultType.SUCCESS;
        }
    }
}
