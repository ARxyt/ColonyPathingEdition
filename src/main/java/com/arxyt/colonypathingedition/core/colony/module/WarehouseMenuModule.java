package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Log;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class WarehouseMenuModule extends AbstractBuildingModule implements IPersistentModule{
    /**
     * The minimum stock tag.
     */
    private static final String TAG_MENU = "menu";

    /**
     * The minimum stock.
     */
    protected final Set<ItemStorage> menu = new HashSet<>();

    /**
     * Initialize the general black list.
     */
    public WarehouseMenuModule(){

    }

    /**
     * Get the restaurant menu.
     * @return the menu.
     */
    public Set<ItemStorage> getMenu()
    {
        return menu;
    }

    /**
     * Add a new menu item.
     * @param itemStack the menu item to add.
     */
    public void addMenuItem(final ItemStack itemStack)
    {
        if (!NewFoodUtils.EDIBLE.test(itemStack))
        {
            Log.getLogger().warn("Tried to add nonedible food stack: " + itemStack);
            return;
        }

        menu.add(new ItemStorage(itemStack));
        markDirty();
    }

    /**
     * Remove a menu item.
     * @param itemStack the menu item to remove.
     */
    public void removeMenuItem(final ItemStack itemStack)
    {
        menu.remove(new ItemStorage(itemStack));
        markDirty();
    }

    @Override
    public void deserializeNBT(final CompoundTag compound)
    {
        if(compound.contains(TAG_MENU)) {
            menu.clear();
        }
        final ListTag minimumStockTagList = compound.getList(TAG_MENU, Tag.TAG_COMPOUND);
        for (int i = 0; i < minimumStockTagList.size(); i++)
        {
            final ItemStack itemStack = ItemStack.of(minimumStockTagList.getCompound(i));
            if (NewFoodUtils.EDIBLE.test(itemStack))
            {
                menu.add(new ItemStorage(itemStack));
            }
        }
    }

    @Override
    public void serializeNBT(final CompoundTag compound)
    {
        @NotNull final ListTag blacklistStockTagList = new ListTag();
        for (final ItemStorage menuItem : menu)
        {
            blacklistStockTagList.add(menuItem.getItemStack().save(new CompoundTag()));
        }
        compound.put(TAG_MENU, blacklistStockTagList);
    }

    @Override
    public void serializeToView(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(menu.size());
        for (final ItemStorage menuItem : menu)
        {
            buf.writeItem(menuItem.getItemStack());
        }
    }
}
