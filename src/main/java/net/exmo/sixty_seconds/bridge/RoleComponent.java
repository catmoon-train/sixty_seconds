package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface RoleComponent extends AutoSyncedComponent {
    Player getPlayer();

    void init();

    void clear();

    @Override
    default boolean shouldSyncWith(ServerPlayer player) {
        return this.getPlayer() == player;
    }

    default void writeToSyncNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }

    default void readFromSyncNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }

    default void writeToSyncNbtWithPlayer(CompoundTag tag, HolderLookup.Provider registryLookup,
            ServerPlayer recipient) {
        writeToSyncNbt(tag, registryLookup);
    }

    @Override
    default void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        CompoundTag tag = new CompoundTag();
        this.writeToSyncNbtWithPlayer(tag, buf.registryAccess(), recipient);
        buf.writeNbt(tag);
    }

    @Override
    default void applySyncPacket(RegistryFriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        if (tag != null) {
            this.readFromSyncNbt(tag, buf.registryAccess());
        }
    }

    default void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }

    default void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
    }
}
