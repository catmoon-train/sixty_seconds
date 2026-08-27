package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SixtySecGameWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<SixtySecGameWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("game"), SixtySecGameWorldComponent.class);

    public enum GameStatus {
        INACTIVE, STARTING, ACTIVE, STOPPING
    }

    private final Level world;
    public GameMode gameMode;
    public GameStatus gameStatus = GameStatus.INACTIVE;
    public int fade = 0;
    public boolean isSkillAvailable = true;
    private boolean canJump = true;
    private int startingPlayerCount = 0;
    private int playerCount = 0;

    public SixtySecGameWorldComponent(Level world) {
        this.world = world;
    }

    public Level world() {
        return world;
    }

    public boolean isRunning() {
        return this.gameStatus == GameStatus.ACTIVE || this.gameStatus == GameStatus.STOPPING;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus status) {
        this.gameStatus = status;
        SixtySecondsMod.RUNNING = status == GameStatus.ACTIVE || status == GameStatus.STARTING
                || status == GameStatus.STOPPING;
        sync();
    }

    public void setJumpAvailable(boolean available) {
        this.canJump = available;
    }

    public boolean isJumpAvailable() {
        return canJump;
    }

    public void setPlayerCount(int count) {
        this.playerCount = count;
        this.startingPlayerCount = count;
    }

    public int getStartingPlayerCount() {
        return startingPlayerCount;
    }

    public void setStartingPlayerCount(int count) {
        this.startingPlayerCount = Math.max(0, count);
        sync();
    }

    public void sync() {
        KEY.sync(this.world);
    }

    public void serverTick() {
        if (!(world instanceof ServerLevel serverWorld)) {
            return;
        }
        if (gameStatus == GameStatus.STARTING) {
            GameUtils.initializeGame(serverWorld);
            return;
        }
        if (gameStatus == GameStatus.STOPPING) {
            GameUtils.finalizeGame(serverWorld);
            return;
        }
        if (isRunning() && gameMode != null) {
            gameMode.tickCommonGameLoop(world);
            gameMode.tickServerGameLoop(serverWorld, this);
        }
    }

    public void clientTick() {
        if (isRunning() && gameMode != null) {
            gameMode.tickClientGameLoop(world);
        }
        if (world != null && world.isClientSide()) {
            net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient.gameComponent = this;
            net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient.areaComponent = AreasWorldComponent.KEY.get(world);
        }
    }
}
