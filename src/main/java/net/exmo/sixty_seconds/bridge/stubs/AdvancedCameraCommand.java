package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancedCameraCommand {
    private AdvancedCameraCommand() {}
    public static void sendIntro(ServerPlayer player, int ticks, double dist, double height) {
        ServerPlayNetworking.send(player, new AdvancedCameraPayload(ticks, dist, height));
    }
}
