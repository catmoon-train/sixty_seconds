package net.exmo.sixty_seconds.lostcities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 星图「已发现建筑」登记表（服务端）。
 */
public final class SixtySecondsDiscoveredBuildings {

    /** 单维度记录的区块格上限（LRU 淘汰）。4096 格 ≈ 64×64 区块，足够覆盖一座大城反复探索。 */
    public static final int MAX_CELLS = 4096;

    /** 自动落盘的最小间隔（游戏刻）。仅在有脏数据时达到间隔才触发一次异步写。 */
    static final int SAVE_INTERVAL_TICKS = 600;

    /** 存档目录名（世界根目录下）。 */
    private static final String DIR_NAME = "sixty_seconds_discovered_buildings";

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** 按维度持有登记表。仅服务端主线程访问（LevelTick / 拼包 / ServerStopping），服务器停止时显式清空。 */
    private static final Map<ServerLevel, Store> STORES = new HashMap<>();

    /** 写盘执行器：单线程串行化，daemon 不阻止 JVM 退出。 */
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sixty-seconds-discovered-save");
        t.setDaemon(true);
        return t;
    });

    /** 是否有在途写盘；有则本次变更并入下一轮，避免任务堆积。 */
    private static final AtomicBoolean SAVING = new AtomicBoolean(false);

    /** 一个已踏入的建筑区块格：建筑 id（多区块建筑为父 id）+ 星级。 */
    private record Cell(String id, int star) {
    }

    private static final class Store {
        /** LRU：访问序淘汰，超过 MAX_CELLS 丢弃最久未踏入的区块格。 */
        final LinkedHashMap<Long, Cell> cells = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Cell> e) {
                return size() > MAX_CELLS;
            }
        };
        boolean loaded = false;
        boolean dirty = false;
    }

    /** 磁盘格式：种子头 + 平行数组（键/建筑id/星级），紧凑且解析零反射开销。 */
    private static final class Saved {
        long seed;
        long[] keys = new long[0];
        String[] ids = new String[0];
        int[] stars = new int[0];
    }

    private SixtySecondsDiscoveredBuildings() {
    }

    /** 区块坐标打包（与项目其它处一致）。 */
    private static long keyOf(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    /**
     * 记录玩家踏入的一个星级建筑区块（星级 1..5；多区块建筑传 multibuilding 父 id）。
     * 重复踏入同一区块只刷新 LRU 顺序，不重复占容量。
     */
    public static void record(ServerLevel level, int cx, int cz, String id, int star) {
        if (level == null || id == null || star < 1 || star > 5) {
            return;
        }
        if (SixtySecondsLostCitiesStarMap.isHiddenFromStarMap(id)) {
            return; // 空置地块等填充建筑：不登记，星图不显示
        }
        Store store = STORES.computeIfAbsent(level, k -> new Store());
        ensureLoaded(level, store);
        synchronized (store.cells) {
            store.cells.put(keyOf(cx, cz), new Cell(id, star));
            store.dirty = true;
        }
    }

    /**
     * 聚合当前维度所有已发现建筑区块 → 按建筑 id 分组的外接矩形区域（世界坐标）。
     * 仅内存操作，供星图下发时与实时扫描合并。
     */
    public static List<SixtySecondsLostCitiesStarMap.BuildingRegion> regions(ServerLevel level) {
        Store store = level == null ? null : STORES.get(level);
        if (store == null || store.cells.isEmpty()) {
            return List.of();
        }
        // id -> {minCx, minCz, maxCx, maxCz, star}
        Map<String, int[]> boxes = new HashMap<>();
        synchronized (store.cells) {
            for (Map.Entry<Long, Cell> e : store.cells.entrySet()) {
                long key = e.getKey();
                int cx = (int) (key >> 32);
                int cz = (int) key;
                Cell cell = e.getValue();
                int[] b = boxes.get(cell.id());
                if (b == null) {
                    boxes.put(cell.id(), new int[]{cx, cz, cx, cz, cell.star()});
                } else {
                    b[0] = Math.min(b[0], cx);
                    b[1] = Math.min(b[1], cz);
                    b[2] = Math.max(b[2], cx);
                    b[3] = Math.max(b[3], cz);
                }
            }
        }
        List<SixtySecondsLostCitiesStarMap.BuildingRegion> result = new ArrayList<>(boxes.size());
        for (Map.Entry<String, int[]> e : boxes.entrySet()) {
            if (SixtySecondsLostCitiesStarMap.isHiddenFromStarMap(e.getKey())) {
                continue; // 旧存档中已登记的空置地块：同样过滤，不上图
            }
            int[] b = e.getValue();
            result.add(new SixtySecondsLostCitiesStarMap.BuildingRegion(
                    e.getKey(),
                    SixtySecondsLostCitiesStarMap.buildingDisplayKey(e.getKey()),
                    b[4],
                    b[0] * 16, b[1] * 16, b[2] * 16 + 15, b[3] * 16 + 15));
        }
        return result;
    }

    /**
     * 实时扫描区域与已发现区域合并：同 id 且外接矩形相交/相触（相邻区块共边留 1 格容差）时取并集。
     * 并集仍是矩形——对 L 形建筑群会略偏大，与实时扫描的洪泛外接矩形行为一致，可接受。
     */
    public static List<SixtySecondsLostCitiesStarMap.BuildingRegion> merge(
            List<SixtySecondsLostCitiesStarMap.BuildingRegion> live,
            List<SixtySecondsLostCitiesStarMap.BuildingRegion> discovered) {
        if (discovered == null || discovered.isEmpty()) {
            return live == null ? List.of() : live;
        }
        if (live == null || live.isEmpty()) {
            return discovered;
        }
        List<SixtySecondsLostCitiesStarMap.BuildingRegion> result = new ArrayList<>(discovered);
        for (SixtySecondsLostCitiesStarMap.BuildingRegion l : live) {
            boolean hit = false;
            for (int i = 0; i < result.size(); i++) {
                SixtySecondsLostCitiesStarMap.BuildingRegion d = result.get(i);
                if (d.id.equals(l.id) && touches(d, l)) {
                    result.set(i, new SixtySecondsLostCitiesStarMap.BuildingRegion(
                            d.id, d.displayName, d.star,
                            Math.min(d.minX, l.minX), Math.min(d.minZ, l.minZ),
                            Math.max(d.maxX, l.maxX), Math.max(d.maxZ, l.maxZ)));
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                result.add(l);
            }
        }
        return result;
    }

    /** 两区域是否相交或相触（相邻区块的矩形共边处间隔 1 格，用 +1 容差视为同一栋）。 */
    private static boolean touches(SixtySecondsLostCitiesStarMap.BuildingRegion a,
                                   SixtySecondsLostCitiesStarMap.BuildingRegion b) {
        return a.minX <= b.maxX + 1 && b.minX <= a.maxX + 1
                && a.minZ <= b.maxZ + 1 && b.minZ <= a.maxZ + 1;
    }

    // ==================== 持久化 ====================

    /** 主线程定期调用（脏且到间隔才真正写盘，写盘在后台线程执行）。 */
    public static void saveIfDirty(ServerLevel level) {
        Store store = level == null ? null : STORES.get(level);
        if (store == null) {
            return;
        }
        Saved snapshot;
        synchronized (store.cells) {
            if (!store.dirty) {
                return;
            }
            snapshot = snapshot(level, store);
            store.dirty = false;
        }
        submitSave(store, fileFor(level), snapshot);
    }

    /** 服务器停止：把所有维度立即落盘（同步等待完成，保证退出前写完）。 */
    public static void saveAllNow() {
        // 先等在途异步写结束（有界 2s），避免旧快照落盘覆盖新数据
        long deadline = System.currentTimeMillis() + 2000;
        while (SAVING.get() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
        for (Map.Entry<ServerLevel, Store> e : List.copyOf(STORES.entrySet())) {
            ServerLevel level = e.getKey();
            Store store = e.getValue();
            Saved snapshot;
            synchronized (store.cells) {
                if (!store.dirty) {
                    continue;
                }
                snapshot = snapshot(level, store);
                store.dirty = false;
            }
            try {
                Files.createDirectories(fileFor(level).getParent());
                Files.writeString(fileFor(level), GSON.toJson(snapshot), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException ignored) {
                // 落盘失败不阻断关服：LostCities 布局由种子确定，大不了玩家重探一次
            }
        }
    }

    /** 服务器完全停止后清空内存（单人换世界不残留上一个维度的静态状态）。 */
    public static void resetAll() {
        STORES.clear();
        SAVING.set(false);
    }

    /** 主线程：脏时做一次纯数组快照（O(cells)，≤4096 项，微秒级）。 */
    private static Saved snapshot(ServerLevel level, Store store) {
        Saved s = new Saved();
        s.seed = level.getSeed();
        synchronized (store.cells) {
            int n = store.cells.size();
            s.keys = new long[n];
            s.ids = new String[n];
            s.stars = new int[n];
            int i = 0;
            for (Map.Entry<Long, Cell> e : store.cells.entrySet()) {
                s.keys[i] = e.getKey();
                s.ids[i] = e.getValue().id();
                s.stars[i] = e.getValue().star();
                i++;
            }
        }
        return s;
    }

    /** 后台写盘（在途合并：已有写盘任务时把脏标记还回去，等下一轮）。 */
    private static void submitSave(Store store, Path file, Saved snapshot) {
        if (!SAVING.compareAndSet(false, true)) {
            synchronized (store.cells) {
                store.dirty = true;
            }
            return;
        }
        try {
            IO.execute(() -> {
                try {
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, GSON.toJson(snapshot), StandardCharsets.UTF_8);
                } catch (IOException | RuntimeException ignored) {
                    synchronized (store.cells) {
                        store.dirty = true; // 写失败保留脏标记，下一轮重试
                    }
                } finally {
                    SAVING.set(false);
                }
            });
        } catch (RejectedExecutionException e) {
            SAVING.set(false);
            synchronized (store.cells) {
                store.dirty = true;
            }
        }
    }

    /** 首次触碰时惰性加载本维度存档；种子不符（删档/换世界）整份作废。 */
    private static void ensureLoaded(ServerLevel level, Store store) {
        if (store.loaded) {
            return;
        }
        store.loaded = true;
        Path file = fileFor(level);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            Saved s = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Saved.class);
            if (s == null || s.ids == null || s.keys == null
                    || s.ids.length != s.keys.length || s.stars == null
                    || s.stars.length != s.keys.length) {
                return; // 文件损坏：当作空表，下次落盘自然覆盖
            }
            if (s.seed != level.getSeed()) {
                return; // 种子变了（删档重开/换世界）：旧数据全部作废
            }
            synchronized (store.cells) {
                // 超限时只保留最近写入的 MAX_CELLS 项（文件尾部）
                int start = Math.max(0, s.keys.length - MAX_CELLS);
                for (int i = start; i < s.keys.length; i++) {
                    store.cells.put(s.keys[i], new Cell(s.ids[i], s.stars[i]));
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 读取失败：当作空表处理，不影响游戏
        }
    }

    private static Path fileFor(ServerLevel level) {
        ResourceLocation dim = level.dimension().location();
        String name = dim.getNamespace() + "_" + dim.getPath().replace('/', '_');
        return level.getServer().getWorldPath(LevelResource.ROOT)
                .resolve(DIR_NAME).resolve(name + ".json");
    }
}
