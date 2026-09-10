package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.exmo.sixty_seconds.menu.ExpansionModuleSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截容器槽点击，阻止玩家手动拿取/移动背包里的屏障占位格。
 * 直接调用本模组自带的 {@link SixtySecondsInventoryLimit#shouldBlockClick}，不依赖 SixtySeconds 基础模组。
 */
@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    public void doClick(int slotIndex, int button, ClickType clickType, Player player, CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotIndex >= 0 && slotIndex < menu.slots.size()) {
            Slot slot = menu.getSlot(slotIndex);
            if (slot instanceof ExpansionModuleSlot moduleSlot) {
                SixtySeconds.LOGGER.info(
                        "[60s][ExpansionDebug] AbstractContainerMenu.doClick reached: player={}, "
                                + "menu={}, slotIndex={}, button={}, clickType={}, carried={}, stored={}",
                        player.getGameProfile().getName(), menu.getClass().getSimpleName(), slotIndex,
                        button, clickType, moduleSlot.getItem(), menu.getCarried());
                if (ExpansionModuleSlot.handleClick(menu, moduleSlot, button, clickType, player)) {
                    menu.broadcastChanges();
                    ci.cancel();
                    return;
                }
            }
        }
        if (SixtySecondsMod.isActive(player.level())
                && SixtySecondsInventoryLimit.shouldBlockClick(
                        menu, slotIndex, button, clickType, player)) {
            ci.cancel();
        }
    }
}
