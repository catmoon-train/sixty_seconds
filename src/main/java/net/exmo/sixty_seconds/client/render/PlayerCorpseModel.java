package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.bridge.entity.PlayerBodyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * 尸体专用的玩家模型：固定为死亡姿势（四肢摊开、头部微侧），不做行走/手臂摆动动画。
 */
public class PlayerCorpseModel extends PlayerModel<PlayerBodyEntity> {

    public PlayerCorpseModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(PlayerBodyEntity entity, float limbSwing, float limbSwingAmount,
                         float ageInTicks, float netHeadYaw, float headPitch) {
        // 尸体姿势：躯干微倾、头部侧歪、双臂大幅下垂摊开、双腿自然分开
        this.head.xRot = 0.25F;
        this.head.yRot = 0.0F;
        this.head.zRot = 0.18F;
        this.hat.copyFrom(this.head);

        this.body.xRot = 0.12F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;

        this.rightArm.xRot = 1.7F;
        this.rightArm.yRot = 0.0F;
        this.rightArm.zRot = -0.2F;
        this.leftArm.xRot = 1.7F;
        this.leftArm.yRot = 0.0F;
        this.leftArm.zRot = 0.2F;

        this.rightLeg.xRot = 0.35F;
        this.rightLeg.yRot = 0.12F;
        this.rightLeg.zRot = 0.0F;
        this.leftLeg.xRot = 0.35F;
        this.leftLeg.yRot = -0.12F;
        this.leftLeg.zRot = 0.0F;

        this.rightArmPose = ArmPose.EMPTY;
        this.leftArmPose = ArmPose.EMPTY;

        // 覆盖层（袖子/裤腿/外套）跟随主肢体，避免装备层错位悬浮
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.jacket.copyFrom(this.body);
    }
}
