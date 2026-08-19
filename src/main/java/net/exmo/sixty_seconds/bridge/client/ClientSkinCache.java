package net.exmo.sixty_seconds.bridge.client;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class ClientSkinCache {
    private ClientSkinCache() {}
    public static ResourceLocation getSkin(UUID uuid) {
        return Minecraft.getInstance().getSkinManager().getInsecureSkin(new GameProfile(uuid, "body")).texture();
    }
}
