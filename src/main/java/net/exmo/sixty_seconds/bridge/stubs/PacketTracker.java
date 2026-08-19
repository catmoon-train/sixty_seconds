package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PacketTracker {
    private PacketTracker() {}
    public static void sendToClient(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
