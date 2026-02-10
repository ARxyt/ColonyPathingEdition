package com.arxyt.colonypathingedition.api.network;

import com.arxyt.colonypathingedition.core.network.CPENetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public interface ICPEMessage<T> {
    void encode(FriendlyByteBuf buf);
    void handle(Supplier<NetworkEvent.Context> ctx);

    default void syncToAll() {
        CPENetwork.CHANNEL.send(
                PacketDistributor.ALL.noArg(),
                this
        );
    }

    default void syncToPlayer(ServerPlayer player) {
        CPENetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                this
        );
    }

    default void syncToTracking(Entity entity) {
        CPENetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY.with(() -> entity),
                this
        );
    }

    default void syncToServer() {
        CPENetwork.CHANNEL.sendToServer(this);
    }
}
