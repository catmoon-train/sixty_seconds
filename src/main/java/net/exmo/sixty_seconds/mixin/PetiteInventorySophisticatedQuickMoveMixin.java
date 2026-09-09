package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables PetiteInventory's alternate storage quick-move path while searching. */
@Mixin(targets = "com.sighs.petiteinventory.platform.inventory.SophisticatedQuickMoveService", remap = false)
public abstract class PetiteInventorySophisticatedQuickMoveMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$useVanillaQuickMove(AbstractContainerMenu menu,
                                                           Player player,
                                                           int slotIndex,
                                                           CallbackInfoReturnable<ItemStack> cir) {
        if (SixtySecondsInventoryLimit.isPetiteInventoryDisabled(player)) {
            // null means "not handled" to PetiteInventory's event listener;
            // vanilla then executes the menu's normal quickMoveStack method.
            cir.setReturnValue(null);
        }
    }
}
