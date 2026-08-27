package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SixtySecPlayerMoodComponent implements AutoSyncedComponent {
    public static final ComponentKey<SixtySecPlayerMoodComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("player_mood"), SixtySecPlayerMoodComponent.class);
    private final Player player;
    public SixtySecPlayerMoodComponent(Player player) { this.player = player; }
    public Player getPlayer() { return player; }
    public void init() {}
    public void clear() {}
    @Override public boolean shouldSyncWith(ServerPlayer player) { return this.getPlayer() == player; }
}
