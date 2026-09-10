package net.exmo.sixty_seconds.client.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.weather.ClientWeatherState;
import net.exmo.sixty_seconds.weather.WeatherVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

/**
 * 每客户端 tick 调用一次：当存在激活的主题化天气时，在玩家周围环形分布生成对应粒子。
 * 密度来自主题配置并受全局倍率与单 tick 上限约束（性能优先）。
 */
public final class WeatherParticleSpawner {
    private static final double RANGE = 16.0;
    private static final long EXPOSURE_CACHE_TICKS = 10L;
    private static net.minecraft.client.multiplayer.ClientLevel exposureLevel;
    private static BlockPos exposureColumn;
    private static long exposureCheckedAt = Long.MIN_VALUE;
    private static boolean playerExposed;

    private WeatherParticleSpawner() {
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.isPaused()) {
            return;
        }
        if (!WeatherVisualConfig.ENABLED.get()) {
            return;
        }
        SixtySecondsEventSystem.EventType type = ClientWeatherState.getEventType();
        if (type == null) {
            return;
        }
        WeatherTheme theme = WeatherThemes.get(type);
        if (theme == null) {
            return;
        }

        Vec3 cam = client.player.position();
        BlockPos playerColumn = BlockPos.containing(cam.x, cam.y + 1.0, cam.z);
        long gameTime = client.level.getGameTime();
        if (client.level != exposureLevel
                || exposureColumn == null
                || !exposureColumn.equals(playerColumn)
                || gameTime - exposureCheckedAt >= EXPOSURE_CACHE_TICKS) {
            exposureLevel = client.level;
            exposureColumn = playerColumn;
            exposureCheckedAt = gameTime;
            // One cached sky check per 10 ticks prevents weather spawning
            // while the camera is under a solid roof, without ray-tracing
            // every particle or every frame.
            playerExposed = client.level.canSeeSky(playerColumn);
        }
        if (!playerExposed) {
            return;
        }

        int count = Math.round(theme.density * (float) WeatherVisualConfig.DENSITY_MULTIPLIER.get().doubleValue());
        count = Math.max(1, Math.min(count, WeatherVisualConfig.MAX_PER_TICK.get()));

        for (int i = 0; i < count; i++) {
            double x = cam.x + (client.level.random.nextDouble() - 0.5) * RANGE * 2.0;
            double z = cam.z + (client.level.random.nextDouble() - 0.5) * RANGE * 2.0;
            double y = cam.y + client.level.random.nextDouble() * 12.0 + 2.0;

            // Do not create a particle below a roof or inside a building.
            // This is one bounded sky-visibility lookup per attempted spawn;
            // the player exposure result above avoids all of them indoors.
            if (!client.level.canSeeSky(BlockPos.containing(x, y, z))) {
                continue;
            }

            double vx = theme.vx + (client.level.random.nextDouble() - 0.5) * theme.jitter;
            double vy = theme.vy + (client.level.random.nextDouble() - 0.5) * theme.jitter * 0.5;
            double vz = theme.vz + (client.level.random.nextDouble() - 0.5) * theme.jitter;

            SimpleParticleType pt = (SimpleParticleType) theme.particle;
            client.level.addParticle(pt, x, y, z, vx, vy, vz);
        }
    }
}
