package com.arxyt.colonypathingedition.core.message;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractBuildingServerMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.arxyt.colonypathingedition.core.minecolonies.module.BuildingModels.TAVERN_RECRUIT;

public class TavernRecruitMessage extends AbstractBuildingServerMessage<IBuilding> {
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "tavern_recruit", TavernRecruitMessage::new);
    private int visitorId;

    public TavernRecruitMessage(IBuildingView building, final int visitorId) {
        super(TYPE,building);
        this.visitorId = visitorId;
    }

    @Override
    protected void onExecute(IPayloadContext ctxIn, ServerPlayer player, IColony colony, IBuilding building) {
        if (player == null) {
            return;
        }
        if (!building.hasModule(TAVERN_RECRUIT)) {
            return;
        }
        building.getModule(TAVERN_RECRUIT).recruit(visitorId, player);
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeInt(visitorId);
    }

    protected TavernRecruitMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        visitorId = buf.readInt();
    }


}
