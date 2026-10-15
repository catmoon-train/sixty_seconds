package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * 服务端→客户端：同步一个 CCA 风格组件到对应客户端玩家。
 * <p>
 * 只保留数据与编解码；客户端接收逻辑在 {@code net.exmo.sixty_seconds.client.ComponentSyncClient}
 * （由 {@code SixtySecondsClient} 注册）——payload 类在专用服务端注册时也会被加载，
 * 不能内嵌引用 {@code Minecraft} 等客户端类的接收逻辑。
 */
public record ComponentSyncS2CPacket(String keyId, UUID entityId, byte[] data) implements CustomPacketPayload {
    public static final Type<ComponentSyncS2CPacket> ID = new Type<>(SixtySeconds.id("component_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComponentSyncS2CPacket> CODEC =
            StreamCodec.ofMember(ComponentSyncS2CPacket::encode, ComponentSyncS2CPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(keyId);
        buf.writeUUID(entityId);
        buf.writeByteArray(data);
    }

    public static ComponentSyncS2CPacket decode(RegistryFriendlyByteBuf buf) {
        return new ComponentSyncS2CPacket(buf.readUtf(), buf.readUUID(), buf.readByteArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
