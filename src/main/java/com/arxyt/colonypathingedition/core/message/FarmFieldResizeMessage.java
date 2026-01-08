package com.arxyt.colonypathingedition.core.message;

import com.ldtteam.common.network.PlayMessageType;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.colony.buildingextensions.FarmField;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import com.minecolonies.core.tileentities.TileEntityScarecrow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.minecolonies.core.colony.buildingextensions.FarmField.MAX_RANGE;

public class FarmFieldResizeMessage extends AbstractColonyServerMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "farmfield_resize_farmland", FarmFieldResizeMessage::new);

    /**
     * The new radius of the field plot.
     */
    private int size;

    /**
     * The specified direction for the new radius.
     */
    private Direction direction;

    /**
     * The field position.
     */
    private BlockPos position;

    /**
     * @param size      the new radius of the field plot
     * @param direction the specified direction for the new radius
     * @param position  the field position.
     */
    public FarmFieldResizeMessage(IColony colony, int size, Direction direction, BlockPos position)
    {
        super(TYPE, colony);
        this.size = size;
        this.direction = direction;
        this.position = position;
    }

    @Override
    protected void onExecute(final IPayloadContext ctxIn, final ServerPlayer player, IColony colony)
    {
        final BlockEntity fieldBlock = player.level().getBlockEntity(position);
        if (fieldBlock instanceof TileEntityScarecrow scarecrow)
        {
            if (size <= 0 || size > MAX_RANGE)
            {
                return;
            }
            scarecrow.setFieldSize(direction, size);
            if (colony != null)
            {
                colony.getBuildingManager()
                        .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                        .map(m -> (FarmField) m)
                        .ifPresent(field -> field.setRadius(direction, size));
            }
        }
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        super.toBytes(buf);
        buf.writeInt(size);
        buf.writeInt(direction.get2DDataValue());
        buf.writeBlockPos(position);
    }

    protected FarmFieldResizeMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        size = buf.readInt();
        direction = Direction.from2DDataValue(buf.readInt());
        position = buf.readBlockPos();
    }
}

