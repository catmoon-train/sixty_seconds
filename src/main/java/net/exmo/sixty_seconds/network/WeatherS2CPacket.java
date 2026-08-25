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
 * forced=true 为指令预览（独立、优先级最高），forced=false 为自然事件；两者写入不同槽位，互不清除。
 * 客户端据此封掉原版雨雪渲染，并生成对应主题的粒子。
 */
public record WeatherS2CPacket(byte weatherId, boolean forced) implements CustomPacketPayload {
    public static final ResourceLocation ID = SixtySeconds.id("weather_sync");
    public static final Type<WeatherS2CPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherS2CPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> {
                        buf.writeByte(packet.weatherId());
                        buf.writeBoolean(packet.forced());
                    },
                    buf -> new WeatherS2CPacket(buf.readByte(), buf.readBoolean()));

    public static void handleClient(WeatherS2CPacket packet) {
        if (packet.forced()) {
            ClientWeatherState.setPreview(packet.weatherId());
        } else {
            ClientWeatherState.setEvent(packet.weatherId());
        }
    }

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
