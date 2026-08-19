package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public final class TMMSounds {
    private TMMSounds() {}
    public static SoundEvent ITEM_REVOLVER_CLICK = SoundEvents.DISPENSER_FAIL;
    public static SoundEvent ITEM_REVOLVER_SHOOT = SoundEvents.CROSSBOW_SHOOT;
    public static SoundEvent ITEM_GRENADE_THROW = SoundEvents.SNOWBALL_THROW;
    public static void bind() {
        if (ModSounds.ITEM_REVOLVER_CLICK != null) ITEM_REVOLVER_CLICK = ModSounds.ITEM_REVOLVER_CLICK;
        if (ModSounds.ITEM_REVOLVER_SHOOT != null) ITEM_REVOLVER_SHOOT = ModSounds.ITEM_REVOLVER_SHOOT;
        if (ModSounds.ITEM_GRENADE_THROW != null) ITEM_GRENADE_THROW = ModSounds.ITEM_GRENADE_THROW;
    }
}
