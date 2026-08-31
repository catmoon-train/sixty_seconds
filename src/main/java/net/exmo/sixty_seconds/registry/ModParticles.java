package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

/** 天气粒子类型。weather_streak/weather_dust 为通用雨幕/柔团，其余为各天气专属贴图粒子。 */
public final class ModParticles {
    public static final SimpleParticleType WEATHER_STREAK = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_DUST = new SimpleParticleType(false);

    // 既有天气专属贴图
    public static final SimpleParticleType WEATHER_POLLUTION_RAIN = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_BLOOD_MOTE = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SPARK = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_RADIATION = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_EMBER = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SNOW = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_BUBBLE = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SAND = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SPORE = new SimpleParticleType(false);

    // 新增天气专属贴图
    public static final SimpleParticleType WEATHER_ASH = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_FIRE_RAIN = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_CRYSTAL = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_TOXIC_SPORE = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SOLAR = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_SOUL = new SimpleParticleType(false);
    public static final SimpleParticleType WEATHER_EMBER_STORM = new SimpleParticleType(false);

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        bus.addListener(ModParticles::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.PARTICLE_TYPE)) {
            return;
        }
        register("weather_streak", WEATHER_STREAK);
        register("weather_dust", WEATHER_DUST);
        register("weather_pollution_rain", WEATHER_POLLUTION_RAIN);
        register("weather_blood_mote", WEATHER_BLOOD_MOTE);
        register("weather_spark", WEATHER_SPARK);
        register("weather_radiation", WEATHER_RADIATION);
        register("weather_ember", WEATHER_EMBER);
        register("weather_snow", WEATHER_SNOW);
        register("weather_bubble", WEATHER_BUBBLE);
        register("weather_sand", WEATHER_SAND);
        register("weather_spore", WEATHER_SPORE);
        register("weather_ash", WEATHER_ASH);
        register("weather_fire_rain", WEATHER_FIRE_RAIN);
        register("weather_crystal", WEATHER_CRYSTAL);
        register("weather_toxic_spore", WEATHER_TOXIC_SPORE);
        register("weather_solar", WEATHER_SOLAR);
        register("weather_soul", WEATHER_SOUL);
        register("weather_ember_storm", WEATHER_EMBER_STORM);
    }

    private static void register(String name, SimpleParticleType type) {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, SixtySeconds.id(name), type);
    }
}
