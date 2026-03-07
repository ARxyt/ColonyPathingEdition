package com.arxyt.colonypathingedition.api.network;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public interface ICPEMessage<T extends CustomPacketPayload> extends CustomPacketPayload {

    // 编码
    void encode(RegistryFriendlyByteBuf buf);

    // 处理
    void handle(IPayloadContext context);

    // payload type
    @NotNull Type<T> type();

    /**
     * 自动生成 TYPE
     */
    static <T extends CustomPacketPayload> Type<T> createType(Class<T> clazz) {
        return new Type<>(ResourceLocation.fromNamespaceAndPath(
                ColonyPathingEdition.MODID,
                clazz.getSimpleName().toLowerCase()
        ));
    }

    /**
     * 自动生成 StreamCodec
     */
    static <T extends ICPEMessage<T>> StreamCodec<RegistryFriendlyByteBuf, T> streamCodec(
            java.util.function.Function<RegistryFriendlyByteBuf, T> decoder) {

        return StreamCodec.of(
                (buf, msg) -> msg.encode(buf),
                decoder::apply
        );
    }

    /**
     * 发送给所有客户端
     */
    default void syncToAll() {
        PacketDistributor.sendToAllPlayers(this);
    }

    /**
     * 发送给指定玩家
     */
    default void syncToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, this);
    }

    /**
     * 发送给追踪某个实体的客户端
     */
    default void syncToTracking(Entity entity) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, this);
    }

    /**
     * 发送给服务器（客户端调用）
     */
    default void syncToServer() {
        PacketDistributor.sendToServer(this);
    }
}
