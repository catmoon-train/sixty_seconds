package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AdvancedCameraPayload(int ticks, double dist, double height) implements CustomPacketPayload {
    public static final Type<AdvancedCameraPayload> TYPE = new Type<>(SixtySeconds.id("adv_camera"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancedCameraPayload> CODEC =
            StreamCodec.ofMember((v, buf) -> { buf.writeVarInt(v.ticks); buf.writeDouble(v.dist); buf.writeDouble(v.height); },
                    buf -> new AdvancedCameraPayload(buf.readVarInt(), buf.readDouble(), buf.readDouble()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
