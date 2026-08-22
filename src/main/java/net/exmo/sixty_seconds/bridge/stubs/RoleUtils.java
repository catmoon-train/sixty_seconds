package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecRole;
import net.minecraft.world.entity.player.Player;

public final class RoleUtils {
    private RoleUtils() {
    }

    public static void changeRole(Player player, SixtySecRole role) {
        SixtySecGameWorldComponent.KEY.get(player.level()).setRole(player, role);
    }
}
