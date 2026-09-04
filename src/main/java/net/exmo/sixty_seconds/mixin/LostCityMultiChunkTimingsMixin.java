package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.LostCities;
import mcjty.lostcities.worldgen.lost.MultiChunk;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 多区块建筑群的生成计时日志降噪。
 * <p>
 * {@code MultiChunk.calculateBuildings} 在规划每个多区块建筑群时会用 info 级
 * 日志输出耗时统计。这类日志只在排查生成性能时有价值，正常运行时属于噪音。
 * <p>
 * 处理策略：默认把 info 降为 debug（且仅在 debug 已启用时才真正输出，
 * 避免构造日志参数的开销）；需要重新观测生成性能时，加 JVM 参数
 * {@code -Dsixty_seconds.lostcities.multichunk_timings=true} 即可恢复 info 级输出，
 * 无需改代码重新打包。
 */
@Mixin(value = MultiChunk.class, remap = false)
public class LostCityMultiChunkTimingsMixin {

    private static final boolean SS_ENABLE_INFO_TIMINGS =
        Boolean.getBoolean("sixty_seconds.lostcities.multichunk_timings");

    @Redirect(
        method = "calculateBuildings",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"
        ),
        require = 0,
        expect = 0
    )
    private void ss$redirectMultiChunkTimingInfo(Logger logger, String message, Object[] params) {
        if (SS_ENABLE_INFO_TIMINGS) {
            logger.info(message, params);
            return;
        }
        if (logger.isDebugEnabled() || LostCities.getLogger().isDebugEnabled()) {
            logger.debug(message, params);
        }
    }

    @Redirect(
        method = "calculateBuildings",
        at = @At(
            value = "INVOKE",
            target = "Lorg/apache/logging/log4j/Logger;debug(Ljava/lang/String;[Ljava/lang/Object;)V"
        ),
        require = 0,
        expect = 0
    )
    private void ss$redirectMultiChunkTimingDebug(Logger logger, String message, Object[] params) {
        logger.debug(message, params);
    }
}
