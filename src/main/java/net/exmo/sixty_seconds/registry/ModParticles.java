package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 两种天气粒子类型：weather_streak(竖直条/雨幕) 与 weather_dust(柔团/雾)。 */
public final class ModParticles {
    public static SimpleParticleType WEATHER_STREAK;
    public static SimpleParticleType WEATHER_DUST;

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModParticles::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.PARTICLE_TYPE)) {
            return;
        }
        WEATHER_STREAK = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                SixtySeconds.id("weather_streak"), new SimpleParticleType(false));
        WEATHER_DUST = Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                SixtySeconds.id("weather_dust"), new SimpleParticleType(false));
    }
}
