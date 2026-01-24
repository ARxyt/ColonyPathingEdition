package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class AlterBlackListMenuItemMessage extends AbstractBuildingServerMessage<IBuilding> {
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "alter_black_list_menu", AlterBlackListMenuItemMessage::new);
    /**
     * The menu item.
     */
    private final ItemStack itemStack;

    /**
     * If add = true, or remove = false.
     */
    private final boolean add;

    /**
     * Add a menu item to the building.
     * @param building the building to add it to.
     * @param itemStack the stack to add.
     * @return the message,
     */
    public static AlterBlackListMenuItemMessage addMenuItem(final IBuildingView building, final ItemStack itemStack)
    {
        return new AlterBlackListMenuItemMessage(building, itemStack, true);
    }

    /**
     * Remove a menu item to the building.
     * @param building the building to remove it from.
     * @param itemStack the stack to remove.
     * @return the message,
     */
    public static AlterBlackListMenuItemMessage removeMenuItem(final IBuildingView building, final ItemStack itemStack)
    {
        return new AlterBlackListMenuItemMessage(building, itemStack, false);
    }

    /**
     * Creates a menu item alteration.
     *
     * @param itemStack to be altered.
     * @param building  the building we're executing on.
     * @param add if add = true if remove = false
     */
    private AlterBlackListMenuItemMessage(final IBuildingView building, final ItemStack itemStack, final boolean add)
    {
        super(TYPE, building);
        this.itemStack = itemStack;
        this.add = add;
    }

    @Override
    public void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, itemStack);
        buf.writeBoolean(add);
    }

    @Override
    public void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        FoodBlackListMenuModule foodBlackListMenuModule = building.getModule(FoodBlackListMenuModule.class);
        if (foodBlackListMenuModule != null)
        {
            if (add)
            {
                foodBlackListMenuModule.addBlackListItem(itemStack);
            }
            else
            {
                foodBlackListMenuModule.removeBlackListItem(itemStack);
            }
        }
    }

    protected AlterBlackListMenuItemMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        add = buf.readBoolean();
    }
}
