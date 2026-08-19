package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ParticipationComponent {
    public static final ComponentKey<ParticipationComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("participation"), ParticipationComponent.class);

    private final Level world;

    public ParticipationComponent(Level world) {
        this.world = world;
    }

    public boolean isParticipating(ServerPlayer player) {
        return player != null && !player.isSpectator();
    }

    public int getOptedOutOnlineCount() {
        return 0;
    }
}
