package net.exmo.sixty_seconds;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Shared mod identity used by copied 60s gameplay code. */
public final class SixtySeconds {
    public static final String MOD_ID = "sixty_seconds";
    public static final Logger LOGGER = LogUtils.getLogger();

    private SixtySeconds() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
