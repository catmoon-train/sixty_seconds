package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.AreasWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;

public final class SixtySecBridgeClient {
    public static SixtySecGameWorldComponent gameComponent;
    public static AreasWorldComponent areaComponent;
    private static boolean specialInventoryForced;

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

    /** Called when the server has opened the menu through /60s inventory. */
    public static void forceSpecialInventoryUntilRoundStart() {
        specialInventoryForced = true;
    }

    public static boolean isSpecialInventoryForced() {
        return specialInventoryForced;
    }

    /** Prevent a command used in one world from affecting the next world. */
    public static void clearSpecialInventoryOverride() {
        specialInventoryForced = false;
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
