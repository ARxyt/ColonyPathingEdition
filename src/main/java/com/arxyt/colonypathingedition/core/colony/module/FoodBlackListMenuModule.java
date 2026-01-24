package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IAltersRequiredItems;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.colony.buildings.modules.ITickingModule;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class FoodBlackListMenuModule extends AbstractBuildingModule implements IPersistentModule, ITickingModule, IAltersRequiredItems {
    /**
     * The black list stock tag.
     */
    private static final String TAG_BLACK_LIST = "black_list_menu";

    /**
     * The black list.
     */
    protected final Set<ItemStorage> black_list = new HashSet<>();

    /**
     * Initialize the general black list.
     */
    public FoodBlackListMenuModule(){
        for (ItemStorage blackListStorage : PathingConfig.food_black_list) {
            if(FoodUtils.EDIBLE.test(blackListStorage.getItemStack())) {
                black_list.add(blackListStorage);
            }
        }
    }

    /**
     * Get the restaurant menu.
     * @return the menu.
     */
    public Set<ItemStorage> getBlackList()
    {
        return black_list;
    }

    /**
     * Add a new black list item.
     * @param itemStack the black list item to add.
     */
    public void addBlackListItem(final ItemStack itemStack)
    {
        if (!FoodUtils.EDIBLE.test(itemStack))
        {
            Log.getLogger().warn("Tried to add nonedible food stack: " + itemStack);
            return;
        }

        black_list.add(new ItemStorage(itemStack));
        markDirty();
    }

    /**
     * Remove a menu item.
     * @param itemStack the menu item to remove.
     */
    public void removeBlackListItem(final ItemStack itemStack)
    {
        black_list.remove(new ItemStorage(itemStack));
    }

    @Override
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        if(compound.contains(TAG_BLACK_LIST)) {
            black_list.clear();
        }
        final ListTag minimumStockTagList = compound.getList(TAG_BLACK_LIST, Tag.TAG_COMPOUND);
        for (int i = 0; i < minimumStockTagList.size(); i++)
        {
            final ItemStack itemStack = ItemStack.parseOptional(provider, minimumStockTagList.getCompound(i));
            if (FoodUtils.EDIBLE.test(itemStack))
            {
                black_list.add(new ItemStorage(itemStack));
            }
        }
    }

    @Override
    public void serializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        @NotNull final ListTag blacklistStockTagList = new ListTag();
        for (final ItemStorage menuItem : black_list)
        {
            blacklistStockTagList.add(menuItem.getItemStack().saveOptional(provider));
        }
        compound.put(TAG_BLACK_LIST, blacklistStockTagList);
    }

    @Override
    public void serializeToView(@NotNull final RegistryFriendlyByteBuf buf)
    {
        buf.writeInt(black_list.size());
        for (final ItemStorage menuItem : black_list)
        {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, menuItem.getItemStack());
        }
    }

    @Override
    public void alterItemsToBeKept(TriConsumer<Predicate<ItemStack>, Integer, Boolean> consumer) {
        // Item in black list no need to be kept.
    }
}
