package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** Syncs a CCA-style component from the server onto the matching client player. */
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

    public static void handleClient(ComponentSyncS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        ComponentKey<?> key = ComponentKey.byId(ResourceLocation.parse(payload.keyId()));
        if (key == null) {
            return;
        }
        // 世界组件（如 SixtySecGameWorldComponent / AreasWorldComponent）挂在 Level 上，
        // 同步包里携带的是收包玩家的 UUID，但客户端必须挂到 ClientLevel，不能挂到玩家。
        Object provider;
        if (key.isWorldAttached()) {
            provider = mc.level;
        } else {
            Player target = mc.level.getPlayerByUUID(payload.entityId());
            provider = target != null ? target : mc.level;
        }
        key.applyFromPacket(provider, payload.data(), mc.player);
    }
}
