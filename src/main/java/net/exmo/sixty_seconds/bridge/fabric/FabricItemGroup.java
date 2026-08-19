package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.world.item.CreativeModeTab;

public final class FabricItemGroup {
    private FabricItemGroup() {
    }

    public static CreativeModeTab.Builder builder() {
        return CreativeModeTab.builder();
    }
}
