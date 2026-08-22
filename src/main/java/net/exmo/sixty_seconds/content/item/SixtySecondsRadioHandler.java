package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.fabric.ServerTickEvents;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.UUID;

/**
 * 对讲机频道维护：
 * <ul>
 *   <li>每 10 tick 清理频道表 {@link RadioItem#CHANNELS} —— 移除掉线、旁观、或背包不再持有对讲机的成员；</li>
 *   <li>开局时由 {@code SixtySecondsManager} 调用 {@link RadioItem#clear()} 重置。</li>
 * </ul>
 */
public final class SixtySecondsRadioHandler {
    private static int tickCounter = 0;

    private SixtySecondsRadioHandler() {
    }

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            if (++tickCounter < 10) {
                return;
            }
            tickCounter = 0;
            purge(server);
        });
    }

    private static void purge(MinecraftServer server) {
        if (RadioItem.CHANNELS.isEmpty()) {
            return;
        }
        Iterator<UUID> it = RadioItem.CHANNELS.keySet().iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null || player.isSpectator()
                    || !player.getInventory().hasAnyMatching(s -> s.is(ModItems.RADIO))) {
                it.remove();
            }
        }
    }
}
