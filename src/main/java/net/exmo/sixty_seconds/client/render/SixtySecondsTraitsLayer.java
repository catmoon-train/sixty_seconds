package net.exmo.sixty_seconds.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.client.model.SixtySecondsTraitsModel;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.monster.Zombie;

/**
 * 在僵尸人形本体之上叠加「变体特征几何」（犄角 / 骨冠 / 兜帽 / 背甲 / 脊刺 /
 * 肋骨 / 气囊 / 喷口 / 尾巴 / 肩甲 / 护颈 / 臂刃 / 利爪 / 幽翼 / 眼芒）。
 *
 * <p>挂载组姿态在 {@link SixtySecondsTraitsModel#copyPose} 中同步父模型（僵尸）的
 * 头 / 身 / 双臂，使附加部件随本体一起摆动；{@link SixtySecondsTraitsModel#animate}
 * 再叠加独立的轻量动画（气囊鼓动、尾摆、翼张、脊刺呼吸）。
 */
public class SixtySecondsTraitsLayer extends RenderLayer<Zombie, ZombieModel<Zombie>> {

    private static final net.minecraft.resources.ResourceLocation TRAITS_TEX =
            SixtySeconds.id("textures/entity/sixty_seconds_monster_traits.png");

    private final SixtySecondsTraitsModel traits;

    public SixtySecondsTraitsLayer(RenderLayerParent<Zombie, ZombieModel<Zombie>> parent,
                                   SixtySecondsTraitsModel traits) {
        super(parent);
        this.traits = traits;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       Zombie entity, float limbSwing, float limbSwingAmount, float partialTicks,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof SixtySecondsMonsterEntity m)) return;

        int bits = (entity instanceof SixtySecondsBossEntity b)
                ? SixtySecondsTraitsModel.forBossVariant(b.getBossVariant())
                : SixtySecondsTraitsModel.forVariant(m.getVariant());
        if (bits == 0) return;

        traits.applyTraits(bits);

        ZombieModel<Zombie> parent = this.getParentModel();
        traits.copyPose(parent.head, parent.body, parent.leftArm, parent.rightArm);
        traits.animate(bits, ageInTicks, limbSwing, limbSwingAmount);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TRAITS_TEX));
        traits.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }
}
