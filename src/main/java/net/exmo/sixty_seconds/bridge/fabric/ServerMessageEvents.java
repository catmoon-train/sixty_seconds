package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ServerMessageEvents {
    public static final Event<AllowChatMessage> ALLOW_CHAT_MESSAGE = new Event<>();

    private ServerMessageEvents() {
    }

    @FunctionalInterface
    public interface AllowChatMessage {
        boolean allowChatMessage(Component message, ServerPlayer sender, String rawText);
    }
}
