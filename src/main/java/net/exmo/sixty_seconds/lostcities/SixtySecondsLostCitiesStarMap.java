package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCities;
import mcjty.lostcities.api.ILostCityInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * LostCities 建筑 → 60 秒「区域星级」自动映射。
 *
 * <p>集中定义 LostCities 各建筑种类对应的危险等级（1..5）。由于 LostCities 的建筑是<b>按区块惰性生成</b>的
 * （无法在创建世界时一次性遍历所有建筑），因此这里采用<b>运行时按坐标反查</b>的方式：当游戏查询某坐标的危险等级时，
 * 通过 LostCities API 判断该坐标是否位于某个已知建筑内，若是则返回其对应星级。这样「创建世界后，建筑自动被划分到星级」——
 * 无需管理员手动用魔杖登记任何建筑。</p>
 *
 * <p>映射规则（与 LostCities 资源目录 {@code data/lostcities/lostcities/} 对应）：</p>
 * <ul>
 *   <li><b>多区块建筑</b> {@code multibuildings/}（center、townhall、multi1~5、huge1/2 等 14 个）→ <b>5 星</b>；</li>
 *   <li><b>散布建筑</b> {@code buildings/}：
 *     <ul>
 *       <li>{@code building1~8} 通用建筑 → 3 星；</li>
 *       <li>{@code cabin} 小屋 → 1 星；</li>
 *       <li>{@code center00/01/10/11} 城市中心 → 3 星；</li>
 *       <li>{@code highway_gas_station} 加油站 → 3 星；</li>
 *       <li>{@code highway_restaurant(_parking)} 公路餐厅 → 2 星；</li>
 *       <li>{@code library} 图书馆、{@code oilrig} 油井、{@code radiotower} 无线电塔、
 *           {@code shopping(_open)} 购物中心/露天市场 → 4 星；</li>
 *       <li>{@code town00/01/10/11} 城镇建筑 → 3 星。</li>
 *     </ul></li>
 *   <li><b>建筑部件</b> {@code parts/}（street/rails/bridge 等 207 个零件）不划分星级——它们不是独立建筑，
 *       仅作为建筑/街道的组装件，由所属建筑或街道决定。</li>
 * </ul>
 *
 * <p>本类只读取 LostCities 提供的 API（{@code mcjty.lostcities.api.*}），不修改 LostCities 任何逻辑。</p>
 */
public final class SixtySecondsLostCitiesStarMap {

    /** 无星级：该坐标不在已知建筑内（街道/部件/非城市），交由后续等级逻辑处理。 */
    public static final int NO_STAR = 0;

    /** 多区块建筑统一 5 星。 */
    private static final int MULTI_BUILDING_STAR = 5;

    private SixtySecondsLostCitiesStarMap() {
    }

    /**
     * 反查某坐标是否位于 LostCities 已知建筑内，返回对应星级（1..5）；非建筑/非城市维度/无 LostCities 时返回 {@link #NO_STAR}。
     *
     * <p>该方法为服务端调用（LostCities 的 {@code getChunkInfo} 是高效缓存查询，但禁止在世界生成期间调用——
     * 而 {@code levelAt} 都是游戏运行时查询，符合要求）。LostCities 未安装/未接入时静默返回 0。</p>
     *
     * @param level 服务端世界
     * @param pos   世界绝对坐标
     * @return 1..5 的星级，或 {@link #NO_STAR}
     */
    public static int starAt(ServerLevel level, BlockPos pos) {
        ILostCityInformation info = lostCitiesInfo(level);
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
     * 返回对应星级；未知名称返回 {@link #NO_STAR}。
     */
    public static int starForBuildingName(String buildingName) {
        if (buildingName == null) {
            return NO_STAR;
        }
        String name = buildingName.toLowerCase(Locale.ROOT);
        // 通用建筑 building1~8 → 3 星
        if (name.startsWith("building")) {
            return 3;
        }
        // 城市中心 center00/01/10/11 → 3 星
        if (name.startsWith("center")) {
            return 3;
        }
        // 城镇 town00/01/10/11 → 3 星
        if (name.startsWith("town")) {
            return 3;
        }
        // 公路餐厅 highway_restaurant(_parking) → 2 星
        if (name.startsWith("highway_restaurant")) {
            return 2;
        }
        // 加油站 highway_gas_station → 3 星
        if (name.startsWith("highway_gas_station")) {
            return 3;
        }
        // 图书馆 library* → 4 星
        if (name.startsWith("library")) {
            return 4;
        }
        // 石油钻井平台 oilrig* → 4 星
        if (name.startsWith("oilrig")) {
            return 4;
        }
        // 无线电塔 radiotower → 4 星
        if (name.startsWith("radiotower")) {
            return 4;
        }
        // 购物中心/露天市场 shopping(_open) → 4 星
        if (name.startsWith("shopping")) {
            return 4;
        }
        // 小屋 cabin → 1 星
        if ("cabin".equals(name)) {
            return 1;
        }
        return NO_STAR;
    }

    /** 获取指定维度的 LostCities 城市信息；该维度不支持城市或 LostCities 未接入时返回 null。 */
    @Nullable
    private static ILostCityInformation lostCitiesInfo(ServerLevel level) {
        try {
            ILostCities lostCities = SixtySecondsLostCitiesAccess.api();
            if (lostCities == null) {
                return null;
            }
            return lostCities.getLostInfo(level);
        } catch (Throwable ignored) {
            // LostCities 不可用（未安装/类路径缺失）：静默降级为无建筑星级。
            return null;
        }
    }
}
