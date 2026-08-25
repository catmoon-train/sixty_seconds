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

    /** 清除（指令 / 自然事件结束）。 */
    public static void clear(ServerLevel level) {
        FORCED.remove(level);
        send(level, null);
    }

    /** 玩家登录补发（若当前有指令强制天气）。 */
    public static void resend(ServerPlayer player) {
        Forced forced = FORCED.get(player.serverLevel());
        if (forced != null) {
            ServerPlayNetworking.send(player, new WeatherS2CPacket((byte) forced.type.ordinal()));
        }
    }
}
