package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.core.colony.module.FoodBlackListMenuModule;
import com.arxyt.colonypathingedition.core.config.PathingConfig;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.util.FoodUtils;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class SyncBlackListMenuItemMessage extends AbstractBuildingServerMessage<IBuilding> {

    /**
     * Empty constructor used when registering the
     */
    public SyncBlackListMenuItemMessage()
    {
        super();
    }

    /**
     * Creates a menu item alteration.
     *
     * @param building  the building we're executing on.
     */
    public SyncBlackListMenuItemMessage(final IBuildingView building)
    {
        super(building);
    }

    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {

    }

    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {

    }

    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony, final IBuilding building)
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
}
