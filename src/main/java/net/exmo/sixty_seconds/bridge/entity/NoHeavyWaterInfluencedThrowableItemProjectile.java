package net.exmo.sixty_seconds.bridge.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;

public abstract class NoHeavyWaterInfluencedThrowableItemProjectile extends ThrowableItemProjectile {
    public NoHeavyWaterInfluencedThrowableItemProjectile(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }
}
