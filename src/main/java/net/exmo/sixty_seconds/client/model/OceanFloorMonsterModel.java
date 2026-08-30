package net.exmo.sixty_seconds.client.model;

import net.exmo.sixty_seconds.entity.OceanFloorMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 海底小型怪物占位模型（复用鲨鱼盒体几何，专属模型/贴图待美术产出后替换）。
 * 本类仅为占位，与 {@link OceanSharkModel} 几何一致，但泛型绑定到 {@link OceanFloorMonsterEntity}
 * 以满足 {@code MobRenderer<OceanFloorMonsterEntity, M>} 的类型约束（M extends EntityModel<T>）。
 */
public class OceanFloorMonsterModel extends EntityModel<OceanFloorMonsterEntity> {

    private final ModelPart root;

    public OceanFloorMonsterModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        PartDefinition bodyDef = part.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -11.0F, 8.0F, 8.0F, 22.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition headDef = bodyDef.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-3.0F, -3.0F, -8.0F, 6.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, -11.0F));
        headDef.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(30, 32)
                        .addBox(-2.0F, -2.0F, -12.0F, 4.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        headDef.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(48, 32)
                        .addBox(-3.0F, 0.0F, -8.0F, 6.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, -0.12F, 0.0F, 0.0F));
        bodyDef.addOrReplaceChild("dorsal_fin",
                CubeListBuilder.create().texOffs(0, 50)
                        .addBox(-0.5F, -8.0F, -2.0F, 1.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, -4.0F, -2.0F));
        PartDefinition tailDef = bodyDef.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(16, 50)
                        .addBox(-2.0F, -3.0F, 0.0F, 4.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 11.0F));
        PartDefinition finDef = tailDef.addOrReplaceChild("tail_fin",
                CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 7.0F));
        finDef.addOrReplaceChild("caudal_upper",
                CubeListBuilder.create().texOffs(42, 50)
                        .addBox(-0.5F, -9.0F, 0.0F, 1.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5F, 0.0F, 0.0F));
        finDef.addOrReplaceChild("caudal_lower",
                CubeListBuilder.create().texOffs(50, 50)
                        .addBox(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4F, 0.0F, 0.0F));
        bodyDef.addOrReplaceChild("left_pectoral",
                CubeListBuilder.create().texOffs(60, 50)
                        .addBox(0.0F, -0.5F, -1.5F, 7.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(4.0F, 2.0F, -4.0F, 0.0F, 0.0F, 0.45F));
        bodyDef.addOrReplaceChild("right_pectoral",
                CubeListBuilder.create().texOffs(60, 58)
                        .addBox(-7.0F, -0.5F, -1.5F, 7.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-4.0F, 2.0F, -4.0F, 0.0F, 0.0F, -0.45F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(OceanFloorMonsterEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        // 占位：静态，无骨骼动画
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight,
            int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
