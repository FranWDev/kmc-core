package dev.franwdev.kmccore.network;

import dev.franwdev.kmccore.KmcCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(KmcCore.MODID, "main"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static int index = 0;

    public static void register() {
        CHANNEL.registerMessage(
                index++,
                SyncConfigPacket.class,
                SyncConfigPacket::encode,
                SyncConfigPacket::decode,
                SyncConfigPacket::handle
        );
    }

    public static void sendToClient(ServerPlayer player, SyncConfigPacket packet) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
