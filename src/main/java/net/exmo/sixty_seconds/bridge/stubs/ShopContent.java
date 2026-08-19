package net.exmo.sixty_seconds.bridge.stubs;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ShopContent {
    private ShopContent() {
    }

    public static List<ShopEntry> getShopEntries(ResourceLocation roleId) {
        return List.of();
    }
}
