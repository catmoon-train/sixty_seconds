package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.bridge.SREGameWorldComponent;
import net.exmo.sixty_seconds.bridge.SRERole;
import net.minecraft.world.entity.player.Player;

public final class RoleUtils {
    private RoleUtils() {
    }

    public static void changeRole(Player player, SRERole role) {
        SREGameWorldComponent.KEY.get(player.level()).setRole(player, role);
    }
}
