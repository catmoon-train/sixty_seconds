package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三代普通 mob 模型：几何逐字取自 E:/mcmodel/java 下 18 份 Blockbench 官方
 * Java 导出（含手动调整），由 gen_java_from_export.py 合并生成。
 *
 * <p>在导出结构基础上做了三类保持静止姿态不变的整理：① 四肢枢轴归一到关节
 * （髋=腿盒顶部中心，肩=手臂旋转枢轴），修正"绕脚旋转"；② 把并进 _l 组的 _r
 * 侧肢体与爪/拳拆到独立部件，行走/攻击时独立刚性摆动；③ 尾巴/翼等 body 子件
 * 通过父级解析。动画：行走、攻击（attackTime）、头部朝向、翼扇、尾摆与各形态
 * 特征动作见 {@link #setupAnim}。</p>
 */
public class SixtySecondsMobModelV3 extends EntityModel<SixtySecondsMonsterEntity> {
    private static final String[] FORM_NAMES = {
            "shambler", "runner", "brute", "spitter", "stalker", "howler", "bloater", "juggernaut", "cinderling", "frostling", "huskbrute", "ravenor", "wailer", "burster", "gorehound", "shadowmute", "bonelord", "spinewalker"
    };

    /** 可动部件在动画中的角色。 */
    private enum Role { LEG, ARM, HEAD, WING, TAIL }

    private static final Map<String, Role> LIMB_ROLE = new HashMap<>();
    private static final Map<String, float[]> LIMB_BASE = new HashMap<>();
    private static final Map<String, List<String>> FORM_LIMBS = new HashMap<>();
    private static String currentForm = "";

    private final ModelPart root;
    private final ModelPart[] forms = new ModelPart[FORM_NAMES.length];

    public SixtySecondsMobModelV3(ModelPart root) {
        this.root = root;
        for (int i = 0; i < FORM_NAMES.length; i++) {
            forms[i] = root.getChild(FORM_NAMES[i]);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addShambler(root);
        addRunner(root);
        addBrute(root);
        addSpitter(root);
        addStalker(root);
        addHowler(root);
        addBloater(root);
        addJuggernaut(root);
        addCinderling(root);
        addFrostling(root);
        addHuskbrute(root);
        addRavenor(root);
        addWailer(root);
        addBurster(root);
        addGorehound(root);
        addShadowmute(root);
        addBonelord(root);
        addSpinewalker(root);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static PartDefinition form(PartDefinition root, String name) {
        currentForm = name;
        return root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
    }

    private static void limbRole(String name, Role role, float rx, float ry, float rz) {
        LIMB_ROLE.put(currentForm + ":" + name, role);
        LIMB_BASE.put(currentForm + ":" + name, new float[]{rx, ry, rz});
        FORM_LIMBS.computeIfAbsent(currentForm, k -> new ArrayList<>()).add(name);
    }

    private static void addShambler(PartDefinition root) {
        PartDefinition p = form(root, "shambler");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(18, 22).addBox(-2.0F, -7.0F, -3.0F, 3.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(38, 2).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -10F, 0F, -0.0008F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(66, 2).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.0024F, 0F, 0.0021F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(102, 2).addBox(-1.0F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0076F, 0F, -0.0043F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, 0F, -2F, 3F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(2, 22).addBox(-2.0F, -1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, 3F, 0F, -0.0075F, -0.0013F, 0.0015F));
        limbRole("arm_r", Role.ARM, -0.0075F, -0.0013F, 0.0015F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(20, 2).addBox(-1.5F, 0F, -2F, 3F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addRunner(PartDefinition root) {
        PartDefinition p = form(root, "runner");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 11F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(34, 2).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -11F, 0F, -0.0003F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, -1F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(56, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 1F, 0F, -0.0043F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4F, 1F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(84, 2).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0116F, 0F, 0F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, 0F, -1.5F, 3F, 14F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2F, 10F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(96, 2).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4F, 1F, 0F, -0.0116F, 0F, 0F));
        limbRole("arm_r", Role.ARM, -0.0116F, 0F, 0F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(18, 2).addBox(-1.5F, 0F, -1.5F, 3F, 14F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2F, 10F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addBrute(PartDefinition root) {
        PartDefinition p = form(root, "brute");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(50, 2).addBox(-6.0F, -12.0F, -3.0F, 12.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(42, 24).addBox(3.0F, -14.0F, -3.5F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(68, 24).addBox(-7.0F, -14.0F, -3.5F, 4.0F, 3.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(90, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(8F, 5F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(2, 24).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0037F, 0F, -0.0018F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(22, 24).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8F, 5F, 0F, -0.0037F, 0F, 0.0018F));
        limbRole("arm_r", Role.ARM, -0.0037F, 0F, 0.0018F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(26, 2).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addSpitter(PartDefinition root) {
        PartDefinition p = form(root, "spitter");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(70, 2).addBox(-3.0F, -7.0F, -4.0F, 6.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 22).addBox(-2.0F, 1.0F, -5.5F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(90, 2).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, 0F, 0.0024F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(5.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(18, 22).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.003F, 0F, -0.0024F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(32, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, 3F, 0F, -0.003F, 0F, 0.0024F));
        limbRole("arm_r", Role.ARM, -0.003F, 0F, 0.0024F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addStalker(PartDefinition root) {
        PartDefinition p = form(root, "stalker");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -2F, 0F, -0.0091F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 14F, -4F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -2F, 2F, 0.0061F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(5F, 11F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("claw_l_r1", CubeListBuilder.create().texOffs(18, 19).addBox(6.0F, -4.0F, -2.5F, 1.0F, 4.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4F, 5F, -3F, 0.0495F, 0F, 0F));
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(100, 2).addBox(-0.5F, -1.0F, -2.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.0046F, 0F, -0.0024F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 15F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(2, 19).addBox(-2.5F, -1.0F, -2.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5F, 11F, 0F, 0.0046F, 0F, 0.0024F));
        limbRole("arm_r", Role.ARM, 0.0046F, 0F, 0.0024F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 9F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 15F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
        pd_arm_r.addOrReplaceChild("claw_r", CubeListBuilder.create().texOffs(28, 19).addBox(-7.122F, -3.8042F, -2.5615F, 1.1199F, 4.0608F, 1.5981F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3F, 1F, 1F, 0F, 0F, 0F));
    }

    private static void addHowler(PartDefinition root) {
        PartDefinition p = form(root, "howler");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, 0F, 0.0012F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(5.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(106, 2).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, -0.0411F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(2, 23).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, 3F, 0F, 0F, 0F, 0.0411F));
        limbRole("arm_r", Role.ARM, 0F, 0F, 0.0411F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addBloater(PartDefinition root) {
        PartDefinition p = form(root, "bloater");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-6.0F, -8.0F, -4.0F, 12.0F, 10.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(46, 2).addBox(-5.0F, -5.0F, -5.5F, 10.0F, 7.0F, 2.5F, new CubeDeformation(0.0F))
		.texOffs(74, 2).addBox(-4.0F, -11.0F, -2.0F, 8.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 16F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(104, 2).addBox(-2.5F, -5.0F, -4.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 8F, -2F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 24).addBox(-2.25F, 0F, -2F, 4.5F, 6F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.25F, 18F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 24).addBox(-2.25F, 0F, -2F, 4.5F, 6F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.25F, 18F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addJuggernaut(PartDefinition root) {
        PartDefinition p = form(root, "juggernaut");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(58, 2).addBox(-6.0F, -14.0F, -3.5F, 12.0F, 14.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(2, 46).addBox(6.0F, -15.0F, -4.0F, 5.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(32, 46).addBox(-11.0F, -15.0F, -4.0F, 5.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 27).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -1F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(34, 27).addBox(-2.5F, 0F, -2.5F, 5F, 6F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.5F, 10F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(82, 27).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0024F, 0F, -0.0012F));
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(58, 27).addBox(-2.5F, 0F, -2.5F, 5F, 6F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.5F, 10F, 0F, 0F, 0F, 0F));
        limbRole("arm_r", Role.ARM, 0F, 0F, 0F);
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-3F, 0F, -3F, 6F, 11F, 6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(30, 2).addBox(-3F, 0F, -3F, 6F, 11F, 6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
        pd_arm_r.addOrReplaceChild("arm_r_r1", CubeListBuilder.create().texOffs(100, 27).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 11.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -11F, 0F, -0.0024F, 0F, 0.0012F));
    }

    private static void addCinderling(PartDefinition root) {
        PartDefinition p = form(root, "cinderling");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(34, 2).addBox(-3.5F, 0.0F, -1.5F, 7.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -10F, 0F, -0.0008F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 4F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(58, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.0018F, 0F, 0.0015F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4F, 4F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(86, 2).addBox(-0.5F, -1.0F, -1.0F, 2.5F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0061F, 0F, -0.003F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(98, 2).addBox(-2.0F, -1.0F, -1.0F, 2.5F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4F, 4F, 0F, -0.0061F, 0F, 0.003F));
        limbRole("arm_r", Role.ARM, -0.0061F, 0F, 0.003F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(18, 2).addBox(-1.5F, 0F, -1.5F, 3F, 10F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addFrostling(PartDefinition root) {
        PartDefinition p = form(root, "frostling");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("shard3_r1", CubeListBuilder.create().texOffs(24, 21).addBox(-0.8F, -4.0F, -0.5F, 1.5F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.7F, -8F, 2.5F, -0.0055F, 0F, 0F));
        pd_body.addOrReplaceChild("shard2_r1", CubeListBuilder.create().texOffs(14, 21).addBox(-0.7F, -4.0F, -0.5F, 1.5F, 4.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.7F, -8F, 2.5F, -0.0061F, 0F, 0F));
        pd_body.addOrReplaceChild("shard1_r1", CubeListBuilder.create().texOffs(2, 21).addBox(-1.0F, -5.0F, -0.5F, 2.0F, 5.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, -8F, 2.5F, -0.0043F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 1F, 0F, -0.0018F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4F, 4F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(102, 2).addBox(-0.5F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0043F, 0F, -0.0024F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 11F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(116, 2).addBox(-2.5F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4F, 4F, 0F, -0.0043F, 0F, 0.0024F));
        limbRole("arm_r", Role.ARM, -0.0043F, 0F, 0.0024F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 11F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addHuskbrute(PartDefinition root) {
        PartDefinition p = form(root, "huskbrute");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(50, 2).addBox(-6.0F, -12.0F, -3.0F, 12.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(42, 24).addBox(-1.0F, -14.0F, 3.0F, 2.0F, 2.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(54, 24).addBox(2.0F, -12.0F, 3.0F, 1.5F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(64, 24).addBox(-3.5F, -13.0F, 3.0F, 1.5F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 14F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(90, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(8F, 4F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(2, 24).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0037F, 0F, -0.0018F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(22, 24).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8F, 4F, 0F, -0.0037F, 0F, 0.0018F));
        limbRole("arm_r", Role.ARM, -0.0037F, 0F, 0.0018F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(26, 2).addBox(-2.5F, 0F, -2.5F, 5F, 10F, 5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 14F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addRavenor(PartDefinition root) {
        PartDefinition p = form(root, "ravenor");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition pd_wing_r_r1 = pd_body.addOrReplaceChild("wing_r_r1", CubeListBuilder.create().texOffs(22, 20).addBox(-5.0F, -1.0F, -1.0F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3F, -7F, -1F, 0F, 0.0043F, -0.0079F));
        limbRole("wing_r_r1", Role.WING, 0F, 0.0043F, -0.0079F);
        PartDefinition pd_wing_l_r1 = pd_body.addOrReplaceChild("wing_l_r1", CubeListBuilder.create().texOffs(2, 20).addBox(0.0F, -1.0F, -1.0F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3F, -7F, 0F, 0F, 0.0043F, 0.0079F));
        limbRole("wing_l_r1", Role.WING, 0F, 0.0043F, 0.0079F);
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(34, 2).addBox(-3.0F, 0.0F, -2.0F, 6.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -10F, 0F, 0.0002F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(58, 2).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.0015F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(84, 2).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.0005F, 0F, -0.0018F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, 0F, -1.5F, 3F, 11F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(96, 2).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5F, 3F, 0F, 0.0012F, 0F, 0.0018F));
        limbRole("arm_r", Role.ARM, 0.0012F, 0F, 0.0018F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(18, 2).addBox(-1.5F, 0F, -1.5F, 3F, 11F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addWailer(PartDefinition root) {
        PartDefinition p = form(root, "wailer");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(66, 2).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(94, 2).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, 0F, 0.0018F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(2, 22).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0018F, 0.0001F, -0.002F));
        p.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(2, 2).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 10.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(34, 2).addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 21F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(16, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 3F, 0F, -0.0018F, 0F, 0.0012F));
        limbRole("arm_r", Role.ARM, -0.0018F, 0F, 0.0012F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(50, 2).addBox(-1.5F, 0F, -1.5F, 3F, 3F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 21F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addBurster(PartDefinition root) {
        PartDefinition p = form(root, "burster");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, -10.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(70, 2).addBox(-3.0F, -5.0F, -3.5F, 3.0F, 5.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(84, 2).addBox(0.0F, -9.0F, 2.0F, 3.0F, 5.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 2F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(98, 2).addBox(-2.5F, -5.0F, -3.0F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 1F, 0F, -0.003F, 0F, 0.0012F));
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create(), PartPose.offsetAndRotation(-4.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_r", Role.ARM, 0F, 0F, 0F);
        pd_arm_r.addOrReplaceChild("arm2_l_r1", CubeListBuilder.create().texOffs(26, 21).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1F, 1F, 0F, 0.003F, 0F, 0.0024F));
        pd_arm_r.addOrReplaceChild("arm2_r_r1", CubeListBuilder.create().texOffs(40, 21).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 11.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10F, 1F, 0F, -0.003F, 0F, -0.0024F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 11F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 11F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 13F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addGorehound(PartDefinition root) {
        PartDefinition p = form(root, "gorehound");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(2, 2).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(40, 2).addBox(-2.5F, -4.0F, -8.0F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition pd_tail_r1 = pd_body.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(2, 23).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -2F, 5F, 0.0073F, 0F, 0F));
        limbRole("tail_r1", Role.TAIL, 0.0073F, 0F, 0F);
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(60, 2).addBox(-2.5F, -4.0F, -6.0F, 5.0F, 6.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(84, 2).addBox(-1.5F, -2.0F, -9.0F, 3.0F, 3.5F, 3.5F, new CubeDeformation(0.0F))
		.texOffs(102, 2).addBox(0.5F, -7.0F, -5.0F, 2.0F, 3.0F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(114, 2).addBox(-2.5F, -7.0F, -5.0F, 2.0F, 3.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, -6F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        PartDefinition pd_leg_fl = p.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(18, 23).addBox(-1.25F, 0F, -1.25F, 2.5F, 9F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.75F, 15F, -5.25F, 0F, 0F, 0F));
        limbRole("leg_fl", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_fr = p.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(30, 23).addBox(-1.25F, 0F, -1.25F, 2.5F, 9F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.75F, 15F, -5.25F, 0F, 0F, 0F));
        limbRole("leg_fr", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_bl = p.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(42, 23).addBox(-1.25F, 0F, -1.25F, 2.5F, 9F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.75F, 15F, 3.75F, 0F, 0F, 0F));
        limbRole("leg_bl", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_leg_br = p.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(54, 23).addBox(-1.25F, 0F, -1.25F, 2.5F, 9F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.75F, 15F, 3.75F, 0F, 0F, 0F));
        limbRole("leg_br", Role.LEG, 0F, 0F, 0F);
    }

    private static void addShadowmute(PartDefinition root) {
        PartDefinition p = form(root, "shadowmute");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(64, 2).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -10F, 0F, -0.0002F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(92, 2).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 1F, 0F, -0.0006F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 3F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(2, 22).addBox(-1.0F, -1.0F, -1.0F, 3.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0055F, 0F, -0.0018F));
        p.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(2, 2).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 11.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(32, 2).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 22F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(16, 22).addBox(-2.0F, -1.0F, -1.0F, 3.0F, 13.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 3F, 0F, -0.0055F, 0F, 0.0018F));
        limbRole("arm_r", Role.ARM, -0.0055F, 0F, 0.0018F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(48, 2).addBox(-1.5F, 0F, -1.5F, 3F, 2F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 22F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addBonelord(PartDefinition root) {
        PartDefinition p = form(root, "bonelord");
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 2).addBox(-3.5F, -1.0F, -2.0F, 7.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(60, 2).addBox(-4.0F, -4.0F, -2.5F, 8.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(90, 2).addBox(-4.0F, -7.0F, -2.5F, 8.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(2, 21).addBox(-3.5F, -10.0F, -2.0F, 7.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(28, 21).addBox(-0.5F, -13.0F, -1.0F, 1.0F, 12.0F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create().texOffs(38, 21).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("horn_r_r1", CubeListBuilder.create().texOffs(26, 39).addBox(-1.0F, -1.5F, -1.0F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, -6F, 0F, 0F, 0F, -0.0073F));
        pd_head.addOrReplaceChild("horn_l_r1", CubeListBuilder.create().texOffs(14, 39).addBox(-0.5F, -1.5F, -1.0F, 1.5F, 2.5F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, -6F, 0F, 0F, 0F, 0.0073F));
        pd_head.addOrReplaceChild("jaw_r1", CubeListBuilder.create().texOffs(70, 21).addBox(-2.5F, 0.0F, -1.0F, 5.0F, 1.5F, 1.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, -2F, 0.0024F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(3.5F, 2F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(88, 21).addBox(-0.5F, -1.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, 0.0024F, 0.0006F, -0.0057F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-1.5F, 0F, -1.5F, 3F, 12F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(100, 21).addBox(-1.5F, -1.0F, -1.0F, 2.0F, 12.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 2F, 0F, -0.0024F, 0.0004F, 0.0042F));
        limbRole("arm_r", Role.ARM, -0.0024F, 0.0004F, 0.0042F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(18, 2).addBox(-1.5F, 0F, -1.5F, 3F, 12F, 3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    private static void addSpinewalker(PartDefinition root) {
        PartDefinition p = form(root, "spinewalker");
        PartDefinition pd_body = p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(46, 23).addBox(-0.5F, -5.0F, 2.2F, 1.0F, 3.0F, 1.4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 12F, 0F, 0F, 0F, 0F));
        pd_body.addOrReplaceChild("sp2_r1", CubeListBuilder.create().texOffs(36, 23).addBox(-0.5F, -5.0F, -0.8F, 1.0F, 4.0F, 1.7F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -4F, 3.3F, 0.003F, 0F, 0F));
        pd_body.addOrReplaceChild("sp1_r1", CubeListBuilder.create().texOffs(26, 23).addBox(-0.5F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -9F, 3F, 0.0061F, 0F, 0F));
        pd_body.addOrReplaceChild("torso_r1", CubeListBuilder.create().texOffs(42, 2).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, -10F, 0F, -0.0003F, 0F, 0F));
        PartDefinition pd_head = p.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        limbRole("head", Role.HEAD, 0F, 0F, 0F);
        pd_head.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(70, 2).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 5.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 2F, -1F, -0.0055F, 0F, 0F));
        PartDefinition pd_arm_l = p.addOrReplaceChild("arm_l", CubeListBuilder.create(), PartPose.offsetAndRotation(4.5F, 2F, 0F, 0F, 0F, 0F));
        limbRole("arm_l", Role.ARM, 0F, 0F, 0F);
        pd_arm_l.addOrReplaceChild("arm_l_r1", CubeListBuilder.create().texOffs(100, 2).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 15.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0F, 0F, 0F, -0.003F, 0F, -0.0024F));
        PartDefinition pd_leg_l = p.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(2, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_l", Role.LEG, 0F, 0F, 0F);
        PartDefinition pd_arm_r = p.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(114, 2).addBox(-2.0F, 0.0F, -1.0F, 3.0F, 15.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.5F, 2F, 0F, 0.003F, 0F, 0.0024F));
        limbRole("arm_r", Role.ARM, 0.003F, 0F, 0.0024F);
        PartDefinition pd_leg_r = p.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(22, 2).addBox(-2F, 0F, -2F, 4F, 12F, 4F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5F, 12F, 0F, 0F, 0F, 0F));
        limbRole("leg_r", Role.LEG, 0F, 0F, 0F);
    }

    @Override
    public void setupAnim(SixtySecondsMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart form : forms) {
            form.visible = false;
        }
        int id = Mth.clamp(entity.getVariant().id, 0, forms.length - 1);
        ModelPart active = forms[id];
        active.visible = true;
        String form = FORM_NAMES[id];

        for (String name : FORM_LIMBS.getOrDefault(form, List.of())) {
            float[] base = LIMB_BASE.get(form + ":" + name);
            if (base == null) continue;
            ModelPart part = resolve(active, name);
            if (part == null) continue;
            part.xRot = base[0]; part.yRot = base[1]; part.zRot = base[2];
        }

        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float swing = Mth.cos(limbSwing * 0.6662F);
        float swingOpp = Mth.cos(limbSwing * 0.6662F + (float) Math.PI);

        float punch = 0.0F;
        if (this.attackTime > 0.0F) {
            float t = 1.0F - this.attackTime;
            punch = Mth.sin((1.0F - t * t * t) * (float) Math.PI);
        }

        for (String name : FORM_LIMBS.getOrDefault(form, List.of())) {
            Role role = LIMB_ROLE.get(form + ":" + name);
            if (role == null) continue;
            ModelPart part = resolve(active, name);
            if (part == null) continue;
            String lst = name.replaceAll("_r\\d+$", "");
            boolean left = lst.endsWith("_l");
            float own = left ? swing : swingOpp;
            float opp = left ? swingOpp : swing;
            boolean heavy = form.equals("juggernaut") || form.equals("bonelord") || form.equals("huskbrute");
            float legAmp = heavy ? 0.45F : 0.8F;
            float armAmp = heavy ? 0.3F : 0.55F;
            switch (role) {
                case LEG -> part.xRot += own * legAmp * walk;
                case ARM -> {
                    part.xRot += opp * armAmp * walk - punch * (heavy ? 1.7F : 1.3F);
                    part.zRot += punch * (left ? 0.25F : -0.25F);
                }
                case HEAD -> {
                    part.yRot += netHeadYaw * ((float) Math.PI / 180F);
                    part.xRot += headPitch * ((float) Math.PI / 180F);
                }
                case WING -> part.zRot += Mth.sin(ageInTicks * 0.55F)
                        * (0.12F + walk * 0.6F) * (left ? 1.0F : -1.0F);
                case TAIL -> part.yRot += Mth.sin(ageInTicks * 0.2F + id) * 0.2F
                        + swing * 0.25F * walk;
            }
        }

        // ---- 形态特征动作 ----
        switch (form) {
            case "howler", "wailer" -> {
                ModelPart head = resolve(active, "head");
                if (head != null) {
                    head.zRot += Mth.sin(ageInTicks * 0.12F) * 0.06F;
                    head.xRot -= punch * 0.35F;
                }
            }
            case "bloater", "burster" ->
                active.y = -Math.abs(Mth.sin(ageInTicks * 0.09F)) * 0.6F;
            case "stalker", "shadowmute" -> {
                active.y = -Math.abs(Mth.sin(limbSwing * 0.6662F)) * 0.5F * walk;
                ModelPart head = resolve(active, "head");
                if (head != null) head.zRot += Mth.sin(ageInTicks * 0.17F) * 0.08F;
            }
            case "cinderling" -> {
                ModelPart al = resolve(active, "arm_l");
                ModelPart ar = resolve(active, "arm_r");
                float fl = Mth.sin(ageInTicks * 0.45F) * 0.05F;
                if (al != null) al.zRot += fl;
                if (ar != null) ar.zRot -= fl;
            }
            case "gorehound" -> {
                ModelPart head = resolve(active, "head");
                if (head != null) {
                    head.xRot += Mth.sin(limbSwing * 0.6662F) * 0.06F * walk - punch * 0.55F;
                }
            }
            case "spitter" -> {
                ModelPart head = resolve(active, "head");
                if (head != null) head.xRot -= punch * 0.6F;
            }
            case "runner" -> {
                ModelPart head = find(active, "head");
                if (head != null) head.xRot += 0.08F;
            }
            case "bonelord" -> {
                ModelPart head = resolve(active, "head");
                ModelPart jaw = head == null ? null : findPrefix(head, "jaw");
                if (jaw != null) jaw.xRot -= punch * 0.55F;
            }
            case "ravenor" ->
                active.y = -Math.abs(Mth.sin(ageInTicks * 0.07F)) * 0.7F;
            default -> { }
        }
    }

    /** 直接子级优先，其次 body/head 的子级（尾、翼等）。 */
    private static ModelPart resolve(ModelPart form, String name) {
        ModelPart part = find(form, name);
        if (part != null) return part;
        for (String hub : new String[]{"body", "head"}) {
            ModelPart hubPart = find(form, hub);
            if (hubPart != null) {
                part = find(hubPart, name);
                if (part != null) return part;
            }
        }
        return null;
    }

    private static ModelPart find(ModelPart form, String name) {
        try {
            return form.getChild(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static ModelPart findPrefix(ModelPart form, String prefix) {
        for (String suffix : new String[]{"", "_r1", "_r2", "_r3"}) {
            try {
                return form.getChild(prefix + suffix);
            } catch (Exception ignored) { }
        }
        return null;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
