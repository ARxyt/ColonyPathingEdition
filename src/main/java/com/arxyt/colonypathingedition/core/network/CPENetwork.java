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

    private static final List<PacketEntry<?>> PACKETS = new ArrayList<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ColonyPathingEdition.MODID);
        CPENetwork.registerMessage(SpecialSeedSyncMessage.class, SpecialSeedSyncMessage::new);

        for (PacketEntry<?> entry : PACKETS) {
            entry.register(registrar);
        }
    }

    // 注册函数（你平时调用的）
    public static <T extends ICPEMessage<T>> void registerMessage(
            Class<T> packetClass,
            Function<RegistryFriendlyByteBuf, T> decoder) {

        PACKETS.add(new PacketEntry<>(packetClass, decoder));
    }

    // NeoForge事件调用
    public static void init(RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar = event.registrar(ColonyPathingEdition.MODID)
                .versioned(PROTOCOL_VERSION);

        for (PacketEntry<?> entry : PACKETS) {
            entry.register(registrar);
        }
    }

    // 内部记录
    @SuppressWarnings("unchecked")
    private record PacketEntry<T extends ICPEMessage<T>>(
            Class<T> clazz,
            Function<RegistryFriendlyByteBuf, T> decoder) {

        void register(PayloadRegistrar registrar) {

            try {

                Field field = clazz.getField("TYPE");

                CustomPacketPayload.Type<T> type =
                        (CustomPacketPayload.Type<T>) field.get(null);

                StreamCodec<RegistryFriendlyByteBuf, T> codec =
                        ICPEMessage.streamCodec(decoder);

                registrar.playToServer(
                        type,
                        codec,
                        ICPEMessage::handle
                );

                registrar.playToClient(
                        type,
                        codec,
                        ICPEMessage::handle
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to register packet: " + clazz, e);
            }
        }
    }
}
