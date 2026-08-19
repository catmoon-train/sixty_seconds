package net.exmo.sixty_seconds.bridge.client;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.client.DeltaTracker;

import java.util.function.BiConsumer;

public interface CommonHudRenderCallback {
    final class CommonRenderEvent {
        public java.util.ArrayList<BiConsumer<FakeGuiGraphics, DeltaTracker>> role_events = new java.util.ArrayList<>();

        public java.util.ArrayList<BiConsumer<FakeGuiGraphics, DeltaTracker>> getConsumer() {
            return role_events;
        }

        public void register(BiConsumer<FakeGuiGraphics, DeltaTracker> consumer) {
            role_events.add(consumer);
        }
    }

    CommonRenderEvent EVENT = new CommonRenderEvent();
}
