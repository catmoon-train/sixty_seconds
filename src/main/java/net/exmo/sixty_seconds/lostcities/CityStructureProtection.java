package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.LostChunkCharacteristics;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 判定原版结构是否可以落地，避免村庄、神殿、要塞等结构覆盖 Lost Cities 生成的城市。
 *
 * <p>Lost Cities 自带 {@code StructureAvoidance}，作用是让<b>城市为已存在的结构让路</b>；
 * 本类解决的是反方向的问题：让<b>原版结构避开城市</b>。两者互补。
 *
 * <p>判定单位是<b>结构的完整包围盒</b>而不是当前区块。大型结构（村庄、要塞）横跨数十个区块，
 * 只看当前区块会漏掉大部分重叠区域，因此这里按包围盒展开成区块范围逐一检查，
 * 任一区块属于城市即判定拒绝。
 *
 * <p>任何异常、缺失信息或超出预算的情况一律<b>放行</b>：宁可让结构正常生成，
 * 也不能因为判定失败而破坏世界生成。
 */
public final class CityStructureProtection {

    /** 包围盒向外扩张的缓冲区块数上限。 */
    public static final int MAX_BUFFER_CHUNKS = 4;

    /** 单次判定最多扫描的区块数，防止超大包围盒拖慢生成。 */
    private static final int MAX_CHUNKS_PER_SCAN = 1024;

    /** 单次判定最多触发的冷计算次数，超出后剩余区块直接放行。 */
    private static final int MAX_COLD_QUERIES_PER_SCAN = 512;

    private static final AtomicLong SCANS = new AtomicLong();
    private static final AtomicLong REJECTS = new AtomicLong();
    private static final AtomicLong ALLOWS = new AtomicLong();
    private static final AtomicLong BUDGET_ABORTS = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    private static volatile boolean enabled = true;

    /** 包围盒向外扩张的缓冲区块数，默认 1 以应对结构包围盒之外的装饰性方块。 */
    private static volatile int bufferChunks = 1;

    private CityStructureProtection() {
    }

    /** 是否启用城市结构保护。关闭后所有结构照常生成。 */
    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** 设置包围盒缓冲区块数，会被限制到 {@code [0, MAX_BUFFER_CHUNKS]}。 */
    public static void setBufferChunks(int value) {
        bufferChunks = Math.max(0, Math.min(MAX_BUFFER_CHUNKS, value));
    }

    public static int bufferChunks() {
        return bufferChunks;
    }

    /**
     * 检查一个结构的包围盒是否与城市重叠。
     *
     * @param provider Lost Cities 维度信息，null 时放行
     * @param box      结构包围盒，null 时放行
     * @param buffer   向外扩张的缓冲区块数，会被限制到 {@code [0, MAX_BUFFER_CHUNKS]}
     * @return 拒绝、放行或未知；{@code complete} 为 false 表示信息不足，调用方应放行且不缓存结果
     */
    public static StructureDecision inspectStructureBox(@Nullable IDimensionInfo provider,
                                                        @Nullable BoundingBox box,
                                                        int buffer) {
        if (!enabled) {
            return StructureDecision.allow();
        }
        if (provider == null || box == null) {
            return StructureDecision.allow();
        }

        SCANS.incrementAndGet();
        ResourceKey<Level> dimension = provider.getType();
        if (dimension == null) {
            return StructureDecision.allow();
        }

        int radius = Math.max(0, Math.min(MAX_BUFFER_CHUNKS, buffer));
        int minChunkX = (box.minX() >> 4) - radius;
        int maxChunkX = (box.maxX() >> 4) + radius;
        int minChunkZ = (box.minZ() >> 4) - radius;
        int maxChunkZ = (box.maxZ() >> 4) + radius;

        long chunkCount = (long) (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
        if (chunkCount > MAX_CHUNKS_PER_SCAN) {
            BUDGET_ABORTS.incrementAndGet();
            return StructureDecision.allow();
        }

        int coldQueries = 0;
        try {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    ChunkCoord coord = new ChunkCoord(dimension, chunkX, chunkZ);

                    LostChunkCharacteristics cached = LostCitiesConcurrency.peekCharacteristics(coord);
                    if (cached != null) {
                        if (cached.isCity) {
                            REJECTS.incrementAndGet();
                            return StructureDecision.reject("city chunk");
                        }
                        continue;
                    }

                    if (coldQueries >= MAX_COLD_QUERIES_PER_SCAN) {
                        // 预算耗尽：剩余区块未知，本次不拒绝，交由后续区块重新判定
                        BUDGET_ABORTS.incrementAndGet();
                        return StructureDecision.unknown();
                    }
                    coldQueries++;

                    LostChunkCharacteristics characteristics = BuildingInfo.getChunkCharacteristics(coord, provider);
                    if (characteristics != null && characteristics.isCity) {
                        REJECTS.incrementAndGet();
                        return StructureDecision.reject("city chunk");
                    }
                }
            }
        } catch (Throwable ignored) {
            FAILURES.incrementAndGet();
            return StructureDecision.allow();
        }

        ALLOWS.incrementAndGet();
        return StructureDecision.allow();
    }

    public static String diagnostics() {
        return "scans=" + SCANS.get() + " rejects=" + REJECTS.get()
                + " allows=" + ALLOWS.get() + " budgetAborts=" + BUDGET_ABORTS.get()
                + " failures=" + FAILURES.get();
    }

    /**
     * 结构落地判定结果。
     *
     * @param reject   是否拒绝该结构落地
     * @param complete 判定是否已完备；false 表示信息不足（例如查询预算耗尽），
     *                 调用方应当放行，且<b>不要</b>缓存该结果，下次重新判定
     * @param reason   判定原因，用于诊断
     */
    public record StructureDecision(boolean reject, boolean complete, String reason) {
        public static StructureDecision allow() {
            return new StructureDecision(false, true, "no city overlap");
        }

        public static StructureDecision unknown() {
            return new StructureDecision(false, false, "insufficient data");
        }

        public static StructureDecision reject(String reason) {
            return new StructureDecision(true, true, reason);
        }
    }
}
