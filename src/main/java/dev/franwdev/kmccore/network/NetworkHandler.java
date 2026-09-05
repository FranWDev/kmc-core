package dev.franwdev.kmccore.network;

import dev.franwdev.kmccore.KmcCore;
import dev.franwdev.kmccore.client.ClientSetupHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(KmcCore.MODID);
        registrar.playToClient(
                SyncConfigPayload.TYPE,
                SyncConfigPayload.STREAM_CODEC,
                NetworkHandler::handleSyncConfig
        );
    }

    private static void handleSyncConfig(SyncConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ClientSetupHandler.handleConfigSync(payload);
            }
        });
    }

    public static void sendToClient(ServerPlayer player, SyncConfigPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}

