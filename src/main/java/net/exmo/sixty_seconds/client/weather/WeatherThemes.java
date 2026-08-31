package net.exmo.sixty_seconds.client.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.registry.ModParticles;
import net.minecraft.core.particles.ParticleType;

import java.util.EnumMap;
import java.util.Map;

import static net.exmo.sixty_seconds.logic.SixtySecondsEventSystem.EventType.*;

/** 为模组内每一种天气定义一套粒子表现。数值集中在下方静态块，便于调参/调密度大小。 */
public final class WeatherThemes {
    public static final WeatherTheme FALLBACK =
            new WeatherTheme(0.8f, 0.8f, 0.85f, 0.3f, 0.7f, 0.05f, ModParticles.WEATHER_DUST, 6, 0f, -0.1f, 0f, 0.1f, 40, 70,
                    0.8f, 0.8f, 0.85f, 0.35f);

    private static final Map<SixtySecondsEventSystem.EventType, WeatherTheme> TABLE =
            new EnumMap<>(SixtySecondsEventSystem.EventType.class);

    static {
        // 顺序: r, g, b, alpha, size, gravity, particle, density, vx, vy, vz, jitter, lifeMin, lifeMax
        put(POLLUTION_RAIN,         0.45f, 0.75f, 0.30f, 0.60f, 0.22f, 0.040f, ModParticles.WEATHER_POLLUTION_RAIN, 12, 0.00f, -0.40f, 0.00f, 0.15f, 25,  40);
        put(SMOG,                   0.35f, 0.32f, 0.30f, 0.28f, 0.70f, 0.000f, ModParticles.WEATHER_DUST,           7,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(COLD_SNAP,              0.80f, 0.90f, 1.00f, 0.50f, 0.20f, 0.035f, ModParticles.WEATHER_SNOW,            9,  0.00f, -0.30f, 0.00f, 0.12f, 30,  45);
        put(ACID_FOG,               0.40f, 0.90f, 0.20f, 0.22f, 0.80f, 0.000f, ModParticles.WEATHER_BUBBLE,          7,  0.04f,  0.00f, 0.04f, 0.12f, 80, 140);
        put(ELECTROMAGNETIC_STORM,  0.55f, 0.35f, 1.00f, 0.60f, 0.32f, 0.000f, ModParticles.WEATHER_SPARK,           6,  0.20f,  0.00f, 0.20f, 0.25f, 25,  40);
        put(SWARM,                  0.20f, 0.20f, 0.15f, 0.70f, 0.16f, 0.000f, ModParticles.WEATHER_DUST,          14,  0.30f,  0.05f, 0.30f, 0.40f, 30,  50);
        put(HEAT_WAVE,              1.00f, 0.50f, 0.10f, 0.30f, 0.45f, -0.008f, ModParticles.WEATHER_DUST,          6,  0.05f,  0.02f, 0.05f, 0.15f, 40,  70);
        put(SANDSTORM,              0.80f, 0.70f, 0.45f, 0.45f, 0.40f, 0.000f, ModParticles.WEATHER_SAND,           16,  0.40f,  0.00f, 0.40f, 0.40f, 30,  55);
        put(EARTHQUAKE,             0.55f, 0.45f, 0.35f, 0.30f, 0.40f, 0.010f, ModParticles.WEATHER_DUST,            8,  0.20f,  0.00f, 0.20f, 0.30f, 25,  40);
        put(METEOR_SHOWER,          1.00f, 0.55f, 0.15f, 0.85f, 0.22f, 0.060f, ModParticles.WEATHER_EMBER,           5,  0.00f, -0.60f, 0.00f, 0.20f, 25,  40);
        put(SPORE_FOG,              0.30f, 0.60f, 0.20f, 0.25f, 0.50f, 0.000f, ModParticles.WEATHER_SPORE,          7,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(HAIL,                   0.85f, 0.90f, 1.00f, 0.80f, 0.16f, 0.090f, ModParticles.WEATHER_DUST,            9,  0.00f, -0.40f, 0.00f, 0.10f, 20,  35);
        put(BLOOD_MOON,             0.60f, 0.05f, 0.05f, 0.25f, 0.50f, 0.000f, ModParticles.WEATHER_BLOOD_MOTE,      8,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(RADIATION_LEAK,         0.50f, 1.00f, 0.20f, 0.40f, 0.35f, 0.000f, ModParticles.WEATHER_RADIATION,       6,  0.05f,  0.00f, 0.05f, 0.12f, 60, 100);
        put(DENSE_FOG,              0.70f, 0.70f, 0.72f, 0.20f, 0.85f, 0.000f, ModParticles.WEATHER_DUST,           9,  0.03f,  0.00f, 0.03f, 0.10f, 100, 160);
        // 新增天气视觉
        put(THUNDERSTORM,           0.85f, 0.85f, 1.00f, 0.70f, 0.20f, 0.040f, ModParticles.WEATHER_STREAK,         10, 0.00f, -0.35f, 0.00f, 0.15f, 25,  40);
        put(TIDAL_SURGE,            0.10f, 0.55f, 0.85f, 0.55f, 0.50f, 0.000f, ModParticles.WEATHER_DUST,          12, 0.20f,  0.00f, 0.20f, 0.30f, 50,  90);
        put(VOID_RIFT,              0.55f, 0.15f, 0.75f, 0.60f, 0.30f, 0.000f, ModParticles.WEATHER_DUST,           8, 0.30f,  0.10f, 0.30f, 0.40f, 30,  50);
        // 新增天气（7种）专属贴图
        put(ASH_FALL,               0.65f, 0.62f, 0.60f, 0.45f, 0.18f, 0.000f, ModParticles.WEATHER_ASH,            13, 0.10f, -0.20f, 0.10f, 0.20f, 60, 110);
        put(FIRE_RAIN,              1.00f, 0.45f, 0.12f, 0.80f, 0.20f, 0.050f, ModParticles.WEATHER_FIRE_RAIN,      10, 0.00f, -0.55f, 0.00f, 0.18f, 20,  35);
        put(CRYSTAL_STORM,          0.55f, 0.90f, 1.00f, 0.55f, 0.22f, 0.000f, ModParticles.WEATHER_CRYSTAL,        9,  0.20f, -0.10f, 0.20f, 0.25f, 35,  60);
        put(TOXIC_SPORE,            0.55f, 0.85f, 0.20f, 0.40f, 0.40f, 0.000f, ModParticles.WEATHER_TOXIC_SPORE,    9,  0.06f,  0.04f, 0.06f, 0.14f, 70, 130);
        put(SOLAR_FLARE,            1.00f, 0.85f, 0.30f, 0.70f, 0.22f, 0.050f, ModParticles.WEATHER_SOLAR,         10, 0.00f, -0.45f, 0.00f, 0.15f, 20,  35);
        put(SOUL_WIND,              0.80f, 0.80f, 0.95f, 0.40f, 0.45f, 0.000f, ModParticles.WEATHER_SOUL,           9,  0.25f,  0.00f, 0.25f, 0.30f, 50,  90);
        put(EMBER_STORM,            1.00f, 0.40f, 0.10f, 0.65f, 0.20f, 0.000f, ModParticles.WEATHER_EMBER_STORM,    11, 0.15f, -0.15f, 0.15f, 0.30f, 40,  70);
        // 第三批：雨类天气视觉（复用既有粒子，配色区分）
        put(ACID_RAIN,              0.30f, 0.80f, 0.20f, 0.60f, 0.22f, 0.040f, ModParticles.WEATHER_POLLUTION_RAIN, 11, 0.00f, -0.40f, 0.00f, 0.15f, 25,  40);
        put(POISON_RAIN,            0.40f, 0.70f, 0.25f, 0.55f, 0.22f, 0.040f, ModParticles.WEATHER_SPORE,          11, 0.00f, -0.40f, 0.00f, 0.15f, 25,  40);
        put(FROST_RAIN,             0.75f, 0.90f, 1.00f, 0.70f, 0.20f, 0.040f, ModParticles.WEATHER_SNOW,            11, 0.00f, -0.35f, 0.00f, 0.15f, 25,  40);
        put(SLIME_RAIN,             0.45f, 0.80f, 0.30f, 0.55f, 0.22f, 0.040f, ModParticles.WEATHER_BUBBLE,         11, 0.00f, -0.40f, 0.00f, 0.15f, 25,  40);
        put(SPARK_RAIN,             1.00f, 0.85f, 0.20f, 0.70f, 0.20f, 0.040f, ModParticles.WEATHER_SPARK,          10, 0.00f, -0.35f, 0.00f, 0.15f, 25,  40);
    }

    private static void put(SixtySecondsEventSystem.EventType t, float r, float g, float b, float a, float sz,
                            float gr, ParticleType<?> p, int d, float vx, float vy, float vz, float j, int lm, int lM) {
        TABLE.put(t, new WeatherTheme(r, g, b, a, sz, gr, p, d, vx, vy, vz, j, lm, lM, r, g, b, 0.4f));
    }

    public static WeatherTheme get(SixtySecondsEventSystem.EventType t) {
        return t == null ? null : TABLE.getOrDefault(t, FALLBACK);
    }
}
