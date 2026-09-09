package net.exmo.sixty_seconds.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.exmo.sixty_seconds.integration.PetiteInventoryIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PetiteInventory already owns NBT-rule parsing and footprint matching. Its
 * current TACZ adapter only recognizes GunId; this adds the two other TACZ
 * item families without copying any inventory or placement logic.
 */
@Mixin(targets = "com.sighs.petiteinventory.config.ItemSizeRuleCache")
public abstract class PetiteInventoryTaczNbtMixin {
    /** Reapply 60 Seconds' exact rules after PetiteInventory reloads its file. */
    @Inject(method = "loadAllRule", at = @At("RETURN"))
    private static void sixtySeconds$reapplyWeightRules(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        PetiteInventoryIntegration.installCurrentRules();
    }

    @Inject(method = "getNBTKey", at = @At("HEAD"), cancellable = true)
    private static void sixtySeconds$matchTaczNbt(String itemId, ItemStack stack,
                                                    CallbackInfoReturnable<String> cir) {
        if (stack == null || stack.isEmpty()) return;

        String field = switch (itemId) {
            case "tacz:ammo" -> "AmmoId";
            case "tacz:attachment" -> "AttachmentId";
            default -> null;
        };
        if (field == null) return;

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(field)) return;
        String value = tag.getString(field);
        if (!value.isEmpty()) {
            cir.setReturnValue(itemId + "{" + field + ":\"" + value + "\"}");
        }
    }
}
