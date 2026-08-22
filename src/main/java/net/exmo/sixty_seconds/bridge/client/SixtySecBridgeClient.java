package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.bridge.AreasWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SixtySecBridgeClient {
    public static SixtySecGameWorldComponent gameComponent;
    public static AreasWorldComponent areaComponent;

    private SixtySecBridgeClient() {
    }

    public static boolean isPlayerAliveAndInSurvivalIgnoreShitSplit() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isSpectator() && !player.isCreative();
    }
}
