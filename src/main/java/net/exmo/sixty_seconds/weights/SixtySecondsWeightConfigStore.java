package net.exmo.sixty_seconds.weights;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.registries.DeferredItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 负重配置的本地存储。配置文件位于世界目录 {@code <world>/sixty_seconds_weight_config.json}。
 *
 * <p>默认配置优先使用模组内置的资源文件 {@code /assets/sixty_seconds/weights/default_weights.json}
 * （为模组内每个注册物品提供默认重量，并附带若干物品标签的统一定义）。该内置 JSON 作为默认配置与
 * 兜底使用；运行时还会通过 {@link ModItems} 反射补充内置 JSON 未覆盖的物品，并在 TACZ 加载时为
 * TACZ 枪械/弹药/配件补充默认重量。
 */
public final class SixtySecondsWeightConfigStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "sixty_seconds_weight_config.json";
    private static final String BUILTIN_PATH = "/assets/sixty_seconds/weights/default_weights.json";

    private static SixtySecondsWeightConfig current;
    private static Path file;

    private SixtySecondsWeightConfigStore() {
    }

    public static SixtySecondsWeightConfig get(MinecraftServer server) {
        if (current == null) load(server);
        return current;
    }

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (Files.exists(file)) {
            try {
                String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                SixtySecondsWeightConfig cfg = GSON.fromJson(json, SixtySecondsWeightConfig.class);
                current = cfg != null ? cfg : defaultConfig();
                if (current.itemWeights == null) current.itemWeights = new LinkedHashMap<>();
                if (current.tagWeights == null) current.tagWeights = new LinkedHashMap<>();
            } catch (IOException e) {
                SixtySeconds.LOGGER.error("Failed to read weight config, using default config", e);
                current = defaultConfig();
            }
        } else {
            current = defaultConfig();
            save(server, current);
        }
    }

    public static void save(MinecraftServer server, SixtySecondsWeightConfig cfg) {
        current = cfg;
        if (file == null) file = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, GSON.toJson(cfg).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            SixtySeconds.LOGGER.error("Failed to save weight config", e);
        }
    }

    /** TACZ 枪械/弹药/配件的具体型号默认重量（键与 TACZ custom_data 中的 id 一致）。仅 TACZ 加载时写入。 */
    private static final Map<String, Double> TACZ_DEFAULTS = new LinkedHashMap<>() {{
        // 手枪 / 步枪 / 狙击 / 霰弹 / 冲锋枪 / 重武器
        put("tacz:glock_17", 5.0);
        put("tacz:taurus943", 5.0);
        put("tacz:cz75", 5.0);
        put("tacz:timeless50", 5.5);
        put("tacz:db_short", 4.0);
        put("tacz:m870", 5.0);
        put("tacz:kar98", 6.0);
        put("tacz:uzi", 4.5);
        put("tacz:sks_tactical", 6.0);
        put("tacz:m320", 4.0);
        put("tacz:rpg7", 9.0);
        put("tacz:ak47", 5.5);
        put("tacz:ai_awp", 7.0);
        // 弹药
        put("tacz:9mm", 0.3);
        put("tacz:22wmr", 0.3);
        put("tacz:12g", 0.4);
        put("tacz:50ae", 0.3);
        put("tacz:792x57", 0.5);
        put("tacz:762x39", 0.5);
        put("tacz:40mm", 0.6);
        put("tacz:338", 0.6);
        put("tacz:rpg_rocket", 1.0);
        put("tacz:ammo_box", 3.0);
        // 配件
        put("tacz:laser_compact", 0.3);
        put("tacz:sight_sro_dot", 0.3);
        put("tacz:scope_acog_ta31", 0.5);
        put("tacz:scope_vudu", 0.6);
        put("tacz:muzzle_a1", 0.5);
        put("tacz:muzzle_a2", 0.5);
        put("tacz:muzzle_a3", 0.5);
        put("tacz:bayonet_6h3", 0.8);
        put("tacz:stock_a1", 0.6);
        put("tacz:stock_a2", 0.6);
        put("tacz:stock_a3", 0.6);
        put("tacz:grip_a1", 0.4);
        put("tacz:grip_a2", 0.4);
        put("tacz:grip_a3", 0.4);
        put("tacz:extended_mag_a1", 0.4);
        put("tacz:extended_mag_a2", 0.4);
        put("tacz:extended_mag_a3", 0.4);
        put("tacz:ammo_mod_a1", 0.3);
        put("tacz:ammo_mod_a2", 0.3);
        put("tacz:ammo_mod_a3", 0.3);
    }};

    public static boolean isTaczLoaded() {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse("tacz:modern_kinetic_gun")).isPresent();
    }

    /** 生成默认配置：优先使用内置资源 {@code default_weights.json} 作为兜底，再补充反射/TACZ 项。 */
    public static SixtySecondsWeightConfig defaultConfig() {
        SixtySecondsWeightConfig cfg = loadBuiltinDefault();
        // 运行时反射补充：捕获内置 JSON 未覆盖或 id 拼写差异的模组物品
        for (Field f : ModItems.class.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Item item = asItem(f.get(null));
                if (item != null) {
                    String id = BuiltInRegistries.ITEM.getKey(item).toString();
                    if (!id.equals("minecraft:air")) {
                        cfg.itemWeights.putIfAbsent(id, guessModItemWeight(f.getName()));
                    }
                }
            } catch (IllegalAccessException ignored) {
                // 跳过不可访问字段
            }
        }
        if (isTaczLoaded()) {
            for (Map.Entry<String, Double> e : TACZ_DEFAULTS.entrySet()) {
                cfg.itemWeights.putIfAbsent(e.getKey(), e.getValue());
            }
        }
        if (cfg.tagWeights == null) cfg.tagWeights = new LinkedHashMap<>();
        cfg.tagWeights.putIfAbsent("#minecraft:planks", 0.5);
        cfg.tagWeights.putIfAbsent("#minecraft:logs", 2.0);
        cfg.tagWeights.putIfAbsent("#minecraft:stone", 2.5);
        cfg.tagWeights.putIfAbsent("#minecraft:storage_blocks", 4.0);
        return cfg;
    }

    /** 从模组内置资源读取默认配置（兜底用）。读取失败或不存在时回退到空配置。 */
    private static SixtySecondsWeightConfig loadBuiltinDefault() {
        try (InputStream in = SixtySecondsWeightConfigStore.class.getResourceAsStream(BUILTIN_PATH)) {
            if (in != null) {
                SixtySecondsWeightConfig cfg = GSON.fromJson(
                        new InputStreamReader(in, StandardCharsets.UTF_8), SixtySecondsWeightConfig.class);
                if (cfg != null) {
                    if (cfg.itemWeights == null) cfg.itemWeights = new LinkedHashMap<>();
                    if (cfg.tagWeights == null) cfg.tagWeights = new LinkedHashMap<>();
                    return cfg;
                }
            }
        } catch (Exception e) {
            SixtySeconds.LOGGER.warn("Failed to read built-in default weight config, fallback to empty config", e);
        }
        SixtySecondsWeightConfig cfg = new SixtySecondsWeightConfig();
        cfg.itemWeights = new LinkedHashMap<>();
        cfg.tagWeights = new LinkedHashMap<>();
        return cfg;
    }

    /** 收集配置面板中可编辑的物品 id：模组物品 + TACZ 型号 + 已配置项（去重）。 */
    public static List<String> collectItemIds(SixtySecondsWeightConfig cfg) {
        Set<String> ids = new LinkedHashSet<>();
        for (Field f : ModItems.class.getDeclaredFields()) {
            f.setAccessible(true);
            try {
                Item item = asItem(f.get(null));
                if (item != null) {
                    String id = BuiltInRegistries.ITEM.getKey(item).toString();
                    if (!id.equals("minecraft:air")) ids.add(id);
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        if (isTaczLoaded()) ids.addAll(TACZ_DEFAULTS.keySet());
        ids.addAll(cfg.itemWeights.keySet());
        return List.copyOf(ids);
    }

    private static Item asItem(Object v) {
        if (v instanceof Item i) return i;
        if (v instanceof DeferredItem<?> di) return di.get();
        return null;
    }

    /** 依据字段名启发式给出模组物品的默认重量。 */
    private static double guessModItemWeight(String name) {
        String n = name.toLowerCase();
        if (n.contains("backpack")) return 3.0;
        if (n.contains("helmet") || n.contains("chestplate") || n.contains("leggings")
                || n.contains("boots") || n.contains("armor") || n.contains("vest")
                || n.contains("suit")) return 4.0;
        if (n.contains("block") || n.contains("door") || n.contains("wall") || n.contains("fence")
                || n.contains("slab")) return 3.0;
        if (n.contains("ammo") || n.contains("bullet") || n.contains("shell")) return 0.5;
        if (n.contains("food") || n.contains("water") || n.contains("potion") || n.contains("drink")
                || n.contains("beer") || n.contains("wine") || n.contains("cola")) return 0.5;
        if (n.contains("scrap") || n.contains("ingot") || n.contains("plate") || n.contains("gear")
                || n.contains("wire") || n.contains("pipe") || n.contains("spring") || n.contains("alloy")
                || n.contains("copper") || n.contains("iron") || n.contains("steel")) return 1.0;
        if (n.contains("book") || n.contains("paper") || n.contains("map") || n.contains("note")
                || n.contains("document")) return 0.3;
        return 1.0;
    }
}
