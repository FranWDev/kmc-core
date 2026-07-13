package dev.franwdev.kmccore.network;

import dev.franwdev.kmccore.client.ClientSetupHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncConfigPacket {

    private final Map<String, String> configs;

    public SyncConfigPacket(Map<String, String> configs) {
        this.configs = configs;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public static void encode(SyncConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.configs.size());
        for (Map.Entry<String, String> entry : msg.configs.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> configs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            String value = buf.readUtf();
            configs.put(key, value);
        }
        return new SyncConfigPacket(configs);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(new PacketHandlerRunner(this));
        context.setPacketHandled(true);
    }

    private static class PacketHandlerRunner implements Runnable {
        private final SyncConfigPacket packet;

        public PacketHandlerRunner(SyncConfigPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            ClientSetupHandler.handleConfigSync(packet);
        }
    }
}
