package net.exmo.sixty_seconds.island;

import com.google.gson.annotations.SerializedName;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

/**
 * 一座海岛的元数据（Gson 序列化，随 {@link SixtySecondsIslands} 落盘）。
 * <p>
 * 地形本体由 {@link SixtySecondsIslandGenerator} 按 {@link #seed} 程序化生成——
 * 客户端海图（SeaChartScreen）用<b>同一个</b> seed 与形状函数重采样，即可画出与实际地形
 * 一致的岛屿轮廓，无需同步任何方块数据。
 */
public class SixtySecondsIsland {
    /** 语言键前缀；岛名 = name_prefix.N + name_suffix.M 两段翻译拼接。 */
    public static final String LANG = "message.sixty_seconds.sixty_seconds.island.";
    public static final int NAME_PREFIX_COUNT = 24;
    public static final int NAME_SUFFIX_COUNT = 4;

    /**
     * 岛屿生态类型：决定色板、植被、树木品种、地形特征。
     * 与等级（level）正交——同等级不同类型的岛外观完全不同。
     * 旧存档缺省=null，Generator 回退到纯 level 色板。
     */
    public enum Type {
        TROPICAL,   // 热带：沙+草+丛林树，高植被
        MARSH,      // 沼泽：泥+灰化土+沼泽橡树，低平，水涝
        VOLCANIC,   // 火山：黑石+玄武岩+岩浆，锥形山，枯树
        CORAL,      // 珊瑚环礁：沙+海晶石，环形礁盘+中央潟湖，无树
        FROST,      // 冰霜：雪+冰+云杉，积雪覆盖
        PLATEAU,    // 高原：平顶+陡崖，陶瓦+石头
        JUNGLE,     // 密林：高密度丛林树+藤蔓+可可
        BARREN,     // 荒芜：砂土+砾石+枯灌木，极稀疏
        RUINS,      // 废墟：坍塌建筑残骸，混凝土碎块，荒草丛生
        QUARANTINE, // 隔离：废弃检疫设施，锈蚀围栏，高危污染
        OIL,        // 油井：废弃钻井平台残骸，黑色油渍，钢架
        MILITARY,   // 军事：废弃前哨堡垒，掩体残壁，弹孔遍布
        ABYSS,      // 深渊：高危稀有岛，灰黑焦土+裂隙，Level5 极少出现，怪物成群
        INFERNO,    // 炼狱：高危稀有岛，熔岩裂隙+焦岩，Level4~5 低概率，极度危险
        FORSAKEN,   // 遗弃：中高危稀有岛，锈蚀废墟+毒雾，Level3~5 低概率
        CRYSTAL,    // 晶簇：紫水晶母岩+石英尖峰，Level3~5 稀有，折射发光
        SWAMP,      // 毒沼：菌丝地表+巨型蘑菇，Level2~4 低概率，毒雾弥漫
        MESA,       // 红台地：红砂岩+陶瓦平顶，Level3~5 低概率，干旱荒红
        ASHEN,      // 灰烬：灰混凝土+木炭枯骸，Level2~4 低概率，死寂灰白
        SCULK,      // 幽匿：地表与浅层由幽匿块构成，Level3~5 稀有，死寂回响，仅生成幽匿神殿
        EVACUATION  // 撤离点：专门的撤离点岛屿，稀有、不刷新物资箱、海图特殊标记，最后一天登岛即撤离
    }

    /** 岛屿规模：小型/中型/大型，决定半径、装饰密度、物资数量。 */
    public enum Size {
        SMALL(0.12F, 2, 4, 0.45F, 0.5F),
        MEDIUM(0.35F, 4, 7, 0.75F, 0.8F),
        LARGE(1.0F, 9, 11, 1.0F, 1.0F);

        public final float radiusMult;
        public final int levelRadiusBonus;
        public final int radiusVariance;
        public final float decoMult;
        public final float supplyMult;

        Size(float radiusMult, int levelRadiusBonus, int radiusVariance, float decoMult, float supplyMult) {
            this.radiusMult = radiusMult;
            this.levelRadiusBonus = levelRadiusBonus;
            this.radiusVariance = radiusVariance;
            this.decoMult = decoMult;
            this.supplyMult = supplyMult;
        }
    }

    @SerializedName("id")
    public int id;
    /** 危险等级 1..5：决定地貌色板、废墟/物资箱/怪物的数量与质量。 */
    @SerializedName("level")
    public int level = 1;
    /** 岛屿生态类型（旧存档缺省=null → Generator 回退纯 level 色板）。 */
    @SerializedName("type")
    public Type type = null;
    /** 岛屿规模（Gson 序列化兼容旧存档：缺省→MEDIUM）。 */
    @SerializedName("size")
    public Size size = Size.MEDIUM;
    @SerializedName("namePrefix")
    public int namePrefix;
    @SerializedName("nameSuffix")
    public int nameSuffix;
    /** 是否为撤离点岛屿：稀有、不刷新物资箱、海图特殊标记、最后一天登岛即撤离。 */
    @SerializedName("isEvacuation")
    public boolean isEvacuation = false;
    /** 撤离点岛屿的专属独立名称索引（独立于普通 name_prefix/name_suffix，避免「碎浪岛 撤离点」式命名）。 */
    @SerializedName("evacNameIndex")
    public int evacNameIndex = 0;
    /** 撤离点专属名称池大小。 */
    public static final int EVAC_NAME_COUNT = 6;
    /**
     * 是否为「炼狱岛」（高难强化岛）：仅<b>部分</b>五星岛按概率获得。
     * 强化了首登守岛怪（更强变体 + 更高血量 + 更多数量）并额外固定驻守一只 Boss。
     * 与 {@link #isEvacuation} 互斥（撤离点岛恒为 false）。
     */
    @SerializedName("hardcore")
    public boolean hardcore = false;
    /**
     * 炼狱岛固定驻守的 Boss 变体（{@link #hardcore} 为真时有效；null 表示不刷 Boss，仅强化守岛怪）。
     * 在 Generator 规划阶段一次性随机决定，落盘后稳定。
     */
    @SerializedName("bossVariant")
    public SixtySecondsBossEntity.BossVariant bossVariant = null;
    /** 地形噪声种子（服务端生成与客户端海图共用）。 */
    @SerializedName("seed")
    public long seed;
    @SerializedName("centerX")
    public int centerX;
    @SerializedName("centerZ")
    public int centerZ;
    /** 海平面 Y（同一群岛统一）。 */
    @SerializedName("seaY")
    public int seaY;
    /** 陆地基准半径（实际岸线随噪声起伏）。 */
    @SerializedName("radius")
    public int radius;
    /** 登岛落点（扬帆传送目标；建岛时在向心一侧的滩头上求得）。 */
    @SerializedName("dockX")
    public int dockX;
    @SerializedName("dockY")
    public int dockY;
    @SerializedName("dockZ")
    public int dockZ;
    /**
     * 自动放置的避难所门坐标（{@link net.exmo.sixty_seconds.config.SixtySecondsConfig#islandShelterDoorEnabled}
     * 开启时给一级岛在建造阶段求得并建门）。{@code shelterDoorY < 0} 表示本岛没有自动门。
     */
    @SerializedName("shelterDoorX")
    public int shelterDoorX;
    @SerializedName("shelterDoorY")
    public int shelterDoorY = Integer.MIN_VALUE;
    @SerializedName("shelterDoorZ")
    public int shelterDoorZ;

    /** 本岛是否有自动放置的避难所门。 */
    public boolean hasShelterDoor() {
        return shelterDoorY != Integer.MIN_VALUE;
    }

    /** 自动放置的避难所门坐标（{@link #hasShelterDoor()} 为真时有效）。 */
    public BlockPos shelterDoorPos() {
        return new BlockPos(shelterDoorX, shelterDoorY, shelterDoorZ);
    }

    /** 岛名（两段翻译键拼接，客户端/服务端同构）。有类型时追加类型标签。 */
    public Component name() {
        // 撤离点岛屿：使用专属独立名称池（如「归航岛」），不附加「撤离点」类型标签，
        // 表现为一个独立的岛屿大类，而非「碎浪岛 撤离点」这种后缀式命名。
        if (isEvacuation || type == Type.EVACUATION) {
            return Component.translatable(LANG + "evac_name." + (evacNameIndex % EVAC_NAME_COUNT))
                    .withStyle(net.minecraft.ChatFormatting.GOLD);
        }
        Component base = Component.translatable(LANG + "name_prefix." + namePrefix)
                .append(Component.translatable(LANG + "name_suffix." + nameSuffix));
        if (type != null) {
            return base.copy().append(Component.literal(" "))
                    .append(Component.translatable(LANG + "type." + type.name().toLowerCase()));
        }
        return base;
    }

    public BlockPos dockPos() {
        return new BlockPos(dockX, dockY, dockZ);
    }

    /** 本岛「单元格」盒：陆地 + 环岛水裙边 + 纵向生成范围（建造/还原/区域地图共用）。 */
    public AABB cellBox() {
        int r = radius + SixtySecondsIslandGenerator.WATER_SKIRT;
        return new AABB(centerX - r, seaY - SixtySecondsIslandGenerator.DEPTH_BELOW_SEA,
                centerZ - r, centerX + r + 1,
                seaY + SixtySecondsIslandGenerator.HEIGHT_ABOVE_SEA, centerZ + r + 1);
    }

    /** 到岛心的水平距离平方。 */
    public double distSqr(double x, double z) {
        double dx = x - (centerX + 0.5);
        double dz = z - (centerZ + 0.5);
        return dx * dx + dz * dz;
    }

    /** 该坐标是否算「登上了本岛」（水平进入陆地半径内且不深潜在海底）。 */
    public boolean isOnIsland(BlockPos pos) {
        return pos.getY() >= seaY - 2 && distSqr(pos.getX() + 0.5, pos.getZ() + 0.5)
                <= (double) (radius + 2) * (radius + 2);
    }

    /** 该坐标是否在本岛单元格（含水裙边）内——危险等级反查用。 */
    public boolean inCell(BlockPos pos) {
        int r = radius + SixtySecondsIslandGenerator.WATER_SKIRT;
        return pos.getY() >= seaY - SixtySecondsIslandGenerator.DEPTH_BELOW_SEA
                && pos.getY() <= seaY + SixtySecondsIslandGenerator.HEIGHT_ABOVE_SEA
                && distSqr(pos.getX() + 0.5, pos.getZ() + 0.5) <= (double) r * r;
    }
}
