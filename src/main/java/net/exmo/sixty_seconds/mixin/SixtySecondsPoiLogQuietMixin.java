package net.exmo.sixty_seconds.mixin;

import net.minecraft.world.entity.ai.village.poi.PoiManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * POI（兴趣点，床/工作方块/信标等）数据错误的日志降级。
 * <p>
 * Lost Cities 的大规模改造会在区块里留下一些指向已消失方块位置的 POI 记录，
 * 原版在发现这类不一致时会走 error 级日志。城市废墟密集时这些记录数量可观，
 * 全是可自愈的数据噪音（POI 会在后续刷新中重建）。
 * <p>
 * 把 PoiManager 内的 error 调用重定向为 debug，信息不丢失（开 debug 可见），
 * 但默认不再刷屏。两条 Redirect 对应 SLF4J 的两种常用参数签名；
 * {@code require = 0} 保证 MC 版本差异下注入失败也只是静默跳过，不阻断启动。
 */
@Mixin(PoiManager.class)
public class SixtySecondsPoiLogQuietMixin {

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V",
            remap = false
        ),
        require = 0,
        expect = 0
    )
    private void ss$downgradePoiErrors(Logger logger, String message, Object arg) {
        logger.debug(message, arg);
    }

    @Redirect(
        method = "*",
        at = @At(
            value = "INVOKE",
            target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V",
            remap = false
        ),
        require = 0,
        expect = 0
    )
    private void ss$downgradePoiErrorsTwo(Logger logger, String message, Object arg1, Object arg2) {
        logger.debug(message, arg1, arg2);
    }
}
