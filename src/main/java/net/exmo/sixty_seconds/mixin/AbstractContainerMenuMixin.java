package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截容器槽点击，阻止玩家手动拿取/移动背包里的屏障占位格。
 * 直接调用本模组自带的 {@link SixtySecondsInventoryLimit#shouldBlockClick}，不依赖 StarRailExpress 基础模组。
 */
@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    public void doClick(int slotIndex, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (SixtySecondsMod.isActive(player.level())
                && SixtySecondsInventoryLimit.shouldBlockClick(
                        (AbstractContainerMenu) (Object) this, slotIndex, button, clickType, player)) {
            ci.cancel();
        }
    }
}
