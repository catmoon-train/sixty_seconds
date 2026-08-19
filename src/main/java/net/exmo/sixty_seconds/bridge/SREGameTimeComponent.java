package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.world.level.Level;

public class SREGameTimeComponent {
    public static final ComponentKey<SREGameTimeComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("game_time"), SREGameTimeComponent.class);

    private final Level world;
    private int resetTime;

    public SREGameTimeComponent(Level world) {
        this.world = world;
    }

    public void setResetTime(int time) {
        this.resetTime = time;
    }

    public int getResetTime() {
        return resetTime;
    }

    public void reset() {
        this.resetTime = 0;
    }
}
