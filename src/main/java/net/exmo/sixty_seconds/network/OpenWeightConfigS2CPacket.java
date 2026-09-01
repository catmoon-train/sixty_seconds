package net.exmo.sixty_seconds.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端 → 客户端：打开负重配置面板（一次性，仅用于打开 GUI，不用于实时数据同步）。
 *
 * <p>客户端接收逻辑位于 {@code SixtySecondsClient}，不在本 payload 内。
 */
public record OpenWeightConfigS2CPacket(SixtySecondsWeightConfig config) implements CustomPacketPayload {

    private static final Gson GSON = new GsonBuilder().create();

    public static final Type<OpenWeightConfigS2CPacket> ID =
            new Type<>(SixtySeconds.id("open_weight_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenWeightConfigS2CPacket> CODEC =
            StreamCodec.ofMember(OpenWeightConfigS2CPacket::encode, OpenWeightConfigS2CPacket::decode);

    public static OpenWeightConfigS2CPacket of(SixtySecondsWeightConfig cfg) {
        return new OpenWeightConfigS2CPacket(cfg);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(GSON.toJson(config));
    }

    public static OpenWeightConfigS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new OpenWeightConfigS2CPacket(GSON.fromJson(buf.readUtf(), SixtySecondsWeightConfig.class));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
