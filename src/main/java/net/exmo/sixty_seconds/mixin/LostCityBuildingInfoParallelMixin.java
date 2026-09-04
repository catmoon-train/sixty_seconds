package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.api.LostChunkCharacteristics;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.varia.TimedCache;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.exmo.sixty_seconds.lostcities.LostCitiesConcurrency;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * 把 BuildingInfo 的三个 memoization 入口从“全局维度锁串行”改造为
 * “按坐标单飞（single-flight）并行”。
 */
@Mixin(value = BuildingInfo.class, remap = false, priority = 800)
public abstract class LostCityBuildingInfoParallelMixin {

    @Shadow @Final private static TimedCache<ChunkCoord, BuildingInfo> BUILDING_INFO_MAP;
    @Shadow @Final private static TimedCache<ChunkCoord, LostChunkCharacteristics> CITY_INFO_MAP;
    @Shadow @Final private static TimedCache<ChunkCoord, Integer> CITY_LEVEL_CACHE;

    @Shadow
    private static LostChunkCharacteristics getChunkCharacteristicsLocked(ChunkCoord coord, IDimensionInfo provider) {
        throw new AssertionError();
    }

    @Shadow
    private static int getCityLevelLocked(ChunkCoord key, IDimensionInfo provider) {
        throw new AssertionError();
    }

    /** worker 线程标记：置位期间所有嵌套查询直接同步计算，禁止再提交或等待。 */
    @Unique
    private static final ThreadLocal<Boolean> SS_IN_WORKER = new ThreadLocal<>();

    @Unique
    private static final ConcurrentHashMap<ChunkCoord, CompletableFuture<LostChunkCharacteristics>> SS_CHARS_FLIGHT =
            new ConcurrentHashMap<>();

    @Unique
    private static final ConcurrentHashMap<ChunkCoord, CompletableFuture<Integer>> SS_LEVEL_FLIGHT =
            new ConcurrentHashMap<>();

    @Unique
    private static final ConcurrentHashMap<ChunkCoord, CompletableFuture<BuildingInfo>> SS_INFO_FLIGHT =
            new ConcurrentHashMap<>();

    @Overwrite
    public static LostChunkCharacteristics getChunkCharacteristics(ChunkCoord coord, IDimensionInfo provider) {
        if (provider == null || provider.getWorld() == null) {
            return getChunkCharacteristicsLocked(coord, provider);
        }
        LostChunkCharacteristics cached = CITY_INFO_MAP.get(coord);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(SS_IN_WORKER.get())) {
            return getChunkCharacteristicsLocked(coord, provider);
        }
        return ss$singleFlight(coord, SS_CHARS_FLIGHT, () -> getChunkCharacteristicsLocked(coord, provider));
    }

    @Overwrite
    public static int getCityLevel(ChunkCoord key, IDimensionInfo provider) {
        if (provider == null || provider.getWorld() == null) {
            return getCityLevelLocked(key, provider);
        }
        Integer cached = CITY_LEVEL_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(SS_IN_WORKER.get())) {
            return getCityLevelLocked(key, provider);
        }
        return ss$singleFlight(key, SS_LEVEL_FLIGHT, () -> getCityLevelLocked(key, provider));
    }

    @Overwrite
    public static BuildingInfo getBuildingInfo(ChunkCoord key, IDimensionInfo provider) {
        if (provider == null || provider.getWorld() == null) {
            return ss$createAndCache(key, provider);
        }
        BuildingInfo cached = BUILDING_INFO_MAP.get(key);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(SS_IN_WORKER.get())) {
            return ss$createAndCache(key, provider);
        }
        return ss$singleFlight(key, SS_INFO_FLIGHT, () -> ss$createAndCache(key, provider));
    }

    /** 与 Lost Cities 的 cleanCache 同步：清空在途单飞表，防止跨维度残留。 */
    @Inject(method = "cleanCache", at = @At("RETURN"), remap = false)
    private static void ss$clearFlights(CallbackInfo ci) {
        SS_CHARS_FLIGHT.clear();
        SS_LEVEL_FLIGHT.clear();
        SS_INFO_FLIGHT.clear();
    }

    /** 复刻原版语义：仅当结构规避结果已知（区块数据已就绪）时才把实例放入缓存。 */
    @Unique
    private static BuildingInfo ss$createAndCache(ChunkCoord key, IDimensionInfo provider) {
        BuildingInfo info = BuildingInfoInvoker.ss$createBuildingInfo(key, provider);
        if (((BuildingInfoInvoker) (Object) info).ss$getStructureAvoidance().isKnown()) {
            BUILDING_INFO_MAP.put(key, info);
        }
        return info;
    }

    /** 按坐标单飞：同坐标冷计算只执行一次，结果对所有等待者可见。 */
    @Unique
    private static <V> V ss$singleFlight(ChunkCoord coord,
                                         ConcurrentHashMap<ChunkCoord, CompletableFuture<V>> flights,
                                         Supplier<V> computer) {
        CompletableFuture<V> future = flights.get(coord);
        if (future == null) {
            CompletableFuture<V> created = new CompletableFuture<>();
            future = flights.computeIfAbsent(coord, k -> created);
            if (future == created) {
                ss$schedule(coord, created, computer, flights);
            }
        }
        return ss$join(future);
    }

    /** 优先派发到 c2me 线程池；池不可用或拒绝时退化为调用线程直接计算。 */
    @Unique
    private static <V> void ss$schedule(ChunkCoord coord,
                                        CompletableFuture<V> future,
                                        Supplier<V> computer,
                                        ConcurrentHashMap<ChunkCoord, CompletableFuture<V>> flights) {
        Runnable task = () -> {
            SS_IN_WORKER.set(Boolean.TRUE);
            try {
                future.complete(computer.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            } finally {
                SS_IN_WORKER.remove();
                flights.remove(coord, future);
            }
        };
        ExecutorService executor = LostCitiesConcurrency.externalExecutor();
        if (executor != null) {
            try {
                executor.execute(task);
                return;
            } catch (Throwable ignored) {
                flights.remove(coord, future);
            }
        }
        SS_IN_WORKER.set(Boolean.TRUE);
        try {
            future.complete(computer.get());
        } catch (Throwable t) {
            future.completeExceptionally(t);
            throw t instanceof RuntimeException runtime ? runtime : new CompletionException(t);
        } finally {
            SS_IN_WORKER.remove();
            flights.remove(coord, future);
        }
    }

    /** join 并解包异步异常，保持与原版同步抛出一致的行为。 */
    @Unique
    private static <V> V ss$join(CompletableFuture<V> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw e;
        }
    }
}
