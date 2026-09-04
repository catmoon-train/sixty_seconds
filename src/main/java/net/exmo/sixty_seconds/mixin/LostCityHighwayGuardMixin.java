package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.lost.Highway;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 高速公路等级缓存的并发安全读取。
 * <p>
 * {@code Highway.getHighwayLevel} 用静态 Map 缓存“区块坐标 → 公路等级”。
 * 多线程世界生成下，缓存的 {@code get} 可能与重建/清理并发，极端情况下抛出
 * ConcurrentModificationException 并炸掉整个区块生成。
 * <p>
 * 这里把缓存读取重定向为异常安全版本：任何读取失败都按“无公路”（-1）处理，
 * 与缓存未命中（null）同义。生成逻辑本就把 -1 当作无公路，行为完全兼容；
 * 最坏情况只是某次重新计算缓存，不会丢失正确性。
 */
@Mixin(value = Highway.class, remap = false)
public abstract class LostCityHighwayGuardMixin {

    @Redirect(
        method = "getHighwayLevel",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        ),
        require = 0
    )
    private static Object ss$safeCacheGet(Map<ChunkCoord, Integer> cache, Object key) {
        Object v;
        try {
            v = cache.get(key);
        } catch (Throwable t) {
            return Integer.valueOf(-1);
        }
        return v != null ? v : Integer.valueOf(-1);
    }
}
