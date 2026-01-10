package com.arxyt.colonypathingedition.core.colony.module;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.core.window.FoodBlackListMenuModuleWindow;
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

public class FoodBlackListMenuModuleView extends AbstractBuildingModuleView {
    /**
     * The black list.
     */
    private final List<ItemStorage> black_list = new ArrayList<>();

    @Override
    public void deserialize(final @NotNull FriendlyByteBuf buf)
    {
        black_list.clear();
        final int size = buf.readInt();
        for (int i = 0; i < size; i++)
        {
            black_list.add(new ItemStorage(buf.readItem()));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public BOWindow getWindow()
    {
        return new FoodBlackListMenuModuleWindow(this);
    }

    @Override
    public ResourceLocation getIconResourceLocation()
    {
        return new ResourceLocation(ColonyPathingEdition.MODID, "textures/gui/modules/black_list.png");
    }

    @Override
    public Component getDesc()
    {
        return Component.translatable("com.arxyt.colonypathingedition.core.black_list");
    }

    /**
     * Get the menu for the restaurant.
     * @return the menu.
     */
    public List<ItemStorage> getMenu()
    {
        return black_list;
    }

}
