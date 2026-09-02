package net.exmo.sixty_seconds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.sixty_seconds.client.model.SixtySecondsMobModelV2;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Renderer for the 18 independent ordinary mob forms. */
public class SixtySecondsMonsterRenderer
        extends MobRenderer<SixtySecondsMonsterEntity, SixtySecondsMobModelV2> {

    /** 整体放大系数。 */
    private static final float SCALE = 1.2F;

    /**
     * 模型原点（实体脚底上方）到地面的距离，单位像素。
     * 本项目自研模型的脚底大致落在模型 y≈27.5（未报问题的基线形态
     * shambler=27.0、brute=28.5 取中值），故统一以 27.5 为地面，
     * 把所有形态的脚底都对齐到此处，确保全部踩地。
     * 若整体仍统一悬空/陷入，只需调此一个常量。
     */
    private static final float GROUND_MODEL_Y = 27.0F;

    /** 各形态脚深（静态最低点模型 Y，单位像素），顺序与 FORM_NAMES 严格一致。 */
    private static final float[] FEET_DEPTH = {
            27.0F, 26.8F, 28.5F, 26.8F, 21.6F, 28.1F, 26.8F, 30.1F,
            26.7F, 26.9F, 29.1F, 24.3F, 29.0F, 27.3F, 25.9F, 27.2F, 29.0F, 27.9F
    };

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsMobModelV2(SixtySecondsMobModelV2.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public void render(SixtySecondsMonsterEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int id = Mth.clamp(entity.getVariant().id, 0, FEET_DEPTH.length - 1);
        // 以脚为锚点放大 SCALE 倍：缩放会把脚从 FEET_DEPTH 拉到 SCALE*FEET_DEPTH（更深），
        // 再整体平移使其落回 GROUND_MODEL_Y（地面），从而所有形态脚底一致踩地。
        float lift = (GROUND_MODEL_Y - SCALE * FEET_DEPTH[id]) / 16.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, lift, 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);
        super.render(entity, yaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsMonsterEntity entity) {
        return entity.textureLocation();
    }

    @Override
    protected boolean shouldShowName(SixtySecondsMonsterEntity entity) {
        return false;
    }
}
