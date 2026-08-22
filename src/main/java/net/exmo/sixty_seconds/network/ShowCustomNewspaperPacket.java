package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Optional;

/** 服务端→客户端：向目标玩家展示一张自定义报纸（内容由命令/系统生成）。 */
public record ShowCustomNewspaperPacket(List<Component> pages, Optional<Component> title, Optional<Component> author)
        implements CustomPacketPayload {
    public static final int MAX_BYTES_PER_CHAR = 4;
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowCustomNewspaperPacket> STREAM_CODEC;
    public static final Type<ShowCustomNewspaperPacket> ID = new Type<>(
            SixtySeconds.id("newspaper/show"));

    static {
        STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list(200)), ShowCustomNewspaperPacket::pages,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs::optional), ShowCustomNewspaperPacket::title,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs::optional), ShowCustomNewspaperPacket::author,
                ShowCustomNewspaperPacket::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
