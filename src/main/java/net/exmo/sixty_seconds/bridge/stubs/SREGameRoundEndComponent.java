package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.world.level.Level;

public class SREGameRoundEndComponent {
    public static final ComponentKey<SREGameRoundEndComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("round_end"), SREGameRoundEndComponent.class);
    private final Level world;
    private GameUtils.WinStatus winStatus = GameUtils.WinStatus.NONE;
    public SREGameRoundEndComponent(Level world) { this.world = world; }
    public GameUtils.WinStatus getWinStatus() { return winStatus; }
    public void setWinStatus(GameUtils.WinStatus status) { this.winStatus = status; }
    public void setRoundEndData(java.util.List<? extends net.minecraft.world.entity.player.Player> players,
            GameUtils.WinStatus status) {
        this.winStatus = status;
    }
}
