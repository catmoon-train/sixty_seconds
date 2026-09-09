package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps PetiteInventory's server-side automation out of the first house
 * search phase.  Returning "not handled" is important: it lets vanilla run
 * the menu's ordinary one-cell quickMoveStack implementation.
 */
@Mixin(targets = "com.sighs.petiteinventory.platform.inventory.QuickMoveService", remap = false)
public abstract class PetiteInventoryQuickMoveMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$useVanillaQuickMove(AbstractContainerMenu menu,
                                                           int slotIndex,
                                                           int button,
                                                           ClickType clickType,
                                                           Player player,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (menu instanceof SpecialInventoryMenu
                || SixtySecondsInventoryLimit.isPetiteInventoryDisabled(player)) {
            cir.setReturnValue(false);
        }
    }
}
