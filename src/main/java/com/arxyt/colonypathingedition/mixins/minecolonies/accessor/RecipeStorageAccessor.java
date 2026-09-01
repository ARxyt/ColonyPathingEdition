package com.arxyt.colonypathingedition.mixins.minecolonies.accessor;

import com.minecolonies.api.crafting.RecipeStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(RecipeStorage.class)
public interface RecipeStorageAccessor {
    @Invoker(value = "insertCraftedItems", remap = false) List<ItemStack> invokeInsertCraftedItems (final List<IItemHandler> handlers, ItemStack outputStack, LootParams context, boolean doInsert);
}
