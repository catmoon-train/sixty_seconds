package net.exmo.sixty_seconds.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes AbstractContainerMenu.addSlot to the vanilla InventoryMenu mixin. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAddSlotInvoker {
    @Invoker("addSlot")
    <T extends Slot> T sixtySeconds$invokeAddSlot(T slot);
}
