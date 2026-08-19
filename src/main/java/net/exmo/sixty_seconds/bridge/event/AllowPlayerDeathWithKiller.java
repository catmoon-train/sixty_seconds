package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface AllowPlayerDeathWithKiller {
    Event<AllowPlayerDeathWithKiller> EVENT = new Event<>(listeners -> (victim, killer, deathReason) -> {
        boolean allow = true;
        for (AllowPlayerDeathWithKiller listener : listeners) {
            allow &= listener.allowDeath(victim, killer, deathReason);
        }
        return allow;
    });

    boolean allowDeath(Player victim, @Nullable Player killer, ResourceLocation deathReason);
}
