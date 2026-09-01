package net.exmo.sixty_seconds.loot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 物资箱搜刮形式配置（服务端全局，存于 {@code serverconfig/sixty_seconds_loot_form.json}）：
 * <ul>
 *   <li>{@code timed}：沿用旧版「站在箱前定时读条」的搜刮形式（保留为可选）。</li>
 *   <li>{@code container}：新形式（默认）——右键打开容器页面，箱内物资先用放大镜占位，
 *       左键放大镜逐个搜刮，读条完成后揭示战利品。越重的物品搜刮越久。</li>
 * </ul>
 */
public final class SixtySecondsLootFormConfig {
    public static final String FILE_NAME = "sixty_seconds_loot_form.json";
    private static final Gson GSON = new Gson();
    private static Data cache;

    public static class Data {
        /** timed = 旧形式；container = 新容器形式（默认）。 */
        public String form = "container";
    }

    private SixtySecondsLootFormConfig() {
    }

    public static Data get(MinecraftServer server) {
        if (cache == null) {
            cache = load(server);
        }
        return cache;
    }

    public static void save(MinecraftServer server, Data data) {
        cache = data;
        Path dir = server.getServerDirectory().resolve("serverconfig");
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
        }
        Path path = dir.resolve(FILE_NAME);
        try {
            Files.writeString(path, GSON.toJson(data));
        } catch (Exception ignored) {
        }
    }

    public static boolean isContainer(MinecraftServer server) {
        Data d = get(server);
        return d != null && "container".equals(d.form);
    }

    private static Data load(MinecraftServer server) {
        Path path = server.getServerDirectory().resolve("serverconfig").resolve(FILE_NAME);
        if (Files.exists(path)) {
            try {
                JsonObject obj = GSON.fromJson(Files.readString(path), JsonObject.class);
                Data d = new Data();
                if (obj != null && obj.has("form")) {
                    String form = obj.get("form").getAsString();
                    if ("timed".equals(form) || "container".equals(form)) {
                        d.form = form;
                    }
                }
                return d;
            } catch (Exception ignored) {
            }
        }
        return new Data();
    }
}
