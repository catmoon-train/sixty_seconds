package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class ClientPlayNetworking {
    static final Map<CustomPacketPayload.Type<?>, BiConsumer<CustomPacketPayload, Context>> HANDLERS =
            new ConcurrentHashMap<>();

    private ClientPlayNetworking() {
    }

    public static void send(CustomPacketPayload payload) {
        if (payload != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(
            CustomPacketPayload.Type<T> type, PlayPayloadHandler<T> handler) {
        HANDLERS.put(type, (payload, ctx) -> handler.receive((T) payload, ctx));
    }

    public static void dispatch(CustomPacketPayload payload) {
        BiConsumer<CustomPacketPayload, Context> handler = HANDLERS.get(payload.type());
        if (handler != null) {
            handler.accept(payload, new SimpleContext());
        }
    }

    public interface PlayPayloadHandler<T extends CustomPacketPayload> {
        void receive(T payload, Context context);
    }

    public interface Context {
        Minecraft client();

        default void execute(Runnable runnable) {
            client().execute(runnable);
        }
    }

    public static final class SimpleContext implements Context {
        @Override
        public Minecraft client() {
            return Minecraft.getInstance();
        }
    }
}
