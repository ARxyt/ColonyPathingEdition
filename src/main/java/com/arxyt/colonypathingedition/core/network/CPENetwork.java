package com.arxyt.colonypathingedition.core.network;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.arxyt.colonypathingedition.api.network.ICPEMessage;
import com.arxyt.colonypathingedition.core.network.message.SpecialSeedSyncMessage;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CPENetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ColonyPathingEdition.MODID).versioned(PROTOCOL_VERSION);;
        CPENetwork.registerClientMessage(SpecialSeedSyncMessage.TYPE, SpecialSeedSyncMessage.CODEC, registrar);
    }

    public static <T extends ICPEMessage<T>> void registerClientMessage(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            PayloadRegistrar registrar) {

        registrar.playToClient(
                type,
                codec,
                ICPEMessage::handle
        );
    }

    public static <T extends ICPEMessage<T>> void registerServerMessage(
            CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec,
            PayloadRegistrar registrar) {

        registrar.playToServer(
                type,
                codec,
                ICPEMessage::handle
        );
    }
}
