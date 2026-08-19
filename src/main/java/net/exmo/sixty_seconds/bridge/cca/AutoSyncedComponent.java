package net.exmo.sixty_seconds.bridge.cca;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** CCA AutoSyncedComponent stand-in. */
public interface AutoSyncedComponent {
    default boolean shouldSyncWith(ServerPlayer player) {
        return true;
    }

    default void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
    }

    default void applySyncPacket(RegistryFriendlyByteBuf buf) {
    }
}
