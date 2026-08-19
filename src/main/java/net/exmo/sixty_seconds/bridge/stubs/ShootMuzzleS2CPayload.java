package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ShootMuzzleS2CPayload(int entityId) implements CustomPacketPayload {
    public static final Type<ShootMuzzleS2CPayload> TYPE = new Type<>(SixtySeconds.id("shoot_muzzle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShootMuzzleS2CPayload> CODEC =
            StreamCodec.ofMember((v, buf) -> buf.writeVarInt(v.entityId), buf -> new ShootMuzzleS2CPayload(buf.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
