package com.arxyt.colonypathingedition.core.colony.view;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.colony.window.FoodBlackListMenuModuleWindow;
import com.arxyt.colonypathingedition.core.colony.window.WarehouseMenuModuleWindow;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.modules.AbstractBuildingModuleView;
import com.minecolonies.api.crafting.ItemStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WarehouseMenuModuleView extends AbstractBuildingModuleView {
    /**
     * The menu.
     */
    private final List<ItemStorage> menu = new ArrayList<>();

    @Override
    public void deserialize(final @NotNull FriendlyByteBuf buf)
    {
        menu.clear();
        final int size = buf.readInt();
        for (int i = 0; i < size; i++)
        {
            menu.add(new ItemStorage(buf.readItem()));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BOWindow getWindow()
    {
        return new WarehouseMenuModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(ColonyPathingEdition.MODID, "textures/gui/modules/warehouse_menu.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.arxyt.colonypathingedition.core.warehouse_menu");
    }

    /**
     * Get the menu for the restaurant.
     * @return the menu.
     */
    public List<ItemStorage> getMenu()
    {
        return menu;
    }

    public boolean hasReachedLimit()
    {
        return menu.size() >= 512;
    }
}
