package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.AreasWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SixtySecBridgeClient {
    public static SixtySecGameWorldComponent gameComponent;
    public static AreasWorldComponent areaComponent;

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
        return inSixtySecondsMode() && gameComponent.getGameStatus()
                != SixtySecGameWorldComponent.GameStatus.INACTIVE;
    }

    public static boolean isPlayerAliveAndInSurvivalIgnoreShitSplit() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isSpectator() && !player.isCreative();
    }
}
