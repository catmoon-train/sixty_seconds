package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/** Client asks the server to replace the local vanilla inventory menu. */
public record OpenSpecialInventoryC2SPacket(boolean preparationOverride) implements CustomPacketPayload {
    public static final Type<OpenSpecialInventoryC2SPacket> ID =
            new Type<>(SixtySeconds.id("open_special_inventory"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSpecialInventoryC2SPacket> CODEC =
            StreamCodec.of((buf, packet) -> buf.writeBoolean(packet.preparationOverride()),
                    buf -> new OpenSpecialInventoryC2SPacket(buf.readBoolean()));

    @Override
    public Type<OpenSpecialInventoryC2SPacket> type() {
        return ID;
    }

    public static void handle(OpenSpecialInventoryC2SPacket packet, ServerPlayer player) {
        if (!canOpenDuringRound(player, packet.preparationOverride())) {
            SixtySeconds.LOGGER.warn("Rejected special inventory request from {} (override={}, phase={}, active={})",
                    player.getGameProfile().getName(), packet.preparationOverride(),
                    SixtySecondsState.get(player.serverLevel()).phase,
                    SixtySecondsMod.isActive(player.level()));
            return;
        }
        SixtySeconds.LOGGER.info("Opening special inventory for {} (override={}, phase={})",
                player.getGameProfile().getName(), packet.preparationOverride(),
                SixtySecondsState.get(player.serverLevel()).phase);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, owner) -> new SpecialInventoryMenu(id, inventory),
                Component.translatable("container.sixty_seconds.special_inventory")));
    }

    private static boolean canOpenDuringRound(ServerPlayer player, boolean preparationOverride) {
        SixtySecondsState.Data data = SixtySecondsState.get(player.serverLevel());
        // The explicit /60s inventory opt-in is allowed before a round has
        // started.  Normal E presses never set this bit, so this does not
        // replace the vanilla inventory for ordinary players.
        if (preparationOverride && data.phase == SixtySecondsPhase.INACTIVE) {
            return true;
        }
        if (!SixtySecondsMod.isActive(player.level())) return false;
        // Normal E opens are limited to game days. The explicit command may
        // opt into the new menu during the preparation/house-search phase.
        return data.phase == SixtySecondsPhase.DAY
                || (preparationOverride && data.phase == SixtySecondsPhase.PREPARATION
                && !SixtySecondsSearchZones.isInSearchZone(player));
    }
}
