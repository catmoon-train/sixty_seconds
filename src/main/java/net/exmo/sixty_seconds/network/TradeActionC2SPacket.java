package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.logic.SixtySecondsTrade;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.exmo.sixty_seconds.SixtySeconds;

/** 客户端→服务端：交易操作。action=0 确认，1 取消。 */
public record TradeActionC2SPacket(int action) implements CustomPacketPayload {
    public static final int CONFIRM = 0;
    public static final int CANCEL = 1;

    public static final Type<TradeActionC2SPacket> ID = new Type<>(SixtySeconds.id("trade_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TradeActionC2SPacket> CODEC =
            StreamCodec.ofMember(TradeActionC2SPacket::encode, TradeActionC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(action);
    }

    public static TradeActionC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new TradeActionC2SPacket(buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(TradeActionC2SPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (payload.action() == CONFIRM) {
            SixtySecondsTrade.confirm(player);
        } else {
            SixtySecondsTrade.cancel(player);
        }
    }
}
