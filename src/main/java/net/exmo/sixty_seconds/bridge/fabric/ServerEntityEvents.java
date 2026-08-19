package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class ServerEntityEvents {
    public static final Event<Load> ENTITY_LOAD = new Event<>();

    private ServerEntityEvents() {
    }

    @FunctionalInterface
    public interface Load {
        void onLoad(Entity entity, ServerLevel world);
    }
}
