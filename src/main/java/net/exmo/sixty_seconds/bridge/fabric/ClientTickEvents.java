package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.client.Minecraft;

public final class ClientTickEvents {
    public static final Event<EndTick> END_CLIENT_TICK = new Event<>();

    private ClientTickEvents() {
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(Minecraft client);
    }
}
