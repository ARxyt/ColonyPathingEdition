package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class CropRotationSeasonCountMessage extends AbstractColonyServerMessage
{
    private BlockPos position;
    private int seasonCount;

    public CropRotationSeasonCountMessage() { super(); }

    public CropRotationSeasonCountMessage(IColony colony, BlockPos pos, int count)
    {
        super(colony);
        this.position = pos;
        this.seasonCount = count;
    }

    @Override
    public void onExecute(NetworkEvent.Context ctx, boolean isLogicalServer, IColony colony)
    {
        if (!isLogicalServer || ctx.getSender() == null) return;

        colony.getServerBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setSeasonCount(seasonCount));
    }

    @Override
    public void toBytesOverride(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(position);
        buf.writeInt(seasonCount);
    }

    @Override
    public void fromBytesOverride(FriendlyByteBuf buf)
    {
        position = buf.readBlockPos();
        seasonCount = buf.readInt();
    }
}
