package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.exmo.sixty_seconds.bridge.event.OnDeathWithBody;
import net.exmo.sixty_seconds.bridge.event.OnPlayerDeath;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GameUtils {
    public static final ArrayList<ServerTaskInfoClasses.ServerTaskInfo> serverTaskQueue = new ArrayList<>();
    public static final ArrayList<ServerTaskInfoClasses.ServerTaskInfo> serverAsynTaskLists = new ArrayList<>();
    public static boolean isStartingGame = false;
    public static boolean isGameStarted = false;
    private static Set<UUID> forcedReadyPlayers;

    private GameUtils() {
    }

    public enum WinStatus {
        NOT_MODIFY, NONE, KILLERS, PASSENGERS, TIME, LOOSE_END, GAMBLER, RECORDER, NO_PLAYER, NIAN_SHOU, LOVERS,
        CUSTOM_COMPONENT, CUSTOM;

        public boolean isKillerWin() {
            return this.equals(WinStatus.KILLERS);
        }

        public boolean isInnocentWin() {
            return this.equals(WinStatus.TIME) || this.equals(WinStatus.PASSENGERS);
        }
    }

    public static void setForcedReadyPlayers(Collection<UUID> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            forcedReadyPlayers = null;
            return;
        }
        forcedReadyPlayers = new LinkedHashSet<>(playerIds);
    }

    public static void clearForcedReadyPlayers() {
        forcedReadyPlayers = null;
    }

    public static List<ServerPlayer> getStartingPlayers(ServerLevel serverWorld) {
        ParticipationComponent participation = ParticipationComponent.KEY.get(serverWorld);
        if (forcedReadyPlayers != null && !forcedReadyPlayers.isEmpty()) {
            List<ServerPlayer> selected = forcedReadyPlayers.stream()
                    .map(serverWorld.getServer().getPlayerList()::getPlayer)
                    .filter(Objects::nonNull)
                    .filter(participation::isParticipating)
                    .toList();
            if (!selected.isEmpty()) {
                return selected;
            }
        }
        return serverWorld.getServer().getPlayerList().getPlayers().stream()
                .filter(participation::isParticipating)
                .toList();
    }

    public static void startGame(ServerLevel world, GameMode gameMode, int time) {
        if (isStartingGame) {
            return;
        }
        SREGameWorldComponent game = SREGameWorldComponent.KEY.get(world);
        if (game.isRunning()) {
            return;
        }
        isStartingGame = true;
        game.gameMode = gameMode;
        SREGameTimeComponent.KEY.get(world).setResetTime(time);
        List<ServerPlayer> players = getStartingPlayers(world);
        if (players.size() < gameMode.minPlayerCount) {
            for (ServerPlayer player : world.getServer().getPlayerList().getPlayers()) {
                player.displayClientMessage(
                        Component.translatable("game.start_error.not_enough_players", gameMode.minPlayerCount), true);
            }
            isStartingGame = false;
            return;
        }
        game.setGameStatus(SREGameWorldComponent.GameStatus.STARTING);
    }

    public static void initializeGame(ServerLevel serverWorld) {
        isStartingGame = false;
        SREGameWorldComponent gameComponent = SREGameWorldComponent.KEY.get(serverWorld);
        ArrayList<ServerPlayer> readyPlayerList = new ArrayList<>(getStartingPlayers(serverWorld));
        gameComponent.setStartingPlayerCount(readyPlayerList.size());
        clearForcedReadyPlayers();
        GameMode mode = gameComponent.getGameMode();
        if (mode == null) {
            mode = SixtySecondsMod.MODE;
            gameComponent.gameMode = mode;
        }
        mode.beforeInitializeGame(serverWorld, gameComponent, readyPlayerList);
        mode.initializeGame(serverWorld, gameComponent, readyPlayerList);
        gameComponent.setGameStatus(SREGameWorldComponent.GameStatus.ACTIVE);
        SixtySecondsMod.RUNNING = true;
        mode.gameStarted(serverWorld, gameComponent, readyPlayerList);
        isGameStarted = true;
    }

    public static void stopGame(ServerLevel world) {
        SREGameWorldComponent component = SREGameWorldComponent.KEY.get(world);
        component.setGameStatus(SREGameWorldComponent.GameStatus.STOPPING);
        if (component.gameMode != null) {
            component.gameMode.stopGame(world);
        }
    }

    public static void finalizeGame(ServerLevel world) {
        SREGameWorldComponent component = SREGameWorldComponent.KEY.get(world);
        component.setGameStatus(SREGameWorldComponent.GameStatus.INACTIVE);
        SixtySecondsMod.RUNNING = false;
        isGameStarted = false;
        isStartingGame = false;
        serverTaskQueue.clear();
    }

    public static void tickTaskQueue(MinecraftServer server) {
        tickList(server, serverTaskQueue);
        tickList(server, serverAsynTaskLists);
    }

    private static void tickList(MinecraftServer server, ArrayList<ServerTaskInfoClasses.ServerTaskInfo> list) {
        if (list.isEmpty()) {
            return;
        }
        ServerTaskInfoClasses.ServerTaskInfo task = list.getFirst();
        if (!task.finished && task.onTick(server)) {
            task.finished = true;
            if (!task.cancelled) {
                task.onFinished();
            }
        }
        list.removeIf(t -> t.finished || t.cancelled);
    }

    public static boolean isPlayerEliminated(Player player) {
        return player == null || player.isSpectator();
    }

    public static boolean isPlayerEliminatedIgnoreShitSplit(Player player) {
        return isPlayerEliminated(player);
    }

    public static boolean isPlayerAliveAndSurvival(Player player) {
        if (player == null) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            GameType type = sp.gameMode.getGameModeForPlayer();
            return type != GameType.SPECTATOR && type != GameType.CREATIVE;
        }
        return !player.isSpectator() && !player.isCreative();
    }

    public static boolean isPlayerAliveAndSurvivalIgnoreShitSplit(Player player) {
        return isPlayerAliveAndSurvival(player);
    }

    public static boolean isPlayerSpectatingOrCreative(Player player) {
        if (player == null) {
            return true;
        }
        if (player instanceof ServerPlayer sp) {
            GameType type = sp.gameMode.getGameModeForPlayer();
            return type == GameType.SPECTATOR || type == GameType.CREATIVE;
        }
        return player.isSpectator() || player.isCreative();
    }

    public static boolean isPlayerSpectator(Player p) {
        return p != null && p.isSpectator();
    }

    public static boolean isPlayerCreative(Player player) {
        return player != null && player.isCreative();
    }

    public static boolean isGameRunning(Player player) {
        return player != null && isGameRunning(player.level());
    }

    public static boolean isGameRunning(Level level) {
        return level != null && SREGameWorldComponent.KEY.get(level).isRunning();
    }

    public static PlayerBodyEntity findPlayerBodyEntity(ServerPlayer player) {
        for (var entity : player.serverLevel().getAllEntities()) {
            if (entity instanceof PlayerBodyEntity body && player.getUUID().equals(body.getPlayerUuid())) {
                return body;
            }
        }
        return null;
    }

    public static void killPlayer(Player victim, boolean spawnBody, @Nullable Player killer) {
        killPlayer(victim, spawnBody, killer, GameConstants.DeathReasons.GENERIC, false);
    }

    public static void killPlayer(Player victim, boolean spawnBody, @Nullable Player killer,
            ResourceLocation deathReason) {
        killPlayer(victim, spawnBody, killer, deathReason, false);
    }

    public static void forceKillPlayer(Player victim, boolean spawnBody, @Nullable Player killer,
            ResourceLocation deathReason) {
        killPlayer(victim, spawnBody, killer, deathReason, true);
    }

    public static void killPlayer(Player victim, boolean spawnBody, @Nullable Player killer,
            ResourceLocation deathReason, boolean forceDeath) {
        if (victim == null || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        GameMode mode = SREGameWorldComponent.KEY.get(level).getGameMode();
        if (mode != null) {
            mode.killPlayer(victim, spawnBody, killer, deathReason, forceDeath);
        } else {
            doKill(victim, spawnBody, killer, deathReason);
        }
    }

    static void doKill(Player victim, boolean spawnBody, @Nullable Player killer, ResourceLocation deathReason) {
        if (!(victim instanceof ServerPlayer serverPlayer) || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        PlayerBodyEntity body = null;
        if (spawnBody && ModEntities.PLAYER_BODY != null) {
            body = ModEntities.PLAYER_BODY.create(level);
            if (body != null) {
                body.setOwner(serverPlayer);
                body.moveTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        serverPlayer.getYRot(), serverPlayer.getXRot());
                level.addFreshEntity(body);
            }
        }
        OnDeathWithBody.EVENT.invoker().onDeath(serverPlayer, killer, deathReason, body);
        OnPlayerDeath.EVENT.invoker().onDeath(serverPlayer, killer, deathReason);
        serverPlayer.setGameMode(GameType.SPECTATOR);
        serverPlayer.getInventory().clearContent();
    }

    public static void revivePlayer(ServerPlayer player, double x, double y, double z) {
        player.teleportTo(x, y, z);
        player.setGameMode(GameType.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        PlayerBodyEntity body = findPlayerBodyEntity(player);
        if (body != null) {
            body.discard();
        }
    }

    public static void resetPlayer(ServerPlayer player) {
        player.getInventory().clearContent();
        player.clearFire();
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
    }
}
