package net.exmo.sixty_seconds.client.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;

import java.util.EnumMap;
import java.util.Map;

import static net.exmo.sixty_seconds.logic.SixtySecondsEventSystem.EventType.*;

/** 为模组内每一种天气定义一套粒子表现。数值集中在下方静态块，便于调参/调密度大小。 */
public final class WeatherThemes {
    public static final WeatherTheme FALLBACK =
            new WeatherTheme(0.8f, 0.8f, 0.85f, 0.3f, 0.7f, 0.05f, false, 6, 0f, -0.1f, 0f, 0.1f, 40, 70);

    private static final Map<SixtySecondsEventSystem.EventType, WeatherTheme> TABLE =
            new EnumMap<>(SixtySecondsEventSystem.EventType.class);

    static {
        // 顺序: r, g, b, alpha, size, gravity, streak, density, vx, vy, vz, jitter, lifeMin, lifeMax
        put(POLLUTION_RAIN,         0.45f, 0.75f, 0.30f, 0.60f, 0.22f, 0.040f, true,  12, 0.00f, -0.40f, 0.00f, 0.15f, 25,  40);
        put(SMOG,                   0.35f, 0.32f, 0.30f, 0.28f, 0.70f, 0.000f, false,  7,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(COLD_SNAP,              0.80f, 0.90f, 1.00f, 0.50f, 0.20f, 0.035f, true,   9,  0.00f, -0.30f, 0.00f, 0.12f, 30,  45);
        put(ACID_FOG,               0.40f, 0.90f, 0.20f, 0.22f, 0.80f, 0.000f, false,  7,  0.04f,  0.00f, 0.04f, 0.12f, 80, 140);
        put(ELECTROMAGNETIC_STORM,  0.55f, 0.35f, 1.00f, 0.60f, 0.32f, 0.000f, false,  6,  0.20f,  0.00f, 0.20f, 0.25f, 25,  40);
        put(SWARM,                  0.20f, 0.20f, 0.15f, 0.70f, 0.16f, 0.000f, false, 14,  0.30f,  0.05f, 0.30f, 0.40f, 30,  50);
        put(HEAT_WAVE,              1.00f, 0.50f, 0.10f, 0.30f, 0.45f, -0.008f,false,  6,  0.05f,  0.02f, 0.05f, 0.15f, 40,  70);
        put(SANDSTORM,              0.80f, 0.70f, 0.45f, 0.45f, 0.40f, 0.000f, false, 16,  0.40f,  0.00f, 0.40f, 0.40f, 30,  55);
        put(EARTHQUAKE,             0.55f, 0.45f, 0.35f, 0.30f, 0.40f, 0.010f, false,  8,  0.20f,  0.00f, 0.20f, 0.30f, 25,  40);
        put(METEOR_SHOWER,          1.00f, 0.55f, 0.15f, 0.85f, 0.22f, 0.060f, true,   5,  0.00f, -0.60f, 0.00f, 0.20f, 25,  40);
        put(SPORE_FOG,              0.30f, 0.60f, 0.20f, 0.25f, 0.50f, 0.000f, false,  7,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(HAIL,                   0.85f, 0.90f, 1.00f, 0.80f, 0.16f, 0.090f, true,   9,  0.00f, -0.40f, 0.00f, 0.10f, 20,  35);
        put(BLOOD_MOON,             0.60f, 0.05f, 0.05f, 0.25f, 0.50f, 0.000f, false,  8,  0.05f,  0.00f, 0.05f, 0.12f, 80, 140);
        put(RADIATION_LEAK,         0.50f, 1.00f, 0.20f, 0.40f, 0.35f, 0.000f, false,  6,  0.05f,  0.00f, 0.05f, 0.12f, 60, 100);
        put(DENSE_FOG,              0.70f, 0.70f, 0.72f, 0.20f, 0.85f, 0.000f, false,  9,  0.03f,  0.00f, 0.03f, 0.10f, 100, 160);
    }

    private static void put(SixtySecondsEventSystem.EventType t, float r, float g, float b, float a, float sz,
                            float gr, boolean s, int d, float vx, float vy, float vz, float j, int lm, int lM) {
        TABLE.put(t, new WeatherTheme(r, g, b, a, sz, gr, s, d, vx, vy, vz, j, lm, lM));
    }

    public static WeatherTheme get(SixtySecondsEventSystem.EventType t) {
        return t == null ? null : TABLE.getOrDefault(t, FALLBACK);
    }
}
