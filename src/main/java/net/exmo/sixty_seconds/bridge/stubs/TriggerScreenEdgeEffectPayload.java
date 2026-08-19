package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TriggerScreenEdgeEffectPayload(int color, int duration, float intensity) implements CustomPacketPayload {
    public static final Type<TriggerScreenEdgeEffectPayload> TYPE = new Type<>(SixtySeconds.id("screen_edge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerScreenEdgeEffectPayload> CODEC =
            StreamCodec.ofMember((v, buf) -> { buf.writeInt(v.color); buf.writeVarInt(v.duration); buf.writeFloat(v.intensity); },
                    buf -> new TriggerScreenEdgeEffectPayload(buf.readInt(), buf.readVarInt(), buf.readFloat()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
