package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.platform.SophisticatedQuickMovePayload;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents PetiteInventory's vanilla-inventory-only packet from touching
 * the component-backed extension slots. */
@Mixin(value = SophisticatedQuickMovePayload.class, remap = false)
public abstract class PetiteInventorySophisticatedQuickMovePayloadMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$ignoreForSpecialInventory(
            SophisticatedQuickMovePayload payload, IPayloadContext context, CallbackInfo ci) {
        if (context.player() != null
                && context.player().containerMenu instanceof SpecialInventoryMenu) {
            ci.cancel();
        }
    }
}
