package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.resources.ResourceLocation;

public final class StarRailExpressID {
    private StarRailExpressID() {
    }

    public static ResourceLocation shortId(String path) {
        return SixtySeconds.id(path);
    }

    public static ResourceLocation id(String path) {
        return SixtySeconds.id(path);
    }
}
