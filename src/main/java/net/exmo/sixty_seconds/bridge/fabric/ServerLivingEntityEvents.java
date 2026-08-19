package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class ServerLivingEntityEvents {
    public static final Event<AllowDeath> ALLOW_DEATH = new Event<>();
    public static final Event<AfterDeath> AFTER_DEATH = new Event<>();
    public static final Event<AllowDamage> ALLOW_DAMAGE = new Event<>();
    public static final Event<AfterDamage> AFTER_DAMAGE = new Event<>();

    private ServerLivingEntityEvents() {
    }

    @FunctionalInterface
    public interface AllowDeath {
        boolean allowDeath(LivingEntity entity, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface AfterDeath {
        void afterDeath(LivingEntity entity, DamageSource source);
    }

    @FunctionalInterface
    public interface AllowDamage {
        boolean allowDamage(LivingEntity entity, DamageSource source, float amount);
    }

    @FunctionalInterface
    public interface AfterDamage {
        void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken,
                boolean blocked);
    }
}
