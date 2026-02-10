package com.arxyt.colonypathingedition.core.network;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.api.network.ICPEMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Function;

public class CPENetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(ColonyPathingEdition.MODID, "network"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static int id = 0;

    private static int nextId() {
        return id++;
    }

    // ⭐ 核心封装
    public static <T extends ICPEMessage<T>> void register(
            Class<T> packetClass,
            Function<FriendlyByteBuf, T> decoder) {

        CHANNEL.registerMessage(
                nextId(),
                packetClass,
                ICPEMessage::encode,
                decoder,
                ICPEMessage::handle
        );
    }
}
