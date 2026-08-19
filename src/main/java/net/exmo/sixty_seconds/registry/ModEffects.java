package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, SixtySeconds.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> USED_BANED = EFFECTS.register("used_baned",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x222222) {});
    public static final DeferredHolder<MobEffect, MobEffect> MOVE_BANED = EFFECTS.register("move_baned",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x444444) {});
    public static final DeferredHolder<MobEffect, MobEffect> BREAK_IN_INTRUDER = EFFECTS.register("break_in_intruder",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x881111) {});
    public static final DeferredHolder<MobEffect, MobEffect> VISION_FOG = EFFECTS.register("vision_fog",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x668899) {});
    public static final DeferredHolder<MobEffect, MobEffect> INVINCIBLE = EFFECTS.register("invincible",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFEE88) {});

    private ModEffects() {}
    public static void register(IEventBus bus) { EFFECTS.register(bus); }
}
