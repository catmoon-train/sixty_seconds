package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.logic.SixtySecondsVisitChat;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.exmo.sixty_seconds.SixtySeconds;

/** 客户端→服务端：在拜访对话中发送一条消息。 */
public record VisitChatSendC2SPacket(String text) implements CustomPacketPayload {
    public static final Type<VisitChatSendC2SPacket> ID = new Type<>(SixtySeconds.id("visit_chat_send"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VisitChatSendC2SPacket> CODEC =
            StreamCodec.ofMember(VisitChatSendC2SPacket::encode, VisitChatSendC2SPacket::decode);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(text);
    }

    public static VisitChatSendC2SPacket decode(RegistryFriendlyByteBuf buf) {
        return new VisitChatSendC2SPacket(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    public static void handle(VisitChatSendC2SPacket payload, ServerPlayNetworking.Context context) {
        SixtySecondsVisitChat.relay(context.player(), payload.text());
    }
}
