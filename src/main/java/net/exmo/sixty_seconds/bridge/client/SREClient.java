package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.bridge.AreasWorldComponent;
import net.exmo.sixty_seconds.bridge.SREGameWorldComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SREClient {
    public static SREGameWorldComponent gameComponent;
    public static AreasWorldComponent areaComponent;

    private SREClient() {
    }

    public static boolean isPlayerAliveAndInSurvivalIgnoreShitSplit() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && !player.isSpectator() && !player.isCreative();
    }
}
