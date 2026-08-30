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
 * 潜水艇盒体模型（代码式几何，无需 JSON）。基线约定与海上载具一致。
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

        // 艇身（长轴沿 Z）
        root.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -4.0F, -16.0F, 14.0F, 8.0F, 32.0F), PartPose.ZERO);
        // 指挥塔（围壳）
        root.addOrReplaceChild("tower", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 5.0F, 8.0F), PartPose.ZERO);
        // 尾舵
        root.addOrReplaceChild("fin", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-1.0F, -3.0F, 12.0F, 2.0F, 6.0F, 8.0F), PartPose.ZERO);
        // 螺旋桨
        root.addOrReplaceChild("prop", CubeListBuilder.create()
                .texOffs(40, 14).addBox(-1.0F, -3.0F, -20.0F, 2.0F, 6.0F, 2.0F), PartPose.ZERO);
        // 舷窗（两侧）
        root.addOrReplaceChild("port", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-8.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.ZERO);
        root.addOrReplaceChild("starboard", CubeListBuilder.create()
                .texOffs(48, 0).addBox(7.0F, -2.0F, -2.0F, 1.0F, 4.0F, 4.0F), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
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
