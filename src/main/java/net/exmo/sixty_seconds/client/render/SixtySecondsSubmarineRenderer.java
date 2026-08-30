package net.exmo.sixty_seconds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.client.model.SixtySecondsSubmarineModel;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSubmarineEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 潜水艇渲染器：与海上载具一致的姿态约定（yaw 旋转 → 翻面 → 抬基线），并叠加俯仰角。
 */
public class SixtySecondsSubmarineRenderer extends EntityRenderer<SixtySecondsSubmarineEntity> {

    private final SixtySecondsSubmarineModel model;
    private final ResourceLocation texture;
    private static final float MODEL_SCALE = 3.0F;

    public SixtySecondsSubmarineRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SixtySecondsSubmarineModel(
                SixtySecondsSubmarineModel.createLayer().bakeRoot());
        this.texture = SixtySeconds.id("textures/entity/sixty_seconds_submarine.png");
        this.shadowRadius = 1.6F;
    }

    @Override
    public void render(SixtySecondsSubmarineEntity entity, float entityYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        // 俯仰（空格抬头 / 左 Ctrl 低头）
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getPitchAngle() * (180.0F / (float) Math.PI)));

        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsSubmarineEntity entity) {
        return texture;
    }
}
