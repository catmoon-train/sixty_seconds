package net.exmo.sixty_seconds.bridge;

public final class SixtySecConfig {
    private static final SixtySecConfig INSTANCE = new SixtySecConfig();

    public int minigameTaskIntervalSeconds = 90;
    public int minigameBlockCooldownSeconds = 30;
    public int safeTimeCooldown = 0;
    public int startGameRequiredPermission = 2;

    private SixtySecConfig() {
    }

    public static SixtySecConfig instance() {
        return INSTANCE;
    }
}
