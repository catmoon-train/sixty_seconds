package net.exmo.sixty_seconds.arena;

import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.exmo.sixty_seconds.bridge.BlockCopyUtils;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.ServerTaskInfoClasses.ServerTaskInfo;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.exmo.sixty_seconds.SixtySeconds;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按队克隆住宅 / 避难所 / 搜索区模板（{@code BlockCopyUtils.copyLayer}）。本模组不做任何地形还原。
 * <p>
 * <b>异步建图</b>：整图克隆方块量巨大，一 tick 内同步完成会触发服务器看门狗 60s 超时卡死。
 * 因此仿 {@code net.exmo.sixty_seconds.bridge.ServerTaskInfoClasses.FullTrainResetTask} 的做法，把工作切成子盒，
 * 用 {@link GameUtils#serverTaskQueue}（每 tick 推进队首、全局 {@code ServerTickEvents} 驱动）跨 tick 分批放置，
 * 建完后再回调 {@code onComplete}（传送/进准备阶段）。坐标/出生点等轻量计算仍在 {@link #build} 同步完成。
 */
public final class SixtySecondsArena {
    /** 每 tick 处理的子盒数（每盒约 {@link #CHUNK_TARGET} 方块）。
     *  调小可减轻单 tick 方块放置量，避免开局建图时服务器「Can't keep up」卡死。 */
    private static final int MAX_CHUNKS_PER_TICK = 1;
    /** 单个子盒目标方块数（越大每 tick 越重）。调小使单 tick 负载低于 tick 预算，消除开局卡顿。 */
    private static final int CHUNK_TARGET = 2000;

    /** 建造掩码：住宅 / 避难所 / 全部。供 /60s build 选择性预建。 */
    public static final int BUILD_RESIDENTIAL = 1;
    public static final int BUILD_SHELTER = 2;
    public static final int BUILD_ALL = BUILD_RESIDENTIAL | BUILD_SHELTER;

    // ── 迟到实体清理窗口（仿列车重置的 chunksToClearEntities 机制）────────────
    // 同步清扫（clearArenaEntities）只能扫到【已加载】的实体；上一局残留在卸载区块里的
    // 尸体/掉落物要等区块加载才入世界——在建图期间布防区域清单，ENTITY_LOAD 时按区清掉。
    /** 清理窗口生效的竞技场区域（建图时布防，尾窗过期作废）。 */
    private static final List<AABB> CLEAR_ZONES = new ArrayList<>();
    /** 清理窗口截止游戏刻；建图期间为 {@link Long#MAX_VALUE}，建完后收成短尾窗。 */
    private static long clearZonesDeadline = 0;
    /** 尾窗长度：玩家传送进场后实体几 tick 内就会入世界，3s 足够；开太长会误删局内新掉落。 */
    private static final int CLEAR_TAIL_TICKS = 60;

    private SixtySecondsArena() {
    }

    /** 模组初始化时注册一次：清理窗口内迟到入世界的上一局尸体/掉落物。 */
    public static void registerEntityClearWindow() {
        net.exmo.sixty_seconds.bridge.fabric.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (CLEAR_ZONES.isEmpty()) {
                return;
            }
            if (world.getGameTime() > clearZonesDeadline) {
                CLEAR_ZONES.clear();
                return;
            }
            // NPC 也要清：其 removeWhenFarAway=false + requiresCustomPersistence=true 会让它在卸载区块里
            // 长存，stopGame 的按类清扫只扫得到已加载实体——不放行这一类，上一局的 NPC 会在新局玩家
            // 进场加载区块时冒出来
            if (!(entity instanceof PlayerBodyEntity) && !(entity instanceof ItemEntity)
                    && !(entity instanceof net.exmo.sixty_seconds.entity.SixtySecondsNpcEntity)) {
                return;
            }
            for (AABB zone : CLEAR_ZONES) {
                if (zone.contains(entity.position())) {
                    // 实体正处于入世界回调中，推迟到任务队列再移除，避免重入实体管理器
                    world.getServer().execute(() -> {
                        if (!entity.isRemoved()) {
                            entity.discard();
                        }
                    });
                    return;
                }
            }
        });
    }

    /**
     * 对每支队伍克隆三种模板并回写出生点/限制盒/门绑定。坐标计算同步完成，方块放置<b>异步分批</b>；
     * 全部放置完成后调用 {@code onComplete}（务必把「传送/进准备阶段」等后续步骤放这里，否则会在方块建好前就传送）。
     * config 为 null / 未配置完整时不克隆，立即回调 {@code onComplete}（模式仍可跑，仅日志告警）。
     */
    public static void build(ServerLevel level, SixtySecondsState.Data data, SixtySecondsConfig config,
            Runnable onComplete) {
        build(level, data, config, onComplete, BUILD_ALL, null);
    }

    public static void build(ServerLevel level, SixtySecondsState.Data data, SixtySecondsConfig config,
            Runnable onComplete, int buildMask) {
        build(level, data, config, onComplete, buildMask, null);
    }

    /**
     * 建图主入口（以 {@code teamBase} 为中心按 Ulam 螺旋建图；预建锚点不为 null 时把整片螺旋刚性平移到该坐标）。
     * @param anchor 预建锚点：不为 null 时把整座竞技场（螺旋中心）从默认中心（{@code teamBase}）刚性平移到该坐标（仅水平位移，
     *               建筑 Y 仍按模板自然落位），用于 {@code /60s build} / 开局在指令输入玩家脚下就地建图。为 null 时走默认中心布局。
     */
    public static void build(ServerLevel level, SixtySecondsState.Data data, SixtySecondsConfig config,
            Runnable onComplete, int buildMask, BlockPos anchor) {
        if (config == null || !config.isComplete()) {
            clearArenaEntities(level, config, List.of(), List.of(), data);
            SixtySeconds.LOGGER.warn("[60s] Area template config incomplete (sixty_seconds_config.json) — skipping per-team clone build.");
            onComplete.run();
            return;
        }

        // 门绑定分两类：门在住宅/避难所模板内 = 该队私有探索区门（随克隆按队加偏移，进各队 searchDoors）；
        // 门在模板外（= 建在共享搜索区里的门）= 每队「出口门」，按队轮转分配——出门落在门口、回家须走自己的门
        BoundingBox residentialBox = config.residentialTemplate.toBox();
        BoundingBox shelterBox = config.shelterTemplate.toBox();
        List<SixtySecondsConfig.DoorBinding> shelterDoorBindings = new ArrayList<>();
        List<SixtySecondsConfig.DoorBinding> exitDoorBindings = new ArrayList<>();
        for (SixtySecondsConfig.DoorBinding b : config.searchDoorBindings) {
            if (b.door == null || b.boxMin == null || b.boxMax == null || b.spawn == null) {
                continue;
            }
            BlockPos doorPos = b.door.toBlockPos();
            if (residentialBox.isInside(doorPos) || shelterBox.isInside(doorPos)) {
                shelterDoorBindings.add(b);
            } else {
                exitDoorBindings.add(b);
            }
        }

        // 每队的避难所偏移：门锚定模式下 = 出口门 - 锚点门（避难所平移到探索区那扇门上），否则 = 队伍网格偏移。
        // 先整表算出来——clearArenaEntities 要按<b>实际</b>落位清残留实体，网格坐标在锚定模式下根本不是避难所所在地。
        boolean ocean = SixtySeconds.isOcean(level);
        List<BlockPos> shelterOffsets = shelterOffsets(config, data, exitDoorBindings, ocean);
        // 住宅落位：始终贴网格。ocean 模式把整座建筑压到 y≈-39，地板贴在 Y=-40（海洋维度 min_y=-64，可达）；
        // 普通模式最低层落在 y≈0。
        // 住宅建造基准 Y 调到 -30：最低层落在 Y=-30（模板 min.y 通常为 0，故偏移 = -30 - min.y）。
        // ocean 模式仍压到 -39 / 地板 -40（海洋维度专用）。
        int residentialBaseY = ocean ? (-39 - config.residentialTemplate.min.y) : (-30 - config.residentialTemplate.min.y);
        List<BlockPos> residentialOffsets = new ArrayList<>();
        for (int i = 0; i < data.teams.size(); i++) {
            BlockPos grid = config.teamOffset(i);
            residentialOffsets.add(new BlockPos(grid.getX(), residentialBaseY, grid.getZ()));
        }

        // 预建锚点：把整座竞技场沿水平刚性平移到指令输入玩家脚下（默认网格原点 teamBase → anchor）。
        // 仅平移 X/Z，建筑 Y 仍按模板自然落位；下方所有坐标（盒/出生点/门）统一叠加该偏移，保证几何自洽。
        BlockPos arenaDelta = BlockPos.ZERO;
        if (anchor != null) {
            arenaDelta = new BlockPos(anchor.getX() - config.teamBase.x, 0, anchor.getZ() - config.teamBase.z);
            List<BlockPos> shiftedS = new ArrayList<>(shelterOffsets.size());
            for (BlockPos o : shelterOffsets) shiftedS.add(o.offset(arenaDelta));
            shelterOffsets = shiftedS;
            List<BlockPos> shiftedR = new ArrayList<>(residentialOffsets.size());
            for (BlockPos o : residentialOffsets) shiftedR.add(o.offset(arenaDelta));
            residentialOffsets = shiftedR;
        }
        // 模板含活板门 → 按地表智能下沉埋地（把每队避难所偏移的 Y 压到让活板门齐地表）。
        // ocean 模式不埋地：埋地会按地表（海洋维度海床 ~Y72）对齐活板门，与 -40 地板相悖。
        boolean buried = ocean ? false : applyShelterBurial(level, config, shelterOffsets);
        if (!heightsFit(level, config, data, residentialOffsets, shelterOffsets)) {
            clearArenaEntities(level, config, List.of(), List.of(), data);
            onComplete.run();
            return;
        }
        clearArenaEntities(level, config, shelterOffsets, residentialOffsets, data);

        // 本模组不做任何地形还原：不记录快照、不回滚上一局残留方块，建图直接就地放置。

        // 净空与克隆分两阶段收集，最后 clearance 全部排在 clone 之前（见下方拼接）：
        // 工作项按列表顺序跨 tick 执行，若按队交错成「队0净空→队0克隆→队1净空→…」，锚定模式下两队的出口门若挨得比
        // 避难所模板还近，队1的净空就会把队0<b>已经建好</b>的避难所挖出洞来。全局先净空后克隆则与顺序无关。
        List<WorkItem> clearance = new ArrayList<>();
        List<WorkItem> clones = new ArrayList<>();
        int arenaMinX = Integer.MAX_VALUE, arenaMaxX = Integer.MIN_VALUE;
        int arenaMinZ = Integer.MAX_VALUE, arenaMaxZ = Integer.MIN_VALUE;
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            BlockPos offset = residentialOffsets.get(team.teamId);
            BlockPos shelterOffset = shelterOffsets.get(team.teamId);
            arenaMinX = Math.min(arenaMinX, residentialBox.minX() + offset.getX());
            arenaMaxX = Math.max(arenaMaxX, residentialBox.maxX() + offset.getX());
            arenaMinZ = Math.min(arenaMinZ, residentialBox.minZ() + offset.getZ());
            arenaMaxZ = Math.max(arenaMaxZ, residentialBox.maxZ() + offset.getZ());
            arenaMinX = Math.min(arenaMinX, shelterBox.minX() + shelterOffset.getX());
            arenaMaxX = Math.max(arenaMaxX, shelterBox.maxX() + shelterOffset.getX());
            arenaMinZ = Math.min(arenaMinZ, shelterBox.minZ() + shelterOffset.getZ());
            arenaMaxZ = Math.max(arenaMaxZ, shelterBox.maxZ() + shelterOffset.getZ());
            // 尝试按导出的 .nbt 模板生成（保留箱子内容物等方块实体），无文件则回退从世界克隆
            CompoundTag resTpl = loadTemplate(level, config.residentialTemplateFile);
            CompoundTag shelTpl = loadTemplate(level, config.shelterTemplateFile);
            // 先净空（挖开克隆区四周/上方的自然地形），再克隆——队数无上限后克隆区会排进山里；
            // 锚定模式下避难所落在探索区门口，净空同样负责挖开门口的原生地形/建筑
            if ((buildMask & BUILD_RESIDENTIAL) != 0) {
                addClearance(level, clearance, config.residentialTemplate.toBox(), offset);
                addConcreteShell(level, clearance, config.residentialTemplate.toBox(), offset);
            }
            // 下沉埋地模式<b>不</b>给避难所净空：copyLayer 直接把埋在地下的模板体（含内部空气）搬过去，
            // 上方地形保留覆盖、只露活板门——净空会把地表挖成坑、暴露基地（本功能要避免的正是这个）。
            if (!buried && (buildMask & BUILD_SHELTER) != 0) {
                addClearance(level, clearance, config.shelterTemplate.toBox(), shelterOffset);
                addConcreteShell(level, clearance, config.shelterTemplate.toBox(), shelterOffset);
            }
            if ((buildMask & BUILD_RESIDENTIAL) != 0)
                addChunks(clones, config.residentialTemplate.toBox(), offset, resTpl);
            if ((buildMask & BUILD_SHELTER) != 0)
                addChunks(clones, config.shelterTemplate.toBox(), shelterOffset, shelTpl);
            // 搜索区不克隆：所有队共用原模板区域（各队玩家会在同一片野外相遇——搜打撤对抗即来源于此）

            team.residentialSpawn = spawnFor(config.residentialSpawn, residentialBox, offset);
            team.shelterSpawn = spawnFor(config.shelterSpawn, shelterBox, shelterOffset);
            // 回家门 / 危险区盒：只认探索区出口门绑定（建在避难所外的那些门）。一队一扇<b>专属</b>门，
            // 按队序号顺序分配、不取模复用。门不够分的队没有专属回家门（returnDoorPos 留 null）——
            // 出门探索现在落在所点门外、全世界自由活动，本就不需要一个「探索区落点」，故不再有全局兜底。
            team.searchZoneSpawn = null;
            team.returnDoorPos = null;
            team.searchZoneBox = null;
            SixtySecondsConfig.DoorBinding exitDoor = team.teamId < exitDoorBindings.size()
                    ? exitDoorBindings.get(team.teamId)
                    : null;
            if (exitDoor != null) {
                team.searchZoneSpawn = exitDoor.spawn.toBlockPos().offset(arenaDelta);
                team.returnDoorPos = exitDoor.door.toBlockPos().offset(arenaDelta);
                AABB bound = aabbOf(exitDoor.boxMin, exitDoor.boxMax, BlockPos.ZERO);
                // 绑定盒太小（快速绑定点了同一格等）视为未圈定 → 留 null（该区无危险区盒，按全局基线算等级）
                if (bound.getXsize() >= 8 && bound.getZsize() >= 8) {
                    team.searchZoneBox = bound;
                }
            }
            team.residentialBox = boxOf(residentialBox, offset);
            team.shelterBox = boxOf(shelterBox, shelterOffset);

            team.searchDoors.clear();
            for (SixtySecondsConfig.DoorBinding b : shelterDoorBindings) {
                // 门在各队克隆的住宅/避难所里：住宅门加网格偏移、避难所门加避难所偏移（两者在锚定模式下不同）；
                // 绑定的探索区用原区域（不加偏移，全队共用）
                BlockPos templateDoor = b.door.toBlockPos();
                BlockPos doorAbs = templateDoor.offset(shelterBox.isInside(templateDoor) ? shelterOffset : offset);
                BlockPos spawnAbs = b.spawn.toBlockPos().offset(arenaDelta);
                AABB boxAbs = aabbOf(b.boxMin, b.boxMax, BlockPos.ZERO);
                team.searchDoors.put(doorAbs, new SixtySecondsState.TeamData.SearchLink(spawnAbs, boxAbs));
            }
        }
        int teams = data.teams.size();
        if (!exitDoorBindings.isEmpty() && exitDoorBindings.size() < teams) {
            SixtySeconds.LOGGER.warn("[60s] Exploration zone has only {} exit doors, fewer than {} teams: extra teams have no dedicated home door"
                    + " (night-raid anchor defaults to null, can only return via their own shelter door). Suggest binding more exit doors in the exploration zone.",
                    exitDoorBindings.size(), teams);
        }
        warnOverlappingShelters(config, shelterOffsets);
        // 全局先净空、后克隆（原因见上）
        List<WorkItem> work = new ArrayList<>(clearance.size() + clones.size());
        work.addAll(clearance);
        work.addAll(clones);
        // ocean 模式：在 Y=-40 铺一层地板，托住所有建筑（建筑最低层在 -39）。放在净空/克隆之后，
        // 避免被净空（会下挖到 -41）把地板挖掉；地板自身走快照，局末照常还原。
        if (ocean && arenaMinX <= arenaMaxX) {
            int margin = 3;
            BoundingBox floorBox = BoundingBox.fromCorners(
                    new BlockPos(arenaMinX - margin, -40, arenaMinZ - margin),
                    new BlockPos(arenaMaxX + margin, -40, arenaMaxZ + margin));
            work.add(new WorkItem(floorBox, BlockPos.ZERO, FLOOR_STATE, false));
        }
        // 预建（/60s build）时游戏尚未 RUNNING，但仍需建图，故用独立的 BUILDING 标记驱动 BuildTask，
        // 不再依赖 RUNNING（否则预建会在首 tick 被误判中止）。
        net.exmo.sixty_seconds.SixtySecondsMod.BUILDING = true;
        GameUtils.serverTaskQueue.add(new BuildTask(level, work, onComplete, teams));
        SixtySeconds.LOGGER.info("[60s] Starting async build: {} teams, {} sub-boxes placed in batches.", teams, work.size());
    }

    /**
     * 建图前校验每队克隆区的 Y 是否落在世界建筑高度内。
     * <p>
     * {@link net.minecraft.world.level.Level#setBlock} 对超出建筑高度的坐标<b>静默返回 false</b>：
     * 越界时建图任务照跑、进度照报，却一格都没放下，玩家随后被传送到空无一物的半空——
     * 现象与「建图很慢」几乎无法区分。所以这里提前拦下并把该填的数算给管理员，而不是建了个寂寞。
     * <p>
     * 注意 {@code teamBase} 是<b>相对模板的偏移量</b>（见 {@link SixtySecondsConfig#teamBase}），
     * 不是住宅要落到的绝对坐标——把绝对坐标填进去正是越界的常见来源。
     */
    private static boolean heightsFit(ServerLevel level, SixtySecondsConfig config,
            SixtySecondsState.Data data, List<BlockPos> residentialOffsets, List<BlockPos> shelterOffsets) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        boolean ok = true;
        int index = 0;
        for (SixtySecondsState.TeamData ignored : data.teams.values()) {
            ok &= fits(level, "住宅", config.residentialTemplate.toBox(), residentialOffsets.get(index),
                    index, minY, maxY, "住宅贴地偏移");
            ok &= fits(level, "避难所", config.shelterTemplate.toBox(), shelterOffsets.get(index),
                    index, minY, maxY, "shelterAnchorDoor / teamBase.y");
            index++;
        }
        if (!ok) {
            SixtySeconds.LOGGER.error("[60s] Clone region exceeds world build height ({}~{}) — build aborted to avoid 'build shows progress but places nothing, "
                    + "players teleported into mid-air'. Adjust sixty_seconds_config.json within the ranges given above, then restart the game.",
                    minY, maxY);
        }
        return ok;
    }

    /** 单个模板盒在给定偏移下是否放得进世界高度；越界时日志直接给出该偏移的可用区间。 */
    private static boolean fits(ServerLevel level, String what, BoundingBox template, BlockPos offset,
            int index, int minY, int maxY, String field) {
        int lo = template.minY() + offset.getY();
        int hi = template.maxY() + offset.getY();
        if (lo >= minY && hi <= maxY) {
            return true;
        }
        SixtySeconds.LOGGER.error("[60s] Team {}'s {} clone at y={}~{} exceeds world build height {}~{}."
                        + " Template y={}~{} ({} tall), current Y offset {}; usable range for {} is {}~{}.",
                index + 1, what, lo, hi, minY, maxY,
                template.minY(), template.maxY(), template.getYSpan(), offset.getY(),
                field, minY - template.minY(), maxY - template.maxY());
        return false;
    }

    /**
     * 锚定模式下两队的出口门可能挨得太近，导致两座避难所（含净空环带）在探索区里叠在一起——
     * 后建的会覆盖先建的，两队共用一堵墙甚至互相打通。这是地图配置问题（门该拉开到大于避难所模板尺寸），
     * 建图不中止，但必须在日志里点名，否则现象是「某队的避难所莫名少了半间房」，极难排查。
     */
    private static void warnOverlappingShelters(SixtySecondsConfig config, List<BlockPos> shelterOffsets) {
        BoundingBox template = config.shelterTemplate.toBox();
        for (int a = 0; a < shelterOffsets.size(); a++) {
            for (int b = a + 1; b < shelterOffsets.size(); b++) {
                if (shelterOffsets.get(a).equals(shelterOffsets.get(b))) {
                    continue; // 同偏移=同一份回退网格位（网格本就按队错开，不是重叠）
                }
                AABB baseA = boxOf(template, shelterOffsets.get(a));
                AABB baseB = boxOf(template, shelterOffsets.get(b));
                if (baseA.inflate(CLEAR_MARGIN).intersects(baseB.inflate(CLEAR_MARGIN))) {
                    SixtySeconds.LOGGER.warn("[60s] Team {} and team {} shelter placements overlap (exit doors closer than the shelter template):"
                            + " later one overwrites earlier. Move these two exploration-zone exit doors apart by more than shelter template size + {} clearance.",
                            a + 1, b + 1, CLEAR_MARGIN);
                } else if (baseA.inflate(100).intersects(baseB)) {
                    // 未重叠但间距不足 100 格（anchor 模式跟随出口门，无法自动校正，仅报警）。
                    SixtySeconds.LOGGER.error("[60s] Team {} and team {} shelter placements are closer than 100 blocks; generation will be incomplete.",
                            a + 1, b + 1);
                }
            }
        }
    }

    /**
     * 逐队算出<b>避难所</b>的克隆偏移（住宅始终走网格，不受此影响）。
     * <p>
     * 门锚定模式（{@code shelterAtSearchDoorEnabled} + 已登记 {@code shelterAnchorDoor} + 该队分到了探索区出口门）：
     * 偏移 = {@code 出口门 - 锚点门}，即把整座避难所平移到「锚点门正好压在这队那扇出口门上」——避难所就直接长在
     * 探索区的门位置，出门即探索。其余情况（开关关 / 没登记锚点 / 门不够分）回退到队伍网格偏移 {@code teamOffset(index)}。
     * <p>
     * 注意锚定模式下<b>不</b>叠加网格偏移：出口门本身已是各队互不相同的世界坐标，再加网格会把避难所甩出探索区。
     */
    private static List<BlockPos> shelterOffsets(SixtySecondsConfig config, SixtySecondsState.Data data,
            List<SixtySecondsConfig.DoorBinding> exitDoorBindings, boolean ocean) {
        // ocean 模式：始终走网格，并把整座避难所压到 y≈-39（地板贴 Y=-40）；不锚定出口门、不埋地
        if (ocean) {
            List<BlockPos> offsets = new ArrayList<>();
            for (int index = 0; index < data.teams.size(); index++) {
                BlockPos g = config.teamOffset(index);
                offsets.add(new BlockPos(g.getX(), -39 - config.shelterTemplate.min.y, g.getZ()));
            }
            return offsets;
        }
        if (config.rvEnabled) {
            List<BlockPos> offsets = new ArrayList<>();
            for (int index = 0; index < data.teams.size(); index++) {
                // 庇护所建造基准 Y 调到 -30：模板最低层落在 Y=-30（门锚定模式才走上面分支贴出口门）
                BlockPos g = config.teamOffset(index);
                offsets.add(new BlockPos(g.getX(), -30 - config.shelterTemplate.min.y, g.getZ()));
            }
            return offsets;
        }
        boolean wantAnchor = config.shelterAtSearchDoorEnabled;
        BlockPos anchor = config.shelterAnchorDoor == null ? null : config.shelterAnchorDoor.toBlockPos();
        if (wantAnchor && anchor == null) {
            SixtySeconds.LOGGER.warn("[60s] shelter_at_door enabled but no shelter anchor door registered"
                    + " (/60s_area anchor <x y z>); this game's shelters fall back to grid clone.");
        }
        List<BlockPos> offsets = new ArrayList<>();
        int anchored = 0;
        for (int index = 0; index < data.teams.size(); index++) {
            SixtySecondsConfig.DoorBinding exitDoor = index < exitDoorBindings.size()
                    ? exitDoorBindings.get(index)
                    : null;
            if (wantAnchor && anchor != null && exitDoor != null) {
                offsets.add(exitDoor.door.toBlockPos().subtract(anchor));
                anchored++;
            } else {
                // 庇护所建造基准 Y 调到 -30：模板最低层落在 Y=-30（门锚定模式才走上面分支贴出口门）
                BlockPos g = config.teamOffset(index);
                offsets.add(new BlockPos(g.getX(), -30 - config.shelterTemplate.min.y, g.getZ()));
            }
        }
        if (wantAnchor && anchor != null && anchored < data.teams.size()) {
            SixtySeconds.LOGGER.warn("[60s] shelter_at_door enabled, but only {} / {} teams got an exploration-zone exit door,"
                    + " the rest fall back to grid clone. Bind more exit doors in the exploration zone.", anchored, data.teams.size());
        }
        return offsets;
    }

    /**
     * 智能下沉埋地：避难所模板里若放了活板门（{@link net.exmo.sixty_seconds.content.block.ShelterTrapdoorBlock}），
     * 把每队 {@code shelterOffset} 的 Y 压低，让活板门顶层落在<b>该队落位处的实际地表</b>——避难所埋进地里、只露活板门。
     * 返回是否发生了下沉（供 {@code build} 跳过避难所净空，避免挖坑暴露基地）。模板没活板门/开关关则不动、返回 false。
     */
    private static boolean applyShelterBurial(ServerLevel level, SixtySecondsConfig config, List<BlockPos> offsets) {
        if (!config.shelterBuryEnabled) {
            return false;
        }
        BlockPos trapdoor = findShelterTrapdoor(level, config);
        if (trapdoor == null) {
            return false;
        }
        BoundingBox tpl = config.shelterTemplate.toBox();
        int minY = level.getMinBuildHeight();
        for (int i = 0; i < offsets.size(); i++) {
            BlockPos off = offsets.get(i);
            int wx = trapdoor.getX() + off.getX();
            int wz = trapdoor.getZ() + off.getZ();
            level.getChunk(wx >> 4, wz >> 4); // 强载该列，getHeight 需要区块已加载
            // 地表最高实心块的 Y（getHeight 返回其上方第一格空气的 y，减 1 即顶面）
            int surfaceTop = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    wx, wz) - 1;
            int curTrapY = trapdoor.getY() + off.getY();
            int newY = off.getY() + (surfaceTop - curTrapY); // 让活板门顶层 = 地表顶面
            // 防击穿世界底：避难所模板最低层不能低于世界底
            if (tpl.minY() + newY < minY) {
                newY = minY - tpl.minY();
            }
            offsets.set(i, new BlockPos(off.getX(), newY, off.getZ()));
        }
        SixtySeconds.LOGGER.info("[60s] Shelter contains trapdoor → smart sink-burial ({} teams aligned to their own surface).", offsets.size());
        return true;
    }

    /** 扫描避难所模板盒，找第一块活板门主控块（模板绝对坐标）；没有返回 null。 */
    private static BlockPos findShelterTrapdoor(ServerLevel level, SixtySecondsConfig config) {
        BoundingBox box = config.shelterTemplate.toBox();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = box.minY(); y <= box.maxY(); y++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).getBlock()
                            instanceof net.exmo.sixty_seconds.content.block.ShelterTrapdoorBlock) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }

    /**
     * 加载结构模板 NBT（保留方块实体/箱子内容物）。优先读世界存档
     * {@code sixty_seconds_templates/<name>.nbt}（玩家用 {@code /60s export_template} 导出的自定义模板），
     * 不存在时回退到模组内置默认模板 {@code data/sixty_seconds/templates/<name>.nbt}
     * （默认 {@code house1}/{@code shelter1}，随模组 jar 分发）。两者均无时返回 null，
     * 此时调用方应回退到从世界克隆。
     */
    public static CompoundTag loadTemplate(ServerLevel level, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // 1) 世界存档下的自定义导出模板优先（覆盖模组内置）
        Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("sixty_seconds_templates");
        Path nbtPath = dir.resolve(name + ".nbt");
        if (Files.exists(nbtPath)) {
            try (java.io.InputStream in = Files.newInputStream(nbtPath)) {
                return NbtIo.readCompressed(in, new NbtAccounter(Long.MAX_VALUE, Integer.MAX_VALUE));
            } catch (Exception e) {
                SixtySeconds.LOGGER.warn("[60s] Failed to load template {}: {}", name, e.getMessage());
            }
        }
        // 2) 回退到模组内置默认模板
        try (java.io.InputStream in = SixtySeconds.class
                .getResourceAsStream("/data/sixty_seconds/templates/" + name + ".nbt")) {
            if (in != null) {
                SixtySeconds.LOGGER.info("[60s] Using built-in mod template: {}", name);
                return NbtIo.readCompressed(in, new NbtAccounter(Long.MAX_VALUE, Integer.MAX_VALUE));
            }
        } catch (Exception e) {
            SixtySeconds.LOGGER.warn("[60s] Failed to load built-in template {}: {}", name, e.getMessage());
        }
        SixtySeconds.LOGGER.warn("[60s] Template {} not found (neither world export dir nor built-in mod) — falling back to world clone", name);
        return null;
    }


    /**
     * 清理竞技场区域（模板区 + 各队克隆区）内上一局残留的尸体和掉落物。
     * 每局开始前调用，防止旧尸体/物品出现在新局的队伍区域中。
     * <p>
     * 三层配合，缺一会漏：
     * <ol>
     *   <li><b>同步清扫</b>（本方法）：只覆盖当前已加载的实体。</li>
     *   <li><b>ENTITY_LOAD 窗口</b>（{@link #registerEntityClearWindow}）：卸载区块里的残留实体
     *       要等区块加载才入世界（克隆区由建图 setBlock 逐步加载、住宅由玩家传送加载），入世界瞬间按区清掉。</li>
     *   <li><b>强载搜索区</b>：搜索区不克隆、建图不触碰其区块，这里主动同步加载，
     *       让其中的残留实体在窗口内入世界被第 2 层清掉（否则要等局中首个探索者踩进去才冒出来）。</li>
     * </ol>
     */
    private static void clearArenaEntities(ServerLevel level, SixtySecondsConfig config,
            List<BlockPos> shelterOffsets, List<BlockPos> residentialOffsets, SixtySecondsState.Data data) {
        if (config == null || !config.isComplete()) {
            return;
        }
        List<AABB> zones = new ArrayList<>();
        // 模板源区域（玩家可能在这里死亡并留下尸体）
        zones.add(boxOf(config.residentialTemplate.toBox(), BlockPos.ZERO));
        zones.add(boxOf(config.shelterTemplate.toBox(), BlockPos.ZERO));
        // 各队克隆区（队数无上限，沿 X 网格延伸；住宅用实际贴地落位）
        for (int i = 0; i < data.teams.size() && i < residentialOffsets.size(); i++) {
            zones.add(boxOf(config.residentialTemplate.toBox(), residentialOffsets.get(i)));
            zones.add(boxOf(config.shelterTemplate.toBox(), shelterOffsets.get(i)));
        }
        // 本局避难所的<b>实际</b>落位：门锚定模式下不在网格上，上面那圈网格盒扫不到它——
        // 不补这一段，长在探索区门口的避难所里会留着上一局的尸体/掉落物
        for (BlockPos shelterOffset : shelterOffsets) {
            zones.add(boxOf(config.shelterTemplate.toBox(), shelterOffset));
        }

        // 先收集再删除：遍历 getAllEntities() 途中 discard 会并发修改实体存储（迭代器可能吐 null 直接 NPE）
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity == null || (!(entity instanceof PlayerBodyEntity) && !(entity instanceof ItemEntity))) {
                continue;
            }
            for (AABB zone : zones) {
                if (zone.contains(entity.position())) {
                    toRemove.add(entity);
                    break;
                }
            }
        }
        for (Entity entity : toRemove) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        if (!toRemove.isEmpty()) {
            SixtySeconds.LOGGER.info("[60s] Cleaned arena leftovers: {} corpses/drops.", toRemove.size());
        }

        // 布防迟到实体清理窗口：建图全程生效（deadline 由 BuildTask 完成后收尾）
        CLEAR_ZONES.clear();
        CLEAR_ZONES.addAll(zones);
        clearZonesDeadline = Long.MAX_VALUE;
    }

    /** 建图收尾：把清理窗口收成短尾窗（覆盖玩家传送进场后的实体入世界），过期自动作废。 */
    private static void armClearZonesTail(ServerLevel level) {
        if (clearZonesDeadline == Long.MAX_VALUE) {
            clearZonesDeadline = level.getGameTime() + CLEAR_TAIL_TICKS;
        }
    }

    /**
     * 出生点配置 → 该队的世界坐标：写<b>模板内绝对坐标</b>时自动换算成相对模板 min 的偏移（克隆区 = 模板 + offset，
     * 故等价于直接加 offset）；不在模板盒内的值按「相对模板 min 的偏移」理解（兼容旧相对写法）。
     */
    private static BlockPos spawnFor(SixtySecondsConfig.Vec spawn, BoundingBox template, BlockPos offset) {
        BlockPos pos = spawn.toBlockPos();
        if (!template.isInside(pos)) {
            pos = pos.offset(template.minX(), template.minY(), template.minZ());
        }
        BlockPos result = pos.offset(offset);
        SixtySeconds.LOGGER.info("[60s] spawnFor: spawn(相对/绝对)={} template=[{},{},{}]~[{},{},{}] offset={} -> 世界落点={}",
                spawn, template.minX(), template.minY(), template.minZ(),
                template.maxX(), template.maxY(), template.maxZ(), offset, result);
        return result;
    }

    /** 把模板盒切成 ≈{@link #CHUNK_TARGET} 方块的子盒，每个配上该队偏移，作为一个放置工作项。
     *  {@code template} 非空时该工作项按导出结构模板 NBT 放置（保留箱子内容物），否则从世界克隆。 */
    private static void addChunks(List<WorkItem> work, BoundingBox templateBox, BlockPos offset, CompoundTag template) {
        BlockPos boxOrigin = new BlockPos(templateBox.minX(), templateBox.minY(), templateBox.minZ());
        for (BoundingBox chunk : buildChunks(templateBox, CHUNK_TARGET)) {
            work.add(new WorkItem(chunk, offset, template, boxOrigin));
        }
    }

    /** 克隆区净空的水平外扩（格）与上方净空高度（格）。 */
    private static final int CLEAR_MARGIN = 2;
    private static final int CLEAR_HEADROOM = 12;
    /** ocean 模式竞技场地板方块（Y=-40 一层，托住所有建筑）。 */
    private static final BlockState FLOOR_STATE = Blocks.STONE.defaultBlockState();
    /** 凿空区外围外壳方块（黑色混凝土，不渗液）：在凿空区最外层 5 面（顶 + 四壁，底面不铺）封一圈，
     *  防止建图瞬间周边液体（水/岩浆）大量涌入凿空区、触发海量方块更新与光照重算把整合服务器压死。 */
    private static final BlockState CONCRETE_SHELL = Blocks.BLACK_CONCRETE.defaultBlockState();

    /**
     * 净空工作项：把克隆目标区<b>四周 {@link #CLEAR_MARGIN} 格环带 + 上方 {@link #CLEAR_HEADROOM} 格</b>
     * 挖成空气——网格排布随队数沿 X 无限延伸，排出预平整区后克隆区会叠进自然山体
     * （模板盒内会被克隆覆写，但盒外的山把房子包住/压顶、门口被堵——「超过 13 队卡到山里」根因）。
     * 净空区同样走快照（非空气才记录），局末照常还原地形。
     */
    private static void addClearance(ServerLevel level, List<WorkItem> work, BoundingBox templateBox,
            BlockPos offset) {
        int minX = templateBox.minX();
        int maxX = templateBox.maxX();
        int minY = templateBox.minY();
        int maxY = templateBox.maxY();
        int minZ = templateBox.minZ();
        int maxZ = templateBox.maxZ();
        // 上方净空（含四角外扩），y 顶不超过世界建筑高度
        int topY = Math.min(maxY + CLEAR_HEADROOM, level.getMaxBuildHeight() - 1 - offset.getY());
        List<BoundingBox> boxes = new ArrayList<>();
        if (topY > maxY) {
            boxes.add(BoundingBox.fromCorners(
                    new BlockPos(minX - CLEAR_MARGIN, maxY + 1, minZ - CLEAR_MARGIN),
                    new BlockPos(maxX + CLEAR_MARGIN, topY, maxZ + CLEAR_MARGIN)));
        }
        // 四周环带（与模板盒同高）：西/东两条全长，南/北两条只补中段（避免与西东重叠重复快照）
        boxes.add(BoundingBox.fromCorners(new BlockPos(minX - CLEAR_MARGIN, minY, minZ - CLEAR_MARGIN),
                new BlockPos(minX - 1, maxY, maxZ + CLEAR_MARGIN)));
        boxes.add(BoundingBox.fromCorners(new BlockPos(maxX + 1, minY, minZ - CLEAR_MARGIN),
                new BlockPos(maxX + CLEAR_MARGIN, maxY, maxZ + CLEAR_MARGIN)));
        boxes.add(BoundingBox.fromCorners(new BlockPos(minX, minY, minZ - CLEAR_MARGIN),
                new BlockPos(maxX, maxY, minZ - 1)));
        boxes.add(BoundingBox.fromCorners(new BlockPos(minX, minY, maxZ + 1),
                new BlockPos(maxX, maxY, maxZ + CLEAR_MARGIN)));
        for (BoundingBox box : boxes) {
            for (BoundingBox chunk : buildChunks(box, CHUNK_TARGET)) {
                work.add(new WorkItem(chunk, offset, true));
            }
        }
    }

    /**
     * 在凿空区（净空盒）最外层铺一层黑色混凝土外壳——顶面 + 四壁共 5 面，<b>底面不铺</b>。
     * <p>为什么要有这层壳：建图瞬间净空会把模板四周/上方的自然地形整片挖开。若挖进水体或岩浆层，
     * 周边液体会立刻从四面八方涌入刚挖空的区域，每一格流动方块都会触发一次方块更新 + 光照重算，
     * 在「每 tick 最多放 2000 格、且带 UPDATE_CLIENTS」的建图节奏下，这两股洪流叠加会把整合服务器线程压死，
     * 客户端等不到世界同步数据而超时（即建图期出现的 "Timed out waiting for world statistics"）。
     * 在最外层包一圈不渗水的黑色混凝土，等于给挖开区加了个盖子，把「瞬时液体大量流动」压到接近零。</p>
     * <p>本模组<b>不做任何地形还原</b>（建图快照只记录、从不回放），所以这层外壳是永久的——
     * 它把建筑体封在一只不渗液的混凝土盒子里，建图后一直留着，正好持续隔绝周边液体。</p>
     * <p>必须在 {@link #addClearance} <b>之后</b>调用：先由净空把外壳所在位置挖成空气，再由本方法覆写成混凝土。</p>
     */
    private static void addConcreteShell(ServerLevel level, List<WorkItem> work, BoundingBox templateBox,
            BlockPos offset) {
        int minX = templateBox.minX() - CLEAR_MARGIN;
        int maxX = templateBox.maxX() + CLEAR_MARGIN;
        int minY = templateBox.minY();
        int minZ = templateBox.minZ() - CLEAR_MARGIN;
        int maxZ = templateBox.maxZ() + CLEAR_MARGIN;
        int topY = Math.min(templateBox.maxY() + CLEAR_HEADROOM, level.getMaxBuildHeight() - 1 - offset.getY());
        // 顶面（y=topY 一整片）
        work.add(new WorkItem(BoundingBox.fromCorners(
                new BlockPos(minX, topY, minZ), new BlockPos(maxX, topY, maxZ)), offset, CONCRETE_SHELL, true));
        // 西壁（x=minX）
        work.add(new WorkItem(BoundingBox.fromCorners(
                new BlockPos(minX, minY, minZ), new BlockPos(minX, topY, maxZ)), offset, CONCRETE_SHELL, true));
        // 东壁（x=maxX）
        work.add(new WorkItem(BoundingBox.fromCorners(
                new BlockPos(maxX, minY, minZ), new BlockPos(maxX, topY, maxZ)), offset, CONCRETE_SHELL, true));
        // 北壁（z=minZ）
        work.add(new WorkItem(BoundingBox.fromCorners(
                new BlockPos(minX, minY, minZ), new BlockPos(maxX, topY, minZ)), offset, CONCRETE_SHELL, true));
        // 南壁（z=maxZ）
        work.add(new WorkItem(BoundingBox.fromCorners(
                new BlockPos(minX, minY, maxZ), new BlockPos(maxX, topY, maxZ)), offset, CONCRETE_SHELL, true));
        // 底面不铺（保持开放）
    }

    /** 三维分块（仿 {@code FullTrainResetTask.buildChunks}）：按体积比例切分，保证至少 1 块。 */
    private static List<BoundingBox> buildChunks(BoundingBox box, int target) {
        List<BoundingBox> chunks = new ArrayList<>();
        int xLen = box.maxX() - box.minX() + 1;
        int yLen = box.maxY() - box.minY() + 1;
        int zLen = box.maxZ() - box.minZ() + 1;
        double scale = Math.cbrt((double) target / ((double) xLen * yLen * zLen));
        int cx = Math.max(1, Math.min(xLen, (int) Math.ceil(xLen * scale)));
        int cy = Math.max(1, Math.min(yLen, (int) Math.ceil(yLen * scale)));
        int cz = Math.max(1, Math.min(zLen, (int) Math.ceil(zLen * scale)));
        for (int y = box.minY(); y <= box.maxY(); y += cy) {
            int yMax = Math.min(box.maxY(), y + cy - 1);
            for (int x = box.minX(); x <= box.maxX(); x += cx) {
                int xMax = Math.min(box.maxX(), x + cx - 1);
                for (int z = box.minZ(); z <= box.maxZ(); z += cz) {
                    int zMax = Math.min(box.maxZ(), z + cz - 1);
                    chunks.add(BoundingBox.fromCorners(new BlockPos(x, y, z), new BlockPos(xMax, yMax, zMax)));
                }
            }
        }
        return chunks;
    }

    /** 放置一个工作项：净空项 = 清容器后挖成空气；克隆项 = 按模板/世界克隆；地板项 = 铺方块。本模组不做地形还原。 */
    private static void placeWorkItem(ServerLevel level, WorkItem item) {
        if (item.isFill()) {
            // 铺设方块项（ocean Y=-40 地板 / 凿空区黑色混凝土外壳）<b>不</b>记录任何原始地形：
            // 本模组不做任何地形还原（快照只记不回放），且此处落点本就被净空挖成了空气，再记只会把空气当原状污染快照表。
            BlockState fs = item.fill();
            for (int y = item.src.minY(); y <= item.src.maxY(); y++) {
                for (int x = item.src.minX(); x <= item.src.maxX(); x++) {
                    for (int z = item.src.minZ(); z <= item.src.maxZ(); z++) {
                        BlockPos dst = new BlockPos(x + item.offset.getX(), y + item.offset.getY(), z + item.offset.getZ());
                        if (!item.overwrite() && level.getBlockState(dst).isAir()) {
                            continue;
                        }
                        net.minecraft.world.Clearable.tryClear(level.getBlockEntity(dst));
                        level.setBlock(dst, fs, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        level.getLightEngine().checkBlock(dst);
                    }
                }
            }
            return;
        }
        BoundingBox src = item.src;
        BlockPos offset = item.offset;
        if (item.clearOnly()) {
            // 净空：只处理非空气格（山体/树木等），清容器 → 挖成空气
            net.minecraft.world.level.block.state.BlockState air =
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int y = src.minY(); y <= src.maxY(); y++) {
                for (int x = src.minX(); x <= src.maxX(); x++) {
                    for (int z = src.minZ(); z <= src.maxZ(); z++) {
                        BlockPos dst = new BlockPos(x + offset.getX(), y + offset.getY(), z + offset.getZ());
                        if (level.getBlockState(dst).isAir()) {
                            continue;
                        }
                        net.minecraft.world.Clearable.tryClear(level.getBlockEntity(dst));
                        level.setBlock(dst, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        level.getLightEngine().checkBlock(dst);
                    }
                }
            }
            return;
        }
        for (int y = src.minY(); y <= src.maxY(); y++) {
            for (int x = src.minX(); x <= src.maxX(); x++) {
                for (int z = src.minZ(); z <= src.maxZ(); z++) {
                    BlockPos dst = new BlockPos(x + offset.getX(), y + offset.getY(), z + offset.getZ());
                    net.minecraft.world.Clearable.tryClear(level.getBlockEntity(dst));
                }
            }
        }
        if (item.template() != null) {
            copyFromTemplate(level, src, item.boxOrigin(), offset, item.template());
        } else {
            BlockCopyUtils.copyLayer(level, src, offset);
        }
    }

    /**
     * 按导出的结构模板 NBT（保留方块实体/箱子内容物）把子盒克隆到目标偏移。
     * {@code boxOrigin} 为<b>完整</b>模板盒（config 盒）的原点，{@code src} 是它被切成的一个子盒；
     * 落点换算统一用 boxOrigin（而非 src.min，否则分块后每块各自原点错位），使模板内相对坐标正确映射到
     * 世界位置，且不受模板 NBT 原始捕获坐标空间影响（内置 shelter1 等也能落到玩家配置的 region）。
     * 未列出的方块（空气）按空气放置。
     */
    private static void copyFromTemplate(ServerLevel level, BoundingBox src, BlockPos boxOrigin, BlockPos offset, CompoundTag template) {
        ListTag blocks = template.getList("blocks", Tag.TAG_COMPOUND);
        ListTag palette = template.getList("palette", Tag.TAG_COMPOUND);
        if (blocks.isEmpty()) {
            return; // 空模板，无内容可放
        }
        // 模板自身原点 = NBT 内所有方块 pos 的最小值（与克隆区 config 盒坐标无关）
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (int i = 0; i < blocks.size(); i++) {
            int[] pos = blocks.getCompound(i).getIntArray("pos");
            minX = Math.min(minX, pos[0]);
            minY = Math.min(minY, pos[1]);
            minZ = Math.min(minZ, pos[2]);
        }
        BlockPos templateMin = new BlockPos(minX, minY, minZ);
        // 相对坐标（模板内，以 NBT 自身原点为 0）→ (方块状态, 方块实体 NBT)。
        // 旧实现用 NBT 绝对坐标当 key、并要求 config 盒坐标 == NBT 坐标空间，导致内置模板
        // （如 shelter1，捕获于原点坐标）与玩家配置的 region（世界其它位置）对不上 → 整片空放。
        // 现统一按「模板相对原点」建查表，落点只取决于 config 盒原点 + 偏移，与模板原始坐标空间无关。
        Map<BlockPos, TemplateBlock> lookup = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag e = blocks.getCompound(i);
            int[] pos = e.getIntArray("pos");
            BlockPos rel = new BlockPos(pos[0] - minX, pos[1] - minY, pos[2] - minZ);
            BlockState state = NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),
                    palette.getCompound(e.getInt("state")));
            CompoundTag nbt = e.contains("nbt") ? e.getCompound("nbt") : null;
            lookup.put(rel, new TemplateBlock(state, nbt));
        }
        BlockPos srcOrigin = boxOrigin;
        // 先放方块（air 为未列出项默认）：克隆区每一格按「相对完整盒原点」去查模板相对原点 → 不论模板 NBT
        // 原始坐标在哪儿，都能正确落到 config 盒所在的世界位置（分块子盒也统一以完整盒原点换算）。
        for (int y = src.minY(); y <= src.maxY(); y++) {
            for (int x = src.minX(); x <= src.maxX(); x++) {
                for (int z = src.minZ(); z <= src.maxZ(); z++) {
                    BlockPos srcPos = new BlockPos(x, y, z);
                    BlockPos rel = srcPos.subtract(srcOrigin);
                    TemplateBlock tb = lookup.get(rel);
                    BlockState state = tb != null ? tb.state : Blocks.AIR.defaultBlockState();
                    BlockPos dst = srcPos.offset(offset);
                    level.setBlock(dst, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    level.getLightEngine().checkBlock(dst);
                }
            }
        }
        // 再放方块实体（箱子内容物等）
        for (int y = src.minY(); y <= src.maxY(); y++) {
            for (int x = src.minX(); x <= src.maxX(); x++) {
                for (int z = src.minZ(); z <= src.maxZ(); z++) {
                    BlockPos srcPos = new BlockPos(x, y, z);
                    BlockPos rel = srcPos.subtract(srcOrigin);
                    TemplateBlock tb = lookup.get(rel);
                    if (tb == null || tb.nbt == null) {
                        continue;
                    }
                    BlockPos dst = srcPos.offset(offset);
                    BlockEntity dstBE = level.getBlockEntity(dst);
                    if (dstBE != null) {
                        CompoundTag tag = tb.nbt.copy();
                        tag.putInt("x", dst.getX());
                        tag.putInt("y", dst.getY());
                        tag.putInt("z", dst.getZ());
                        dstBE.loadWithComponents(tag, level.registryAccess());
                        dstBE.setChanged();
                    }
                }
            }
        }
    }

    /** 模板里单格：方块状态 + 可选的方块实体 NBT（箱子内容物等）。 */
    private static final class TemplateBlock {
        final BlockState state;
        final CompoundTag nbt;
        TemplateBlock(BlockState state, CompoundTag nbt) {
            this.state = state;
            this.nbt = nbt;
        }
    }

    private static AABB boxOf(BoundingBox box, BlockPos offset) {
        return new AABB(
                box.minX() + offset.getX(), box.minY() + offset.getY(), box.minZ() + offset.getZ(),
                box.maxX() + offset.getX() + 1, box.maxY() + offset.getY() + 1, box.maxZ() + offset.getZ() + 1);
    }

    private static AABB aabbOf(SixtySecondsConfig.Vec min, SixtySecondsConfig.Vec max, BlockPos offset) {
        int minX = Math.min(min.x, max.x) + offset.getX();
        int minY = Math.min(min.y, max.y) + offset.getY();
        int minZ = Math.min(min.z, max.z) + offset.getZ();
        int maxX = Math.max(min.x, max.x) + offset.getX();
        int maxY = Math.max(min.y, max.y) + offset.getY();
        int maxZ = Math.max(min.z, max.z) + offset.getZ();
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /** 一个放置工作项：源模板子盒 + 该队网格偏移；{@code clearOnly}=净空项（目标区挖成空气，不克隆）。
     *  {@code template} 非空时按导出的结构模板 NBT（保留方块实体/箱子内容物）放置，否则从世界克隆。
     *  {@code fill} 非空时为本项铺设方块：ocean 模式 Y=-40 地板（石）或凿空区黑色混凝土外壳；本模组不做任何地形还原，故不记快照。
     *  {@code overwrite}=true 时连空气格也强制覆写（混凝土外壳需覆盖已凿空的空气区），false 时跳过空气（地板沿用旧行为）。三者互斥。 */
    private record WorkItem(BoundingBox src, BlockPos offset, boolean clearOnly, CompoundTag template,
            BlockState fill, boolean overwrite, BlockPos boxOrigin) {
        WorkItem(BoundingBox src, BlockPos offset) {
            this(src, offset, false, null, null, false, null);
        }
        WorkItem(BoundingBox src, BlockPos offset, boolean clearOnly) {
            this(src, offset, clearOnly, null, null, false, null);
        }
        WorkItem(BoundingBox src, BlockPos offset, CompoundTag template) {
            this(src, offset, false, template, null, false, null);
        }
        /** template 非空时携带「完整模板盒原点」，供 copyFromTemplate 按模板相对原点映射，
         *  避免分块后每个子盒各自以自身 min 当原点导致整体错位。 */
        WorkItem(BoundingBox src, BlockPos offset, CompoundTag template, BlockPos boxOrigin) {
            this(src, offset, false, template, null, false, boxOrigin);
        }
        /** fill 非空时铺设该方块；overwrite=true 时连空气格也覆盖（混凝土外壳用），false 时跳过空气（地板）。 */
        WorkItem(BoundingBox src, BlockPos offset, BlockState fill, boolean overwrite) {
            this(src, offset, false, null, fill, overwrite, null);
        }
        boolean isFill() {
            return fill != null;
        }
    }

    /** 跨 tick 分批放置方块的任务；全部完成后回调 {@code onComplete}。 */
    private static final class BuildTask extends ServerTaskInfo {
        private final ServerLevel level;
        private final List<WorkItem> work;
        private final Runnable onComplete;
        private final int teams;
        private int index = 0;
        private int tickCounter = 0;

        private BuildTask(ServerLevel level, List<WorkItem> work,
                Runnable onComplete, int teams) {
            this.level = level;
            this.work = work;
            this.onComplete = onComplete;
            this.teams = teams;
        }

        @Override
        public boolean onTick(MinecraftServer server) {
            // 建图途中若已标记「不在建图」（/60s stop 或 finalizeGame 清了 BUILDING）：立即中止，
            // 不再放置、不触发完成回调（避免复活已结束的对局）。预建时 RUNNING 为 false，故不能依赖它。
            if (!net.exmo.sixty_seconds.SixtySecondsMod.BUILDING) {
                this.cancelled = true;
                return true;
            }
            int done = 0;
            while (index < work.size() && done < MAX_CHUNKS_PER_TICK) {
                // 单个工项容错：某块放置抛异常（如 1.21 下模板 NBT 解析异常）只跳过并记日志，
                try {
                    placeWorkItem(level, work.get(index));
                } catch (Exception e) {
                    net.exmo.sixty_seconds.SixtySeconds.LOGGER.error("Construction work item failed and skipped：{}", work.get(index), e);
                }
                index++;
                done++;
            }
            // 进度显示与其他模式（60s start 的 FullTrainResetTask/OnlySomeBlockResetTask）保持一致：
            // 走 actionbar（true）、黄色、每 10 tick(~0.5s) 刷新一次，而非聊天栏每秒刷屏。
            if (index < work.size() && (++tickCounter % 10) == 0) {
                int percent = (int) (100.0 * index / Math.max(1, work.size()));
                Component msg = Component.translatable("message.sixty_seconds.sixty_seconds.building", percent)
                        .withStyle(net.minecraft.ChatFormatting.YELLOW);
                for (ServerPlayer player : level.players()) {
                    player.displayClientMessage(msg, true);
                }
            }
            return index >= work.size();
        }

        @Override
        public void onFinished() {
            // 建图收尾：无论正常完成还是中途取消，都清掉 BUILDING，避免残留 true 卡住后续建图
            net.exmo.sixty_seconds.SixtySecondsMod.BUILDING = false;
            // 无论正常完成还是中途取消，都把迟到实体清理窗口收成短尾窗（否则常驻误删局内掉落）
            armClearZonesTail(level);
            if (cancelled) {
                return;
            }
            SixtySeconds.LOGGER.info("[60s] Async build complete: {} teams.", teams);
            // 进度收尾：与其他模式一致，最后在 actionbar 打出 100%（黄色），保证进度视觉走满。
            Component done = Component.translatable("message.sixty_seconds.sixty_seconds.building", 100)
                    .withStyle(net.minecraft.ChatFormatting.YELLOW);
            for (ServerPlayer player : level.players()) {
                player.displayClientMessage(done, true);
            }
            // 建图收尾：强制清掉所有残留物品掉落物（生成过程中从容器/方块实体掉出、或液体冲刷带出的实体），
            // 避免局内实体与网络同步负载堆积。此时玩家尚未进场，清干净不影响任何玩家物品。
            int clearedDrops = 0;
            for (Entity e : level.getAllEntities()) {
                if (e instanceof ItemEntity && !e.isRemoved()) {
                    e.discard();
                    clearedDrops++;
                }
            }
            if (clearedDrops > 0)
                SixtySeconds.LOGGER.info("[60s] Build complete: force-cleared {} leftover item drops.", clearedDrops);

            onComplete.run();
        }
    }
}
