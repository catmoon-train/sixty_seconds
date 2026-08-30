package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;

/**
 * 60s 自研怪 / Boss 的「变体特征附加几何」。
 *
 * <p>僵尸人形本体保持原版比例（走路/攻击动画完全复用），本模型只在其上叠加一层
 * 变体专属部件：犄角、骨冠、兜帽、背甲、脊刺、外露肋骨、气囊、蒸汽喷口、
 * 尾巴、肩甲、护颈、臂刃、利爪、幽翼、眼芒。
 *
 * <p>坐标系与 {@code HumanoidModel} 完全一致（y=0 为颈部/肩线，y=24 为脚底），
 * 三个挂载组会在 {@code SixtySecondsTraitsLayer} 里同步父模型的头/身/双臂旋转。
 *
 * <p>贴图 {@code sixty_seconds_monster_traits.png}（128×128），UV 由
 * {@code tools/gen_monster_textures.py} 直接解析本文件生成。
 */
public class SixtySecondsTraitsModel extends Model {

    /** 特征位标记：一个变体由若干位组合而成。 */
    public static final int HORN = 1;            // 小犄角 ×2
    public static final int HORN_BIG = 1 << 1;   // 巨角 ×2
    public static final int CREST = 1 << 2;      // 头脊/冠
    public static final int HOOD = 1 << 3;       // 兜帽
    public static final int EYE_GLOW = 1 << 4;   // 眼芒
    public static final int JAW = 1 << 5;        // 外露下颌
    public static final int SPINES = 1 << 6;     // 背脊刺列
    public static final int BACK_PLATE = 1 << 7; // 背甲
    public static final int CHEST_PLATE = 1 << 8;// 胸甲
    public static final int SAC = 1 << 9;        // 背部气囊
    public static final int RIBS = 1 << 10;      // 外露肋骨
    public static final int SHOULDER = 1 << 11;  // 肩甲 ×2
    public static final int WINGS = 1 << 12;     // 幽翼 ×2
    public static final int TAIL = 1 << 13;      // 三节尾
    public static final int BLADE = 1 << 14;     // 臂刃 ×2
    public static final int CLAW = 1 << 15;      // 利爪 ×2
    public static final int VENT = 1 << 16;      // 蒸汽喷口 ×2
    public static final int COLLAR = 1 << 17;    // 护颈

    private static final int SPINE_COUNT = 5;
    private static final int RIB_COUNT = 3;

    private final ModelPart root;
    private final ModelPart headMount;
    private final ModelPart bodyMount;
    private final ModelPart armLeftMount;
    private final ModelPart armRightMount;

    private final ModelPart hornL;
    private final ModelPart hornR;
    private final ModelPart hornBigL;
    private final ModelPart hornBigR;
    private final ModelPart crest;
    private final ModelPart hood;
    private final ModelPart eyeGlow;
    private final ModelPart jaw;
    private final ModelPart[] spines = new ModelPart[SPINE_COUNT];
    private final ModelPart backPlate;
    private final ModelPart chestPlate;
    private final ModelPart sac;
    private final ModelPart[] ribsL = new ModelPart[RIB_COUNT];
    private final ModelPart[] ribsR = new ModelPart[RIB_COUNT];
    private final ModelPart shoulderL;
    private final ModelPart shoulderR;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart bladeL;
    private final ModelPart bladeR;
    private final ModelPart clawL;
    private final ModelPart clawR;
    private final ModelPart ventL;
    private final ModelPart ventR;
    private final ModelPart collar;

    public SixtySecondsTraitsModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.headMount = root.getChild("head_mount");
        this.bodyMount = root.getChild("body_mount");
        this.armLeftMount = root.getChild("arm_left_mount");
        this.armRightMount = root.getChild("arm_right_mount");

        this.hornL = headMount.getChild("horn_l");
        this.hornR = headMount.getChild("horn_r");
        this.hornBigL = headMount.getChild("horn_big_l");
        this.hornBigR = headMount.getChild("horn_big_r");
        this.crest = headMount.getChild("crest");
        this.hood = headMount.getChild("hood");
        this.eyeGlow = headMount.getChild("eye_glow");
        this.jaw = headMount.getChild("jaw");

        for (int i = 0; i < SPINE_COUNT; i++) {
            spines[i] = bodyMount.getChild("spine_" + i);
        }
        for (int i = 0; i < RIB_COUNT; i++) {
            ribsL[i] = bodyMount.getChild("rib_l_" + i);
            ribsR[i] = bodyMount.getChild("rib_r_" + i);
        }
        this.backPlate = bodyMount.getChild("back_plate");
        this.chestPlate = bodyMount.getChild("chest_plate");
        this.sac = bodyMount.getChild("sac");
        this.shoulderL = bodyMount.getChild("shoulder_l");
        this.shoulderR = bodyMount.getChild("shoulder_r");
        this.wingL = bodyMount.getChild("wing_l");
        this.wingR = bodyMount.getChild("wing_r");
        this.ventL = bodyMount.getChild("vent_l");
        this.ventR = bodyMount.getChild("vent_r");
        this.collar = bodyMount.getChild("collar");
        this.tail0 = bodyMount.getChild("tail_0");
        this.tail1 = tail0.getChild("tail_1");
        this.tail2 = tail1.getChild("tail_2");

        this.bladeL = armLeftMount.getChild("blade_l");
        this.clawL = armLeftMount.getChild("claw_l");
        this.bladeR = armRightMount.getChild("blade_r");
        this.clawR = armRightMount.getChild("claw_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── 头部挂载（与 HumanoidModel#head 同枢轴）────────────────
        PartDefinition head = root.addOrReplaceChild("head_mount", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -7.0F, -1.0F, -0.35F, 0.0F, 0.45F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -7.0F, -1.0F, -0.35F, 0.0F, -0.45F));
        head.addOrReplaceChild("horn_big_l",
                CubeListBuilder.create().texOffs(10, 0).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offsetAndRotation(3.5F, -6.0F, 0.0F, -0.2F, 0.0F, 0.6F));
        head.addOrReplaceChild("horn_big_r",
                CubeListBuilder.create().texOffs(10, 0).mirror().addBox(-1.5F, -9.0F, -1.5F, 3.0F, 9.0F, 3.0F),
                PartPose.offsetAndRotation(-3.5F, -6.0F, 0.0F, -0.2F, 0.0F, -0.6F));
        head.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -5.0F, -4.0F, 0.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, -8.0F, 0.0F));
        head.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(44, 0).addBox(-5.0F, -7.0F, -5.0F, 10.0F, 7.0F, 10.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        head.addOrReplaceChild("eye_glow",
                CubeListBuilder.create().texOffs(86, 0).addBox(-3.0F, -1.0F, -0.5F, 6.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -4.0F, -4.3F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(86, 6).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 3.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -0.5F, 0.25F, 0.0F, 0.0F));

        // ── 躯干挂载（与 HumanoidModel#body 同枢轴）─────────────────
        PartDefinition body = root.addOrReplaceChild("body_mount", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < SPINE_COUNT; i++) {
            body.addOrReplaceChild("spine_" + i,
                    CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 1.0F + i * 2.4F, 2.6F, 0.5F, 0.0F, 0.0F));
        }
        body.addOrReplaceChild("back_plate",
                CubeListBuilder.create().texOffs(10, 16).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 0.5F, 2.1F));
        body.addOrReplaceChild("chest_plate",
                CubeListBuilder.create().texOffs(36, 16).addBox(-4.5F, 0.0F, -2.0F, 9.0F, 8.0F, 2.0F),
                PartPose.offset(0.0F, 1.5F, -2.1F));
        body.addOrReplaceChild("sac",
                CubeListBuilder.create().texOffs(60, 16).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 5.0F, 5.5F));
        for (int i = 0; i < RIB_COUNT; i++) {
            body.addOrReplaceChild("rib_l_" + i,
                    CubeListBuilder.create().texOffs(94, 16).addBox(0.0F, -3.0F, -2.5F, 0.0F, 6.0F, 5.0F),
                    PartPose.offsetAndRotation(4.3F, 3.0F + i * 3.0F, 0.0F, 0.0F, 0.0F, 0.25F));
            body.addOrReplaceChild("rib_r_" + i,
                    CubeListBuilder.create().texOffs(94, 16).addBox(0.0F, -3.0F, -2.5F, 0.0F, 6.0F, 5.0F),
                    PartPose.offsetAndRotation(-4.3F, 3.0F + i * 3.0F, 0.0F, 0.0F, 0.0F, -0.25F));
        }
        body.addOrReplaceChild("shoulder_l",
                CubeListBuilder.create().texOffs(0, 34).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(5.5F, 1.5F, 0.0F, 0.0F, 0.0F, -0.2F));
        body.addOrReplaceChild("shoulder_r",
                CubeListBuilder.create().texOffs(0, 34).mirror().addBox(-2.5F, -2.0F, -2.5F, 5.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(-5.5F, 1.5F, 0.0F, 0.0F, 0.0F, 0.2F));
        body.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(22, 34).addBox(0.0F, 0.0F, 0.0F, 0.0F, 10.0F, 12.0F),
                PartPose.offsetAndRotation(3.0F, 1.0F, 2.2F, 0.0F, -0.7F, 0.0F));
        body.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(22, 34).addBox(0.0F, 0.0F, 0.0F, 0.0F, 10.0F, 12.0F),
                PartPose.offsetAndRotation(-3.0F, 1.0F, 2.2F, 0.0F, 0.7F, 0.0F));
        body.addOrReplaceChild("vent_l",
                CubeListBuilder.create().texOffs(48, 58).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(3.5F, 1.0F, 3.0F));
        body.addOrReplaceChild("vent_r",
                CubeListBuilder.create().texOffs(48, 58).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(-3.5F, 1.0F, 3.0F));
        body.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(62, 58).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition t0 = body.addOrReplaceChild("tail_0",
                CubeListBuilder.create().texOffs(50, 34).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, 0.6F, 0.0F, 0.0F));
        PartDefinition t1 = t0.addOrReplaceChild("tail_1",
                CubeListBuilder.create().texOffs(70, 34).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 5.5F, 0.25F, 0.0F, 0.0F));
        t1.addOrReplaceChild("tail_2",
                CubeListBuilder.create().texOffs(86, 34).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, 0.25F, 0.0F, 0.0F));

        // ── 双臂挂载（与 HumanoidModel#leftArm / rightArm 同枢轴）───
        PartDefinition armL = root.addOrReplaceChild("arm_left_mount", CubeListBuilder.create(),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        armL.addOrReplaceChild("blade_l",
                CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, 0.0F, -6.0F, 2.0F, 3.0F, 12.0F),
                PartPose.offsetAndRotation(1.5F, 8.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        armL.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(30, 58).addBox(-1.5F, 0.0F, -2.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 10.5F, -1.0F));
        PartDefinition armR = root.addOrReplaceChild("arm_right_mount", CubeListBuilder.create(),
                PartPose.offset(-5.0F, 2.0F, 0.0F));
        armR.addOrReplaceChild("blade_r",
                CubeListBuilder.create().texOffs(0, 58).mirror().addBox(-1.0F, 0.0F, -6.0F, 2.0F, 3.0F, 12.0F),
                PartPose.offsetAndRotation(-1.5F, 8.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        armR.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(30, 58).mirror().addBox(-1.5F, 0.0F, -2.5F, 3.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 10.5F, -1.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    /** 按特征位开关部件可见性。 */
    public void applyTraits(int traits) {
        boolean any = traits != 0;
        headMount.visible = any;
        bodyMount.visible = any;
        armLeftMount.visible = any;
        armRightMount.visible = any;
        if (!any) {
            return;
        }
        hornL.visible = hornR.visible = (traits & HORN) != 0;
        hornBigL.visible = hornBigR.visible = (traits & HORN_BIG) != 0;
        crest.visible = (traits & CREST) != 0;
        hood.visible = (traits & HOOD) != 0;
        eyeGlow.visible = (traits & EYE_GLOW) != 0;
        jaw.visible = (traits & JAW) != 0;

        boolean spine = (traits & SPINES) != 0;
        for (ModelPart p : spines) {
            p.visible = spine;
        }
        boolean rib = (traits & RIBS) != 0;
        for (int i = 0; i < RIB_COUNT; i++) {
            ribsL[i].visible = rib;
            ribsR[i].visible = rib;
        }
        backPlate.visible = (traits & BACK_PLATE) != 0;
        chestPlate.visible = (traits & CHEST_PLATE) != 0;
        sac.visible = (traits & SAC) != 0;
        shoulderL.visible = shoulderR.visible = (traits & SHOULDER) != 0;
        wingL.visible = wingR.visible = (traits & WINGS) != 0;
        ventL.visible = ventR.visible = (traits & VENT) != 0;
        collar.visible = (traits & COLLAR) != 0;
        tail0.visible = (traits & TAIL) != 0;
        bladeL.visible = bladeR.visible = (traits & BLADE) != 0;
        clawL.visible = clawR.visible = (traits & CLAW) != 0;
    }

    /** 同步父级人形骨骼姿态，让附加件跟随头/身/臂一起动。 */
    public void copyPose(ModelPart parentHead, ModelPart parentBody,
                         ModelPart parentLeftArm, ModelPart parentRightArm) {
        headMount.copyFrom(parentHead);
        bodyMount.copyFrom(parentBody);
        armLeftMount.copyFrom(parentLeftArm);
        armRightMount.copyFrom(parentRightArm);
    }

    /** 轻量待机动画：气囊起伏、尾巴摆动、幽翼张合、脊刺抖动。 */
    public void animate(int traits, float ageInTicks, float limbSwing, float limbSwingAmount) {
        if (traits == 0) {
            return;
        }
        float t = ageInTicks * 0.1F;
        if ((traits & SAC) != 0) {
            float pulse = 1.0F + Mth_sin(t * 1.4F) * 0.09F;
            sac.xScale = sac.yScale = sac.zScale = pulse;
        }
        if ((traits & TAIL) != 0) {
            tail0.yRot = Mth_sin(limbSwing * 0.4F) * 0.25F;
            tail1.yRot = Mth_sin(limbSwing * 0.4F - 0.6F) * 0.3F;
            tail2.yRot = Mth_sin(limbSwing * 0.4F - 1.2F) * 0.35F;
            tail0.xRot = 0.6F + Mth_sin(t * 0.7F) * 0.08F;
        }
        if ((traits & WINGS) != 0) {
            float flap = 0.7F + Mth_sin(t * 0.9F) * 0.22F + limbSwingAmount * 0.3F;
            wingL.yRot = -flap;
            wingR.yRot = flap;
        }
        if ((traits & SPINES) != 0) {
            for (int i = 0; i < SPINE_COUNT; i++) {
                spines[i].xRot = 0.5F + Mth_sin(t * 1.2F - i * 0.5F) * 0.10F;
            }
        }
        if ((traits & VENT) != 0) {
            float v = 1.0F + Mth_sin(t * 2.2F) * 0.12F;
            ventL.yScale = ventR.yScale = v;
        }
        if ((traits & EYE_GLOW) != 0) {
            eyeGlow.zScale = 1.0F + Mth_sin(t * 2.6F) * 0.35F;
        }
    }

    private static float Mth_sin(float v) {
        return net.minecraft.util.Mth.sin(v);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    /** 常规小怪变体 → 特征位组合。SHAMBLER..JUGGERNAUT 等旧变体返回 0（保持原版僵尸外观）。 */
    public static int forVariant(SixtySecondsMonsterEntity.Variant v) {
        return switch (v) {
            case CINDERLING -> HORN | EYE_GLOW | CLAW;
            case FROSTLING -> HORN | CREST | EYE_GLOW;
            case HUSKBRUTE -> HORN_BIG | SHOULDER | JAW | BACK_PLATE;
            case RAVENOR -> WINGS | EYE_GLOW | CLAW;
            case WAILER -> HOOD | CREST | EYE_GLOW;
            case BURSTER -> SPINES | JAW | CLAW;
            case GOREHOUND -> SHOULDER | CLAW | JAW;
            case SHADOWMUTE -> WINGS | HOOD | EYE_GLOW;
            case BONELORD -> RIBS | HORN_BIG | CHEST_PLATE | JAW;
            case SPINEWALKER -> SPINES | BACK_PLATE | HORN;
            default -> 0;
        };
    }

    /** 常规 Boss 变体 → 特征位组合（含 5 个原始老变体 + 5 个新变体）。 */
    public static int forBossVariant(SixtySecondsBossEntity.BossVariant v) {
        return switch (v) {
            case RAVAGER -> HORN_BIG | SHOULDER | JAW | BACK_PLATE;
            case COLOSSUS -> HORN_BIG | SHOULDER | CHEST_PLATE | BACK_PLATE | COLLAR;
            case NECROMANCER -> HOOD | HORN | EYE_GLOW | RIBS;
            case PLAGUEBEARER -> HOOD | SAC | VENT | CLAW | EYE_GLOW;
            case SPECTER -> WINGS | HOOD | EYE_GLOW | BLADE | CREST;
            case INFERNO -> HORN_BIG | SHOULDER | CLAW | EYE_GLOW | BACK_PLATE;
            case FROSTBITE -> HORN_BIG | CREST | EYE_GLOW | CHEST_PLATE;
            case SWARMKEEPER -> SAC | SHOULDER | VENT | CLAW;
            case STORMHERALD -> WINGS | HORN_BIG | EYE_GLOW | COLLAR;
            case VOIDWEAVER -> WINGS | HOOD | EYE_GLOW | TAIL | CREST;
            default -> 0;
        };
    }
}
