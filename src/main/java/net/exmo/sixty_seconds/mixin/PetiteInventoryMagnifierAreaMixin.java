package net.exmo.sixty_seconds.mixin;

import com.sighs.petiteinventory.inventory.Area;
import net.exmo.sixty_seconds.content.item.SixtySecondsLootMagnifierItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes a loot magnifier inherit the footprint of the loot hidden inside it. */
@Mixin(targets = "com.sighs.petiteinventory.platform.inventory.ItemInventoryService", remap = false)
public abstract class PetiteInventoryMagnifierAreaMixin {
    @Inject(method = "getArea", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sixtySeconds$lootMagnifierArea(ItemStack stack,
            CallbackInfoReturnable<Area<ItemStack>> cir) {
        if (!SixtySecondsLootMagnifierItem.isMagnifier(stack)) return;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        int width = tag.getInt("AreaWidth");
        int height = tag.getInt("AreaHeight");
        if (width > 0 && height > 0) {
            cir.setReturnValue(new Area<>(width, height, stack));
        }
    }
}
