package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.client.SixtySecondsSanityShader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 {@code GameRenderer#render} 写入主渲染目标后叠加低理智后处理（去色/色差），
 * 并在窗口尺寸变化时同步重建交换缓冲。
 * <p>移植自 SRE 的 {@code io.wifi.starrailexpress.mixin.client.MixinGameRenderer}。</p>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    private void sixtySeconds$renderSanityPostProcess(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
        GameRenderer renderer = (GameRenderer) (Object) this;

        if (renderer != null && bl && renderer.getMinecraft().level != null) {
            SixtySecondsSanityShader gui = SixtySecondsSanityShader.instance;
            gui.initPostProcessor();
            gui.renderPostProcess(deltaTracker.getGameTimeDeltaPartialTick(true));
        }
    }

    @Inject(method = "resize(II)V", at = @At("TAIL"))
    private void sixtySeconds$resize(int pWidth, int pHeight, CallbackInfo ci) {
        if (SixtySecondsSanityShader.instance != null) {
            SixtySecondsSanityShader.instance.resize(pWidth, pHeight);
        }
    }
}
