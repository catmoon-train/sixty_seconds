package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** Client asks the server to replace the local vanilla inventory menu. */
public record OpenSpecialInventoryC2SPacket() implements CustomPacketPayload {
    public static final Type<OpenSpecialInventoryC2SPacket> ID =
            new Type<>(SixtySeconds.id("open_special_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpecialInventoryC2SPacket> CODEC =
            StreamCodec.unit(new OpenSpecialInventoryC2SPacket());

    @Override
    public Type<OpenSpecialInventoryC2SPacket> type() {
        return ID;
    }

    public static void handle(OpenSpecialInventoryC2SPacket ignored, ServerPlayer player) {
        if (!canOpenDuringRound(player)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new SpecialInventoryMenu(id, inventory),
                Component.translatable("container.sixty_seconds.special_inventory")));
    }

    private static boolean canOpenDuringRound(ServerPlayer player) {
        if (!SixtySecondsMod.isActive(player.level())) return false;
        SixtySecondsState.Data data = SixtySecondsState.get(player.serverLevel());
        // The preparation/house-search phase deliberately keeps the special UI
        // disabled. /60s inventory is the explicit inactive-mode escape hatch.
        return data.phase == SixtySecondsPhase.DAY;
    }
}
