package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SyncBlackListMenuItemMessage extends AbstractBuildingServerMessage<IBuilding> {
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "sync_black_list_menu", SyncBlackListMenuItemMessage::new);

    /**
     * Creates a menu item alteration.
     *
     * @param building  the building we're executing on.
     */
    public SyncBlackListMenuItemMessage(final IBuildingView building)
    {
        super(TYPE, building);
    }

    @Override
    public void toBytes(@NotNull final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
    }

    @Override
    public void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony, final IBuilding building)
    {
        FoodBlackListMenuModule foodBlackListMenuModule = building.getModule(FoodBlackListMenuModule.class);
        if (foodBlackListMenuModule != null)
        {
            final Set<ItemStorage> blackListToAlter = foodBlackListMenuModule.getBlackList();
            for (ItemStorage blackListStorage : PathingConfig.food_black_list) {
                if(FoodUtils.EDIBLE.test(blackListStorage.getItemStack()) && !blackListToAlter.contains(blackListStorage)) {
                    foodBlackListMenuModule.addBlackListItem(blackListStorage.getItemStack());
                }
            }
        }
    }

    protected SyncBlackListMenuItemMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
    }
}
