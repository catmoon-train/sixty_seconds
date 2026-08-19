package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ServerPlayNetworking {
    static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Context>> HANDLERS =
            new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player != null && payload != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        HANDLERS.put(type, (payload, ctx) -> handler.receive((T) payload, ctx));
    }

    public static boolean dispatch(CustomPacketPayload payload, Context ctx) {
        BiConsumer<CustomPacketPayload, Context> handler = HANDLERS.get(payload.type());
        if (handler != null) {
            handler.accept(payload, ctx);
            return true;
        }
        return false;
    }

    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        ServerPlayer player();

        MinecraftServer server();
    }

    public record SimpleContext(ServerPlayer player) implements Context {
        @Override
        public MinecraftServer server() {
            return player.getServer();
        }
    }
}
