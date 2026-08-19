package net.exmo.sixty_seconds.bridge;

public final class SREConfig {
    private static final SREConfig INSTANCE = new SREConfig();

    public int minigameTaskIntervalSeconds = 90;
    public int minigameBlockCooldownSeconds = 30;
    public int safeTimeCooldown = 0;
    public int startGameRequiredPermission = 2;

    private SREConfig() {
    }

    public static SREConfig instance() {
        return INSTANCE;
    }
}
