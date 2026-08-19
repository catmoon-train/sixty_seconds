package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.RoleComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.world.entity.player.Player;

public class DynamicShopComponent implements RoleComponent {
    public static final ComponentKey<DynamicShopComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("dynamic_shop"), DynamicShopComponent.class);
    private final Player player;
    public DynamicShopComponent(Player player) { this.player = player; }
    @Override public Player getPlayer() { return player; }
    @Override public void init() {}
    @Override public void clear() {}
    public void setMultiplier(net.minecraft.resources.ResourceLocation itemId, double multiplier) {}
}
