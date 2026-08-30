package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, SixtySeconds.MOD_ID);

    public static SoundEvent ITEM_REVOLVER_CLICK;
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_CLICK = SOUNDS.register("item.revolver.click", () -> {
        ITEM_REVOLVER_CLICK = SoundEvent.createVariableRangeEvent(SixtySeconds.id("item.revolver.click"));
        return ITEM_REVOLVER_CLICK;
    });
    public static SoundEvent ITEM_REVOLVER_SHOOT;
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_SHOOT = SOUNDS.register("item.revolver.shoot", () -> {
        ITEM_REVOLVER_SHOOT = SoundEvent.createVariableRangeEvent(SixtySeconds.id("item.revolver.shoot"));
        return ITEM_REVOLVER_SHOOT;
    });
    public static SoundEvent ITEM_GRENADE_THROW;
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_GRENADE = SOUNDS.register("item.grenade.throw", () -> {
        ITEM_GRENADE_THROW = SoundEvent.createVariableRangeEvent(SixtySeconds.id("item.grenade.throw"));
        return ITEM_GRENADE_THROW;
    });
    public static SoundEvent BROKEN_ALARM;
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_ALARM = SOUNDS.register("broken_alarm", () -> {
        BROKEN_ALARM = SoundEvent.createVariableRangeEvent(SixtySeconds.id("broken_alarm"));
        return BROKEN_ALARM;
    });

    public static SoundEvent COUGH;
    public static final DeferredHolder<SoundEvent, SoundEvent> HOLD_COUGH = SOUNDS.register("cough", () -> {
        COUGH = SoundEvent.createVariableRangeEvent(SixtySeconds.id("cough"));
        return COUGH;
    });

    private ModSounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
