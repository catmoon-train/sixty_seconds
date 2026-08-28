package net.exmo.sixty_seconds.weather;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置：天气粒子的总开关、密度与大小倍率、单 tick 单玩家上限（性能保护），
 * 以及 HUD 状态栏的显示位置。位于公共包，仅依赖 ModConfigSpec（服务端亦可加载，不会引入客户端类）。
 */
public final class WeatherVisualConfig {
    private WeatherVisualConfig() {
    }

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue DENSITY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SIZE_MULTIPLIER;
    public static final ModConfigSpec.IntValue MAX_PER_TICK;
    public static final ModConfigSpec.BooleanValue SKY_TINT_ENABLED;
    public static final ModConfigSpec.DoubleValue SKY_TINT_STRENGTH;
    public static final ModConfigSpec.ConfigValue<String> HUD_SIDE;

    static {
        ENABLED = BUILDER.comment("启用天气粒子覆盖（封掉原版雨雪渲染并替换为主题化粒子）")
                .define("enabled", true);
        DENSITY_MULTIPLIER = BUILDER.comment("粒子密度倍率（表现强度，越大越密）")
                .defineInRange("densityMultiplier", 1.0, 0.0, 4.0);
        SIZE_MULTIPLIER = BUILDER.comment("粒子大小倍率（表现强度，越大越明显）")
                .defineInRange("sizeMultiplier", 1.0, 0.3, 3.0);
        MAX_PER_TICK = BUILDER.comment("单 tick 单玩家最大粒子数（性能保护上限）")
                .defineInRange("maxParticlesPerTick", 24, 4, 80);
        SKY_TINT_ENABLED = BUILDER.comment("天气激活时给天空染色（跟随主题色）")
                .define("skyTintEnabled", true);
        SKY_TINT_STRENGTH = BUILDER.comment("天空染色强度倍率（0=无，1=完全覆盖主题色）")
                .defineInRange("skyTintStrength", 0.8, 0.0, 1.0);
        HUD_SIDE = BUILDER.comment("neoforge.config.sixty_seconds.hudSide.comment")
                .define("hudSide", "left");
        SPEC = BUILDER.build();
    }

    /** 状态栏是否绘制在屏幕左侧（默认），否则为右侧。 */
    public static boolean isHudLeft() {
        return !"right".equalsIgnoreCase(HUD_SIDE.get());
    }
}
