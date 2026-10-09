package com.arxyt.colonypathingedition.api.job_specializer;

import net.minecraft.world.item.ItemStack;

public interface JobWithEatingLimit {
    boolean canEat(final ItemStack stack);
}
