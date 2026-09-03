package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.LinkedHashMap;
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
     * 城市建筑「精确名称 → 星级」映射表。
     * <ul>
     *   <li>原版 LostCities / 60秒 自带城区建筑（building1-8、town*、center*、cabin、library*、oilrig*、
     *       shopping*、highway_*、radiotower、safezone、evacuationpoint）：键为<b>不含命名空间</b>的文件名；
     *       这些建筑注册在 {@code lostcities} 命名空间，{@link #starForBuildingName} 在剥离命名空间后按裸名匹配。</li>
     *       即 {@code data/lce/lostcities/...}。故这里的键使用<b>含命名空间的全名</b>（如 {@code lce:aircraftcarrier}），
     *       由 {@link #starForBuildingName} 先按全名匹配，避免与原版同名建筑（若将来引入）混淆。
     * </ul>
     * 仅当建筑名与本表键<b>完全一致</b>时才匹配，不使用任何前缀（startsWith）匹配，以避免新增大楼栋被错误归类。
     * 未出现在本表的城市建筑名返回 {@link #UNGRADED}（真正无级别）。
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
            Map.entry("radiotower", 4),
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
            Map.entry("town11", 2),
            // ---- 第三方自定义建筑：已迁移到 LostCities-extend 数据包（命名空间 lce）----
            // 键统一使用含命名空间全名（lce:<建筑名>），与原版 lostcities 命名空间拆开。
            // 1 星
            Map.entry("lce:subhousel3ya_1_2_combo", 1),
            Map.entry("lce:crashedhelicopter2", 1),
            Map.entry("lce:scatteredhouse2", 1),
            Map.entry("lce:scatteredhouse1", 1),
            // 2 星
            Map.entry("lce:mbd", 5),
            Map.entry("lce:subhousel3ya_3_bend_street", 2),
            Map.entry("lce:subhousel3ya_4_and_5_combo", 2),
            Map.entry("lce:subhousel3ya_3_6_7_8_9_structurebundel", 2),
            Map.entry("lce:floodedmalll3ya", 2),
            Map.entry("lce:radio_tower1", 4),
            Map.entry("lce:watch_tower1", 2),
            Map.entry("lce:radio_tower2", 4),
            // 3 星
            Map.entry("lce:smallshop", 3),
            Map.entry("lce:4buildingsrow", 3),
            Map.entry("lce:gasstation1", 3),
            Map.entry("lce:walmart2", 3),
            Map.entry("lce:shop2", 3),
            Map.entry("lce:l3ya_spruceforest_cabin_1", 3),
            // 4 星
            Map.entry("lce:firestation12", 4),
            Map.entry("lce:observatory", 4),
            Map.entry("lce:shop3", 4),
            Map.entry("lce:bigmall", 4),
            Map.entry("lce:policestation", 4),
            Map.entry("lce:bufschooll3ya", 4),
            Map.entry("lce:aircraftcarrier2", 4),
            Map.entry("lce:aircraftcarrier", 4),
            // 5 星
            Map.entry("lce:tallbuilding2", 5),
            Map.entry("lce:massivebuildingwithhelicopter", 5),
            Map.entry("lce:mediumbuildingczp1", 5),
            Map.entry("lce:skyscraper", 5),
            Map.entry("lce:skyscraper1", 5),
            Map.entry("lce:hugebuilding1", 5),
            Map.entry("lce:hugebuilding2", 5),
            Map.entry("lce:tv_tower", 5),
            Map.entry("lce:wd1imp", 5),
            Map.entry("lce:gianthospital", 5),
            // ---- pomkotsworld 迁移建筑（命名空间 lce2）：按城市风格划分星级 ----
            // 军事基地（base + offshore）= 5 星；下列机场设施按需求单独下调：
            //   机场地面基地         = 3 星（尚可利用）
            //   机场地面基地（废墟） = 2 星（残骸，价值低）
            Map.entry("lce2:base/base_aircraft_ground", 3),
            Map.entry("lce2:base/base_aircraft_ground_heri", 2),
            Map.entry("lce2:base/base_aircraft_hanger", 5),
            Map.entry("lce2:base/base_building_body", 5),
            Map.entry("lce2:base/base_building_medium", 5),
            Map.entry("lce2:base/base_container", 4),
            Map.entry("lce2:base/base_ground_airvent", 5),
            Map.entry("lce2:base/base_ground_ew", 5),
            Map.entry("lce2:base/base_ground_ns", 5),
            Map.entry("lce2:base/base_hanger", 5),
            Map.entry("lce2:base/base_missile_large", 5),
            Map.entry("lce2:base/base_missile_large_multi", 5),
            Map.entry("lce2:base/base_missile_launcher_l", 5),
            Map.entry("lce2:base/base_missile_launcher_m", 5),
            Map.entry("lce2:base/base_space_shuttle", 5),
            Map.entry("lce2:offshore/offshore_empty", 5),
            Map.entry("lce2:offshore/offshore_mega_platform", 5),
            // 工厂（factory）= 4 星
            Map.entry("lce2:factory/factory_black_building", 4),
            Map.entry("lce2:factory/factory_building_main_huge", 5),
            Map.entry("lce2:factory/factory_building_main_large", 4),
            Map.entry("lce2:factory/factory_building_main_medium", 4),
            Map.entry("lce2:factory/factory_building_sub", 4),
            Map.entry("lce2:factory/factory_containers_medium", 4),
            Map.entry("lce2:factory/factory_crane", 4),
            Map.entry("lce2:factory/factory_furnace", 4),
            Map.entry("lce2:factory/factory_garage", 4),
            Map.entry("lce2:factory/factory_process_plant", 4),
            Map.entry("lce2:factory/factory_rack", 4),
            Map.entry("lce2:factory/factory_reactor", 4),
            Map.entry("lce2:factory/factory_tank_medium", 4),
            Map.entry("lce2:factory/factory_tank_sphere", 4),
            Map.entry("lce2:factory/factory_tank_water", 4),
            // 港口（port + ships）= 5 星
            Map.entry("lce2:port/port_tanker", 5),
            Map.entry("lce2:port/port_tanker_small", 5),
            Map.entry("lce2:ships/ships_warship", 5),
            // 采石场（quarry）= 4 星
            Map.entry("lce2:quarry/quarry_dump_e", 4),
            Map.entry("lce2:quarry/quarry_dump_n", 4),
            Map.entry("lce2:quarry/quarry_dump_s", 4),
            Map.entry("lce2:quarry/quarry_dump_w", 4),
            Map.entry("lce2:quarry/quarry_mega_machine", 4),
            Map.entry("lce2:quarry/quarry_office", 4),
            Map.entry("lce2:quarry/quarry_stone_large", 4),
            Map.entry("lce2:quarry/quarry_stone_middle", 4),
            // 太阳能（megasolar）：电池板阵列 2 星；太阳能反应堆按需求上调为 5 星
            Map.entry("lce2:megasolar/megasolar_cell_huge", 2),
            Map.entry("lce2:megasolar/megasolar_cell_medium", 2),
            Map.entry("lce2:megasolar/megasolar_reactor", 5),
            // 标准（scattered 多栋建筑）：通信天线 5 星、集装箱 4 星、其余 3 星
            Map.entry("lce2:scattered/scattered_antenna", 5),
            Map.entry("lce2:scattered/scattered_container", 4),
            Map.entry("lce2:scattered/scattered_cube", 3),
            Map.entry("lce2:scattered/scattered_farmland", 3),
            Map.entry("lce2:scattered/scattered_garage", 3),
            Map.entry("lce2:scattered/scattered_gasstand", 3),
            Map.entry("lce2:scattered/scattered_hanger", 3),
            Map.entry("lce2:scattered/scattered_lighthouse", 3),
            Map.entry("lce2:scattered/scattered_minifactory", 3),
            Map.entry("lce2:scattered/scattered_radiotower", 4),
            Map.entry("lce2:scattered/scattered_scaffold", 3),
            Map.entry("lce2:scattered/scattered_telescope", 3),
            Map.entry("lce2:scattered/scattered_watertank", 3),
            Map.entry("lce2:scattered/wind_mill", 3),
            // 散落建筑（世界 loose scattered）：与 scattered 子目录版保持一致（天线 5 星、集装箱 4 星）
            Map.entry("lce2:scattered_antenna", 5),
            Map.entry("lce2:scattered_container", 4),
            Map.entry("lce2:scattered_cube", 3),
            Map.entry("lce2:scattered_farmland", 3),
            Map.entry("lce2:scattered_garage", 3),
            Map.entry("lce2:scattered_gasstand", 3),
            Map.entry("lce2:scattered_hanger", 3),
            Map.entry("lce2:scattered_lighthouse", 3),
            Map.entry("lce2:scattered_minifactory", 3),
            Map.entry("lce2:scattered_radiotower", 4),
            Map.entry("lce2:scattered_scaffold", 3),
            Map.entry("lce2:scattered_telescope", 3),
            Map.entry("lce2:scattered_watertank", 3),
            Map.entry("lce2:wind_mill", 3),
            Map.entry("lce2:warship", 3),
            Map.entry("lce2:mega_platform", 3)
    );

    /**
     * lostcities-modern-tweaks（命名空间 {@code lcmt}）建筑的独立星级映射表。
     *
     * <p>评级规则（与 {@code citystyle_common.json} 的生成权重 factor 对应）：</p>
     * <ul>
     *   <li>住宅楼 {@code building1}~{@code building8}：factor &lt; 0.3 → 3 星，factor &gt;= 0.3 → 2 星；</li>
     *   <li>{@code cabin} 小屋 → 1 星；</li>
     *   <li>{@code center/…}（数据中心）、{@code factory/…}（工厂）→ 5 星；</li>
     *   <li>{@code library/…}、{@code shopping/…}、{@code shopping/shopping_open…}、{@code townhall/…} → 4 星。</li>
     * </ul>
     */
    private static final Map<String, Integer> LCMT_BUILDING_STARS = Map.ofEntries(
            // 住宅楼：factor 0.3/0.4 → 2 星，0.2 → 3 星
            Map.entry("lcmt:building1", 2),
            Map.entry("lcmt:building2", 2),
            Map.entry("lcmt:building3", 3),
            Map.entry("lcmt:building4", 3),
            Map.entry("lcmt:building5", 2),
            Map.entry("lcmt:building6", 3),
            Map.entry("lcmt:building7", 3),
            Map.entry("lcmt:building8", 2),
            // 小屋
            Map.entry("lcmt:cabin", 1),
            // 数据中心：5 星
            Map.entry("lcmt:center/center00", 5),
            Map.entry("lcmt:center/center01", 5),
            Map.entry("lcmt:center/center10", 5),
            Map.entry("lcmt:center/center11", 5),
            // 工厂：5 星
            Map.entry("lcmt:factory/factory_00", 5),
            Map.entry("lcmt:factory/factory_01", 5),
            Map.entry("lcmt:factory/factory_02", 5),
            Map.entry("lcmt:factory/factory_10", 5),
            Map.entry("lcmt:factory/factory_11", 5),
            Map.entry("lcmt:factory/factory_12", 5),
            // 图书馆：4 星
            Map.entry("lcmt:library/library00", 4),
            Map.entry("lcmt:library/library01", 4),
            Map.entry("lcmt:library/library10", 4),
            Map.entry("lcmt:library/library11", 4),
            // 购物中心：4 星
            Map.entry("lcmt:shopping/shopping00", 4),
            Map.entry("lcmt:shopping/shopping01", 4),
            Map.entry("lcmt:shopping/shopping10", 4),
            Map.entry("lcmt:shopping/shopping11", 4),
            // 露天市场：4 星
            Map.entry("lcmt:shopping/shopping_open00", 4),
            Map.entry("lcmt:shopping/shopping_open01", 4),
            Map.entry("lcmt:shopping/shopping_open10", 4),
            Map.entry("lcmt:shopping/shopping_open11", 4),
            // 市政厅：4 星
            Map.entry("lcmt:townhall/town00", 4),
            Map.entry("lcmt:townhall/town01", 4),
            Map.entry("lcmt:townhall/town10", 4),
            Map.entry("lcmt:townhall/town11", 4)
    );

    /**
     * lostcities-modern-tweaks（lcmt）的<b>多区块建筑（multibuilding）</b>星级映射表。
     *
     * <p>lcmt 的功能型建筑（数据中心/工厂/图书馆/购物中心/露天市场/市政厅）在实际生成时都是通过
     * multibuilding 组合的（见 {@code citystyle_common.json} 的 {@code multibuildings} 段），
     * 此时 {@code ILostChunkInfo#getMultiBuildingInfo()} 非空，需要按 multibuilding 类型
     * （{@code buildingType()}）区分，否则会全部落到默认的 5 星。</p>
     */
    private static final Map<String, Integer> LCMT_MULTI_BUILDING_STARS = Map.ofEntries(
            Map.entry("lcmt:center", 5),
            Map.entry("lcmt:factory", 5),
            Map.entry("lcmt:library", 4),
            Map.entry("lcmt:shopping", 4),
            Map.entry("lcmt:shopping_open", 4),
            Map.entry("lcmt:townhall", 4)
    );

    /**
     * DeceasedCraft（命名空间 {@code deceasedcraft}）<b>普通建筑</b>的星级映射表。
     *
     * <p>评级依据官方 Wiki 的区域划分（{@code deceasedcraft.wiki.gg}）：</p>
     * <ul>
     *   <li><b>Residential（郊区住宅区，起始点、低危）</b>：住宅 A–D、小型公寓 A–D（含损毁）、
     *       加油站、快餐店、服装店、五金店、寿司餐厅、树屋/小屋/餐车 → 1~2 星；</li>
     *   <li><b>City（城市，中高危）</b>：酒店 A–D、办公楼 1–3、高档服饰店、仓库 → 3 星；
     *       办公楼 4/5、城市银行、警察分局 → 4 星；</li>
     *   <li><b>Wasteland / 工业（高危）</b>：工厂 A/B、加工车间 → 4 星。</li>
     * </ul>
     *
     * <p>DeceasedCraft 的 <b>multibuilding（多区块建筑群）不需要在此登记</b>——
     * 它们走 {@link #starForMultiBuilding} 的默认 {@link #MULTI_BUILDING_STAR}（5 星），
     * 与「多区块建筑默认 5 星」的规则一致。</p>
     */
    private static final Map<String, Integer> DC_BUILDING_STARS = Map.ofEntries(
            // ---- 1 星：花店 / 树屋 / 散落小屋 / 餐车 / 村舍（几乎无危险）----
            Map.entry("deceasedcraft:building_flowershop", 1),
            Map.entry("deceasedcraft:scattered_cabin_1", 1),
            Map.entry("deceasedcraft:scattered_cabin_2", 1),
            Map.entry("deceasedcraft:scattered_cabin_3", 1),
            Map.entry("deceasedcraft:scattered_treehouse_1", 1),
            Map.entry("deceasedcraft:scattered_foodtruck_1", 1),
            Map.entry("deceasedcraft:villager_1", 1),
            // ---- 2 星：郊区住宅 / 小型公寓（含损毁）/ 普通商铺 / 加油站 / 损毁仓库 / 幸存者藏身处 ----
            Map.entry("deceasedcraft:building_residentiala", 2),
            Map.entry("deceasedcraft:building_residentialb", 2),
            Map.entry("deceasedcraft:building_residentialc", 2),
            Map.entry("deceasedcraft:building_residentiald", 2),
            Map.entry("deceasedcraft:building_apartmentsmalla", 2),
            Map.entry("deceasedcraft:building_apartmentsmallabroken", 2),
            Map.entry("deceasedcraft:building_apartmentsmallb", 2),
            Map.entry("deceasedcraft:building_apartmentsmallbbroken", 2),
            Map.entry("deceasedcraft:building_apartmentsmallc", 2),
            Map.entry("deceasedcraft:building_apartmentsmallcbroken", 2),
            Map.entry("deceasedcraft:building_apartmentsmalld", 2),
            Map.entry("deceasedcraft:building_clothstore", 2),
            Map.entry("deceasedcraft:building_fastfood", 2),
            Map.entry("deceasedcraft:building_gasstation", 2),
            Map.entry("deceasedcraft:building_hardwarestorece", 2),
            Map.entry("deceasedcraft:building_hardwarestorecedamaged", 2),
            Map.entry("deceasedcraft:building_hardwarestoreie", 2),
            Map.entry("deceasedcraft:building_restaurantsushi", 2),
            Map.entry("deceasedcraft:building_warehouseadamaged", 2),
            Map.entry("deceasedcraft:building_warehousebdamaged", 2),
            Map.entry("deceasedcraft:scattered_hideout_1", 2),
            // ---- 3 星：酒店 / 办公楼 1–3 / 高档服饰店 / 仓库 / 废土加油站 / 损毁工厂 ----
            Map.entry("deceasedcraft:building_hotela", 3),
            Map.entry("deceasedcraft:building_hotelb", 3),
            Map.entry("deceasedcraft:building_hotelc", 3),
            Map.entry("deceasedcraft:building_hoteld", 3),
            Map.entry("deceasedcraft:building_office1a", 3),
            Map.entry("deceasedcraft:building_office1b", 3),
            Map.entry("deceasedcraft:building_office2a", 3),
            Map.entry("deceasedcraft:building_office2b", 3),
            Map.entry("deceasedcraft:building_office3a", 3),
            Map.entry("deceasedcraft:building_office3b", 3),
            Map.entry("deceasedcraft:building_office3c", 3),
            Map.entry("deceasedcraft:building_premiumclothstore", 3),
            Map.entry("deceasedcraft:building_warehousea", 3),
            Map.entry("deceasedcraft:building_warehouseb", 3),
            Map.entry("deceasedcraft:building_wastelandgasstation", 3),
            Map.entry("deceasedcraft:building_factorycdamaged", 3),
            // ---- 4 星：办公楼 4/5 / 城市银行 / 警察分局 / 工厂 A/B / 加工车间（高价值、重感染）----
            Map.entry("deceasedcraft:building_office4", 4),
            Map.entry("deceasedcraft:building_office5", 4),
            Map.entry("deceasedcraft:building_officebank", 4),
            Map.entry("deceasedcraft:building_policeoffice1", 4),
            Map.entry("deceasedcraft:building_factorya", 4),
            Map.entry("deceasedcraft:building_factoryb", 4),
            Map.entry("deceasedcraft:building_processingunita", 4)
    );

    /**
     * DeceasedCraft 建筑（含 multibuilding 建筑群）的显示名映射表：
     * 键为含命名空间的全名，值为语言文件 {@code building.sixty_seconds.sixty_seconds.<后缀>} 的后缀。
     * 没有这张表时，星图 / 进入提示会直接显示 {@code deceasedcraft:multi_hospital} 这类未翻译的原始 id。
     */
    private static final Map<String, String> DC_BUILDING_DISPLAY = Map.ofEntries(
            // ---- multibuilding 建筑群（星图按父 id 出图，必须登记父 id）----
            Map.entry("deceasedcraft:multi_alienresearch", "multi_alienresearch"),
            Map.entry("deceasedcraft:multi_apartmentmedium", "multi_apartmentmedium"),
            Map.entry("deceasedcraft:multi_arcfurnace", "multi_arcfurnace"),
            Map.entry("deceasedcraft:multi_cabin1", "multi_cabin1"),
            Map.entry("deceasedcraft:multi_cabin2", "multi_cabin2"),
            Map.entry("deceasedcraft:multi_cabin3", "multi_cabin3"),
            Map.entry("deceasedcraft:multi_cafe", "multi_cafe"),
            Map.entry("deceasedcraft:multi_campsite1", "multi_campsite1"),
            Map.entry("deceasedcraft:multi_casino", "multi_casino"),
            Map.entry("deceasedcraft:multi_communitycenter", "multi_communitycenter"),
            Map.entry("deceasedcraft:multi_convenientstore", "multi_convenientstore"),
            Map.entry("deceasedcraft:multi_courtyardofficetower", "multi_courtyardofficetower"),
            Map.entry("deceasedcraft:multi_farmhouse1", "multi_farmhouse1"),
            Map.entry("deceasedcraft:multi_filmworkstower", "multi_filmworkstower"),
            Map.entry("deceasedcraft:multi_flatapartment", "multi_flatapartment"),
            Map.entry("deceasedcraft:multi_flatlarge", "multi_flatlarge"),
            Map.entry("deceasedcraft:multi_flatmedium", "multi_flatmedium"),
            Map.entry("deceasedcraft:multi_gallery", "multi_gallery"),
            Map.entry("deceasedcraft:multi_gasstation", "multi_gasstation"),
            Map.entry("deceasedcraft:multi_gunstore", "multi_gunstore"),
            Map.entry("deceasedcraft:multi_hospital", "multi_hospital"),
            Map.entry("deceasedcraft:multi_hospitaldamaged", "multi_hospitaldamaged"),
            Map.entry("deceasedcraft:multi_laboratory", "multi_laboratory"),
            Map.entry("deceasedcraft:multi_militarybase", "multi_militarybase"),
            Map.entry("deceasedcraft:multi_militaryradiocamp", "multi_militaryradiocamp"),
            Map.entry("deceasedcraft:multi_nuclearpowerplant", "multi_nuclearpowerplant"),
            Map.entry("deceasedcraft:multi_oasiscondo", "multi_oasiscondo"),
            Map.entry("deceasedcraft:multi_officethefirst", "multi_officethefirst"),
            Map.entry("deceasedcraft:multi_planecrash1", "multi_planecrash1"),
            Map.entry("deceasedcraft:multi_policeapartment", "multi_policeapartment"),
            Map.entry("deceasedcraft:multi_policestation", "multi_policestation"),
            Map.entry("deceasedcraft:multi_polyclinic", "multi_polyclinic"),
            Map.entry("deceasedcraft:multi_pumpjack", "multi_pumpjack"),
            Map.entry("deceasedcraft:multi_residentiale", "multi_residentiale"),
            Map.entry("deceasedcraft:multi_residentialf", "multi_residentialf"),
            Map.entry("deceasedcraft:multi_residentialsurvivor", "multi_residentialsurvivor"),
            Map.entry("deceasedcraft:multi_storageyard", "multi_storageyard"),
            Map.entry("deceasedcraft:multi_sunkenclub", "multi_sunkenclub"),
            Map.entry("deceasedcraft:multi_sunkenmonumentpark", "multi_sunkenmonumentpark"),
            Map.entry("deceasedcraft:multi_taigaresidence", "multi_taigaresidence"),
            Map.entry("deceasedcraft:multi_terraceplaza", "multi_terraceplaza"),
            Map.entry("deceasedcraft:multi_thering", "multi_thering"),
            Map.entry("deceasedcraft:multi_timbertower", "multi_timbertower"),
            Map.entry("deceasedcraft:multi_verticalschool", "multi_verticalschool"),
            Map.entry("deceasedcraft:multi_workerquarters", "multi_workerquarters"),
            Map.entry("deceasedcraft:multi_workshop", "multi_workshop"),
            // ---- 普通建筑 ----
            Map.entry("deceasedcraft:building_apartmentsmalla", "building_apartmentsmalla"),
            Map.entry("deceasedcraft:building_apartmentsmallabroken", "building_apartmentsmallabroken"),
            Map.entry("deceasedcraft:building_apartmentsmallb", "building_apartmentsmallb"),
            Map.entry("deceasedcraft:building_apartmentsmallbbroken", "building_apartmentsmallbbroken"),
            Map.entry("deceasedcraft:building_apartmentsmallc", "building_apartmentsmallc"),
            Map.entry("deceasedcraft:building_apartmentsmallcbroken", "building_apartmentsmallcbroken"),
            Map.entry("deceasedcraft:building_apartmentsmalld", "building_apartmentsmalld"),
            Map.entry("deceasedcraft:building_clothstore", "building_clothstore"),
            Map.entry("deceasedcraft:building_factorya", "building_factorya"),
            Map.entry("deceasedcraft:building_factoryb", "building_factoryb"),
            Map.entry("deceasedcraft:building_factorycdamaged", "building_factorycdamaged"),
            Map.entry("deceasedcraft:building_fastfood", "building_fastfood"),
            Map.entry("deceasedcraft:building_flowershop", "building_flowershop"),
            Map.entry("deceasedcraft:building_gasstation", "building_gasstation"),
            Map.entry("deceasedcraft:building_hardwarestorece", "building_hardwarestorece"),
            Map.entry("deceasedcraft:building_hardwarestorecedamaged", "building_hardwarestorecedamaged"),
            Map.entry("deceasedcraft:building_hardwarestoreie", "building_hardwarestoreie"),
            Map.entry("deceasedcraft:building_hotela", "building_hotela"),
            Map.entry("deceasedcraft:building_hotelb", "building_hotelb"),
            Map.entry("deceasedcraft:building_hotelc", "building_hotelc"),
            Map.entry("deceasedcraft:building_hoteld", "building_hoteld"),
            Map.entry("deceasedcraft:building_office1a", "building_office1a"),
            Map.entry("deceasedcraft:building_office1b", "building_office1b"),
            Map.entry("deceasedcraft:building_office2a", "building_office2a"),
            Map.entry("deceasedcraft:building_office2b", "building_office2b"),
            Map.entry("deceasedcraft:building_office3a", "building_office3a"),
            Map.entry("deceasedcraft:building_office3b", "building_office3b"),
            Map.entry("deceasedcraft:building_office3c", "building_office3c"),
            Map.entry("deceasedcraft:building_office4", "building_office4"),
            Map.entry("deceasedcraft:building_office5", "building_office5"),
            Map.entry("deceasedcraft:building_officebank", "building_officebank"),
            Map.entry("deceasedcraft:building_policeoffice1", "building_policeoffice1"),
            Map.entry("deceasedcraft:building_premiumclothstore", "building_premiumclothstore"),
            Map.entry("deceasedcraft:building_processingunita", "building_processingunita"),
            Map.entry("deceasedcraft:building_residentiala", "building_residentiala"),
            Map.entry("deceasedcraft:building_residentialb", "building_residentialb"),
            Map.entry("deceasedcraft:building_residentialc", "building_residentialc"),
            Map.entry("deceasedcraft:building_residentiald", "building_residentiald"),
            Map.entry("deceasedcraft:building_restaurantsushi", "building_restaurantsushi"),
            Map.entry("deceasedcraft:building_warehousea", "building_warehousea"),
            Map.entry("deceasedcraft:building_warehouseadamaged", "building_warehouseadamaged"),
            Map.entry("deceasedcraft:building_warehouseb", "building_warehouseb"),
            Map.entry("deceasedcraft:building_warehousebdamaged", "building_warehousebdamaged"),
            Map.entry("deceasedcraft:building_wastelandgasstation", "building_wastelandgasstation"),
            Map.entry("deceasedcraft:scattered_cabin_1", "scattered_cabin_1"),
            Map.entry("deceasedcraft:scattered_cabin_2", "scattered_cabin_2"),
            Map.entry("deceasedcraft:scattered_cabin_3", "scattered_cabin_3"),
            Map.entry("deceasedcraft:scattered_foodtruck_1", "scattered_foodtruck_1"),
            Map.entry("deceasedcraft:scattered_hideout_1", "scattered_hideout_1"),
            Map.entry("deceasedcraft:scattered_treehouse_1", "scattered_treehouse_1"),
            Map.entry("deceasedcraft:villager_1", "villager_1")
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
     * 每维度、按区块坐标缓存 {@code getChunkInfo} 结果（有界 LRU）。
     * LostCities 的 {@code getChunkInfo} 是<b>同步且较重</b>的生成方法：在<b>未加载</b>区块上调用会触发区块生成并等待
     * （spark 中表现为 {@code managedBlock}/{@code waitingForTask}，即主线程/服务端线程卡死）。
     * {@code LostCityTodoTreeGuard}（查询前先 {@code hasChunk} 守卫，不触发未加载区块的生成
     * 与 {@code FeatureCache}/{@code LostCitiesCacheBridge}（按区块缓存 {@code getChunkInfo} 结果，避免重复重算），
     * 这里统一收口为 {@link #safeChunkInfo}：① 未加载区块直接返回 null，绝不调用 {@code getChunkInfo}；
     * ② 已加载区块的结果缓存复用。LostCities 城市数据由种子确定性生成，同坐标缓存不会失真。
     */
    private static final int CHUNK_INFO_CAPACITY = 4096;
    /**
     * 外层用 {@link WeakHashMap} 弱键（维度卸载后即可被 GC，避免强引用 ServerLevel 造成内存泄漏），
     * 与文件内 {@code INFO_CACHE}/{@code NULL_RETRY_AT}/{@code EVAC_CENTERS} 及 LC2H 的缓存生命周期策略一致。
     * 内层为每维度的有界 LRU（见 {@link #safeChunkInfo}）。
     */
    private static final Map<ServerLevel, Map<Long, ILostChunkInfo>> CHUNK_INFO_CACHE =
            Collections.synchronizedMap(new WeakHashMap<ServerLevel, Map<Long, ILostChunkInfo>>());

    private static long chunkInfoKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    /**
     * 安全的 {@code getChunkInfo} 收口：未加载区块不调用（防卡死），已加载区块结果缓存（防重算）。
     * 仅用于服务端已加载世界；调用方应已持有 {@code ILostCityInformation}（见 {@link #cityInfo}）。
     */
    @Nullable
    public static ILostChunkInfo safeChunkInfo(Level level, ILostCityInformation info, int cx, int cz) {
        if (info == null || !level.hasChunk(cx, cz)) {
            return null; // ① 防卡死核心：未加载区块跳过，绝不触发 getChunkInfo 引发区块生成/等待
        }
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        ServerLevel sl = (ServerLevel) level;
        long key = chunkInfoKey(cx, cz);
        // ② 已加载区块：先查缓存，命中直接返回（外层弱键 Map 的 check-then-act 需外部同步）
        Map<Long, ILostChunkInfo> cache;
        synchronized (CHUNK_INFO_CACHE) {
            cache = CHUNK_INFO_CACHE.get(sl);
            if (cache == null) {
                cache = Collections.synchronizedMap(new LruCache(CHUNK_INFO_CAPACITY));
                CHUNK_INFO_CACHE.put(sl, cache);
            }
        }
        ILostChunkInfo hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        ILostChunkInfo chunk;
        try {
            chunk = info.getChunkInfo(cx, cz);
        } catch (Throwable t) {
            return null; // LostCities 异常：降级为无数据，不把异常抛给调用方线程
        }
        if (chunk != null) {
            cache.put(key, chunk);
        }
        return chunk;
    }

    /** 有界 LRU：超过容量时淘汰最久未访问项；线程安全由外层 {@link Collections#synchronizedMap} 保证。 */
    private static final class LruCache extends LinkedHashMap<Long, ILostChunkInfo> {
        private final int cap;
        LruCache(int cap) {
            super(cap + 1, 0.75f, true);
            this.cap = cap;
        }
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ILostChunkInfo> e) {
            return size() > cap;
        }
    }

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
        ILostChunkInfo chunk = safeChunkInfo(level, info, pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null || !chunk.isCity()) {
            return NO_STAR;
        }
        // 多区块建筑按 multibuilding 的 id 判定星级（不是子块 id，见 starForMultiBuilding 注释）；
        // lcmt 的功能型 multibuilding 细分为 4/5 星，LCE/LCE2 按 BUILDING_STARS 登记的父名细分，其余兜底 5 星。
        ILostChunkInfo.MultiBuildingInfo multiBuilding = chunk.getMultiBuildingInfo();
        if (multiBuilding != null) {
            return starForMultiBuilding(multiBuilding.buildingType().toString());
        }
        // 普通建筑：按名称映射
        ResourceLocation id = chunk.getBuildingId();
        if (id == null) {
            return NO_STAR;
        }
        return starForBuildingName(id.toString());
    }

    /**
     * 根据建筑名称返回对应星级（1..5）；未知名称返回 {@link #UNGRADED}（城市建筑内但未登记，真正无级别）。
     *
     * <p>优先按完整 id（含命名空间，如 {@code lcmt:building1}）匹配 lostcities-modern-tweaks 建筑；
     * 匹配不到时再剥离命名空间，按原版 lostcities 建筑名（不含命名空间，如 {@code building1}）匹配。</p>
     */
    public static int starForBuildingName(String buildingName) {
        if (buildingName == null) {
            return NO_STAR;
        }
        String name = buildingName.toLowerCase(Locale.ROOT);
        // 先按完整 id（含命名空间）匹配 lostcities-modern-tweaks（lcmt）建筑：
        // 其建筑名与原版 lostcities 同名（building1~8、cabin）或位于子目录（center/…、factory/…），
        // 必须保留命名空间才能与原版（lostcities:…）拆开评级。
        Integer lcmtStar = LCMT_BUILDING_STARS.get(name);
        if (lcmtStar != null) {
            return lcmtStar;
        }
        // 按完整 id（含命名空间）匹配 LostCities-extend（lce）自定义建筑：
        // 这些建筑已从 lostcities 命名空间迁移到 lce 命名空间（见 BUILDING_STARS 注释），
        // 键为全名（如 lce:aircraftcarrier），必须保留命名空间才能正确评级。
        Integer lceStar = BUILDING_STARS.get(name);
        if (lceStar != null) {
            return lceStar;
        }
        // 按完整 id（含命名空间）匹配 DeceasedCraft（deceasedcraft）建筑：
        // 普通建筑按 DC_BUILDING_STARS 评级；其 multibuilding 建筑群走
        // starForMultiBuilding 的默认 5 星，不经此处。
        Integer dcStar = DC_BUILDING_STARS.get(name);
        if (dcStar != null) {
            return dcStar;
        }
        // 防御：调用方可能误传入带命名空间的全名（如 lostcities:building1），统一取路径部分。
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
     * 多区块建筑（multibuilding）的星级（1..5）。
     *
     * <p><b>为什么不能用子块的 buildingId 查表</b>：LCE/LCE2 数据包的建筑<b>全部</b>以 multibuilding 形式
     * 组织——每一个 multibuilding（如 {@code lce:skyscraper}）由若干 {@code _行_列} 子块构成
     * （{@code lce:skyscraper_0_0}、{@code _0_1}、{@code _1_0}、{@code _1_1}）。
     * LostCities 的 {@code ILostChunkInfo#getBuildingId()} 返回的是<b>子块</b> id，
     * 而 {@code #getMultiBuildingInfo()#buildingType()} 返回的是<b>父</b> id。
     * {@link #BUILDING_STARS} 中 {@code lce:*} / {@code lce2:*} 登记的是<b>父名</b>，
     * 因此必须用 multibuilding id 查表，拿子块 id 精确匹配永远命中不了。
     *
     * <p>lcmt 的功能型 multibuilding 细分为 4/5 星（见 {@link #LCMT_MULTI_BUILDING_STARS}）；
     * 其余未登记的 multibuilding 统一 {@link #MULTI_BUILDING_STAR}。
     */
    public static int starForMultiBuilding(String multiBuildingId) {
        if (multiBuildingId == null) {
            return MULTI_BUILDING_STAR;
        }
        String name = multiBuildingId.toLowerCase(Locale.ROOT);
        Integer lcmtStar = LCMT_MULTI_BUILDING_STARS.get(name);
        if (lcmtStar != null) {
            return lcmtStar;
        }
        Integer star = BUILDING_STARS.get(name);
        if (star != null) {
            return star;
        }
        return MULTI_BUILDING_STAR;
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

    /**
     * 物资箱生成专用星级（多区块建筑版本）。
     *
     * <p>LCE/LCE2 的建筑全部是 multibuilding，{@code BuildingInfo#getBuildingId()} 给的是子块名
     * （{@code lce:skyscraper_0_0}），若拿它走 {@link #lootStarForBuildingName} 会因精确匹配失败而落到
     * {@link #DEFAULT_BUILDING_STAR} 兜底，导致整栋楼的星级细分失效（全部一样）。
     * 这里改用 multibuilding 父名（{@code lce:skyscraper}）查表，与 {@link #starForMultiBuilding} 一致。
     */
    public static int lootStarForMultiBuilding(String multiBuildingId) {
        int s = starForMultiBuilding(multiBuildingId);
        if (s < 1) {
            return DEFAULT_BUILDING_STAR;
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
        ILostChunkInfo chunk = safeChunkInfo(level, info, pos.getX() >> 4, pos.getZ() >> 4);
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
            Map.entry("evacuationpoint", "evacuation"),
            // ---- 第三方自定义建筑显示名：已迁移到 LostCities-extend 数据包（命名空间 lce）----
            // 键用含命名空间全名（lce:<建筑名>）；翻译键后缀保持原建筑名不变（building.sixty_seconds.sixty_seconds.<名>）。
            Map.entry("lce:subhousel3ya_1_2_combo", "subhousel3ya_1_2_combo"),
            Map.entry("lce:crashedhelicopter2", "crashedhelicopter2"),
            Map.entry("lce:scatteredhouse2", "scatteredhouse2"),
            Map.entry("lce:scatteredhouse1", "scatteredhouse1"),
            Map.entry("lce:mbd", "mbd"),
            Map.entry("lce:subhousel3ya_3_bend_street", "subhousel3ya_3_bend_street"),
            Map.entry("lce:subhousel3ya_4_and_5_combo", "subhousel3ya_4_and_5_combo"),
            Map.entry("lce:subhousel3ya_3_6_7_8_9_structurebundel", "subhousel3ya_3_6_7_8_9_structurebundel"),
            Map.entry("lce:floodedmalll3ya", "floodedmalll3ya"),
            Map.entry("lce:radio_tower1", "radio_tower1"),
            Map.entry("lce:watch_tower1", "watch_tower1"),
            Map.entry("lce:radio_tower2", "radio_tower2"),
            Map.entry("lce:smallshop", "smallshop"),
            Map.entry("lce:4buildingsrow", "4buildingsrow"),
            Map.entry("lce:gasstation1", "gasstation1"),
            Map.entry("lce:walmart2", "walmart2"),
            Map.entry("lce:shop2", "shop2"),
            Map.entry("lce:l3ya_spruceforest_cabin_1", "l3ya_spruceforest_cabin_1"),
            Map.entry("lce:firestation12", "firestation12"),
            Map.entry("lce:observatory", "observatory"),
            Map.entry("lce:shop3", "shop3"),
            Map.entry("lce:bigmall", "bigmall"),
            Map.entry("lce:policestation", "policestation"),
            Map.entry("lce:bufschooll3ya", "bufschooll3ya"),
            Map.entry("lce:aircraftcarrier2", "aircraftcarrier2"),
            Map.entry("lce:aircraftcarrier", "aircraftcarrier"),
            Map.entry("lce:tallbuilding2", "tallbuilding2"),
            Map.entry("lce:massivebuildingwithhelicopter", "massivebuildingwithhelicopter"),
            Map.entry("lce:mediumbuildingczp1", "mediumbuildingczp1"),
            Map.entry("lce:skyscraper", "skyscraper"),
            Map.entry("lce:skyscraper1", "skyscraper1"),
            Map.entry("lce:hugebuilding1", "hugebuilding1"),
            Map.entry("lce:hugebuilding2", "hugebuilding2"),
            Map.entry("lce:tv_tower", "tv_tower"),
            Map.entry("lce:wd1imp", "wd1imp"),
            Map.entry("lce:gianthospital", "gianthospital"),
            // ---- pomkotsworld 迁移建筑（命名空间 lce2）显示名 ----
            Map.entry("lce2:base/base_aircraft_ground", "mil_base_aircraft_ground"),
            Map.entry("lce2:base/base_aircraft_ground_heri", "mil_base_aircraft_ground_heri"),
            Map.entry("lce2:base/base_aircraft_hanger", "mil_base_aircraft_hangar"),
            Map.entry("lce2:base/base_building_body", "mil_base_building_body"),
            Map.entry("lce2:base/base_building_medium", "mil_base_building_medium"),
            Map.entry("lce2:base/base_container", "mil_base_container"),
            Map.entry("lce2:base/base_ground_airvent", "mil_base_ground_airvent"),
            Map.entry("lce2:base/base_ground_ew", "mil_base_ground_ew"),
            Map.entry("lce2:base/base_ground_ns", "mil_base_ground_ns"),
            Map.entry("lce2:base/base_hanger", "mil_base_hangar"),
            Map.entry("lce2:base/base_missile_large", "mil_base_missile_large"),
            Map.entry("lce2:base/base_missile_large_multi", "mil_base_missile_large_multi"),
            Map.entry("lce2:base/base_missile_launcher_l", "mil_base_missile_launcher_l"),
            Map.entry("lce2:base/base_missile_launcher_m", "mil_base_missile_launcher_m"),
            Map.entry("lce2:base/base_space_shuttle", "mil_base_space_shuttle"),
            Map.entry("lce2:offshore/offshore_empty", "offshore_empty"),
            Map.entry("lce2:offshore/offshore_mega_platform", "offshore_mega_platform"),
            Map.entry("lce2:factory/factory_black_building", "fac_black_building"),
            Map.entry("lce2:factory/factory_building_main_huge", "fac_main_huge"),
            Map.entry("lce2:factory/factory_building_main_large", "fac_main_large"),
            Map.entry("lce2:factory/factory_building_main_medium", "fac_main_medium"),
            Map.entry("lce2:factory/factory_building_sub", "fac_sub"),
            Map.entry("lce2:factory/factory_containers_medium", "fac_containers"),
            Map.entry("lce2:factory/factory_crane", "fac_crane"),
            Map.entry("lce2:factory/factory_furnace", "fac_furnace"),
            Map.entry("lce2:factory/factory_garage", "fac_garage"),
            Map.entry("lce2:factory/factory_process_plant", "fac_process_plant"),
            Map.entry("lce2:factory/factory_rack", "fac_rack"),
            Map.entry("lce2:factory/factory_reactor", "fac_reactor"),
            Map.entry("lce2:factory/factory_tank_medium", "fac_tank_medium"),
            Map.entry("lce2:factory/factory_tank_sphere", "fac_tank_sphere"),
            Map.entry("lce2:factory/factory_tank_water", "fac_tank_water"),
            Map.entry("lce2:port/port_tanker", "port_tanker"),
            Map.entry("lce2:port/port_tanker_small", "port_tanker_small"),
            Map.entry("lce2:ships/ships_warship", "ship_warship"),
            Map.entry("lce2:quarry/quarry_dump_e", "quarry_dump_e"),
            Map.entry("lce2:quarry/quarry_dump_n", "quarry_dump_n"),
            Map.entry("lce2:quarry/quarry_dump_s", "quarry_dump_s"),
            Map.entry("lce2:quarry/quarry_dump_w", "quarry_dump_w"),
            Map.entry("lce2:quarry/quarry_mega_machine", "quarry_mega_machine"),
            Map.entry("lce2:quarry/quarry_office", "quarry_office"),
            Map.entry("lce2:quarry/quarry_stone_large", "quarry_stone_large"),
            Map.entry("lce2:quarry/quarry_stone_middle", "quarry_stone_middle"),
            Map.entry("lce2:megasolar/megasolar_cell_huge", "solar_cell_huge"),
            Map.entry("lce2:megasolar/megasolar_cell_medium", "solar_cell_medium"),
            Map.entry("lce2:megasolar/megasolar_reactor", "solar_reactor"),
            Map.entry("lce2:scattered/scattered_antenna", "scat_antenna"),
            Map.entry("lce2:scattered/scattered_container", "scat_container"),
            Map.entry("lce2:scattered/scattered_cube", "scat_cube"),
            Map.entry("lce2:scattered/scattered_farmland", "scat_farmland"),
            Map.entry("lce2:scattered/scattered_garage", "scat_garage"),
            Map.entry("lce2:scattered/scattered_gasstand", "scat_gasstand"),
            Map.entry("lce2:scattered/scattered_hanger", "scat_hangar"),
            Map.entry("lce2:scattered/scattered_lighthouse", "scat_lighthouse"),
            Map.entry("lce2:scattered/scattered_minifactory", "scat_minifactory"),
            Map.entry("lce2:scattered/scattered_radiotower", "scat_radiotower"),
            Map.entry("lce2:scattered/scattered_scaffold", "scat_scaffold"),
            Map.entry("lce2:scattered/scattered_telescope", "scat_telescope"),
            Map.entry("lce2:scattered/scattered_watertank", "scat_watertank"),
            Map.entry("lce2:scattered/wind_mill", "scat_windmill"),
            Map.entry("lce2:scattered_antenna", "scat_antenna"),
            Map.entry("lce2:scattered_container", "scat_container"),
            Map.entry("lce2:scattered_cube", "scat_cube"),
            Map.entry("lce2:scattered_farmland", "scat_farmland"),
            Map.entry("lce2:scattered_garage", "scat_garage"),
            Map.entry("lce2:scattered_gasstand", "scat_gasstand"),
            Map.entry("lce2:scattered_hanger", "scat_hangar"),
            Map.entry("lce2:scattered_lighthouse", "scat_lighthouse"),
            Map.entry("lce2:scattered_minifactory", "scat_minifactory"),
            Map.entry("lce2:scattered_radiotower", "scat_radiotower"),
            Map.entry("lce2:scattered_scaffold", "scat_scaffold"),
            Map.entry("lce2:scattered_telescope", "scat_telescope"),
            Map.entry("lce2:scattered_watertank", "scat_watertank"),
            Map.entry("lce2:wind_mill", "scat_windmill"),
            Map.entry("lce2:warship", "ship_warship"),
            Map.entry("lce2:mega_platform", "offshore_mega_platform"),
            // ---- lostcities-modern-tweaks（lcmt）建筑显示名：避免回退成裸 id（如 factory/factory_00）----
            Map.entry("lcmt:factory/factory_00", "lcmt_fac_00"),
            Map.entry("lcmt:factory/factory_01", "lcmt_fac_01"),
            Map.entry("lcmt:factory/factory_02", "lcmt_fac_02"),
            Map.entry("lcmt:factory/factory_10", "lcmt_fac_10"),
            Map.entry("lcmt:factory/factory_11", "lcmt_fac_11"),
            Map.entry("lcmt:factory/factory_12", "lcmt_fac_12"),
            Map.entry("lcmt:center", "lcmt_center"),
            Map.entry("lcmt:factory", "lcmt_factory"),
            Map.entry("lcmt:library", "lcmt_library"),
            Map.entry("lcmt:shopping", "lcmt_shopping"),
            Map.entry("lcmt:shopping_open", "lcmt_shopping_open"),
            Map.entry("lcmt:townhall", "lcmt_townhall"),
            // ---- lcmt 子建筑（裸名含子目录，需全名映射避免回退成 center/center00 等）----
            Map.entry("lcmt:center/center00", "lcmt_center_00"),
            Map.entry("lcmt:center/center01", "lcmt_center_01"),
            Map.entry("lcmt:center/center10", "lcmt_center_10"),
            Map.entry("lcmt:center/center11", "lcmt_center_11"),
            Map.entry("lcmt:library/library00", "lcmt_lib_00"),
            Map.entry("lcmt:library/library01", "lcmt_lib_01"),
            Map.entry("lcmt:library/library10", "lcmt_lib_10"),
            Map.entry("lcmt:library/library11", "lcmt_lib_11"),
            Map.entry("lcmt:shopping/shopping00", "lcmt_shop_00"),
            Map.entry("lcmt:shopping/shopping01", "lcmt_shop_01"),
            Map.entry("lcmt:shopping/shopping10", "lcmt_shop_10"),
            Map.entry("lcmt:shopping/shopping11", "lcmt_shop_11"),
            Map.entry("lcmt:shopping/shopping_open00", "lcmt_shopopen_00"),
            Map.entry("lcmt:shopping/shopping_open01", "lcmt_shopopen_01"),
            Map.entry("lcmt:shopping/shopping_open10", "lcmt_shopopen_10"),
            Map.entry("lcmt:shopping/shopping_open11", "lcmt_shopopen_11"),
            Map.entry("lcmt:townhall/town00", "lcmt_town_00"),
            Map.entry("lcmt:townhall/town01", "lcmt_town_01"),
            Map.entry("lcmt:townhall/town10", "lcmt_town_10"),
            Map.entry("lcmt:townhall/town11", "lcmt_town_11"),

            // ---- 多区块建筑（multibuilding）的父 id ----
            // 星图对整栋多区块建筑是「按父 id 出图」的（见 buildingStarRegions 里的 regionKeyOf），
            // 显示名也必须登记在这里，否则 buildingDisplayKey 查不到、星图上会直接显示
            // 诸如 lostcities:multi2 这种未翻译的原始 id。
            // 注意：这里必须放完整的父 id（含命名空间）；下面那些裸名条目（building1 等）
            // 靠 buildingDisplayKey 剥离命名空间的兜底才会命中，父 id 无法享受该兜底
            // （剥离后 multi2 仍不在表里），所以必须显式登记。
            Map.entry("lostcities:multi1", "multi1"),
            Map.entry("lostcities:multi2", "multi2"),
            Map.entry("lostcities:multi3", "multi3"),
            Map.entry("lostcities:multi4", "multi4"),
            Map.entry("lostcities:multi5", "multi5"),
            Map.entry("lostcities:huge1", "huge1"),
            Map.entry("lostcities:huge2", "huge2"),
            Map.entry("lostcities:center", "downtown"),
            Map.entry("lostcities:library", "library"),
            Map.entry("lostcities:shopping", "shopping_mall"),
            Map.entry("lostcities:shopping_open", "open_market"),
            Map.entry("lostcities:townhall", "townhall"),
            Map.entry("lostcities:oilrig", "oil_rig"),
            Map.entry("lostcities:highway_restaurant", "highway_restaurant"),
            // lcmt 的多区块建筑与 lostcities 同名同用途，沿用同一套显示名
            Map.entry("lcmt:multi1", "multi1"),
            Map.entry("lcmt:multi2", "multi2"),
            Map.entry("lcmt:multi3", "multi3"),
            Map.entry("lcmt:multi4", "multi4"),
            Map.entry("lcmt:multi5", "multi5"),
            Map.entry("lcmt:huge1", "huge1"),
            Map.entry("lcmt:huge2", "huge2")
    );

    /** 建筑 id（LostCities 资源文件名，含命名空间）→ 翻译键；未知 id 原样返回（兜底）。 */
    public static String buildingDisplayKey(String id) {
        if (id == null) {
            return "?";
        }
        String key = id.toLowerCase(Locale.ROOT);
        // 先按完整 id（含命名空间，如 lce:aircraftcarrier）匹配 LostCities-extend 自定义建筑。
        String suffix = BUILDING_DISPLAY.get(key);
        if (suffix != null) {
            return BUILDING_LANG + suffix;
        }
        // DeceasedCraft（deceasedcraft）建筑：multibuilding 父 id 与普通建筑均登记在
        // DC_BUILDING_DISPLAY，避免星图上显示 deceasedcraft:multi_hospital 这类原始 id。
        String dcSuffix = DC_BUILDING_DISPLAY.get(key);
        if (dcSuffix != null) {
            return BUILDING_LANG + dcSuffix;
        }
        // 防御：剥离命名空间后按裸名匹配原版 / 安全区 / 撤离点建筑。
        int sep = key.indexOf(':');
        if (sep >= 0) {
            key = key.substring(sep + 1);
        }
        suffix = BUILDING_DISPLAY.get(key);
        return suffix != null ? BUILDING_LANG + suffix : key;
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
                ILostChunkInfo c = safeChunkInfo(level, info, cx, cz);
                if (c == null || !c.isCity() || c.getBuildingId() == null) {
                    continue;
                }
                // 区域标识与星级：
                //  · 多区块建筑（LCE/LCE2 的 lce:skyscraper 等）各子块 buildingId 互不相同
                //    （_0_0 / _0_1 / _1_0 / _1_1），必须整体按 multibuilding 的父 id 处理——
                //    否则既查不到星级映射（表里登记的是父名），又会被洪泛切成若干碎片区域。
                //  · 普通建筑直接用 buildingId。
                String regionKey = regionKeyOf(c);
                ILostChunkInfo.MultiBuildingInfo mb = c.getMultiBuildingInfo();
                String id;
                int star;
                if (mb != null) {
                    id = mb.buildingType().toString();
                    star = starForMultiBuilding(id);
                } else {
                    id = c.getBuildingId().toString();
                    star = starForBuildingName(id);
                }
                if (star < 1 || star > 5) {
                    continue; // 仅危险度 1~5 的建筑上图（安全区/撤离点等不含星级）
                }
                // 洪泛填充：收集与当前 chunk 连通、且属于同一栋楼的所有已加载 chunk
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
                        ILostChunkInfo nc = safeChunkInfo(level, info, nx, nz);
                        if (nc == null || !nc.isCity() || nc.getBuildingId() == null) {
                            continue;
                        }
                        if (!regionKeyOf(nc).equals(regionKey)) {
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

    /**
     * 区块所属「一栋楼」的标识：多区块建筑用其 multibuilding 父 id，普通建筑用 buildingId。
     * 用于星图洪泛填充时判断两个 chunk 是否属于同一栋建筑。
     */
    private static String regionKeyOf(ILostChunkInfo chunk) {
        ILostChunkInfo.MultiBuildingInfo mb = chunk.getMultiBuildingInfo();
        if (mb != null) {
            return "m:" + mb.buildingType();
        }
        ResourceLocation id = chunk.getBuildingId();
        return "b:" + (id == null ? "" : id);
    }
}
