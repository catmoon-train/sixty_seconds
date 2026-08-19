package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ServerTickEvents {
    public static final Event<EndWorldTick> END_WORLD_TICK = new Event<>();
    public static final Event<EndServerTick> END_SERVER_TICK = new Event<>();
    public static final Event<StartServerTick> START_SERVER_TICK = new Event<>();

    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface EndWorldTick {
        void onEndTick(ServerLevel world);
    }

    @FunctionalInterface
    public interface EndServerTick {
        void onEndTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface StartServerTick {
        void onStartTick(MinecraftServer server);
    }
}
