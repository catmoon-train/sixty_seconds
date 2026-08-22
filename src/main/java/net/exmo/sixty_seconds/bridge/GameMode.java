package net.exmo.sixty_seconds.bridge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class GameMode {
    public final ResourceLocation identifier;
    public final int defaultStartTime;
    public final int minPlayerCount;
    public long safeTimeStarted = 0;

    public GameMode(ResourceLocation identifier, int defaultStartTime, int minPlayerCount) {
        this.identifier = identifier;
        this.defaultStartTime = defaultStartTime;
        this.minPlayerCount = minPlayerCount;
    }

    public boolean canPickBodyContent() {
        return false;
    }

    public boolean canSeeBodyContent() {
        return false;
    }

    public boolean shouldRecordPlayerStats() {
        return false;
    }

    public boolean requiresAssignedRole() {
        return true;
    }

    public boolean hasMood() {
        return false;
    }

    public boolean enablePlayAreaDetections() {
        return true;
    }

    public boolean hasSafeTime() {
        return false;
    }

    public boolean autoTriggerGameTrueStarted() {
        return true;
    }

    public void beforeInitializeGame(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent,
            List<ServerPlayer> players) {
    }

    public void initializeGame(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent,
            List<ServerPlayer> players) {
    }

    public void afterInitializeGame(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent,
            List<ServerPlayer> players) {
    }

    public void gameStarted(ServerLevel serverWorld, SixtySecGameWorldComponent gameComponent,
            ArrayList<ServerPlayer> readyPlayerList) {
    }

    public void tickServerGameLoop(ServerLevel serverWorld, SixtySecGameWorldComponent gameWorldComponent) {
    }

    public void tickCommonGameLoop(Level level) {
    }

    public void tickClientGameLoop(Level level) {
    }

    public void stopGame(ServerLevel world) {
    }

    public void limitSpectatorPlayer(ServerPlayer player, SixtySecGameWorldComponent gameWorldComponent,
            AreasWorldComponent areas) {
    }

    public void killPlayer(Player victim, boolean spawnBody, @Nullable Player killer,
            ResourceLocation deathReason, boolean forceDeath) {
        GameUtils.doKill(victim, spawnBody, killer, deathReason);
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
