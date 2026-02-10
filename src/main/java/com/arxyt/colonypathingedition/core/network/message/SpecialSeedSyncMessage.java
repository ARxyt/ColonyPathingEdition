package com.arxyt.colonypathingedition.core.network.message;

import com.arxyt.colonypathingedition.api.network.ICPEMessage;
import com.arxyt.colonypathingedition.core.data.farmlandmap.SpecialSeedManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SpecialSeedSyncMessage implements ICPEMessage<SpecialSeedSyncMessage> {
    private final Map<ResourceLocation, ResourceLocation> mappings;

    public SpecialSeedSyncMessage(Map<ResourceLocation, ResourceLocation> mappings) {
        this.mappings = mappings;
    }

    public SpecialSeedSyncMessage(FriendlyByteBuf buf) {

        int size = buf.readInt();

        this.mappings = new HashMap<>();

        for (int i = 0; i < size; i++) {
            mappings.put(buf.readResourceLocation(), buf.readResourceLocation());
        }
    }

    @Override
    public void encode(FriendlyByteBuf buf) {

        buf.writeInt(mappings.size());

        mappings.forEach((seed, soil) -> {
            buf.writeResourceLocation(seed);
            buf.writeResourceLocation(soil);
        });
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {

        ctx.get().enqueueWork(() -> SpecialSeedManager.rebuildFromMappings(mappings));

        ctx.get().setPacketHandled(true);
    }
}
