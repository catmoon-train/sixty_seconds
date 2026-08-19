package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.List;

public final class KeyBindingHelper {
    public static final List<KeyMapping> KEYS = new ArrayList<>();

    private KeyBindingHelper() {
    }

    public static KeyMapping registerKeyBinding(KeyMapping mapping) {
        KEYS.add(mapping);
        return mapping;
    }
}
