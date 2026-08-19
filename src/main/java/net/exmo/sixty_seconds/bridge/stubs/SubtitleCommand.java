package net.exmo.sixty_seconds.bridge.stubs;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public final class SubtitleCommand {
    private SubtitleCommand() {}
    public static void sendToPlayerTop(ServerPlayer player, Component title, Component subtitle, int stay, boolean unused) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, stay, 10));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
    public static void sendToPlayerTop(ServerPlayer player, Component title, Component subtitle, int stay) {
        sendToPlayerTop(player, title, subtitle, stay, false);
    }
    public static void sendToPlayerBottom(ServerPlayer player, Component message, int stay) {
        player.displayClientMessage(message, true);
    }

    public static void sendToPlayerBottom(ServerPlayer player, Component message, Component unused, int stay) {
        sendToPlayerBottom(player, message, stay);
    }
}
