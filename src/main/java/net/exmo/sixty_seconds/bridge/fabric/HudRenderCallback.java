package net.exmo.sixty_seconds.bridge.fabric;

import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

public interface HudRenderCallback {
    Event<HudRenderCallback> EVENT = new Event<>();

    void onHudRender(GuiGraphics graphics, DeltaTracker tickCounter);
}
