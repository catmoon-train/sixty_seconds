package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class SixtySecPlayerMinigameTaskComponent implements AutoSyncedComponent {
    public static final ComponentKey<SixtySecPlayerMinigameTaskComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("minigame_task"), SixtySecPlayerMinigameTaskComponent.class);

    private final Player player;
    public int tokens = 0;
    public int pendingMinigameTasks = 1;
    public String targetMinigameId;
    public final java.util.HashMap<Long, Long> blockCooldownUntil = new java.util.HashMap<>();

    public SixtySecPlayerMinigameTaskComponent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override public boolean shouldSyncWith(ServerPlayer player) { return this.getPlayer() == player; }

    public int getTokens() {
        return tokens;
    }

    public void addTokens(int amount) {
        this.tokens = Math.max(0, this.tokens + amount);
        sync();
    }

    public boolean hasPendingTask() {
        return pendingMinigameTasks > 0;
    }

    public boolean isBlockUsed(net.minecraft.core.BlockPos pos) {
        Long until = blockCooldownUntil.get(pos.asLong());
        return until != null && player.level().getGameTime() < until;
    }

    public void startBlockCooldown(net.minecraft.core.BlockPos pos) {
        blockCooldownUntil.put(pos.asLong(), player.level().getGameTime() + 20 * 90);
        sync();
    }

    public void setTokens(int amount) {
        this.tokens = Math.max(0, amount);
        sync();
    }

    public void sync() {
        KEY.sync(this.player);
    }

    public void init() {
        this.tokens = 0;
        sync();
    }

    public void clear() {
        init();
    }
}
