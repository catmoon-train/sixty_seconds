package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInformation;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.entity.SixtySecondsNpcEntity;
import net.exmo.sixty_seconds.logic.SixtySecondsNpcSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * LostCities 建筑按种类固定刷 NPC，且<b>只刷一次、打死不补</b>：
 * <ul>
 *   <li><b>安全区</b>（{@code safezone}，{@link SixtySecondsLostCitiesStarMap#SAFE_STAR}）→ 刷 3 个军人 NPC（站桩护卫）；</li>
 *   <li><b>4 类交易建筑</b>（{@code highway_gas_station}/{@code highway_restaurant}/{@code oilrig}/{@code shopping}）
 *       → 各随机刷 1~5 个商人 NPC（站桩交易，自带默认交易表）。</li>
 * </ul>
 *
 * <p><b>时机</b>：监听 {@link ChunkEvent.Load}（区块完全加载、可安全 addFreshEntity 实体）。
 * 世界生成期间（worldgen / PostGen）还没到「加载」阶段，不能放实体，故不在那两处刷。</p>
 *
 * <p><b>只刷一次</b>：用 {@code Level -> Set<ChunkPos>} 记录本世界已处理过的区块；
 * 一个 chunk 只在首次加载时判定一次，之后（重启/重载/玩家往返）都跳过。
 * 配合 NPC 本身的「非战场、身边无人 2 分钟自散」——但站桩 NPC 在有人活动时不会散，
 * 且本类<b>不</b>做任何补刷，所以死了就是死了，符合需求。</p>
 *
 * <p><b>落点</b>：用 {@link SixtySecondsNpcSpawner#findGroundSpot} 在 chunk 盒内找可站立地面，
 * 同建筑多次刷点时散开（不同 XZ），避免叠在一起。</p>
 */
public final class SixtySecondsLostCityNpcGen {

    /** 安全区建筑（文件名，不含命名空间）：固定刷 3 个军人。 */
    private static final Set<String> SAFE_BUILDINGS = Set.of("safezone");

    /** 交易建筑（文件名前缀）：各随机刷 1~5 个商人。 */
    private static final Set<String> MERCHANT_BUILDING_PREFIXES = Set.of(
            "highway_gas_station", "highway_restaurant", "oilrig", "shopping");

    /** 安全区固定刷的军人数量。 */
    private static final int SAFEZONE_SOLDIER_COUNT = 3;
    /** 交易建筑商人数量随机下限 / 上限。 */
    private static final int MERCHANT_MIN = 1;
    private static final int MERCHANT_MAX = 5;

    /** 已处理过的 chunk（按世界记录），保证每个 chunk 只刷一次。 */
    private static final WeakHashMap<Level, Set<ChunkPos>> DONE = new WeakHashMap<>();

    private SixtySecondsLostCityNpcGen() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SixtySecondsLostCityNpcGen::onChunkLoad);
    }

    @SubscribeEvent
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // 仅本模式运行中的世界才刷（与物资箱逻辑一致：局外不生成）
        if (!SixtySecondsMod.isActive(level)) {
            return;
        }
        LevelChunk chunk = (LevelChunk) event.getChunk();
        ChunkPos cp = chunk.getPos();

        // 去重：本 chunk 已处理过则跳过（重启/重载/玩家往返都不重刷）
        Set<ChunkPos> done = DONE.computeIfAbsent(level, k -> java.util.Collections.newSetFromMap(new WeakHashMap<>()));
        if (!done.add(cp)) {
            return;
        }

        String buildingName = buildingNameAt(level, cp);
        if (buildingName == null) {
            return; // 非建筑 chunk：不刷
        }
        String name = buildingName.toLowerCase(Locale.ROOT);

        RandomSource random = level.getRandom();
        AABB box = chunkBox(level, cp);

        if (SAFE_BUILDINGS.contains(name)) {
            for (int i = 0; i < SAFEZONE_SOLDIER_COUNT; i++) {
                spawnGarrisoned(level, box, random, SixtySecondsNpcEntity.Variant.SOLDIER, "default", 6);
            }
            return;
        }

        for (String prefix : MERCHANT_BUILDING_PREFIXES) {
            if (name.startsWith(prefix)) {
                int count = MERCHANT_MIN + random.nextInt(MERCHANT_MAX - MERCHANT_MIN + 1);
                for (int i = 0; i < count; i++) {
                    spawnGarrisoned(level, box, random, SixtySecondsNpcEntity.Variant.MERCHANT, "default", 5);
                }
                return;
            }
        }
    }

    /** 在 chunk 盒内找一个地面点刷一只站桩 NPC（军人/商人）。找不到地面则跳过。 */
    private static void spawnGarrisoned(ServerLevel level, AABB box, RandomSource random,
            SixtySecondsNpcEntity.Variant variant, String profile, int garrisonRadius) {
        BlockPos spot = SixtySecondsNpcSpawner.findGroundSpot(level, box, random);
        if (spot == null) {
            return;
        }
        SixtySecondsNpcSpawner.spawnAt(level, spot, variant, random.nextFloat() * 360.0F, profile, garrisonRadius, -1);
    }

    /** chunk 的世界高度 AABB（XZ 锁定 16×16，Y 用世界可建范围）。 */
    private static AABB chunkBox(Level level, ChunkPos cp) {
        int minX = cp.getMinBlockX();
        int minZ = cp.getMinBlockZ();
        int maxX = cp.getMaxBlockX() + 1;
        int maxZ = cp.getMaxBlockZ() + 1;
        return new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ);
    }

    /**
     * 取该 chunk 中心所在建筑名（不含命名空间的 path）。非城市/非建筑/无 LostCities 返回 null。
     * 复用 {@link SixtySecondsLostCitiesStarMap} 的缓存 info。
     */
    @Nullable
    private static String buildingNameAt(ServerLevel level, ChunkPos cp) {
        ILostCityInformation info = SixtySecondsLostCitiesStarMap.cityInfo(level);
        if (info == null) {
            return null;
        }
        ILostChunkInfo chunk = SixtySecondsLostCitiesStarMap.safeChunkInfo(level, info, cp.x, cp.z);
        if (chunk == null || !chunk.isCity() || chunk.getBuildingId() == null) {
            return null;
        }
        return chunk.getBuildingId().getPath();
    }
}
