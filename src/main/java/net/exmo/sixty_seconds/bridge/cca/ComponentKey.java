package net.exmo.sixty_seconds.bridge.cca;

import io.netty.buffer.Unpooled;
import net.exmo.sixty_seconds.network.ComponentSyncS2CPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CCA ComponentKey stand-in: weak-keyed instances per player/level plus optional sync packets.
 */
public final class ComponentKey<T> {
    private static final Map<ResourceLocation, ComponentKey<?>> KEYS = new ConcurrentHashMap<>();

    private final ResourceLocation id;
    private final Class<T> type;
    private final Map<Object, T> instances = new WeakHashMap<>();

    private ComponentKey(ResourceLocation id, Class<T> type) {
        this.id = id;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    static <T> ComponentKey<T> getOrCreate(ResourceLocation id, Class<T> type) {
        return (ComponentKey<T>) KEYS.computeIfAbsent(id, k -> new ComponentKey<>(id, type));
    }

    public ResourceLocation id() {
        return id;
    }

    public T get(Object provider) {
        if (provider == null) {
            throw new NullPointerException("component provider");
        }
        synchronized (instances) {
            T existing = instances.get(provider);
            if (existing != null) {
                return existing;
            }
            T created = instantiate(provider);
            instances.put(provider, created);
            return created;
        }
    }

    public Optional<T> maybeGet(Object provider) {
        if (provider == null) {
            return Optional.empty();
        }
        return Optional.of(get(provider));
    }

    public void sync(Object provider) {
        T value = get(provider);
        if (!(value instanceof AutoSyncedComponent auto)) {
            return;
        }
        if (provider instanceof Player player && player instanceof ServerPlayer owner) {
            if (owner.level() instanceof ServerLevel level) {
                for (ServerPlayer recipient : level.players()) {
                    if (auto.shouldSyncWith(recipient)) {
                        send(auto, recipient, player.getUUID());
                    }
                }
            }
            return;
        }
        if (provider instanceof ServerLevel level) {
            for (ServerPlayer recipient : level.players()) {
                if (auto.shouldSyncWith(recipient)) {
                    send(auto, recipient, recipient.getUUID());
                }
            }
        }
    }

    private void send(AutoSyncedComponent auto, ServerPlayer recipient, java.util.UUID entityId) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), recipient.registryAccess());
        auto.writeSyncPacket(buf, recipient);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        buf.release();
        net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking.send(recipient,
                new ComponentSyncS2CPacket(id.toString(), entityId, data));
    }

    public void applyFromPacket(Object provider, byte[] data, Player self) {
        T value = get(provider);
        if (value instanceof AutoSyncedComponent auto && self != null && self.level() != null) {
            RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data),
                    self.level().registryAccess());
            auto.applySyncPacket(buf);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ComponentKey<T> byId(ResourceLocation id) {
        return (ComponentKey<T>) KEYS.get(id);
    }

    private T instantiate(Object provider) {
        try {
            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length == 1 && params[0].isInstance(provider)) {
                    ctor.setAccessible(true);
                    return type.cast(ctor.newInstance(provider));
                }
            }
            if (provider instanceof Player && hasCtor(Player.class)) {
                Constructor<T> ctor = type.getDeclaredConstructor(Player.class);
                ctor.setAccessible(true);
                return ctor.newInstance((Player) provider);
            }
            if (provider instanceof Level && hasCtor(Level.class)) {
                Constructor<T> ctor = type.getDeclaredConstructor(Level.class);
                ctor.setAccessible(true);
                return ctor.newInstance((Level) provider);
            }
            Constructor<T> empty = type.getDeclaredConstructor();
            empty.setAccessible(true);
            return empty.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot create " + type.getName() + " for " + provider, e);
        }
    }

    private boolean hasCtor(Class<?> arg) {
        try {
            type.getDeclaredConstructor(arg);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
