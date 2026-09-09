package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.platform.PlaceItemPayload;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PetiteInventory's place payload writes directly to PlayerInventory. That
 * is correct for vanilla screens, but extension slots in the 60s menu are
 * backed by the stats component and must go through the menu.
 */
@Mixin(value = PlaceItemPayload.class, remap = false)
public abstract class PetiteInventoryPlaceItemPayloadMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$ignoreForSpecialInventory(
            PlaceItemPayload payload, IPayloadContext context, CallbackInfo ci) {
        if (context.player() != null
                && context.player().containerMenu instanceof SpecialInventoryMenu) {
            ci.cancel();
        }
    }
}
