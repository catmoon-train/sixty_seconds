package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsBossModel;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒 Boss 渲染器。
 *
 * <p>所有 Boss 变体（含尸潮领主 RAVAGER 的人形建模 {@code humanoid}）一律使用自定义建模
 * {@link SixtySecondsBossModel}，贴图沿用各变体自有纹理
 * {@link SixtySecondsBossEntity#textureLocation()}（尸潮领主用 sixty_seconds_boss_ravager.png）。
 * <p>模型按实体制变体在 {@link #setupAnim} 中切换可见性；渲染器本身只注册给
 * {@code SIXTY_SECONDS_BOSS} 一种实体，通过变体区分建模。
 */
public class SixtySecondsBossRenderer
        extends MobRenderer<SixtySecondsBossEntity, EntityModel<SixtySecondsBossEntity>> {

    /** 自定义建模（含所有 Boss 变体的建模，含尸潮领主 humanoid）。 */
    private final SixtySecondsBossModel customModel;

    public SixtySecondsBossRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsBossModel(SixtySecondsBossModel.createLayer().bakeRoot()), 0.7F);
        this.customModel = (SixtySecondsBossModel) super.getModel(); // 取构造时传入的自定义模型
    }

    @Override
    public void render(SixtySecondsBossEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public EntityModel<SixtySecondsBossEntity> getModel() {
        // 所有变体（含尸潮领主 RAVAGER）一律使用自定义建模
        return customModel;
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsBossEntity entity) {
        // 贴图沿用各 Boss 变体自有纹理（尸潮领主用 sixty_seconds_boss_ravager.png，非原版僵尸图）
        return entity.textureLocation();
    }

    @Override
    protected boolean shouldShowName(SixtySecondsBossEntity entity) {
        return false;
    }
}
