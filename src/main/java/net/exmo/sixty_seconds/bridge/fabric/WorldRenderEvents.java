package net.exmo.sixty_seconds.bridge.fabric;

public final class WorldRenderEvents {
    public static final Event<AfterTranslucent> AFTER_TRANSLUCENT = new Event<>();
    public static final Event<AfterTranslucent> AFTER_ENTITIES = new Event<>();

    private WorldRenderEvents() {
    }

    @FunctionalInterface
    public interface AfterTranslucent {
        void afterTranslucent(WorldRenderContext context);
    }
}
