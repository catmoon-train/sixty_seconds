package net.exmo.sixty_seconds.client.weather;

/** 某种天气的粒子表现参数。 */
public final class WeatherTheme {
    public final float r, g, b;          // 颜色 (0..1)
    public final float alpha;            // 不透明度 (0..1)
    public final float size;             // 粒子大小（受全局 sizeMultiplier 影响）
    public final float gravity;          // 每 tick 重力 (负=上升)
    public final boolean streak;         // true=使用雨幕贴图(竖直条)，false=使用烟雾贴图(柔团)
    public final int density;            // 每 tick 每玩家基础粒子数
    public final float vx, vy, vz;       // 基础速度
    public final float jitter;           // 速度随机扰动幅度
    public final int lifeMin, lifeMax;   // 寿命范围(tick)

    public WeatherTheme(float r, float g, float b, float alpha, float size, float gravity, boolean streak,
                        int density, float vx, float vy, float vz, float jitter, int lifeMin, int lifeMax) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = alpha;
        this.size = size;
        this.gravity = gravity;
        this.streak = streak;
        this.density = density;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.jitter = jitter;
        this.lifeMin = lifeMin;
        this.lifeMax = lifeMax;
    }
}
