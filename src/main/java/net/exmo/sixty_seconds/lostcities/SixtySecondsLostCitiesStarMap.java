package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * LostCities 建筑 → 60 秒「区域星级」自动映射。
 *
 * <p>集中定义 LostCities 各建筑种类对应的危险等级（1..5）。由于 LostCities 的建筑是<b>按区块惰性生成</b>的
 * （无法在创建世界时一次性遍历所有建筑），因此这里采用<b>运行时按坐标反查</b>的方式：当游戏查询某坐标的危险等级时，
 * 通过 LostCities API 判断该坐标是否位于某个已知建筑内，若是则返回其对应星级。这样「创建世界后，建筑自动被划分到星级」——
 * 无需管理员手动用魔杖登记任何建筑。</p>
 *
 * <p>映射规则（与 LostCities 资源目录 {@code data/lostcities/lostcities/} 对应，<b>全部按 JSON 文件名精确登记，不使用任何前缀匹配</b>）：</p>
 * <ul>
 *   <li><b>多区块建筑</b> {@code multibuildings/}（center、townhall、multi1~5、huge1/2 等 14 个）→ <b>5 星</b>；</li>
 *   <li><b>散布建筑</b> {@code buildings/}（按精确文件名登记于 {@code BUILDING_STARS}）：
 *     <ul>
 *       <li>{@code building1}~{@code building8} 通用建筑 → 3 星；</li>
 *       <li>{@code cabin} 小屋 → 1 星；</li>
 *       <li>{@code center00/01/10/11} 城市中心 → 3 星；</li>
 *       <li>{@code highway_gas_station} 加油站 → 3 星；</li>
 *       <li>{@code highway_restaurant}、{@code highway_restaurant_parking} 公路餐厅 → 2 星；</li>
 *       <li>{@code library00/01/10/11} 图书馆、{@code oilrig00/01/10/11} 油井、
 *           {@code radiotower} 无线电塔、{@code shopping00/01/10/11} 与 {@code shopping_open00/01/10/11}
 *           购物中心/露天市场 → 4 星；</li>
 *       <li>{@code town00/01/10/11} 城镇建筑 → 3 星。</li>
 *       <li>{@code safezone} 60秒模组自带安全区建筑（由 {@code export_building} 导出注册）→ <b>0 级（安全区）</b>。</li>
 *       <li>{@code evacuationpoint} 60秒模组自带撤离点建筑（由 {@code export_building} 导出注册）→ <b>不具有星级</b>
 *           （返回 {@link #UNGRADED}），其所在位置被直升机撤离系统识别为撤离点（见 {@link #isEvacuationPoint}）。</li>
 *     </ul></li>
 *   <li><b>建筑部件</b> {@code parts/}（street/rails/bridge 等 207 个零件）不划分星级——它们不是独立建筑，
 *       仅作为建筑/街道的组装件，由所属建筑或街道决定。</li>
 *   <li><b>未登记建筑名</b>（位于某 LostCities 建筑内但本表未列出）→ 返回 {@link #UNGRADED}（-1），即真正「无级别」：
 *       既不计入危险星级（>0），也不当作安全区（0），在等级系统中跳过建筑星级分支、落到门绑定/全局基线。</li>
 * </ul>
 *
 * <p>本类只读取 LostCities 提供的 API（{@code mcjty.lostcities.api.*}），不修改 LostCities 任何逻辑。</p>
 */
public final class SixtySecondsLostCitiesStarMap {

    /**
     * 无星级：该坐标不在任何 LostCities 建筑内（街道/部件/非城市维度），交由后续等级逻辑处理。
     * 注意它只是「没有建筑星级」，仍会被 {@code SixtySecondsAreaLevels} 按岛屿/门绑定/全局基线判定，并非「安全区」。
     */
    public static final int NO_STAR = 0;

    /**
     * 无级别：坐标位于某 LostCities 建筑内，但其建筑名未在本映射表中登记。
     * 这是真正「不具有任何级别」的地方——既不是安全区（≠ 0），也没有危险星级（> 0），
     * 在 {@code SixtySecondsAreaLevels} 中会跳过建筑星级分支、直接落到后续等级逻辑（门绑定/全局基线）。
     */
    public static final int UNGRADED = -1;

    /**
     * 安全区建筑：坐标位于 60秒模组自带的 safezone 等安全区建筑内。注意它使用专属负值而非 0，
     * 以区别于 {@link #NO_STAR}（非建筑也应落入后续逻辑）——{@code SixtySecondsAreaLevels} 见到此标记
     * 会直接返回 0 级（安全区）。
     */
    public static final int SAFE_STAR = -2;

    /**
     * 星图动态加载建筑星级区域时，服务端从玩家当前位置向外扫描的区块半径（仅扫描已加载区块，不强制生成）。
     * 参考海图：服务端从世界生成数据计算区域并下发给客户端。默认 16 区块（≈256 格）半径，
     * 只覆盖玩家附近城区，避免一次性扫描过多区块导致打开星图时卡顿。
     */
    public static final int STAR_MAP_SCAN_RADIUS_CHUNKS = 16;

    /** 多区块建筑统一 5 星。 */
    private static final int MULTI_BUILDING_STAR = 5;
    /** 物资箱生成专用：未登记的城市建筑（UNGRADED）默认给的星级，避免绝大多数建筑无箱。 */
    public static final int DEFAULT_BUILDING_STAR = 3;

    /**
     * 60秒模组自带的安全区建筑集合（建筑文件名，不含命名空间）。这些建筑由 {@code export_building}
     * 导出的结构注册而来，固定为安全区（{@link #SAFE_STAR}），不计入未知建筑的无级别处理。
     */
    private static final Set<String> SAFE_BUILDINGS = Set.of("safezone");

    /**
     * 60秒模组自带的撤离点建筑集合（建筑文件名，不含命名空间）。这些建筑由 {@code export_building}
     * 导出的结构注册而来，作为直升机撤离点候选区域：<b>不具有星级</b>（返回 {@link #UNGRADED}），并在直升机
     * 撤离判定时被识别为撤离区（见 {@link #isEvacuationPoint}）。
     */
    private static final Set<String> EVAC_BUILDINGS = Set.of("evacuationpoint");

    /**
     * 城市建筑「精确名称 → 星级」映射表。键为 LostCities 资源目录
     * {@code data/lostcities/lostcities/buildings/} 下的 JSON 文件名（不含命名空间）。
     * 仅当建筑名与本表键<b>完全一致</b>时才匹配，不使用任何前缀（startsWith）匹配，
     * 以避免新增大楼栋被错误归类。未出现在本表的城市建筑名返回 {@link #UNGRADED}（真正无级别）。
     */
    // 约一半楼栋划为 2 星（蓝）：building1-8 / town00-11 / 高速加油站·餐厅·停车场 / 广播铁塔 / 露天集市
    private static final Map<String, Integer> BUILDING_STARS = Map.ofEntries(
            Map.entry("building1", 2),
            Map.entry("building2", 2),
            Map.entry("building3", 2),
            Map.entry("building4", 2),
            Map.entry("building5", 3),
            Map.entry("building6", 3),
            Map.entry("building7", 3),
            Map.entry("building8", 3),
            Map.entry("cabin", 1),
            Map.entry("center00", 3),
            Map.entry("center01", 3),
            Map.entry("center10", 3),
            Map.entry("center11", 3),
            Map.entry("highway_gas_station", 2),
            Map.entry("highway_restaurant", 2),
            Map.entry("highway_restaurant_parking", 2),
            Map.entry("library00", 3),
            Map.entry("library01", 3),
            Map.entry("library10", 4),
            Map.entry("library11", 4),
            Map.entry("oilrig00", 5),
            Map.entry("oilrig01", 5),
            Map.entry("oilrig10", 5),
            Map.entry("oilrig11", 5),
            Map.entry("radiotower", 5),
            Map.entry("shopping00", 4),
            Map.entry("shopping01", 4),
            Map.entry("shopping10", 4),
            Map.entry("shopping11", 5),
            Map.entry("shopping_open00", 3),
            Map.entry("shopping_open01", 3),
            Map.entry("shopping_open10", 3),
            Map.entry("shopping_open11", 3),
            Map.entry("town00", 2),
            Map.entry("town01", 3),
            Map.entry("town10", 3),
            Map.entry("town11", 2)
    );

    /**
     * {@code ILostCityInformation} 按维度缓存：它在单个 {@code ServerLevel} 生命周期内是稳定的，
     * 而 {@code levelAt}/{@code isSafeZone} 是高频运行时查询（PvP 每次攻击、每次刷怪、每次领箱都走一遍），
     * 避免每次都经 {@code api().getLostInfo(level)} + try/catch 重新取。
     * 用 WeakHashMap 以免阻止世界对象被 GC（维度卸载后自动清理）。
     */
    private static final Map<ServerLevel, ILostCityInformation> INFO_CACHE = new WeakHashMap<>();
    /** 解析为 null（维度暂未就绪 / 非城市维度）时的重试冷却时间戳，避免每 tick 反复调用 getLostInfo，也避免首帧失败就永久失效。 */
    private static final Map<ServerLevel, Long> NULL_RETRY_AT = new WeakHashMap<>();

    /**
     * 撤离点建筑中心坐标缓存（按维度）。世界生成阶段（{@code PostGenCityChunkEvent}）由
     * {@link #registerEvacuationPoint} 登记；撤离点指南针 / 直升机撤离系统直接读取本表，
     * <b>避免在物品使用（主线程 {@code ServerboundUseItemPacket.handle}）时全图扫描 {@code getChunkInfo}</b>——
     * 那种扫描既重又会在未生成区块上阻塞主线程（spark 中表现为 {@code managedBlock}/{@code waitingForTask} 卡顿）。
     * 用 {@link WeakHashMap} 以免阻止世界对象被 GC（维度卸载后自动清理）。
     * 值用 {@link java.util.concurrent.CopyOnWriteArrayList}：写入（生成期）少、读取（右键/每 tick）多，且读时不加锁也更安全。
     */
    private static final Map<ServerLevel, java.util.List<BlockPos>> EVAC_CENTERS = new WeakHashMap<>();
    private static final Object EVAC_LOCK = new Object();

    private SixtySecondsLostCitiesStarMap() {
    }

    /**
     * 世界生成期调用：登记一个撤离点建筑的中心坐标（水平方向用于最近点判定）。
     * 可能在区块生成工作线程上调用，故加 {@link #EVAC_LOCK} 保护。
     */
    public static void registerEvacuationPoint(ServerLevel level, BlockPos center) {
        if (level == null || center == null) {
            return;
        }
        synchronized (EVAC_LOCK) {
            EVAC_CENTERS.computeIfAbsent(level, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(center);
        }
    }

    /**
     * 读取缓存，返回离 {@code playerPos} 最近的撤离点建筑中心（按水平距离），无则返回 {@code null}。
     * 复杂度 O(缓存大小)，纯内存遍历，不触碰任何区块加载 / 世界生成，绝不在主线程阻塞。
     */
    @Nullable
    public static BlockPos nearestEvacuationPoint(ServerLevel level, BlockPos playerPos) {
        java.util.List<BlockPos> list;
        synchronized (EVAC_LOCK) {
            list = EVAC_CENTERS.get(level);
        }
        if (list == null || list.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        for (BlockPos c : list) {
            double dx = c.getX() - playerPos.getX();
            double dz = c.getZ() - playerPos.getZ();
            double d = dx * dx + dz * dz;
            if (d < bestSqr) {
                bestSqr = d;
                best = c;
            }
        }
        return best;
    }


    /**
     * 反查某坐标是否位于 LostCities 已知建筑内，返回对应星级（1..5）；未知建筑名返回 {@link #UNGRADED}（真正无级别）；
     * 非建筑/非城市维度/无 LostCities 时返回 {@link #NO_STAR}（仅表示「无建筑星级」，仍走后续等级逻辑）。
     *
     * <p>该方法为服务端调用（LostCities 的 {@code getChunkInfo} 是高效缓存查询，但禁止在世界生成期间调用——
     * 而 {@code levelAt} 都是游戏运行时查询，符合要求）。LostCities 未安装/未接入时静默返回 0。</p>
     *
     * @param level 服务端世界
     * @param pos   世界绝对坐标
     * @return 1..5 的星级、{@link #UNGRADED}（-1，城市建筑内但名未登记）、或 {@link #NO_STAR}（0，非建筑）
     */
    public static int starAt(ServerLevel level, BlockPos pos) {
        ILostCityInformation info = cachedLostCitiesInfo(level);
        if (info == null) {
            return NO_STAR;
        }
        ILostChunkInfo chunk = info.getChunkInfo(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || !chunk.isCity()) {
            return NO_STAR;
        }
        // 多区块建筑优先判定 5 星（无论其内部 chunk 归属哪个 building part）
        if (chunk.getMultiBuildingInfo() != null) {
            return MULTI_BUILDING_STAR;
        }
        // 普通建筑：按名称映射
        ResourceLocation id = chunk.getBuildingId();
        if (id == null) {
            return NO_STAR;
        }
        return starForBuildingName(id.getPath());
    }

    /**
     * 根据建筑名称（{@code data/lostcities/lostcities/buildings/} 下的 JSON 文件名，不含命名空间）
     * 返回对应星级（1..5）；未知名称返回 {@link #UNGRADED}（城市建筑内但未登记，真正无级别）。
     */
    public static int starForBuildingName(String buildingName) {
        if (buildingName == null) {
            return NO_STAR;
        }
        // 防御：调用方可能误传入带命名空间的全名（如 lostcities:building1），统一取路径部分。
        String name = buildingName.toLowerCase(Locale.ROOT);
        int sep = name.indexOf(':');
        if (sep >= 0) {
            name = name.substring(sep + 1);
        }
        // 60秒模组自带的安全区建筑（由 export_building 导出的结构注册而来）：固定为安全区（SAFE_STAR）。
        if (SAFE_BUILDINGS.contains(name)) {
            return SAFE_STAR;
        }
        // 60秒模组自带的撤离点建筑（由 export_building 导出的结构注册而来）：不具有星级（UNGRADED），
        // 但其所在位置被直升机撤离系统识别为撤离区（见 isEvacuationPoint）。
        if (EVAC_BUILDINGS.contains(name)) {
            return UNGRADED;
        }
        // 精确名称匹配：仅当建筑名与 BUILDING_STARS 中的键完全一致时才返回对应星级。
        Integer star = BUILDING_STARS.get(name);
        if (star != null) {
            return star;
        }
        // 未知建筑名：该坐标确实位于某 LostCities 建筑内，但本表未登记其危险度。
        // 返回 UNGRADED（而非 NO_STAR/星级），让上层把它当作真正「无级别」的地方处理。
        return UNGRADED;
    }

    /**
     * 物资箱生成专用星级（与 {@link #starAt} 的危险等级语义解耦）：
     * <ul>
     *   <li>已知建筑 → 返回其原映射星级（1..5）；</li>
     *   <li>安全区（SAFE_STAR）→ 0，不撒箱；</li>
     *   <li>撤离点（EVAC）→ 0，不撒箱；</li>
     *   <li>其余「位于城市建筑内但未登记」的建筑（UNGRADED，如各类默认建筑、multibuilding 大建筑）
     *       返回 {@link #DEFAULT_BUILDING_STAR}，避免 LostCities 绝大多数建筑类型不在白名单而被整栋跳过、
     *       导致「大多数建筑没有物资箱」。</li>
     * </ul>
     */
    public static int lootStarForBuildingName(String buildingName) {
        int s = starForBuildingName(buildingName);
        if (s == SAFE_STAR) {
            return 0;
        }
        if (s == UNGRADED) {
            return isEvacuationBuildingName(buildingName) ? 0 : DEFAULT_BUILDING_STAR;
        }
        return s;
    }

    private static boolean isEvacuationBuildingName(String buildingName) {
        if (buildingName == null) {
            return false;
        }
        String name = buildingName.toLowerCase(Locale.ROOT);
        int sep = name.indexOf(':');
        if (sep >= 0) {
            name = name.substring(sep + 1);
        }
        return EVAC_BUILDINGS.contains(name);
    }

    /**
     * 判断给定坐标是否位于 60秒模组自带的撤离点建筑（{@code EVAC_BUILDINGS}）内。
     * 用于直升机撤离系统把该建筑所在位置当作撤离点区域。
     *
     * @param level 服务端世界
     * @param pos   世界绝对坐标
     * @return 位于撤离点建筑内返回 true（LostCities 未接入时恒为 false）
     */
    public static boolean isEvacuationPoint(ServerLevel level, BlockPos pos) {
        ILostCityInformation info = cachedLostCitiesInfo(level);
        if (info == null) {
            return false;
        }
        ILostChunkInfo chunk = info.getChunkInfo(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || !chunk.isCity() || chunk.getBuildingId() == null) {
            return false;
        }
        return EVAC_BUILDINGS.contains(chunk.getBuildingId().getPath().toLowerCase(Locale.ROOT));
    }

    /** 获取指定维度的 LostCities 城市信息（带按维度缓存）；该维度不支持城市或 LostCities 未接入时返回 null。 */
    /**
     * 公开访问器：返回该维度（服务端世界）的 LostCities 城市信息，未接入/不支持时返回 null。
     * 供 NPC 生成器等在运行时按坐标反查建筑名使用（带按维度缓存）。
     */
    @Nullable
    public static ILostCityInformation cityInfo(ServerLevel level) {
        return cachedLostCitiesInfo(level);
    }

    @Nullable
    private static ILostCityInformation cachedLostCitiesInfo(ServerLevel level) {
        ILostCityInformation cached = INFO_CACHE.get(level);
        if (cached != null) {
            return cached;
        }
        // 之前解析为 null（维度暂未就绪 / 非城市维度）：未到重试冷却则继续返回 null，避免每 tick 反复调用。
        Long retryAt = NULL_RETRY_AT.get(level);
        if (retryAt != null && level.getGameTime() < retryAt) {
            return null;
        }
        return resolveAndCache(level);
    }

    @Nullable
    private static ILostCityInformation resolveAndCache(ServerLevel level) {
        ILostCityInformation info = null;
        try {
            ILostCities lostCities = SixtySecondsLostCitiesAccess.api();
            if (lostCities != null) {
                info = lostCities.getLostInfo(level);
            }
        } catch (Throwable ignored) {
            // LostCities 不可用（未安装/类路径缺失）：静默降级为无建筑星级。
            info = null;
        }
        if (info != null) {
            INFO_CACHE.put(level, info);   // 仅缓存成功结果
        } else {
            // 解析失败/暂不可用：约 5 秒后重试，避免“首帧未就绪就永久失效”，也不会每 tick 狂调 getLostInfo。
            NULL_RETRY_AT.put(level, level.getGameTime() + 100);
        }
        return info;
    }

    // ------------------------------------------------------------------
    // 建筑显示名：仅登记翻译键后缀，具体文案见语言文件（en_us / zh_cn / zh_tw）。
    // 玩家在星图 / 进入提示中看到的名称由调用方解析为 Component.translatable。
    // ------------------------------------------------------------------
    private static final String BUILDING_LANG = "building.sixty_seconds.sixty_seconds.";

    private static final Map<String, String> BUILDING_DISPLAY = Map.ofEntries(
            Map.entry("building1", "residential_ruin"),
            Map.entry("building2", "residential_ruin"),
            Map.entry("building3", "residential_ruin"),
            Map.entry("building4", "residential_ruin"),
            Map.entry("building5", "residential_ruin"),
            Map.entry("building6", "residential_ruin"),
            Map.entry("building7", "residential_ruin"),
            Map.entry("building8", "residential_ruin"),
            Map.entry("cabin", "cabin"),
            Map.entry("center00", "downtown_ne"),
            Map.entry("center01", "downtown_nw"),
            Map.entry("center10", "downtown_se"),
            Map.entry("center11", "downtown_sw"),
            Map.entry("highway_gas_station", "gas_station"),
            Map.entry("highway_restaurant", "highway_restaurant"),
            Map.entry("highway_restaurant_parking", "highway_parking"),
            Map.entry("library00", "library"),
            Map.entry("library01", "library"),
            Map.entry("library10", "library"),
            Map.entry("library11", "library"),
            Map.entry("oilrig00", "oil_rig"),
            Map.entry("oilrig01", "oil_rig"),
            Map.entry("oilrig10", "oil_rig"),
            Map.entry("oilrig11", "oil_rig"),
            Map.entry("radiotower", "radio_tower"),
            Map.entry("shopping00", "shopping_mall"),
            Map.entry("shopping01", "shopping_mall"),
            Map.entry("shopping10", "shopping_mall"),
            Map.entry("shopping11", "shopping_mall"),
            Map.entry("shopping_open00", "open_market"),
            Map.entry("shopping_open01", "open_market"),
            Map.entry("shopping_open10", "open_market"),
            Map.entry("shopping_open11", "open_market"),
            Map.entry("town00", "townhouse"),
            Map.entry("town01", "townhouse"),
            Map.entry("town10", "townhouse"),
            Map.entry("town11", "townhouse"),
            Map.entry("safezone", "safe_zone"),
            Map.entry("evacuationpoint", "evacuation")
    );

    /** 建筑 id（LostCities 资源文件名）→ 翻译键；未知 id 原样返回（兜底）。 */
    public static String buildingDisplayKey(String id) {
        if (id == null) {
            return "?";
        }
        String suffix = BUILDING_DISPLAY.get(id.toLowerCase(Locale.ROOT));
        return suffix != null ? BUILDING_LANG + suffix : id;
    }

    /** 一个建筑连通区域的几何与星级信息，供星图下发。 */
    public static final class BuildingRegion {
        public final String id;
        public final String displayName;
        public final int star;
        public final int minX, minZ, maxX, maxZ;

        public BuildingRegion(String id, String displayName, int star, int minX, int minZ, int maxX, int maxZ) {
            this.id = id;
            this.displayName = displayName;
            this.star = star;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
        }
    }

    /**
     * 参考海图加载方式：服务端从 LostCities 世界生成数据动态计算「玩家附近建筑星级区域」，返回给星图渲染。
     * 仅扫描<b>已加载</b>区块（{@code level.hasChunk}），不强制区块生成，因此不会在主线程阻塞。
     * 对连通的同名建筑 chunk 做洪泛填充，得到其外接矩形（世界坐标）与星级。
     *
     * @param centerChunkX 玩家所在区块 X
     * @param centerChunkZ 玩家所在区块 Z
     * @param radiusChunks  扫描半径（区块）
     */
    public static List<BuildingRegion> buildingStarRegions(ServerLevel level, int centerChunkX, int centerChunkZ, int radiusChunks) {
        ILostCityInformation info = cityInfo(level);
        List<BuildingRegion> result = new ArrayList<>();
        if (info == null) {
            return result;
        }
        Set<Long> visited = new HashSet<>();
        int r = radiusChunks;
        for (int cx = centerChunkX - r; cx <= centerChunkX + r; cx++) {
            for (int cz = centerChunkZ - r; cz <= centerChunkZ + r; cz++) {
                long key = ((long) cx << 32) | (cz & 0xffffffffL);
                if (!visited.add(key)) {
                    continue; // 已并入某个区域，跳过
                }
                if (!level.hasChunk(cx, cz)) {
                    continue; // 未加载：不强制生成，留待玩家探索后重开星图时再纳入
                }
                ILostChunkInfo c = info.getChunkInfo(cx, cz);
                if (c == null || !c.isCity() || c.getBuildingId() == null) {
                    continue;
                }
                String id = c.getBuildingId().getPath();
                int star = starForBuildingName(id);
                if (star < 1 || star > 5) {
                    continue; // 仅危险度 1~5 的建筑上图（安全区/撤离点等不含星级）
                }
                // 洪泛填充：收集与当前 chunk 连通、且建筑 id 相同的所有已加载 chunk
                int minCX = cx, minCZ = cz, maxCX = cx, maxCZ = cz;
                Deque<long[]> stack = new ArrayDeque<>();
                stack.push(new long[]{cx, cz});
                while (!stack.isEmpty()) {
                    long[] cur = stack.pop();
                    int x = (int) cur[0], z = (int) cur[1];
                    minCX = Math.min(minCX, x);
                    minCZ = Math.min(minCZ, z);
                    maxCX = Math.max(maxCX, x);
                    maxCZ = Math.max(maxCZ, z);
                    int[][] nb = {{x + 1, z}, {x - 1, z}, {x, z + 1}, {x, z - 1}};
                    for (int[] n : nb) {
                        int nx = n[0], nz = n[1];
                        long nk = ((long) nx << 32) | (nz & 0xffffffffL);
                        if (!visited.add(nk)) {
                            continue;
                        }
                        if (!level.hasChunk(nx, nz)) {
                            continue;
                        }
                        ILostChunkInfo nc = info.getChunkInfo(nx, nz);
                        if (nc == null || !nc.isCity() || nc.getBuildingId() == null) {
                            continue;
                        }
                        if (!nc.getBuildingId().getPath().equals(id)) {
                            continue;
                        }
                        stack.push(new long[]{nx, nz});
                    }
                }
                int minX = minCX * 16;
                int minZ = minCZ * 16;
                int maxX = maxCX * 16 + 15;
                int maxZ = maxCZ * 16 + 15;
                result.add(new BuildingRegion(id, buildingDisplayKey(id), star, minX, minZ, maxX, maxZ));
            }
        }
        return result;
    }
}
