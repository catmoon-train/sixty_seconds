package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.logic.SixtySecondsExpansionModuleContainer;
import net.exmo.sixty_seconds.menu.ExpansionModuleSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the same expansion-module socket to the vanilla inventory menu. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuModuleSlotMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void sixtySeconds$addModuleSocket(Inventory inventory, boolean active, Player owner,
            CallbackInfo ci) {
        ((AbstractContainerMenuAddSlotInvoker) (Object) this).sixtySeconds$invokeAddSlot(
                new ExpansionModuleSlot(
                new SixtySecondsExpansionModuleContainer(owner), 77, 44));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void sixtySeconds$blockModuleQuickMove(Player player, int index,
            CallbackInfoReturnable<ItemStack> cir) {
        // The module may only be removed through its socket, and only while
        // its embedded container is empty (ExpansionModuleSlot enforces that).
        if (index == 46) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
