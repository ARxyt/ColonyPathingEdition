package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.colony.module.WarehouseMenuModule;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class AlterWarehouseMenuItemMessage extends AbstractBuildingServerMessage<IBuilding> {
    /**
     * The menu item.
     */
    private ItemStack itemStack;

    /**
     * If add = true, or remove = false.
     */
    private boolean add;

    /**
     * Empty constructor used when registering the
     */
    public AlterWarehouseMenuItemMessage()
    {
        super();
    }

    /**
     * Add a menu item to the building.
     * @param building the building to add it to.
     * @param itemStack the stack to add.
     * @return the message,
     */
    public static AlterWarehouseMenuItemMessage addMenuItem(final IBuildingView building, final ItemStack itemStack)
    {
        return new AlterWarehouseMenuItemMessage(building, itemStack, true);
    }

    /**
     * Remove a menu item to the building.
     * @param building the building to remove it from.
     * @param itemStack the stack to remove.
     * @return the message,
     */
    public static AlterWarehouseMenuItemMessage removeMenuItem(final IBuildingView building, final ItemStack itemStack)
    {
        return new AlterWarehouseMenuItemMessage(building, itemStack, false);
    }

    /**
     * Creates a menu item alteration.
     *
     * @param itemStack to be altered.
     * @param building  the building we're executing on.
     * @param add if add = true if remove = false
     */
    private AlterWarehouseMenuItemMessage(final IBuildingView building, final ItemStack itemStack, final boolean add)
    {
        super(building);
        this.itemStack = itemStack;
        this.add = add;
    }

    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        itemStack = buf.readItem();
        add = buf.readBoolean();
    }

    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeItem(itemStack);
        buf.writeBoolean(add);
    }

    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final IBuilding building)
    {
        WarehouseMenuModule warehouseMenuModule = building.getModule(WarehouseMenuModule.class);
        if (warehouseMenuModule != null)
        {
            if (add)
            {
                warehouseMenuModule.addMenuItem(itemStack);
            }
            else
            {
                warehouseMenuModule.removeMenuItem(itemStack);
            }
        }
    }
}
