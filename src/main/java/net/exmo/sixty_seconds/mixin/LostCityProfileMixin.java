package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.config.LostCityProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强制把 LostCities 的破坏/废墟/植被相关参数调高
 * 这些字段在 LostCityProfile 构造时被各种 init 方法赋默认值，这里在构造末尾覆盖。
 */
@Mixin(LostCityProfile.class)
public abstract class LostCityProfileMixin {

    // ===== 建筑废墟化 =====
    /** 建筑被削顶/塌陷的概率（默认 0.05）。 */
    private static final float FORCED_RUIN_CHANCE = 0.5f;
    /** 废墟破坏层起始高度占楼高的最小比例（默认 0.8）。调低=更多楼从更低处被削。 */
    private static final float FORCED_RUIN_MINLEVEL_PERCENT = 0.5f;
    /** 废墟破坏层起始高度占楼高的最大比例（默认 1.0）。 */
    private static final float FORCED_RUIN_MAXLEVEL_PERCENT = 0.9f;

    // ===== 爆炸碎块外溢 =====
    /** 受损区块碎块溢出到相邻区块的系数（默认 200，越小碎块越多）。 */
    private static final int FORCED_DEBRIS_FACTOR = 100;
    /** 爆炸坑最大高度（默认 90）。 */
    private static final int FORCED_EXPLOSION_MAXHEIGHT = 90;

    // ===== 瓦砾 / 植被覆盖 =====
    /** 泥土覆盖层尺度（默认 3.0，越小层越厚）。 */
    private static final float FORCED_RUBBLE_DIRT_SCALE = 1.0f;
    /** 落叶覆盖层尺度（默认 6.0，越小层越厚）。 */
    private static final float FORCED_RUBBLE_LEAVE_SCALE = 2.0f;
    /** 建筑外墙爬藤概率（默认 0.009）。 */
    private static final float FORCED_VINE_CHANCE = 0.014f;
    /** 楼与街道边界杂叶概率（默认 0.1）。 */
    private static final float FORCED_RANDOM_LEAF_BLOCK_CHANCE = 0.18f;
    /** 杂叶厚度（默认 2）。 */
    private static final int FORCED_RANDOM_LEAF_BLOCK_THICKNESS = 2;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void sixtySecondsForceApocalypticProfile(CallbackInfo ci) {
        LostCityProfile profile = (LostCityProfile) (Object) this;

        profile.RUIN_CHANCE = FORCED_RUIN_CHANCE;
        profile.RUIN_MINLEVEL_PERCENT = FORCED_RUIN_MINLEVEL_PERCENT;
        profile.RUIN_MAXLEVEL_PERCENT = FORCED_RUIN_MAXLEVEL_PERCENT;

        profile.DEBRIS_TO_NEARBYCHUNK_FACTOR = FORCED_DEBRIS_FACTOR;
        profile.EXPLOSION_MAXHEIGHT = FORCED_EXPLOSION_MAXHEIGHT;

        profile.RUBBLELAYER = true;
        profile.RUBBLE_DIRT_SCALE = FORCED_RUBBLE_DIRT_SCALE;
        profile.RUBBLE_LEAVE_SCALE = FORCED_RUBBLE_LEAVE_SCALE;
        profile.VINE_CHANCE = FORCED_VINE_CHANCE;
        profile.CHANCE_OF_RANDOM_LEAFBLOCKS = FORCED_RANDOM_LEAF_BLOCK_CHANCE;
        profile.THICKNESS_OF_RANDOM_LEAFBLOCKS = FORCED_RANDOM_LEAF_BLOCK_THICKNESS;
    }
}
