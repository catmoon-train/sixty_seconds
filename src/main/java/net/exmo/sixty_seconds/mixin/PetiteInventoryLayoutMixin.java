package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.client.ScreenLayoutSettings;
import net.exmo.sixty_seconds.client.screen.SixtySecondsSearchZonesClient;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps PetiteInventory disabled for every screen during house searching. */
@Mixin(value = ScreenLayoutSettings.class, remap = false)
public abstract class PetiteInventoryLayoutMixin {
    @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySecondsDisableDuringSearch(Screen screen,
            CallbackInfoReturnable<Boolean> cir) {
        if (SixtySecondsSearchZonesClient.isInSearchZone()) {
            cir.setReturnValue(false);
        }
    }
}
