package net.exmo.sixty_seconds.bridge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class SixtySecRole {
    private final ResourceLocation identifier;
    private final Component name;

    public SixtySecRole(ResourceLocation identifier, Component name) {
        this.identifier = identifier;
        this.name = name;
    }

    public ResourceLocation getIdentifier() {
        return identifier;
    }

    public Component getName() {
        return name;
    }

    public boolean isKiller() {
        return false;
    }

    public boolean canUseSabotage() {
        return false;
    }
}
