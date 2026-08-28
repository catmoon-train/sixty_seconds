package net.exmo.sixty_seconds.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端配置：状态栏显示位置（左侧 / 右侧）。纯客户端偏好，不随世界存档变化。
 */
public final class SixtySecondsClientConfig {
    private SixtySecondsClientConfig() {
    }

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    /** 状态栏位置：left=左侧, right=右侧（默认左侧，即“左中侧”）。 */
    public static final ModConfigSpec.ConfigValue<String> HUD_SIDE;

    static {
        HUD_SIDE = BUILDER.comment("neoforge.config.sixty_seconds.hudSide.comment")
                .define("hudSide", "left");
        SPEC = BUILDER.build();
    }

    /** 是否为左侧（非 right 即视为左侧）。 */
    public static boolean isLeft() {
        return !"right".equalsIgnoreCase(HUD_SIDE.get());
    }
}
