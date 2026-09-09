package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.client.ScreenLayoutSettings;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
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
        if (SixtySecBridgeClient.shouldDisablePetiteInventory()) {
            cir.setReturnValue(false);
        } else if (SixtySecBridgeClient.shouldForcePetiteInventory()) {
            // The setting is persisted by PetiteInventory per screen class.
            // The 60 Seconds shelter UI must not inherit an old opt-out from
            // a previous test run or from a different screen.
            cir.setReturnValue(true);
        }
    }
}
