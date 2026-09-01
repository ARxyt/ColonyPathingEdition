package com.arxyt.colonypathingedition.core.ai.actions.netherworker;

import com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ActionType.PICKUP;
import static com.arxyt.colonypathingedition.core.ai.actions.handler.AdventureActionHandler.ResultType.SUCCESS;

public class NetherWorkerPickupAction extends AdventureActionHandler.Action{
    private final ItemStack currStack;

    public NetherWorkerPickupAction(ItemStack currStack){
        super(PICKUP);
        this.currStack = currStack;
    }

    @Override
    public AdventureActionHandler.ResultType tick() {
        rewards = List.of(currStack.copy());
        return SUCCESS;
    }
}
