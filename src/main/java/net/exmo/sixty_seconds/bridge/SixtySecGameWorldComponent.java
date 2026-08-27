package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    
    @Override
    public void writeSyncPacket(RegistryFriendlyByteBuf buf, ServerPlayer recipient) {
        buf.writeEnum(gameStatus);
        if (gameMode != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(gameMode.identifier);
        } else {
            buf.writeBoolean(false);
        }
        buf.writeInt(fade);
        buf.writeBoolean(isSkillAvailable);
    }

    @Override
    public void applySyncPacket(RegistryFriendlyByteBuf buf) {
        this.gameStatus = buf.readEnum(GameStatus.class);
        if (buf.readBoolean()) {
            ResourceLocation id = buf.readResourceLocation();
            // 客户端按 id 查到的 GameMode 是另一份实例，与服务器侧的 SixtySecondsMod.MODE 不是同一个对象，
            // 导致所有「getGameMode() == SixtySecondsMod.MODE」判断在客户端恒为 false（HUD/状态栏全不显示）。
            // 对本模组自己的模式，直接复用 SixtySecondsMod.MODE 实例，使 == 比较在客户端也成立。
            this.gameMode = id.equals(SixtySecondsMod.MODE_ID) && SixtySecondsMod.MODE != null
                    ? SixtySecondsMod.MODE : SixtySecGameModes.get(id);
        } else {
            this.gameMode = null;
        }
        this.fade = buf.readInt();
        this.isSkillAvailable = buf.readBoolean();
    }
}
