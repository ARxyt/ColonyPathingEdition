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

public class CropRotationLengthUpdateMessage extends AbstractColonyServerMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "rotation_length_farmland", CropRotationLengthUpdateMessage::new);
    private final BlockPos position;
    private final int seasonIndex;
    private final int length;


    public CropRotationLengthUpdateMessage(IColony colony, BlockPos pos, int seasonIndex, int length)
    {
        super(TYPE, colony);
        this.position = pos;
        this.seasonIndex = seasonIndex;
        this.length = length;
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony)
    {
        colony.getBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setSeasonDuration(seasonIndex, length));
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeBlockPos(position);
        buf.writeInt(seasonIndex);
        buf.writeInt(length);
    }

    protected CropRotationLengthUpdateMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        position = buf.readBlockPos();
        seasonIndex = buf.readInt();
        length = buf.readInt();
    }
}