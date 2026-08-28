package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.exmo.sixty_seconds.client.screen.SixtySecondsInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在打开原版玩家物品栏时替换为受限制的 {@link SixtySecondsInventoryScreen}。
 * 比 ScreenEvent.Opening 更可靠（始终拦截得到 E 键打开背包的调用）。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void sixtySecondsReplaceInventoryScreen(Screen screen, CallbackInfo ci) {
        if (!(screen instanceof InventoryScreen inv)) {
            return;
        }
        Minecraft mc = (Minecraft) (Object) this;
        LocalPlayer player = mc.player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }
        // 受限背包：60s 模式（按家庭身份/每日槽位限制）会对锁定槽位塞入屏障占位。
        // 只要处于模式内或检测到占位即切换受限界面。
        boolean restricted = SixtySecBridgeClient.inSixtySecondsMode()
                || hasBarrierSlots(player.getInventory());
        if (!restricted) {
            return;
        }
        InventoryMenu menu = inv.getMenu();
        Screen replacement = new SixtySecondsInventoryScreen(
                menu, player.getInventory(), Component.translatable("container.inventory"));
        ci.cancel();
        mc.setScreen(replacement); // 递归调用：replacement 非 InventoryScreen，不会再次拦截
    }

    private static boolean hasBarrierSlots(Inventory inv) {
        for (net.minecraft.world.item.ItemStack s : inv.items) {
            if (s.is(Items.BARRIER)) {
                return true;
            }
        }
        for (net.minecraft.world.item.ItemStack s : inv.armor) {
            if (s.is(Items.BARRIER)) {
                return true;
            }
        }
        for (net.minecraft.world.item.ItemStack s : inv.offhand) {
            if (s.is(Items.BARRIER)) {
                return true;
            }
        }
        return false;
    }
}
