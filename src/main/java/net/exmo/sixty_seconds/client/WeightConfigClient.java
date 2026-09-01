package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;

/**
 * 客户端缓存的负重配置。来自打开配置面板时的 S2C 包，供 HUD 本地计算显示使用（不依赖实时同步包）。
 * 当缓存为空时，回退到模组内置默认配置（从 {@code default_weights.json} 读取），保证未打开配置面板时
 * 也能本地显示负重，无需服务端额外发包。
 */
public final class WeightConfigClient {

    private static SixtySecondsWeightConfig cached;
    private static SixtySecondsWeightConfig builtinFallback;

    private WeightConfigClient() {
    }

    public static void set(SixtySecondsWeightConfig cfg) {
        cached = cfg;
    }

    public static SixtySecondsWeightConfig get() {
        return cached;
    }

    /** 优先返回已同步的服务器配置；为空时回退到内置默认配置（只解析一次并缓存）。 */
    public static SixtySecondsWeightConfig getOrBuiltin() {
        if (cached != null) {
            return cached;
        }
        if (builtinFallback == null) {
            builtinFallback = SixtySecondsWeightConfigStore.loadBuiltinDefault();
        }
        return builtinFallback;
    }
}
