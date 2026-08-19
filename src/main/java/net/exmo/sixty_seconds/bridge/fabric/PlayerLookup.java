package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Collection;
import java.util.List;

public final class PlayerLookup {
    private PlayerLookup() {
    }

    public static Collection<ServerPlayer> tracking(Entity entity) {
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            return level.getEntitiesOfClass(ServerPlayer.class, entity.getBoundingBox().inflate(128.0));
        }
        return List.of();
    }

    public static Collection<ServerPlayer> tracking(LevelChunk chunk) {
        if (chunk.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            return level.getEntitiesOfClass(ServerPlayer.class,
                    new net.minecraft.world.phys.AABB(chunk.getPos().getMinBlockX(), level.getMinBuildHeight(),
                            chunk.getPos().getMinBlockZ(), chunk.getPos().getMaxBlockX() + 1,
                            level.getMaxBuildHeight(), chunk.getPos().getMaxBlockZ() + 1));
        }
        return List.of();
    }

    public static Collection<ServerPlayer> all(net.minecraft.server.MinecraftServer server) {
        return List.copyOf(server.getPlayerList().getPlayers());
    }
}
