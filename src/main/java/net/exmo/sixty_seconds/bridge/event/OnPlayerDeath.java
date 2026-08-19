package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface OnPlayerDeath {
    Event<OnPlayerDeath> EVENT = new Event<>(listeners -> (victim, killer, deathReason) -> {
        for (OnPlayerDeath listener : listeners) {
            listener.onDeath(victim, killer, deathReason);
        }
    });

    void onDeath(Player victim, @Nullable Player killer, ResourceLocation deathReason);
}
