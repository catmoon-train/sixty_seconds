package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;

/**
 * 客户端缓存的负重配置。来自打开配置面板时的 S2C 包，供 HUD 本地计算显示使用（不依赖实时同步包）。
 */
public final class WeightConfigClient {

    private static SixtySecondsWeightConfig cached;

    private WeightConfigClient() {
    }

    public static void set(SixtySecondsWeightConfig cfg) {
        cached = cfg;
    }

    public static SixtySecondsWeightConfig get() {
        return cached;
    }
}
