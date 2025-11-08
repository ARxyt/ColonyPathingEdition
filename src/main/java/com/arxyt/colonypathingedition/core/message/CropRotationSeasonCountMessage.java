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

public class CropRotationSeasonCountMessage extends AbstractColonyServerMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "rotation_season_farmland", CropRotationSeasonCountMessage::new);
    private final BlockPos position;
    private final int seasonCount;


    public CropRotationSeasonCountMessage(IColony colony, BlockPos pos, int count)
    {
        super(TYPE, colony);
        this.position = pos;
        this.seasonCount = count;
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, final IColony colony)
    {
        colony.getBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setSeasonCount(seasonCount));
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeBlockPos(position);
        buf.writeInt(seasonCount);
    }

    protected CropRotationSeasonCountMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        position = buf.readBlockPos();
        seasonCount = buf.readInt();
    }
}
