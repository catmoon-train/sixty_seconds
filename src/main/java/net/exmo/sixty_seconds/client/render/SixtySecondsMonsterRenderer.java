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

    /**
     * 各形态脚深（形态最低点的模型 Y，单位像素），顺序与
     * {@link SixtySecondsMobModelV2#FORM_NAMES} 严格一致。
     * 模型原点在头顶、向下延伸到脚，故直接以原点缩放会让脚下沉约半个身高；
     * 这里用脚深把模型“抬起”半个脚深后再放大，使脚始终踩在地面。
     */
    private static final float[] FEET_DEPTH = {
            27.3F, 26.7F, 28.5F, 26.8F, 21.8F, 28.1F, 26.8F, 30.1F,
            24.7F, 26.9F, 29.1F, 22.3F, 29.0F, 25.4F, 25.0F, 27.2F, 29.0F, 27.7F
    };

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsMobModelV2(SixtySecondsMobModelV2.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public void render(SixtySecondsMonsterEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        int id = Mth.clamp(entity.getVariant().id, 0, FEET_DEPTH.length - 1);
        // 以脚为锚点放大 1.5 倍：先上移半个脚深（抵消“以头顶原点放大”造成的下沉），再缩放
        float lift = 0.5F * FEET_DEPTH[id] / 16.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, lift, 0.0F);
        poseStack.scale(1.5F, 1.5F, 1.5F);
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
