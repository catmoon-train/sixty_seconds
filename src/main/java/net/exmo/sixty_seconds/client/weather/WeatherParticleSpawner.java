package net.exmo.sixty_seconds.client.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.registry.ModParticles;
import net.exmo.sixty_seconds.weather.ClientWeatherState;
import net.exmo.sixty_seconds.weather.WeatherVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 每客户端 tick 调用一次：当存在激活的主题化天气时，在玩家周围环形分布生成对应粒子。
 * 密度来自主题配置并受全局倍率与单 tick 上限约束（性能优先）。
 */
public final class WeatherParticleSpawner {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherParticleSpawner.class);
    private static final double RANGE = 16.0;
    private static boolean firstTickLogged = false;
    private static SixtySecondsEventSystem.EventType lastLoggedType = null;
    private static boolean enabledWarned = false;

    private WeatherParticleSpawner() {
    }

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null || client.isPaused()) {
            if (!firstTickLogged) {
                firstTickLogged = true;
                LOG.info("[60s-weather] spawner 首帧：level/player 为空或已暂停，跳过");
            }
            return;
        }
        if (!WeatherVisualConfig.ENABLED.get()) {
            if (!enabledWarned) {
                enabledWarned = true;
                LOG.warn("[60s-weather] spawner 跳过：WeatherVisualConfig.ENABLED=false");
            }
            return;
        }
        if (!firstTickLogged) {
            firstTickLogged = true;
            LOG.info("[60s-weather] spawner 已启用 ENABLED=true");
        }
        SixtySecondsEventSystem.EventType type = ClientWeatherState.getEventType();
        if (type == null) {
            if (lastLoggedType != null) {
                LOG.info("[60s-weather] 无激活天气，停止生成粒子");
                lastLoggedType = null;
            }
            return;
        }
        WeatherTheme theme = WeatherThemes.get(type);
        if (theme == null) {
            LOG.warn("[60s-weather] 天气 {} 无对应主题，跳过", type);
            return;
        }

        int count = Math.round(theme.density * (float) WeatherVisualConfig.DENSITY_MULTIPLIER.get().doubleValue());
        count = Math.max(1, Math.min(count, WeatherVisualConfig.MAX_PER_TICK.get()));

        if (lastLoggedType != type) {
            LOG.info("[60s-weather] 开始生成天气粒子 type={} density={} streak={} count={} 粒子类型={}",
                    type, theme.density, theme.streak, count,
                    theme.streak ? "weather_streak" : "weather_dust");
            lastLoggedType = type;
        }

        Vec3 cam = client.player.position();
        for (int i = 0; i < count; i++) {
            double x = cam.x + (client.level.random.nextDouble() - 0.5) * RANGE * 2.0;
            double z = cam.z + (client.level.random.nextDouble() - 0.5) * RANGE * 2.0;
            double y = cam.y + client.level.random.nextDouble() * 12.0 + 2.0;

            double vx = theme.vx + (client.level.random.nextDouble() - 0.5) * theme.jitter;
            double vy = theme.vy + (client.level.random.nextDouble() - 0.5) * theme.jitter * 0.5;
            double vz = theme.vz + (client.level.random.nextDouble() - 0.5) * theme.jitter;

            SimpleParticleType pt = theme.streak
                    ? ModParticles.WEATHER_STREAK
                    : ModParticles.WEATHER_DUST;
            client.level.addParticle(pt, x, y, z, vx, vy, vz);
        }
    }
}
