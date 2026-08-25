package net.exmo.sixty_seconds.weather;

import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.network.WeatherS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端维护「指令强制天气」（纯预览，独立于自然事件系统）并向维度内玩家广播。
 * forced=true 的包写入客户端预览槽，自然事件系统调用 {@link #send} 时传 forced=false，写入事件槽，
 * 二者互不清除：自然事件结束不会清掉正在进行的指令预览。
 *
 * 注意：延迟清除不能用 server.tell(new TickTask(...)) —— 本版本 MinecraftServer.tell 只有 Runnable 重载，
 * TickTask 实现了 Runnable 会被当成下一 tick 立即执行，导致预览瞬间被 clear。故用自管的到期 tick 映射，
 * 由 {@link #serverTick} 每服务端 tick 检查。
 */
public final class WeatherSync {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherSync.class);
    private record Forced(SixtySecondsEventSystem.EventType type) {
    }

    private static final Map<ServerLevel, Forced> FORCED = new HashMap<>();
    /** 维度 -> 应执行 clear 的服务端 tick（来自指令预览的持续时间）。 */
    private static final Map<ServerLevel, Long> CLEAR_AT = new HashMap<>();

    private WeatherSync() {
    }

    /** 向维度内所有玩家广播天气类型（null 表示清除对应槽位）。forced=true 为指令预览，false 为自然事件。 */
    public static void send(ServerLevel level, SixtySecondsEventSystem.EventType type, boolean forced) {
        byte id = type == null ? (byte) -1 : (byte) type.ordinal();
        LOG.info("[60s-weather] 服务端广播 weatherId={} forced={} 玩家数={}", id, forced, level.players().size());
        WeatherS2CPacket packet = new WeatherS2CPacket(id, forced);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /** 指令强制开启（预览，优先级最高）。会取消任何待执行的到期清除。 */
    public static void force(ServerLevel level, SixtySecondsEventSystem.EventType type) {
        LOG.info("[60s-weather] force 指令预览 type={}", type);
        FORCED.put(level, new Forced(type));
        CLEAR_AT.remove(level);
        send(level, type, true);
    }

    /** 安排在未来 ticks 个 tick 后自动清除预览（替代会立即执行的 server.tell(TickTask)）。 */
    public static void scheduleClear(ServerLevel level, int ticks) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        long at = server.getTickCount() + Math.max(1, ticks);
        CLEAR_AT.put(level, at);
        LOG.info("[60s-weather] 已安排预览清除，约 {} tick 后 (当前={}, 到期={})", ticks, server.getTickCount(), at);
    }

    /** 结束指令预览：清除预览槽，客户端回退到自然事件（若有）。 */
    public static void clear(ServerLevel level) {
        FORCED.remove(level);
        CLEAR_AT.remove(level);
        send(level, null, true);
    }

    /** 每服务端 tick 调用：到达到期时间的维度执行清除。由 NeoForgeEvents.onServerTickPost 驱动。 */
    public static void serverTick(MinecraftServer server) {
        if (CLEAR_AT.isEmpty()) {
            return;
        }
        long now = server.getTickCount();
        for (ServerLevel level : server.getAllLevels()) {
            Long at = CLEAR_AT.get(level);
            if (at != null && now >= at) {
                LOG.info("[60s-weather] 预览到期，自动清除 level={}", level.dimension().location());
                clear(level);
            }
        }
    }

    /** 玩家登录补发（若当前有指令强制天气）。 */
    public static void resend(ServerPlayer player) {
        Forced forced = FORCED.get(player.serverLevel());
        if (forced != null) {
            ServerPlayNetworking.send(player, new WeatherS2CPacket((byte) forced.type.ordinal(), true));
        }
    }
}
