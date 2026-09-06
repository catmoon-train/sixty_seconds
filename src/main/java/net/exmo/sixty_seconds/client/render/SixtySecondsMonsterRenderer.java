package net.exmo.sixty_seconds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.sixty_seconds.client.model.SixtySecondsMobModelV3;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer for the 18 independent ordinary mob forms.
 *
 * <p>使用 Blockbench 重制的第三代模型（{@link SixtySecondsMobModelV3}）。
 * 全部形态的脚底均建模在模型 y=24 基线上（= 实体地面），因此整体缩放
 * {@link #SCALE} 直接以实体原点为锚，无需再按形态抬升。</p>
 */
public class SixtySecondsMonsterRenderer
        extends MobRenderer<SixtySecondsMonsterEntity, SixtySecondsMobModelV3> {

    /** 整体放大系数。 */
    private static final float SCALE = 1.15F;

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsMobModelV3(SixtySecondsMobModelV3.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public void render(SixtySecondsMonsterEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
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
