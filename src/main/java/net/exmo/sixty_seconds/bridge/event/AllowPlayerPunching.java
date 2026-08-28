package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.world.entity.player.Player;

/** SixtySeconds mixin hook; vanilla already allows punching, so this is a no-op registry. */
public interface AllowPlayerPunching {
    Event<AllowPlayerPunching> EVENT = new Event<>(listeners -> player -> {
        for (AllowPlayerPunching listener : listeners) {
            if (listener.allow(player)) {
                return true;
            }
        }
        return false;
    });

    boolean allow(Player player);
}
