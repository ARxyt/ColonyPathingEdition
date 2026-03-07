package com.arxyt.colonypathingedition.core.event;

import com.arxyt.colonypathingedition.ColonyPathingEdition;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ColonyPathingEdition.MODID)
public class CitizenTrackingHandler {

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if(event.getEntity().level().isClientSide) {
            return;
        }

        if(!(event.getTarget() instanceof AbstractEntityCitizen)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) event.getEntity();

        player.connection.send(new ClientboundTeleportEntityPacket(event.getTarget()));
    }
}
