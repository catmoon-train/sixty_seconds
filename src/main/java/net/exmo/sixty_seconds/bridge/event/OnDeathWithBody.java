package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public interface OnDeathWithBody {
    Event<OnDeathWithBody> EVENT = new Event<>(listeners -> (victim, killer, deathReason, body) -> {
        for (OnDeathWithBody listener : listeners) {
            listener.onDeath(victim, killer, deathReason, body);
        }
    });

    void onDeath(Player victim, @Nullable Player killer, ResourceLocation deathReason, @Nullable PlayerBodyEntity body);
}
