package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class CropRotationLengthUpdateMessage extends AbstractColonyServerMessage
{
    private BlockPos position;
    private int seasonIndex;
    private int length;

    public CropRotationLengthUpdateMessage() { super(); }

    public CropRotationLengthUpdateMessage(IColony colony, BlockPos pos, int seasonIndex, int length)
    {
        super(colony);
        this.position = pos;
        this.seasonIndex = seasonIndex;
        this.length = length;
    }

    @Override
    public void onExecute(NetworkEvent.Context ctx, boolean isLogicalServer, IColony colony)
    {
        if (!isLogicalServer || ctx.getSender() == null) return;

        colony.getServerBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setSeasonDuration(seasonIndex, length));
    }

    @Override
    public void toBytesOverride(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(position);
        buf.writeInt(seasonIndex);
        buf.writeInt(length);
    }

    @Override
    public void fromBytesOverride(FriendlyByteBuf buf)
    {
        position = buf.readBlockPos();
        seasonIndex = buf.readInt();
        length = buf.readInt();
    }
}