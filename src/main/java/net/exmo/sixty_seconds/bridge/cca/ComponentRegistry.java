package net.exmo.sixty_seconds.bridge.cca;

import net.minecraft.resources.ResourceLocation;

public final class ComponentRegistry {
    private ComponentRegistry() {
    }

    public static <T> ComponentKey<T> getOrCreate(ResourceLocation id, Class<T> type) {
        return ComponentKey.getOrCreate(id, type);
    }
}
