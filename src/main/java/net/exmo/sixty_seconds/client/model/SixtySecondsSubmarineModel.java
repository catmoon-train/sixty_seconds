package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSubmarineEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * 潜水艇盒体模型（代码式几何，无需 JSON），128×128 贴图。
 * 长轴沿 Z（前方 +Z），艇身 + 指挥塔 + 潜望镜 + 舱口 + 尾舵 + 双侧鳍 + 底部鳍 +
 * 尾部螺旋桨（桨毂 + 四片桨叶）+ 前部玻璃罩 + 舷窗。贴图由 gen_submarine_texture.py
 * 按本文件盒体 UV 解析驱动生成，改模型后重跑脚本即可同步。
 */
public class SixtySecondsSubmarineModel extends EntityModel<SixtySecondsSubmarineEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(SixtySeconds.id("sixty_seconds_submarine"), "main");

    private final ModelPart root;

    public SixtySecondsSubmarineModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 艇身（长轴沿 Z，前方 +Z）
        root.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -4.0F, -16.0F, 14.0F, 8.0F, 32.0F), PartPose.ZERO);
        // 指挥塔（围壳）
        root.addOrReplaceChild("tower", CubeListBuilder.create()
                .texOffs(0, 44).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 5.0F, 8.0F), PartPose.ZERO);
        // 潜望镜
        root.addOrReplaceChild("periscope", CubeListBuilder.create()
                .texOffs(30, 44).addBox(-1.0F, -12.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.ZERO);
        // 舱口
        root.addOrReplaceChild("hatch", CubeListBuilder.create()
                .texOffs(40, 44).addBox(-1.5F, -9.6F, -1.5F, 3.0F, 1.0F, 3.0F), PartPose.ZERO);
        // 尾舵（竖直方向舵）
        root.addOrReplaceChild("fin", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-1.0F, -3.0F, -18.0F, 2.0F, 6.0F, 8.0F), PartPose.ZERO);
        // 侧鳍（左 / 右）
        root.addOrReplaceChild("fin_l", CubeListBuilder.create()
                .texOffs(24, 60).addBox(-11.0F, -1.0F, 4.0F, 4.0F, 2.0F, 10.0F), PartPose.ZERO);
        root.addOrReplaceChild("fin_r", CubeListBuilder.create()
                .texOffs(56, 60).addBox(7.0F, -1.0F, 4.0F, 4.0F, 2.0F, 10.0F), PartPose.ZERO);
        // 底部鳍
        root.addOrReplaceChild("fin_bottom", CubeListBuilder.create()
                .texOffs(0, 82).addBox(-3.0F, -1.0F, -14.0F, 6.0F, 2.0F, 8.0F), PartPose.ZERO);
        // 螺旋桨桨毂
        root.addOrReplaceChild("prop", CubeListBuilder.create()
                .texOffs(90, 0).addBox(-1.5F, -2.0F, -20.0F, 3.0F, 4.0F, 2.0F), PartPose.ZERO);
        // 螺旋桨桨叶（竖 2 + 横 2）
        root.addOrReplaceChild("blade_vl", CubeListBuilder.create()
                .texOffs(104, 0).addBox(-1.0F, -5.0F, -20.0F, 1.0F, 6.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("blade_vr", CubeListBuilder.create()
                .texOffs(110, 0).addBox(0.0F, -5.0F, -20.0F, 1.0F, 6.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("blade_ht", CubeListBuilder.create()
                .texOffs(104, 9).addBox(-3.0F, -2.5F, -20.0F, 6.0F, 1.0F, 1.0F), PartPose.ZERO);
        root.addOrReplaceChild("blade_hb", CubeListBuilder.create()
                .texOffs(104, 13).addBox(-3.0F, 1.5F, -20.0F, 6.0F, 1.0F, 1.0F), PartPose.ZERO);
        // 前部玻璃罩
        root.addOrReplaceChild("nose_glass", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-6.0F, -2.0F, 16.0F, 12.0F, 5.0F, 2.0F), PartPose.ZERO);
        // 舷窗
        root.addOrReplaceChild("window_l", CubeListBuilder.create()
                .texOffs(30, 96).addBox(-7.5F, -1.0F, 2.0F, 1.0F, 3.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("window_r", CubeListBuilder.create()
                .texOffs(42, 96).addBox(6.5F, -1.0F, 2.0F, 1.0F, 3.0F, 4.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SixtySecondsSubmarineEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        // 潜水艇为载具盒体，无骨骼动画
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
            int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
