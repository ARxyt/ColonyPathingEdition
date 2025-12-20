package com.arxyt.colonypathingedition.core.message;

import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModels.TAVERN_RECRUIT;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public class TavernRecruitMessage extends AbstractBuildingServerMessage<IBuilding> {

    private int visitorId;

    public TavernRecruitMessage() { super(); }

    @Override
    protected void onExecute(NetworkEvent.Context ctxIn, boolean isLogicalServer, IColony colony,
                             IBuilding building) {
        final Player player = ctxIn.getSender();
        if (player == null) {
            return;
        }
        if (!building.hasModule(TAVERN_RECRUIT)) {
            return;
        }
        building.getModule(TAVERN_RECRUIT).recruit(visitorId, player);
    }

    public TavernRecruitMessage(IBuildingView building, final int visitorId) {
        super(building);
        this.visitorId = visitorId;
    }

    @Override
    protected void toBytesOverride(FriendlyByteBuf buf) {
        buf.writeInt(visitorId);
    }

    @Override
    protected void fromBytesOverride(FriendlyByteBuf buf) {
        visitorId = buf.readInt();
    }
}
