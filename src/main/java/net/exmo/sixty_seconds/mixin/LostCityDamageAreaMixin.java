package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import mcjty.lostcities.worldgen.lost.DamageArea;
import mcjty.lostcities.worldgen.lost.Explosion;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 强制提升 LostCities 的「爆炸/损伤」密度，从而让城市呈现更多陨石坑/炸弹坑，增强末日破碎感。
 *
 * <p>LostCities 本身就有这套机制（{@code DamageArea.getExplosionAt / getMiniExplosionAt}
 * 以 {@code profile.EXPLOSION_CHANCE / MINI_EXPLOSION_CHANCE} 的概率决定是否在区块内生成爆炸，
 * 随后 {@code LostCityTerrainFeature.breakBlocksForDamageNew} 炸毁方块并生成瓦砾/碎块，
 * 形成弹坑）。默认概率极低（big≈0.002、mini≈0.03），几乎看不到坑。
 *
 * <p>本 mixin 在这两个方法入口以更高的概率（{@link #FORCED_BIG_CHANCE} / {@link #FORCED_MINI_CHANCE}）
 * 强制返回一个爆炸；半径/中心坐标使用与原始代码<b>完全相同</b>的种子公式重建 Random，
 * 保证生成的坑尺寸/位置依旧符合 profile 配置、且确定性可复现。
 * （城市样式 cityStyle 的 explosionChance 默认 null，不会二次过滤我们的强制爆炸。）</p>
 *
 * <p>想削弱/关闭：把下面两个常量调小（或设为 0 即完全不强制，退回 LostCities 默认极低概率）。</p>
 */
@Mixin(DamageArea.class)
public abstract class LostCityDamageAreaMixin {

    /** 大陨石坑（big explosion）的强制生成概率（LostCities 默认 0.002）。 */
    private static final float FORCED_BIG_CHANCE = 0.004f;
    /** 小炸弹坑（mini explosion）的强制生成概率（LostCities 默认 0.03，这里略高一点）。 */
    private static final float FORCED_MINI_CHANCE = 0.05f;
    /** 大陨石坑半径浮动区间（含）：30~65。 */
    private static final int BIG_EXPLOSION_MIN_RADIUS = 30;
    private static final int BIG_EXPLOSION_MAX_RADIUS = 65;
    /** 小炸弹坑半径浮动区间（含）：5~30。 */
    private static final int MINI_EXPLOSION_MIN_RADIUS = 5;
    private static final int MINI_EXPLOSION_MAX_RADIUS = 30;

    @Shadow private long seed;
    @Shadow private LostCityProfile profile;

    @Inject(method = "getExplosionAt", at = @At("HEAD"), cancellable = true)
    private void sixty_seconds_forceBigExplosion(ChunkCoord coord, IDimensionInfo provider, CallbackInfoReturnable<Explosion> cir) {
        if (FORCED_BIG_CHANCE <= 0f) {
            return;
        }
        // 与原始 getExplosionAt 完全一致的随机序列：先 roll 概率，再依次取半径/x/y/z
        Random r = new Random(seed + (long) coord.chunkZ() * 295075153L + (long) coord.chunkX() * 797003437L);
        if (r.nextFloat() < FORCED_BIG_CHANCE) {
            int hi = Math.max(BIG_EXPLOSION_MIN_RADIUS, BIG_EXPLOSION_MAX_RADIUS);
            int lo = Math.min(BIG_EXPLOSION_MIN_RADIUS, BIG_EXPLOSION_MAX_RADIUS);
            int radius = lo + r.nextInt(hi - lo + 1);
            int y = BuildingInfo.getBuildingInfo(coord, provider).cityLevel * 6
                    + profile.EXPLOSION_MINHEIGHT + r.nextInt(profile.EXPLOSION_MAXHEIGHT - profile.EXPLOSION_MINHEIGHT);
            cir.setReturnValue(new Explosion(radius, new BlockPos(
                    (coord.chunkX() << 4) + r.nextInt(16),
                    y,
                    (coord.chunkZ() << 4) + r.nextInt(16))));
            cir.cancel();
        }
    }

    @Inject(method = "getMiniExplosionAt", at = @At("HEAD"), cancellable = true)
    private void sixty_seconds_forceMiniExplosion(ChunkCoord coord, IDimensionInfo provider, CallbackInfoReturnable<Explosion> cir) {
        if (FORCED_MINI_CHANCE <= 0f) {
            return;
        }
        // 与原始 getMiniExplosionAt 完全一致的随机序列（不同乘子）
        Random r = new Random(seed + (long) coord.chunkZ() * 1400305337L + (long) coord.chunkX() * 573259391L);
        if (r.nextFloat() < FORCED_MINI_CHANCE) {
            int hi = Math.max(MINI_EXPLOSION_MIN_RADIUS, MINI_EXPLOSION_MAX_RADIUS);
            int lo = Math.min(MINI_EXPLOSION_MIN_RADIUS, MINI_EXPLOSION_MAX_RADIUS);
            int radius = lo + r.nextInt(hi - lo + 1);
            int y = BuildingInfo.getBuildingInfo(coord, provider).cityLevel * 6
                    + profile.MINI_EXPLOSION_MINHEIGHT + r.nextInt(profile.MINI_EXPLOSION_MAXHEIGHT - profile.MINI_EXPLOSION_MINHEIGHT);
            cir.setReturnValue(new Explosion(radius, new BlockPos(
                    (coord.chunkX() << 4) + r.nextInt(16),
                    y,
                    (coord.chunkZ() << 4) + r.nextInt(16))));
            cir.cancel();
        }
    }
}
