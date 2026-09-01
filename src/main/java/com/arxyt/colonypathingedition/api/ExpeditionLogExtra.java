package com.arxyt.colonypathingedition.api;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ExpeditionLogExtra {
    boolean removeLoot(@NotNull final ItemStack stack);
}
