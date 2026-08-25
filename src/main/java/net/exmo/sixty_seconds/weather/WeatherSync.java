package net.exmo.sixty_seconds.weather;

import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.network.WeatherS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端维护「当前展示中的天气」（指令强制触发时使用），并向维度内所有玩家广播同步包。
 * 自然事件系统也会直接调用 {@link #send} 同步当次事件类型。
 */
public final class WeatherSync {
    private record Forced(SixtySecondsEventSystem.EventType type) {
    }

    private static final Map<ServerLevel, Forced> FORCED = new HashMap<>();

    private WeatherSync() {
    }

    /** 是否当前有指令强制天气生效（用于避免自然事件 endEvent 误清除预览）。 */
    public static boolean isForced(ServerLevel level) {
        return FORCED.containsKey(level);
    }

    /** 向维度内所有玩家广播当前天气类型（null 表示清除）。 */
    public static void send(ServerLevel level, SixtySecondsEventSystem.EventType type) {
        byte id = type == null ? (byte) -1 : (byte) type.ordinal();
        WeatherS2CPacket packet = new WeatherS2CPacket(id);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /** 指令强制开启：记录并在到期前持续展示，到期由 {@link #clear} 清除。 */
    public static void force(ServerLevel level, SixtySecondsEventSystem.EventType type) {
        FORCED.put(level, new Forced(type));
        send(level, type);
    }

    /** 清除指令预览。若当前仍有自然事件在运行，则切回自然事件显示，而非清空。 */
    public static void clear(ServerLevel level) {
        FORCED.remove(level);
        SixtySecondsEventSystem.EventType event = SixtySecondsEventSystem.activeEventType(level);
        send(level, event); // event 为 null 时发 -1（清除）
    }

    /** 玩家登录补发（若当前有指令强制天气）。 */
    public static void resend(ServerPlayer player) {
        Forced forced = FORCED.get(player.serverLevel());
        if (forced != null) {
            ServerPlayNetworking.send(player, new WeatherS2CPacket((byte) forced.type.ordinal()));
        }
    }
}
