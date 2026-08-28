package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁止玩家把 60s 模式用来占位的 BARRIER 扔进世界。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void preventBarrierDrop(boolean p_36133_, CallbackInfoReturnable<Boolean> ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        Level level = player.level();
        if (level == null || !SixtySecondsMod.isActive(level)) {
            return;
        }
        if (player.getInventory().getSelected().is(Items.BARRIER)) {
            ci.cancel();
        }
    }
}
