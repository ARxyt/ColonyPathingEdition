package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class CropRotationAdvanceDayMessage extends AbstractColonyServerMessage {
    private BlockPos position;
    private int currentDate;
    private int currentDay;
    private int currentSeason;

    public CropRotationAdvanceDayMessage() { super(); }

    public CropRotationAdvanceDayMessage(IColony colony, BlockPos pos, int date, int day, int season)
    {
        super(colony);
        this.position = pos;
        this.currentDate = date;
        this.currentDay = day;
        this.currentSeason = season;
    }

    @Override
    public void onExecute(NetworkEvent.Context ctx, boolean isLogicalServer, IColony colony)
    {
        if (!isLogicalServer || ctx.getSender() == null) return;

        colony.getServerBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.updateAdvanceDay(currentDate,currentDay,currentSeason));
    }

    @Override
    public void toBytesOverride(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(position);
        buf.writeInt(currentDate);
        buf.writeInt(currentDay);
        buf.writeInt(currentSeason);
    }

    @Override
    public void fromBytesOverride(FriendlyByteBuf buf)
    {
        position = buf.readBlockPos();
        currentDate = buf.readInt();
        currentDay = buf.readInt();
        currentSeason = buf.readInt();
    }
}
