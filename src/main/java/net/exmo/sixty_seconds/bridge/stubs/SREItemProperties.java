package net.exmo.sixty_seconds.bridge.stubs;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SREItemProperties {
    private SREItemProperties() {}
    public interface HeldLikeRevolver {}
    public interface LeftClickHurtable {
        boolean onServerAttack(ServerPlayer attacker, ServerPlayer target, ItemStack mainhandItem);
    }
}
