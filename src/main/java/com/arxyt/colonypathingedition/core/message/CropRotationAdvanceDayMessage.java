package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.arxyt.colonypathingedition.core.costants.AdditionalContants;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CropRotationAdvanceDayMessage extends AbstractColonyServerMessage {
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "advance_day_farmland", CropRotationAdvanceDayMessage::new);

    private final BlockPos position;
    private final int currentDate;
    private final int currentDay;
    private final int currentSeason;

    public CropRotationAdvanceDayMessage(IColony colony, BlockPos pos, int date, int day, int season)
    {
        super(TYPE,colony);
        this.position = pos;
        this.currentDate = date;
        this.currentDay = day;
        this.currentSeason = season;
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, IColony colony)
    {
        colony.getBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.updateAdvanceDay(currentDate,currentDay,currentSeason));
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeBlockPos(position);
        buf.writeInt(currentDate);
        buf.writeInt(currentDay);
        buf.writeInt(currentSeason);
    }

    protected CropRotationAdvanceDayMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        position = buf.readBlockPos();
        currentDate = buf.readInt();
        currentDay = buf.readInt();
        currentSeason = buf.readInt();
    }
}
