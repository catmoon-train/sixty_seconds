package net.exmo.sixty_seconds;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/** Shared mod identity used by copied 60s gameplay code. */
public final class SixtySeconds {
    public static final String MOD_ID = "sixty_seconds";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 海洋（海岛）维度：{@code sixty_seconds:ocean}。通过数据包 {@code data/sixty_seconds/dimension/ocean.json} 注册。 */
    public static final ResourceKey<Level> OCEAN_DIMENSION =
            ResourceKey.create(Registries.DIMENSION, id("ocean"));

    private SixtySeconds() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
