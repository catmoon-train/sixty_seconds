package net.exmo.sixty_seconds.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在六十秒模式进行期间，抑制原版 HUD 的血量/护甲/饥饿/氧气/坐骑血量渲染，
 * 改由 {@code SixtySecondsHud} 的自定义状态栏统一呈现（与 StarRailExpress 设计一致）。
 * 直接作用于原版 {@link Gui} 的各子渲染方法，避免依赖特定 NeoForge 图层事件 API。
 * 同一方法提供两种形参顺序的描述符以兼容不同 1.21.x 构建；{@code require = 0} 保证不匹配也不致命。
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    private static boolean isActive() {
        return SixtySecondsMod.MODE != null
                && SixtySecBridgeClient.gameComponent != null
                && SixtySecBridgeClient.gameComponent.isRunning()
                && SixtySecBridgeClient.gameComponent.getGameMode() == SixtySecondsMod.MODE;
    }

    @Inject(method = "renderHearts(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideHearts(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderFood(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideFoodG(GuiGraphics guiGraphics, int food, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderFood(IILnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideFoodI(int a, int b, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderArmor(Lnet/minecraft/client/gui/GuiGraphics;III)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideArmorG(GuiGraphics guiGraphics, int a, int b, int c, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderArmor(IIILnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideArmorI(int a, int b, int c, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderAir(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideAirG(GuiGraphics guiGraphics, int a, int b, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderAir(IILnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideAirI(int a, int b, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderHealthMount(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideMountG(GuiGraphics guiGraphics, int a, int b, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }

    @Inject(method = "renderHealthMount(IILnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void sixtySeconds_hideMountI(int a, int b, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (isActive()) ci.cancel();
    }
}
