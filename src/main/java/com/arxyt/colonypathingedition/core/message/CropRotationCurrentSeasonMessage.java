package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class CropRotationCurrentSeasonMessage extends AbstractColonyServerMessage {
    private BlockPos position;
    private int currentSeason;

    public CropRotationCurrentSeasonMessage() { super(); }

    public CropRotationCurrentSeasonMessage(IColony colony, BlockPos pos, int season)
    {
        super(colony);
        this.position = pos;
        this.currentSeason = season;
    }

    @Override
    public void onExecute(NetworkEvent.Context ctx, boolean isLogicalServer, IColony colony)
    {
        if (!isLogicalServer || ctx.getSender() == null) return;

        colony.getServerBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setCurrentSeason(currentSeason));
    }

    @Override
    public void toBytesOverride(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(position);
        buf.writeInt(currentSeason);
    }

    @Override
    public void fromBytesOverride(FriendlyByteBuf buf)
    {
        position = buf.readBlockPos();
        currentSeason = buf.readInt();
    }
}
