package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.AreasWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.client.screen.SixtySecondsSearchZonesClient;

public final class SixtySecBridgeClient {
    public static SixtySecGameWorldComponent gameComponent;
    public static AreasWorldComponent areaComponent;
    private static boolean specialInventoryForced;
    /**
     * The vanilla inventory key can be repeated while the server is still
     * opening the authoritative menu.  Do not enqueue one C2S request per
     * key repeat; a burst of open-menu packets can make an integrated server
     * appear to freeze and can leave several client screens racing each other.
     */
    private static long specialInventoryRequestNanos;
    private static final long SPECIAL_INVENTORY_REQUEST_TIMEOUT_NANOS = 2_000_000_000L;

    private SixtySecBridgeClient() {
    }

    /** 当前客户端是否处于本模式（模式已加载，不要求游戏进行中）。 */
    public static boolean inSixtySecondsMode() {
        return SixtySecondsMod.MODE != null && gameComponent != null
                && gameComponent.getGameMode() == SixtySecondsMod.MODE;
    }

    /**
     * 是否应显示末日60秒状态栏：已在本模式中，且处于“开局/进行中/收尾”阶段。
     * <p>
     * 仅排除 {@code INACTIVE}（真正未载入模式 / 游戏已彻底结束），避免结束后仍残留自定义 HUD。
     */
    public static boolean shouldShowHud() {
        boolean inMode = inSixtySecondsMode();
        SixtySecGameWorldComponent.GameStatus status =
                gameComponent != null ? gameComponent.getGameStatus() : null;
        return inMode && status != SixtySecGameWorldComponent.GameStatus.INACTIVE;
    }

    /** Whether pressing E should ask the server for the Tarkov-style menu. */
    public static boolean shouldOpenSpecialInventory() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }

        // /60s inventory is also a valid opt-in before the round exists.  Do
        // not clear this flag merely because the client has no active game
        // component yet; it is precisely what makes subsequent E presses
        // reopen the same menu.
        if (specialInventoryForced) {
            // House searching is still the legacy phase, even if the command
            // was used before the player went outside.
            if (shouldDisablePetiteInventory()) {
                return false;
            }
            if (inSixtySecondsMode() && gameComponent != null
                    && (gameComponent.getGameStatus() == SixtySecGameWorldComponent.GameStatus.INACTIVE
                    || gameComponent.getGameStatus() == SixtySecGameWorldComponent.GameStatus.STOPPING)) {
                specialInventoryForced = false;
                return false;
            }
            return true;
        }

        if (!inSixtySecondsMode() || gameComponent == null) {
            return false;
        }

        SixtySecGameWorldComponent.GameStatus status = gameComponent.getGameStatus();
        if (status == SixtySecGameWorldComponent.GameStatus.INACTIVE
                || status == SixtySecGameWorldComponent.GameStatus.STOPPING) {
            return false;
        }

        // dayNumber is 0 during preparation.  The explicit /60s inventory
        // command keeps the new menu active during that phase.
        return specialInventoryForced
                || (status == SixtySecGameWorldComponent.GameStatus.ACTIVE
                && SixtySecondsStatsComponent.KEY.get(player).dayNumber > 0);
    }

    /**
     * Whether the first house-search phase must use the legacy inventory.
     * The client does not receive a search-zone packet for the initial house,
     * so checking only {@link SixtySecondsSearchZonesClient} is insufficient.
     */
    public static boolean shouldDisablePetiteInventory() {
        if (SixtySecondsSearchZonesClient.isInSearchZone()) {
            return true;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !inSixtySecondsMode() || gameComponent == null) {
            return false;
        }
        return gameComponent.getGameStatus() == SixtySecGameWorldComponent.GameStatus.ACTIVE
                && SixtySecondsStatsComponent.KEY.get(player).dayNumber <= 0;
    }

    /** Forces PetiteInventory on for every container after the first shelter day. */
    public static boolean shouldForcePetiteInventory() {
        if (shouldDisablePetiteInventory()) {
            return false;
        }
        if (specialInventoryForced) {
            return true;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && inSixtySecondsMode() && gameComponent != null
                && gameComponent.getGameStatus() == SixtySecGameWorldComponent.GameStatus.ACTIVE
                && SixtySecondsStatsComponent.KEY.get(player).dayNumber > 0;
    }

    /** Called when the server has opened the menu through /60s inventory. */
    public static void forceSpecialInventoryUntilRoundStart() {
        specialInventoryForced = true;
        clearSpecialInventoryRequest();
    }

    public static boolean isSpecialInventoryForced() {
        return specialInventoryForced;
    }

    /** Prevent a command used in one world from affecting the next world. */
    public static void clearSpecialInventoryOverride() {
        specialInventoryForced = false;
        clearSpecialInventoryRequest();
    }

    /** Returns true exactly once until the server opens the menu or timeout. */
    public static boolean beginSpecialInventoryRequest() {
        long now = System.nanoTime();
        if (now - specialInventoryRequestNanos < SPECIAL_INVENTORY_REQUEST_TIMEOUT_NANOS) {
            return false;
        }
        specialInventoryRequestNanos = now;
        return true;
    }

    public static void clearSpecialInventoryRequest() {
        specialInventoryRequestNanos = 0L;
    }

    /**
     * The legacy restricted screen remains the active screen during the
     * preparation/house-search phase.  The new menu is deliberately limited
     * to actual game days, while the old screen is still useful elsewhere.
     */
    public static boolean shouldUseLegacyInventory() {
        return inSixtySecondsMode() && !shouldOpenSpecialInventory();
    }

    public static boolean isPlayerAliveAndInSurvivalIgnoreShitSplit() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isSpectator() && !player.isCreative();
    }
}
