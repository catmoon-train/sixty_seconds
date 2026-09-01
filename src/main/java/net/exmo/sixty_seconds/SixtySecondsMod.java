package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.bridge.SixtySecID;
import net.exmo.sixty_seconds.bridge.GameMode;
import net.exmo.sixty_seconds.bridge.SixtySecGameModes;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 末日60秒模式的引导装配：通过公开入口 {@link SixtySecGameModes#registerGameMode} 注册模式，
 * <b>不改动</b> {@code io.wifi} 内任何文件；仅需在 {@code Sixtyseconds.onInitialize} 里调用一次 {@link #init()}
 * （与 {@code GooseDuckMod.init()} 一致）。
 */
public final class SixtySecondsMod {
    /** 模式 ID：{@code sixty_seconds:sixty_seconds}，可用 {@code /60s start} 启动。 */
    public static final ResourceLocation MODE_ID = SixtySecID.shortId("sixty_seconds");

    /** 注册后的模式实例（init 后非空）。 */
    public static GameMode MODE;

    /** 本模式是否正在进行（供无世界上下文的 mixin 判断，如食物不可堆叠）。开局置 true，结束置 false。 */
    public static volatile boolean RUNNING = false;

    /** 是否正在建图（含预建 /60s build 与正式 /60s start）。独立于 RUNNING：预建时游戏未开始但建图仍需进行，
     *  BuildTask 以本标记为唯一中止依据（/60s stop 或收尾时置 false），避免预建因 RUNNING=false 被误中止。 */
    public static volatile boolean BUILDING = false;

    /** 预建结果：/60s build 预先建好的住宅+避难所数据，/60s start 时若队伍数一致则跳过建图直接复用。 */
    public static net.exmo.sixty_seconds.state.SixtySecondsState.Data PREBUILT_DATA = null;
    /** 预建掩码（BUILD_RESIDENTIAL / BUILD_SHELTER / BUILD_ALL，见 SixtySecondsArena）。 */
    public static int PREBUILT_MASK = 0;
    /** 预建锚点：/60s build 时指令输入玩家脚下的坐标，用于让 start 续建缺失部分时落位一致。 */
    public static net.minecraft.core.BlockPos PREBUILT_ANCHOR = null;

    private SixtySecondsMod() {
    }

    public static void init() {
        registerCommands();
        net.exmo.sixty_seconds.command.SixtySecondsWeatherCommand.register();
        SixtySecondsCreativeTab.register(); // 统一创造标签页（须在物品入页前注册）
        MODE = SixtySecGameModes.registerGameMode(new SixtySecondsGameMode(MODE_ID));
        net.exmo.sixty_seconds.arena.SixtySecondsArena.registerEntityClearWindow(); // 开局清卸载区块里的残留尸体/掉落物
        net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem.register();
        net.exmo.sixty_seconds.logic.SixtySecondsMonsterSystem.registerEvents();
        net.exmo.sixty_seconds.logic.SixtySecondsWeightSystem.register();
        net.exmo.sixty_seconds.command.SixtySecondsWeightCommand.register();
        net.exmo.sixty_seconds.logic.SixtySecondsStations.register(); // 合成台绑定（书桌/灶台/浴缸）
        net.exmo.sixty_seconds.logic.SixtySecondsCorpseLoot.register(); // 死亡物品装入尸体箱可搜刮
        net.exmo.sixty_seconds.logic.SixtySecondsLootSearch.register(); // 物资箱搜刮全局推进（游戏外也生效）
        net.exmo.sixty_seconds.logic.SixtySecondsDefenseSystem.register(); // 夜袭者死亡掉废料
        net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.register(); // PVE 游荡怪死亡掉废料
        net.exmo.sixty_seconds.logic.SixtySecondsReconnect.register(); // 掉线备份/重连恢复（背包+状态）
        net.exmo.sixty_seconds.logic.SixtySecondsAutoJoin.register(); // 中途新玩家自动补入未满队伍
        net.exmo.sixty_seconds.logic.SixtySecondsRockets.register(); // RPG 火箭投射物全局推进
        net.exmo.sixty_seconds.logic.SixtySecondsAirdrop.register(); // 指令空投下落动画全局推进
        net.exmo.sixty_seconds.content.item.SixtySecondsRopeItem.register(); // 临时绳索到期清除
        net.exmo.sixty_seconds.content.item.SixtySecondsGrapplingHookItem.register(); // 钩锁荡索摔落保护
        net.exmo.sixty_seconds.logic.SixtySecondsProximityChat.register(); // 邻近聊天（只有附近玩家能看到）
        net.exmo.sixty_seconds.content.block.SixtySecondsBaseUtilityBlock.register(); // 基地报警器/玩偶/次声波音响
        net.exmo.sixty_seconds.logic.SixtySecondsMystic.register(); // 神秘技术：复活图腾右键尸体
        net.exmo.sixty_seconds.island.SixtySecondsIslands.register(); // 海岛远征：收音机侦听岛屿情报
        net.exmo.sixty_seconds.logic.SixtySecondsNpcSystem.register(); // NPC 死亡掉落（随身物资+废料）
        net.exmo.sixty_seconds.content.item.SixtySecondsRadioHandler.register(); // 对讲机频道：掉线/旁观/弃机自动退出
        registerDropRule(); // 本模式放行丢弃物品（全局默认禁丢，见 KeyBindingMixin/DropRules）
        registerChatHudRule(); // 本模式放行聊天栏渲染（存活玩家默认被 ChatHudMixin 隐藏）
        net.exmo.sixty_seconds.lostcities.SixtySecondsLostCityNpcGen.register(); // LostCities 建筑固定刷 NPC（安全区/交易建筑，只刷一次）
    }

    /**
     * 允许本模式存活玩家看到聊天栏：{@code ChatHudMixin} 默认对局内存活玩家隐藏聊天渲染
     * （仅 {@code ChatHudRules} 放行的职业/玩家可见），而 60s 有邻近聊天玩法
     * （{@code SixtySecondsProximityChat}），聊天栏被隐藏时消息根本看不到。
     */
    private static void registerChatHudRule() {
    }

    private static void registerDropRule() {
    }

    private static void registerCommands() {
        net.exmo.sixty_seconds.command.SixtySecondsStartCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsSaveCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsStopCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsAutoCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsAreaCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsExportBuildingCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsExportTemplateCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsApplyTemplateCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsHelicopterCommand.register();
        net.exmo.sixty_seconds.command.OceanCreatureCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsDifficultyCommand.register();
        net.exmo.sixty_seconds.command.SixtySecondsLootFormCommand.register();
    }

    /** 当前世界是否正在运行本模式。 */
    public static boolean isActive(Level level) {
        return MODE != null && SixtySecGameWorldComponent.KEY.get(level).getGameMode() == MODE;
    }
}
