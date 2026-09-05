package dev.franwdev.kmccore.network;

import dev.franwdev.kmccore.KmcCore;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPayload(Map<String, String> configs) implements CustomPacketPayload {

    public static final Type<SyncConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(KmcCore.MODID, "sync_config"));

    public static final StreamCodec<ByteBuf, SyncConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
            SyncConfigPayload::configs,
            SyncConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }
}
