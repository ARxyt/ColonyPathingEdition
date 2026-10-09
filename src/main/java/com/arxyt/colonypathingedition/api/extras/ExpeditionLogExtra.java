package com.arxyt.colonypathingedition.api.extras;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ExpeditionLogExtra {
    boolean removeLoot(@NotNull final ItemStack stack);
}
