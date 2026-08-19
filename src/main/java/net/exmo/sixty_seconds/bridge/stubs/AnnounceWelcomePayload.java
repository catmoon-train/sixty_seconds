package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AnnounceWelcomePayload(String role, int killers, int targets) implements CustomPacketPayload {
    public AnnounceWelcomePayload(String role, int killers) {
        this(role, killers, 0);
    }

    public static final Type<AnnounceWelcomePayload> TYPE = new Type<>(SixtySeconds.id("announce_welcome"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnnounceWelcomePayload> CODEC =
            StreamCodec.ofMember((v, buf) -> {
                buf.writeUtf(v.role);
                buf.writeVarInt(v.killers);
                buf.writeVarInt(v.targets);
            }, buf -> new AnnounceWelcomePayload(buf.readUtf(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
