package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.client.ClientInventoryContext;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ensures all PetiteInventory mouse/drag hooks become no-ops while searching. */
@Mixin(value = ClientInventoryContext.class, remap = false)
public abstract class PetiteInventoryClientGridMixin {
    @Inject(method = "isClientGridSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$disableGridDuringSearch(Slot slot,
                                                               CallbackInfoReturnable<Boolean> cir) {
        // Keep PetiteInventory's grid calculation and rendering active.  The
        // custom menu disables only its own client-side inventory mutation;
        // this hook must still be available for multi-cell highlighting and
        // click mapping.
        if (SixtySecBridgeClient.shouldDisablePetiteInventory()) {
            cir.setReturnValue(false);
        }
    }
}
