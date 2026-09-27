package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.core.util.NewFoodUtils;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModule;
import com.minecolonies.api.colony.buildings.modules.IPersistentModule;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.Utils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class WarehouseMenuModule extends AbstractBuildingModule implements IPersistentModule
{

    /**
     * The minimum stock tag.
     */
    private static final String TAG_MENU = "menu";

    /**
     * The minimum stock.
     */
    protected final Set<ItemStorage> menu = new HashSet<>();

    /**
     * Get the restaurant menu.
     * @return the menu.
     */
    public Set<ItemStorage> getMenu()
    {
        return menu;
    }

    /**
     * Create a warehouse menu module.
     */
    public WarehouseMenuModule()
    {

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

        if (menu.size() >= 512)
        {
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
    public void deserializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        menu.clear();
        final ListTag minimumStockTagList = compound.getList(TAG_MENU, Tag.TAG_COMPOUND);
        for (int i = 0; i < minimumStockTagList.size(); i++)
        {
            final ItemStack itemStack = ItemStack.parseOptional(provider, minimumStockTagList.getCompound(i));
            if (NewFoodUtils.EDIBLE.test(itemStack))
            {
                menu.add(new ItemStorage(itemStack));
            }
        }
    }

    @Override
    public void serializeNBT(@NotNull final HolderLookup.Provider provider, final CompoundTag compound)
    {
        @NotNull final ListTag minimumStockTagList = new ListTag();
        for (final ItemStorage menuItem : menu)
        {
            minimumStockTagList.add(menuItem.getItemStack().saveOptional(provider));
        }
        compound.put(TAG_MENU, minimumStockTagList);
    }

    @Override
    public void serializeToView(@NotNull final RegistryFriendlyByteBuf buf)
    {
        buf.writeInt(menu.size());
        for (final ItemStorage menuItem : menu)
        {
            Utils.serializeCodecMess(buf, menuItem.getItemStack());
        }
    }
}
