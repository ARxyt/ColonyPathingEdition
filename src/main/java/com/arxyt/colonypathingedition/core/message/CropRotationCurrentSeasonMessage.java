package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CropRotationCurrentSeasonMessage extends AbstractColonyServerMessage {
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "current_season_farmland", CropRotationCurrentSeasonMessage::new);
    private final BlockPos position;
    private final int currentSeason;

    public CropRotationCurrentSeasonMessage(IColony colony, BlockPos pos, int season)
    {
        super(TYPE, colony);
        this.position = pos;
        this.currentSeason = season;
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony)
    {
        colony.getBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setCurrentSeason(currentSeason));
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeBlockPos(position);
        buf.writeInt(currentSeason);
    }

    protected CropRotationCurrentSeasonMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        position = buf.readBlockPos();
        currentSeason = buf.readInt();
    }
}
