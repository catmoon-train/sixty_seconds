package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.context.CommandContext;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

/** Opens the special inventory even while the 60 Seconds round is inactive. */
public final class SixtySecondsInventoryCommand {
    private SixtySecondsInventoryCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(literal("60s")
                        .then(literal("inventory")
                                .executes(SixtySecondsInventoryCommand::open))));
    }

    private static int open(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception ignored) {
            context.getSource().sendFailure(Component.translatable(
                    "command.sixty_seconds.inventory.player_only"));
            return 0;
        }
        if (SixtySecondsSearchZones.isInSearchZone(player)
                || SixtySecondsState.get(player.serverLevel()).phase == SixtySecondsPhase.PREPARATION) {
            context.getSource().sendFailure(Component.translatable(
                    "command.sixty_seconds.inventory.search_blocked"));
            return 0;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inventory, ignored) -> new SpecialInventoryMenu(id, inventory),
                Component.translatable("container.sixty_seconds.special_inventory")));
        return 1;
    }
}
