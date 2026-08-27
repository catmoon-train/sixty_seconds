package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class DynamicShopComponent implements AutoSyncedComponent {
    public static final ComponentKey<DynamicShopComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("dynamic_shop"), DynamicShopComponent.class);
    private final Player player;
    public DynamicShopComponent(Player player) { this.player = player; }
    public Player getPlayer() { return player; }
    public void init() {}
    public void clear() {}
    @Override public boolean shouldSyncWith(ServerPlayer player) { return this.getPlayer() == player; }
    public void setMultiplier(net.minecraft.resources.ResourceLocation itemId, double multiplier) {}
}
