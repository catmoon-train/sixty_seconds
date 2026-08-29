package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.api.LostChunkCharacteristics;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.exmo.sixty_seconds.lostcities.LostCitiesConcurrency;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;

/**
 * 为 Lost Cities 的区块特征查询提供无锁快路径，使其在并行区块生成环境下不会
 * 把所有工作线程都堵在同一把维度监视器上。
 *
 * <p>Lost Cities 原实现把整段查询包在 {@code synchronized (getDimensionLock(dimension))} 里：
 * <pre>
 *     public static LostChunkCharacteristics getChunkCharacteristics(ChunkCoord coord, IDimensionInfo provider) {
 *         synchronized (getDimensionLock(coord.dimension())) {
 *             return getChunkCharacteristicsLocked(coord, provider);
 *         }
 *     }
 * </pre>
 * 即使结果已经在并发缓存里，每次读取仍要先抢这把锁。并行区块生成时线程越多竞争越激烈，
 * 反而抵消了多线程带来的收益。{@code #getCityLevel} 同理，且它会被
 * {@code getChunkCharacteristics} 内部调用，形成同一把锁的嵌套获取。
 *
 * <p>本 mixin 在持锁之前插入一层前置无锁缓存：
 * <ul>
 *   <li><b>命中</b>：直接返回结果，跳过整个 {@code synchronized} 块，多线程真正并行；</li>
 *   <li><b>未命中</b>：不做任何干预，照常走 Lost Cities 原本的持锁冷计算路径，
 *       结果算完后在 RETURN 处回填前置缓存。</li>
 * </ul>
 * 因此冷计算仍严格保持 Lost Cities 原有的串行语义——不会重复计算、不会有半成品状态泄漏、
 * 生成结果与关闭本 mixin 时完全一致。
 *
 * <p>采用 {@code @Inject} 而非 {@code @Overwrite}，不替换任何原有代码，与其它修改
 * {@code BuildingInfo} 的 mixin 可以共存。本 mixin 不引入任何新锁或阻塞等待，无死锁风险。
 *
 * <p>另外会顺带检测 c2me 是否加载，并登记可供复用的外部线程池。
 */
@Mixin(value = BuildingInfo.class, remap = false)
public class LostCityConcurrentCharacteristicsMixin {

    private static final Logger LOGGER = LogManager.getLogger(LostCityConcurrentCharacteristicsMixin.class);

    private static volatile boolean detectionDone = false;

    @Inject(method = "getChunkCharacteristics", at = @At("HEAD"), cancellable = true)
    private static void sixtySeconds$characteristicsFastPath(ChunkCoord coord,
                                                             IDimensionInfo provider,
                                                             CallbackInfoReturnable<LostChunkCharacteristics> cir) {
        sixtySeconds$detectEnvironment();
        if (!LostCitiesConcurrency.isEnabled() || !isCacheable(provider)) {
            return;
        }
        LostChunkCharacteristics cached = LostCitiesConcurrency.peekCharacteristics(coord);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getChunkCharacteristics", at = @At("RETURN"))
    private static void sixtySeconds$publishCharacteristics(ChunkCoord coord,
                                                            IDimensionInfo provider,
                                                            CallbackInfoReturnable<LostChunkCharacteristics> cir) {
        if (!isCacheable(provider)) {
            return;
        }
        LostCitiesConcurrency.publishCharacteristics(coord, cir.getReturnValue());
    }

    @Inject(method = "getCityLevel", at = @At("HEAD"), cancellable = true)
    private static void sixtySeconds$cityLevelFastPath(ChunkCoord key,
                                                       IDimensionInfo provider,
                                                       CallbackInfoReturnable<Integer> cir) {
        if (!LostCitiesConcurrency.isEnabled() || !isCacheable(provider)) {
            return;
        }
        Integer cached = LostCitiesConcurrency.peekCityLevel(key);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getCityLevel", at = @At("RETURN"))
    private static void sixtySeconds$publishCityLevel(ChunkCoord key,
                                                      IDimensionInfo provider,
                                                      CallbackInfoReturnable<Integer> cir) {
        if (!isCacheable(provider)) {
            return;
        }
        Integer value = cir.getReturnValue();
        if (value != null) {
            LostCitiesConcurrency.publishCityLevel(key, value);
        }
    }

    /** 与 Lost Cities 的 cleanCache 同步，避免前置缓存残留已失效的维度数据。 */
    @Inject(method = "cleanCache", at = @At("RETURN"))
    private static void sixtySeconds$clearCaches(CallbackInfo ci) {
        LostCitiesConcurrency.clearCaches();
    }

    /**
     * 仅在真正的世界生成上下文中启用前置缓存。
     * Lost Cities 预览界面下 {@code getWorld()} 为 null，此时配置可能随时变动，
     * 需要每次重新计算，不能缓存。
     */
    private static boolean isCacheable(@Nullable IDimensionInfo provider) {
        return provider != null && provider.getWorld() != null;
    }

    /** 检测并行区块生成环境（c2me）是否加载，并登记可复用的外部线程池。只执行一次。 */
    private static void sixtySeconds$detectEnvironment() {
        if (detectionDone) {
            return;
        }
        synchronized (LostCityConcurrentCharacteristicsMixin.class) {
            if (detectionDone) {
                return;
            }
            boolean loaded = false;
            ExecutorService executor = null;
            try {
                loaded = ModList.get().isLoaded("c2me");
                if (loaded) {
                    Class<?> globalExecutors = Class.forName("com.ishland.c2me.base.common.GlobalExecutors");
                    executor = (ExecutorService) globalExecutors.getField("executor").get(null);
                }
            } catch (Throwable ignored) {
                // c2me 未加载或其内部类布局变化：按未加载处理，快路径仍然有效
            }
            LostCitiesConcurrency.setC2meDetected(loaded, executor);
            detectionDone = true;
            if (loaded) {
                LOGGER.info("[sixty_seconds] 检测到 c2me，已启用 Lost Cities 区块特征并发快路径（executor={}）",
                        executor != null ? "已接入" : "未接入");
            }
        }
    }
}
