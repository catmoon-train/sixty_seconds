package net.exmo.sixty_seconds.registry;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

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

    // ─────────────────────────────────────────────────────────────
    // 每个小怪的独立音效（环境 / 受伤 / 死亡）。
    // 当前暂用原版音频路径，便于直接区分；后续如需自定义，
    // 在 resources/assets/sixty_seconds/sounds/ 放入对应 .ogg，
    // 并把 sounds.json 里 value 改成 "sixty_seconds:文件名" 即可。
    // key 为变体枚举名小写（如 "shambler"、"bonelord"）。
    // ─────────────────────────────────────────────────────────────
    private static final String[][] MONSTER_TRIPLETS = {
            {"shambler",    "entity/husk/ambient",            "entity/husk/hurt",            "entity/husk/death"},
            {"runner",      "entity/phantom/ambient",         "entity/phantom/hurt",         "entity/phantom/death"},
            {"brute",       "entity/ravager/ambient",         "entity/ravager/hurt",         "entity/ravager/death"},
            {"spitter",     "entity/witch/ambient",           "entity/witch/hurt",           "entity/witch/death"},
            {"stalker",     "entity/enderman/ambient",        "entity/enderman/hurt",        "entity/enderman/death"},
            {"howler",      "entity/wolf/ambient",            "entity/wolf/hurt",            "entity/wolf/death"},
            {"bloater",     "entity/zoglin/ambient",          "entity/zoglin/hurt",          "entity/zoglin/death"},
            {"juggernaut",  "entity/warden/ambient",          "entity/warden/hurt",          "entity/warden/death"},
            {"cinderling",  "entity/blaze/ambient",           "entity/blaze/hurt",           "entity/blaze/death"},
            {"frostling",   "entity/stray/ambient",           "entity/stray/hurt",           "entity/stray/death"},
            {"huskbrute",   "entity/hoglin/ambient",          "entity/hoglin/hurt",          "entity/hoglin/death"},
            {"ravenor",     "entity/vex/ambient",             "entity/vex/hurt",             "entity/vex/death"},
            {"wailer",      "entity/ghast/ambient",           "entity/ghast/hurt",           "entity/ghast/death"},
            {"burster",     "entity/vindicator/ambient",      "entity/vindicator/hurt",      "entity/vindicator/death"},
            {"gorehound",   "entity/zombified_piglin/ambient","entity/zombified_piglin/hurt","entity/zombified_piglin/death"},
            {"shadowmute",  "entity/spider/ambient",          "entity/spider/hurt",          "entity/spider/death"},
            {"bonelord",    "entity/wither_skeleton/ambient", "entity/wither_skeleton/hurt", "entity/wither_skeleton/death"},
            {"spinewalker", "entity/skeleton/ambient",        "entity/skeleton/hurt",        "entity/skeleton/death"},
    };

    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MONSTER_AMBIENT = new HashMap<>();
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MONSTER_HURT = new HashMap<>();
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> MONSTER_DEATH = new HashMap<>();

    static {
        for (String[] t : MONSTER_TRIPLETS) {
            String key = t[0];
            MONSTER_AMBIENT.put(key, registerMonster("monster." + key + ".ambient"));
            MONSTER_HURT.put(key, registerMonster("monster." + key + ".hurt"));
            MONSTER_DEATH.put(key, registerMonster("monster." + key + ".death"));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerMonster(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(SixtySeconds.id(id)));
    }

    /** 取某变体的环境音（key = 变体枚举名小写）。 */
    public static SoundEvent ambientOf(String variantKey) {
        return MONSTER_AMBIENT.getOrDefault(variantKey, MONSTER_AMBIENT.get("shambler")).get();
    }

    public static SoundEvent hurtOf(String variantKey) {
        return MONSTER_HURT.getOrDefault(variantKey, MONSTER_HURT.get("shambler")).get();
    }

    public static SoundEvent deathOf(String variantKey) {
        return MONSTER_DEATH.getOrDefault(variantKey, MONSTER_DEATH.get("shambler")).get();
    }

    private ModSounds() {}
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
}
