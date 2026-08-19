package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public final class ServerPlayConnectionEvents {
    public static final Event<Join> JOIN = new Event<>();
    public static final Event<Disconnect> DISCONNECT = new Event<>();

    private ServerPlayConnectionEvents() {
    }

    public static final class Handler {
        private final ServerPlayer player;

        public Handler(ServerPlayer player) {
            this.player = player;
        }

        public ServerPlayer getPlayer() {
            return player;
        }
    }

    @FunctionalInterface
    public interface Join {
        void onPlayReady(Handler handler, Object sender, MinecraftServer server);
    }

    @FunctionalInterface
    public interface Disconnect {
        void onPlayDisconnect(Handler handler, MinecraftServer server);
    }

    /** Unused; kept so copied signatures that mention ServerGamePacketListenerImpl compile. */
    public static ServerGamePacketListenerImpl unusedListener() {
        return null;
    }
}
