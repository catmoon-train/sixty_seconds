package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.weather.ClientWeatherState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端→客户端：同步当前进行中的「主题化天气」类型（枚举 ordinal，-1 表示无）。
 * 客户端据此外部封掉原版雨雪渲染，并生成对应主题的粒子。
 */
public record WeatherS2CPacket(byte weatherId) implements CustomPacketPayload {
    public static final ResourceLocation ID = SixtySeconds.id("weather_sync");
    public static final Type<WeatherS2CPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherS2CPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> buf.writeByte(packet.weatherId()),
                    buf -> new WeatherS2CPacket(buf.readByte()));

    public static void handleClient(WeatherS2CPacket packet) {
        ClientWeatherState.set(packet.weatherId());
    }

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
