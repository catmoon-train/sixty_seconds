package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public interface OnRenderRoleName {
    Event<OnRenderRoleName> EVENT = new Event<>(listeners -> (player, current) -> {
        Component result = current;
        for (OnRenderRoleName listener : listeners) {
            result = listener.render(player, result);
        }
        return result;
    });

    Event<RenderPlayerExtraInterface> RENDER_PLAYER_EXTRA = new Event<>(listeners -> (self, target, context, tickCounter, font) -> {
        for (RenderPlayerExtraInterface listener : listeners) {
            listener.renderExtra(self, target, context, tickCounter, font);
        }
    });

    Component render(Player player, Component current);

    @FunctionalInterface
    interface RenderPlayerExtraInterface {
        void renderExtra(Player self, Player target, FakeGuiGraphics context, DeltaTracker tickCounter, Font font);
    }
}
