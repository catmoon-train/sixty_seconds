package net.exmo.sixty_seconds.bridge;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SixtySecGameModes {
    public static final List<GameMode> MODES = new ArrayList<>();
    public static GameMode MURDER;

    private SixtySecGameModes() {
    }

    public static GameMode registerGameMode(GameMode mode) {
        MODES.add(mode);
        return mode;
    }

    public static GameMode get(ResourceLocation id) {
        for (GameMode mode : MODES) {
            if (mode.identifier.equals(id)) {
                return mode;
            }
        }
        return null;
    }
}
