package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.RoleComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.world.entity.player.Player;

public class SixtySecPlayerShopComponent implements RoleComponent {
    public static final ComponentKey<SixtySecPlayerShopComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("player_shop"), SixtySecPlayerShopComponent.class);
    private final Player player;
    public SixtySecPlayerShopComponent(Player player) { this.player = player; }
    @Override public Player getPlayer() { return player; }
    @Override public void init() {}
    @Override public void clear() {}
    public void addToBalance(int amount) {}
}
