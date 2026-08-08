package com.arxyt.colonypathingedition.api;

import net.minecraft.world.item.ItemStack;

public interface JobWithEatingLimit {
    boolean canEat(final ItemStack stack);
}
