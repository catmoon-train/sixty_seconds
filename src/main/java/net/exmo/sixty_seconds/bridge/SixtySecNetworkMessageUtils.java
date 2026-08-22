package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.network.ShowCustomNewspaperPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

public final class SixtySecNetworkMessageUtils {
    private SixtySecNetworkMessageUtils() {
    }

    public static void sendBroadcast(ServerPlayer player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, false);
        }
    }

    public static void sendNewspaper(ServerPlayer target, Component message,
            Optional<Component> title, Optional<Component> author) {
        if (target == null) {
            return;
        }
        ServerPlayNetworking.send(target, new ShowCustomNewspaperPacket(List.of(message), title, author));
    }

    public static void sendNewspaper(ServerPlayer target, List<Component> message,
            Optional<Component> title, Optional<Component> author) {
        if (target == null) {
            return;
        }
        ServerPlayNetworking.send(target, new ShowCustomNewspaperPacket(message, title, author));
    }
}
