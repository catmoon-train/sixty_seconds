package net.exmo.sixty_seconds.client.weather;

import net.exmo.sixty_seconds.registry.ModParticles;
import net.exmo.sixty_seconds.weather.ClientWeatherState;
import net.exmo.sixty_seconds.weather.WeatherVisualConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

/**
 * 通用天气粒子：渲染为受光照/透明度的广告牌贴图，颜色/大小/速度由当前激活天气主题决定。
 * 由 spawner 根据主题选择 {@code weather_streak}(竖直条) 或 {@code weather_dust}(柔团) 两种类型。
 */
public class WeatherParticle extends TextureSheetParticle {
    private final float baseAlpha;

    protected WeatherParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.setSprite(sprite);

        WeatherTheme theme = WeatherThemes.get(ClientWeatherState.getEventType());
        if (theme == null) {
            theme = WeatherThemes.FALLBACK;
        }

        this.lifetime = theme.lifeMin + level.random.nextInt(theme.lifeMax - theme.lifeMin + 1);
        this.gravity = theme.gravity;
        this.hasPhysics = false;

        float size = theme.size * (float) WeatherVisualConfig.SIZE_MULTIPLIER.get().doubleValue();
        this.quadSize = size;
        this.setSize(size, size);

        this.xd = vx;
        this.yd = vy;
        this.zd = vz;

        this.rCol = theme.r;
        this.gCol = theme.g;
        this.bCol = theme.b;
        this.baseAlpha = theme.alpha;
        this.alpha = 0.0F; // 淡入
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age < 6) {
            this.alpha = this.baseAlpha * (this.age / 6.0F);
        } else if (this.age > this.lifetime - 8) {
            this.alpha = this.baseAlpha * Mth.clamp((this.lifetime - this.age) / 8.0F, 0.0F, 1.0F);
        } else {
            this.alpha = this.baseAlpha;
        }
    }

    @NotNull
    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return (15 << 20) | 15; // 全亮，确保主题粒子清晰可见
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final ResourceLocation spriteLoc;
        private TextureAtlasSprite sprite;

        public Provider(ResourceLocation spriteLoc) {
            this.spriteLoc = spriteLoc;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level,
                                       double x, double y, double z, double xs, double ys, double zs) {
            if (sprite == null) {
                sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_PARTICLES).apply(spriteLoc);
            }
            return new WeatherParticle(level, x, y, z, xs, ys, zs, sprite);
        }
    }
}
