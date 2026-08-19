package net.exmo.sixty_seconds.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;

/**
 * 客户端→服务端：玩家在指定世界坐标打标点，服务端转发给同队队友。
 * @param x 世界坐标 X
 * @param y 世界坐标 Y
 * @param z 世界坐标 Z
 */
public record TeamPingC2SPacket(int x, int y, int z) implements CustomPacketPayload {
    public static final Type<TeamPingC2SPacket> ID = new Type<>(SixtySeconds.id("team_ping_c2s"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeamPingC2SPacket> CODEC =
            StreamCodec.ofMember(TeamPingC2SPacket::encode, TeamPingC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(z);
    }

    public static TeamPingC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new TeamPingC2SPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TeamPingC2SPacket payload, ServerPlayNetworking.Context context) {
        var player = context.player();
        int teamId = SixtySecondsStatsComponent.KEY.get(player).teamId;
        TeamPingS2CPacket outbound = new TeamPingS2CPacket(player.getUUID(), payload.x(), payload.y(), payload.z());
        for (var other : player.serverLevel().players()) {
            if (other == player) {
                TeamPingS2CPacket.sendTo(other, outbound);
                continue;
            }
            if (SixtySecondsStatsComponent.KEY.get(other).teamId == teamId && teamId >= 0) {
                TeamPingS2CPacket.sendTo(other, outbound);
            }
        }
    }
}
