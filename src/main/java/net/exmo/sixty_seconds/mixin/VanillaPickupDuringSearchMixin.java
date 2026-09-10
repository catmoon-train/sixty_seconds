package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import com.sighs.petiteinventory.event.InventoryEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypasses PetiteInventory's admission result during house searching. Petite
 * Inventory injects an admission callback into Inventory.add; changing the
 * result at the original admission object is the stable way to let vanilla's
 * complete insertion algorithm run, including for multi-cell items.
 */
@Mixin(value = InventoryEvents.Admission.class, remap = false, priority = 2000)
public abstract class VanillaPickupDuringSearchMixin {
    @Inject(method = "isHandled", at = @At("HEAD"), cancellable = true, remap = false)
    private void sixtySeconds$ignorePetiteAdmission(CallbackInfoReturnable<Boolean> cir) {
        InventoryEvents.Admission admission = (InventoryEvents.Admission) (Object) this;
        Object subject = admission.player();
        if (subject instanceof Player player
                && SixtySecondsInventoryLimit.isPetiteInventoryDisabled(player)) {
            cir.setReturnValue(false);
        }
    }
}
