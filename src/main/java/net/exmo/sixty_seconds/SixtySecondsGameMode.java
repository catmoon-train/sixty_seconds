package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.bridge.GameMode;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.arena.SixtySecondsArena;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.logic.SixtySecondsManager;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 末日60秒模式入口：生命周期只做编排，具体逻辑在 {@code logic} / {@code arena} / {@code state} 包中。
 * <b>不走</b>默认 {@code baseInitialize}（它按列车房间传送玩家，60s 图没有房间出生点会把玩家丢进虚空）——
 * 见 {@link #beforeInitializeGame}；玩家在异步建图完成后才被统一传送（模板建完才真正开始）。
 * 60s 地图请设 {@code AreasSettings.noReset=true}，模板方能不被 FullTrainReset 覆盖。
 */
public class SixtySecondsGameMode extends GameMode {

    public SixtySecondsGameMode(ResourceLocation identifier) {
        super(identifier, 60, 1);
    }

    /**
     * 覆写掉 {@code GameUtils.baseInitialize}：那会按「列车房间」发钥匙/信封并把玩家传送到
     * {@code getSpawnPos(areas, room)}——60s 专用图没有房间出生点，fallback 直接把玩家平移进<b>虚空</b>
     * （这就是「开局不建住宅/避难所、掉进虚空」的根因）。这里只做必要重置、<b>不传送参战玩家</b>，
     * 传送统一放在异步建图完成回调（{@code SixtySecondsManager.onBuildComplete}）。
     */
    @Override
    public void beforeInitializeGame(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent,
            List<ServerPlayer> players) {
        net.exmo.sixty_seconds.logic.SixtySecondsGameSetup.prepareWorld(serverWorld, gameWorldComponent, players);
    }

    @Override
    public boolean hasMood() {
        return false;
    }

    @Override
    public boolean shouldRecordPlayerStats() {
        return false;
    }

    /** 本模组不依赖角色系统：允许没有职业的存活生还者（否则会被强制变旁观）。 */
    @Override
    public boolean requiresAssignedRole() {
        return false;
    }

    /** 尸体可搜刮：存活玩家也能打开尸体箱（内容由 SixtySecondsCorpseLoot 在死亡时装填）。 */
    @Override
    public boolean canSeeBodyContent() {
        return true;
    }

    /**
     * 尸体可拿取：60s 玩家没有角色身份（{@link #requiresAssignedRole()} 返 false），若不覆写此项，
     * {@code PlayerBodyEntityContainer} 的取物门控全部回退到 role==null → false，导致「能开尸体箱却拿不出东西」。
     * 与 {@code canSeeBodyContent} 成对开启（对照 RepairEscapeGameMode）。
     */
    @Override
    public boolean canPickBodyContent() {
        return true;
    }

    /**
     * 60s 的住宅/避难所/探索区克隆在 playArea <b>之外</b>（teamBase 网格偏移 2048+）：
     * 必须关闭区域外检测，否则存活玩家一出生就被判「跌出列车」处死（含水域/黑暗检查）。
     * 同 {@code FourthRoomGameMode} 的做法。
     */
    @Override
    public boolean enablePlayAreaDetections() {
        return false;
    }

    /** 旁观者同理不限制在 playArea 内——否则观战者飞到各队避难所/探索区就被拉回列车。 */
    @Override
    public void limitSpectatorPlayer(ServerPlayer player, SixtySecGameWorldComponent gameWorldComponent,
            net.exmo.sixty_seconds.bridge.AreasWorldComponent areas) {
        // 60s 不做旁观限制（游戏区域分散在 playArea 之外的克隆网格里）
    }

    @Override
    public void initializeGame(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent,
            List<ServerPlayer> players) {
        SixtySecondsSearchZones.reset(serverWorld);
        SixtySecondsState.reset(serverWorld);
        SixtySecondsManager.begin(serverWorld, gameWorldComponent, players);
    }

    @Override
    public void tickServerGameLoop(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent) {
        super.tickServerGameLoop(serverWorld, gameWorldComponent);
        SixtySecondsManager.tick(serverWorld, gameWorldComponent);
        net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.autoSaveIfNeeded(serverWorld);
    }

    @Override
    public void stopGame(ServerLevel world) {
        net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.resetAll(world);
        net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.delete(world); // 一局结束：删除上一局存档
        net.exmo.sixty_seconds.SixtySecondsMod.RUNNING = false;
        // 清掉预建标记：游戏结束（或中途 stop）后，预建方块会被还原，几何数据不再有效
        net.exmo.sixty_seconds.SixtySecondsMod.PREBUILT_DATA = null;
        net.exmo.sixty_seconds.SixtySecondsMod.PREBUILT_MASK = 0;
        net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit.clear(world);
        net.exmo.sixty_seconds.logic.SixtySecondsVisitSystem.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsVisitChat.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsVisiting.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsTrade.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsEventSystem.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsMinigameRotation.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsDoorHighlight.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsLootSearch.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsReconnect.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsAutoJoin.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsRescue.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsRockets.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsAirdrop.reset(world);
        net.exmo.sixty_seconds.content.item.SixtySecondsRopeItem.reset();
        net.exmo.sixty_seconds.content.item.SixtySecondsGrapplingHookItem.reset();
        net.exmo.sixty_seconds.logic.SixtySecondsDefenseSystem.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsAreaBossSystem.reset(world); // 区域固定Boss/伤害Boss
        net.exmo.sixty_seconds.logic.SixtySecondsRvRaidSystem.reset(world);    // 房车夜袭突袭者/尸潮
        net.exmo.sixty_seconds.logic.SixtySecondsNpcSystem.reset(world); // 清偷窃会话 + 全图清 NPC
        net.exmo.sixty_seconds.logic.SixtySecondsAutoRevive.reset(world); // 清尸体标记 + 复活倒计时
        net.exmo.sixty_seconds.logic.SixtySecondsPowerSystem.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsAreaLevels.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsWhisperSystem.clear(world);
        net.exmo.sixty_seconds.logic.TrapCageSystem.reset(world);
        net.exmo.sixty_seconds.logic.SixtySecondsRvSystem.reset(world); // 房车：解除强载区块 + 清房车（须在 State.reset 前）
        SixtySecondsSearchZones.reset(world);
        SixtySecondsState.reset(world);
        // 区域地图回退到全图 playArea
        for (net.minecraft.server.level.ServerPlayer player : world.players()) {
            net.exmo.sixty_seconds.network.SixtySecondsMapZoneS2CPacket.sendClear(player);
        }
    }
}
