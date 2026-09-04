package net.exmo.sixty_seconds.lostcities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 星图「建筑星级区域」计算服务（服务端）。
 *
 * <p><b>线程安全依据</b>：后台线程只做三类操作——{@code ServerLevel#hasChunk}
 * （只读）、{@code ILostCityInformation#getChunkInfo}（落到 LostCities 的
 * {@code BuildingInfo.getBuildingInfo}，该路径已由 sixty_seconds 的
 * {@code LostCityBuildingInfoParallelMixin} 并发化：TimedCache 并发缓存 +
 * 按坐标单飞）、以及纯内存映射表查询。与 c2me 生成工作线程的访问级别一致。</p>
 */
public final class StarMapRegionService {

    /** 缓存有效期（毫秒）。过期后下一次请求会触发后台重算，但旧数据仍可先行下发。 */
    private static final long CACHE_TTL_MS = 15_000;

    /** 单线程后台执行器：串行化重算，daemon 线程不阻止 JVM 退出。 */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sixty-seconds-starmap");
        t.setDaemon(true);
        return t;
    });

    private static final Object LOCK = new Object();
    private static List<SixtySecondsLostCitiesStarMap.BuildingRegion> cached = List.of();
    private static long centerKey = Long.MIN_VALUE;
    private static long computedAtMs;
    private static final AtomicBoolean COMPUTING = new AtomicBoolean(false);

    private StarMapRegionService() {
    }

    private static long keyOf(int pcx, int pcz) {
        return ((long) pcx << 32) | (pcz & 0xffffffffL);
    }

    /**
     * 主线程调用：返回当前可用的区域快照。
     * 缓存命中（同中心、TTL 内）返回最新数据；否则返回最近一次的旧数据（可能为空表）。
     * <b>绝不触发扫描</b>，调用开销为 O(1)。
     */
    public static List<SixtySecondsLostCitiesStarMap.BuildingRegion> snapshot(int pcx, int pcz) {
        synchronized (LOCK) {
            return centerKey == keyOf(pcx, pcz) ? cached : List.of();
        }
    }

    /** 缓存是否对（pcx,pcz）仍然新鲜（命中且未过期）。 */
    public static boolean isFresh(int pcx, int pcz) {
        synchronized (LOCK) {
            return centerKey == keyOf(pcx, pcz)
                    && System.currentTimeMillis() - computedAtMs < CACHE_TTL_MS;
        }
    }

    /**
     * 需要时投递一次后台重算（幂等：已有在途任务或缓存新鲜时什么都不做）。
     * 重算完成后会回主线程调用 {@code onDone}（可为 null）。
     *
     * @return true 表示本次实际投递了任务
     */
    public static boolean refreshIfNeeded(ServerLevel level, ServerPlayer player, int pcx, int pcz,
                                          @Nullable Runnable onDone) {
        if (isFresh(pcx, pcz)) {
            return false;
        }
        if (!COMPUTING.compareAndSet(false, true)) {
            return false; // 已有在途重算，等它完成后的自动推送
        }
        try {
            EXECUTOR.execute(() -> {
                try {
                    List<SixtySecondsLostCitiesStarMap.BuildingRegion> fresh =
                            SixtySecondsLostCitiesStarMap.buildingStarRegions(
                                    level, pcx, pcz, SixtySecondsLostCitiesStarMap.STAR_MAP_SCAN_RADIUS_CHUNKS);
                    synchronized (LOCK) {
                        cached = fresh;
                        centerKey = keyOf(pcx, pcz);
                        computedAtMs = System.currentTimeMillis();
                    }
                    // 回主线程推送更新后的完整数据（玩家已下线时静默放弃）
                    level.getServer().execute(() -> {
                        try {
                            if (player.isAlive() && player.connection != null && onDone != null) {
                                onDone.run();
                            }
                        } catch (Throwable ignored) {
                        }
                    });
                } catch (Throwable ignored) {
                    // 后台重算失败（世界正在卸载等）：降级为保持旧缓存，绝不把异常带崩线程
                } finally {
                    COMPUTING.set(false);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            COMPUTING.set(false);
            return false;
        }
    }

    /** 服务端停止时调用：关闭后台线程池（残留在途任务自然结束）。 */
    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
