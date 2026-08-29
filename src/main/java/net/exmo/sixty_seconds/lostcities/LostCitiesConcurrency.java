package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.LostChunkCharacteristics;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.setup.Registration;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lost Cities 并发支持：多线程区块生成下的无锁快路径与维度信息解析。
 *
 * <p>Lost Cities 用 {@code synchronized (getDimensionLock(dimension))} 包住
 * {@code BuildingInfo#getChunkCharacteristics} 与 {@code #getCityLevel}。
 * 在并行区块生成环境下所有工作线程会挤在同一把维度监视器上，缓存命中这种
 * 本该无锁的读取也被迫串行，吞吐量随线程数增加反而下降。
 *
 * <p>这里提供一个<b>前置无锁缓存</b>：命中时直接返回结果，完全不进入
 * {@code synchronized} 块；未命中时才回退到 Lost Cities 原本的持锁路径。
 * 这样冷计算仍保持 Lost Cities 原有的串行语义（结果不变、不会重复计算），
 * 而占绝大多数的缓存命中则真正并行。
 *
 * <p>本类不引入任何新的锁或阻塞等待，因此不存在死锁风险。
 */
public final class LostCitiesConcurrency {

    /** 单维度前置缓存的条目上限，超出后整体清空，防止长时间运行累积过多条目。 */
    private static final int MAX_CACHE_ENTRIES = 1 << 16;

    private static final Map<ChunkCoord, LostChunkCharacteristics> CHARACTERISTICS_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<ChunkCoord, Integer> CITY_LEVEL_CACHE =
            new ConcurrentHashMap<>();

    private static final AtomicLong CACHE_HITS = new AtomicLong();
    private static final AtomicLong CACHE_MISSES = new AtomicLong();

    private static volatile boolean enabled = true;
    private static volatile boolean c2meDetected = false;
    private static volatile ExecutorService externalExecutor;

    private LostCitiesConcurrency() {
    }

    /** 前置缓存开关。关闭后备选路径全部失效，行为与 Lost Cities 原版一致。 */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            clearCaches();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** 记录 c2me 是否已加载，以及可复用的外部线程池。 */
    public static void setC2meDetected(boolean detected, @Nullable ExecutorService executor) {
        c2meDetected = detected;
        externalExecutor = executor;
    }

    public static boolean isC2meDetected() {
        return c2meDetected;
    }

    /** 并行区块生成环境下可复用的线程池；c2me 未加载时为 null。 */
    @Nullable
    public static ExecutorService externalExecutor() {
        return externalExecutor;
    }

    /**
     * 读取前置缓存中的区块特征。未命中返回 null，调用方应回退到 Lost Cities 原路径。
     */
    @Nullable
    public static LostChunkCharacteristics peekCharacteristics(ChunkCoord coord) {
        if (!enabled || coord == null) {
            return null;
        }
        LostChunkCharacteristics cached = CHARACTERISTICS_CACHE.get(coord);
        if (cached != null) {
            CACHE_HITS.incrementAndGet();
            return cached;
        }
        CACHE_MISSES.incrementAndGet();
        return null;
    }

    /** 写入前置缓存。仅用于缓存 Lost Cities 原路径已算出的结果。 */
    public static void publishCharacteristics(ChunkCoord coord, LostChunkCharacteristics characteristics) {
        if (!enabled || coord == null || characteristics == null) {
            return;
        }
        if (CHARACTERISTICS_CACHE.size() >= MAX_CACHE_ENTRIES) {
            CHARACTERISTICS_CACHE.clear();
        }
        CHARACTERISTICS_CACHE.put(coord, characteristics);
    }

    @Nullable
    public static Integer peekCityLevel(ChunkCoord coord) {
        if (!enabled || coord == null) {
            return null;
        }
        return CITY_LEVEL_CACHE.get(coord);
    }

    public static void publishCityLevel(ChunkCoord coord, int cityLevel) {
        if (!enabled || coord == null) {
            return;
        }
        if (CITY_LEVEL_CACHE.size() >= MAX_CACHE_ENTRIES) {
            CITY_LEVEL_CACHE.clear();
        }
        CITY_LEVEL_CACHE.put(coord, cityLevel);
    }

    /** 与 Lost Cities 的 {@code BuildingInfo#cleanCache} 保持一致，清空全部前置缓存。 */
    public static void clearCaches() {
        CHARACTERISTICS_CACHE.clear();
        CITY_LEVEL_CACHE.clear();
    }

    public static String diagnostics() {
        return "hits=" + CACHE_HITS.get() + " misses=" + CACHE_MISSES.get()
                + " characteristics=" + CHARACTERISTICS_CACHE.size()
                + " cityLevels=" + CITY_LEVEL_CACHE.size();
    }

    /**
     * 解析指定世界生成维度的 Lost Cities 维度信息。
     * 非 Lost Cities 维度、维度信息尚未就绪或 Lost Cities 缺失时返回 null，
     * 调用方应据此放行而非中断世界生成。
     */
    @Nullable
    public static IDimensionInfo dimensionInfo(WorldGenLevel level) {
        if (level == null) {
            return null;
        }
        try {
            return Registration.LOSTCITY_FEATURE.get().getDimensionInfo(level);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 构造指定维度的区块坐标。 */
    public static ChunkCoord chunkCoord(Level level, int chunkX, int chunkZ) {
        return new ChunkCoord(level.dimension(), chunkX, chunkZ);
    }
}
