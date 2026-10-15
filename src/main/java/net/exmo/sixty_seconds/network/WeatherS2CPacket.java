package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 服务端→客户端：同步当前进行中的「主题化天气」类型（枚举 ordinal，-1 表示无）。
 * forced=true 为指令预览（独立、优先级最高），forced=false 为自然事件；两者写入不同槽位，互不清除。
 * 客户端据此封掉原版雨雪渲染，并生成对应主题的粒子。
 * <p>
 * 只保留数据与编解码；客户端接收逻辑（写 {@code ClientWeatherState} 槽位）在
 * {@code SixtySecondsClient} 注册，不写在 payload 里。
 */
public record WeatherS2CPacket(byte weatherId, boolean forced) implements CustomPacketPayload {
    public static final Type<WeatherS2CPacket> TYPE = new Type<>(SixtySeconds.id("weather_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WeatherS2CPacket> CODEC =
            StreamCodec.ofMember(
                    (packet, buf) -> {
                        buf.writeByte(packet.weatherId());
                        buf.writeBoolean(packet.forced());
                    },
                    buf -> new WeatherS2CPacket(buf.readByte(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
