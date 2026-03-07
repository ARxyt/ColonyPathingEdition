package com.arxyt.colonypathingedition.core.message;

import com.arxyt.colonypathingedition.api.FarmFieldExtra;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildingextensions.registry.BuildingExtensionRegistries;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public class CropRotationSeedUpdateMessage extends AbstractColonyServerMessage
{
    private BlockPos position;
    private int seasonIndex;
    private ItemStack seed;

    public CropRotationSeedUpdateMessage() { super(); }

    public CropRotationSeedUpdateMessage(IColony colony, BlockPos pos, int seasonIndex, ItemStack seed)
    {
        super(colony);
        this.position = pos;
        this.seasonIndex = seasonIndex;
        this.seed = seed;
    }

    @Override
    public void onExecute(NetworkEvent.Context ctx, boolean isLogicalServer, IColony colony)
    {
        if (!isLogicalServer || ctx.getSender() == null) return;

        colony.getServerBuildingManager()
                .getMatchingBuildingExtension(f -> f.getBuildingExtensionType().equals(BuildingExtensionRegistries.farmField.get()) && f.getPosition().equals(position))
                .map(m -> (FarmFieldExtra) m)
                .ifPresent(field -> field.setSeasonSeed(seasonIndex, seed));
    }

    @Override
    public void toBytesOverride(FriendlyByteBuf buf)
    {
        buf.writeBlockPos(position);
        buf.writeInt(seasonIndex);
        buf.writeItem(seed);
    }

    @Override
    public void fromBytesOverride(FriendlyByteBuf buf)
    {
        position = buf.readBlockPos();
        seasonIndex = buf.readInt();
        seed = buf.readItem();
    }
}
