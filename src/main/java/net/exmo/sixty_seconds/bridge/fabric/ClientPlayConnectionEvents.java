package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class ClientPlayConnectionEvents {
    public static final Event<Disconnect> DISCONNECT = new Event<>();

    private ClientPlayConnectionEvents() {
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(ClientPacketListener handler, Minecraft client);
    }
}
