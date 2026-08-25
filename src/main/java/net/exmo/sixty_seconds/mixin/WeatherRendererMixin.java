package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.weather.ClientWeatherState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 纯客户端：当存在激活的主题化天气时，封掉原版雨雪的「粒子生成(tickRain)」与「下落幕渲染(renderSnowAndRain)」，
 * 二者由 WeatherParticleSpawner 用主题化粒子替换。无主题天气时原版行为完全不变。
 */
@Mixin(LevelRenderer.class)
public abstract class WeatherRendererMixin {

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void sixty_cancelTickRain(Camera camera, CallbackInfo ci) {
        if (ClientWeatherState.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void sixty_cancelRenderSnowAndRain(LightTexture lightTexture, float partialTick,
                                               double camX, double camY, double camZ, CallbackInfo ci) {
        if (ClientWeatherState.isActive()) {
            ci.cancel();
        }
    }
}
