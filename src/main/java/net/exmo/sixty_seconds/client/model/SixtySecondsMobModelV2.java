package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
 * 第二代普通 mob 模型。
 *
 * <p>每个变体拥有自己的根节点和自己的几何结构。这里不再保留旧模型中的
 * humanoid 兼容分支，也不通过缩放同一副人形骨架来伪装不同生物。</p>
 *
 * <p><b>动画</b>：需要摆动的部件（腿/臂/翼/尾）用 {@link #limb} 注册，它会把部件
 * 原点迁移到关节处并记录角色；{@link #setupAnim} 按角色施加行走摆动、攻击挥击、
 * 翼扇动与尾部摆动。静止姿态由 {@link #limb} 的逆旋转补偿保证与迁移前完全一致。</p>
 */
public class SixtySecondsMobModelV2 extends EntityModel<SixtySecondsMonsterEntity> {
    private static final String[] FORM_NAMES = {
            "shambler", "runner", "brute", "spitter", "stalker", "howler", "bloater",
            "juggernaut", "cinderling", "frostling", "huskbrute", "ravenor", "wailer",
            "burster", "gorehound", "shadowmute", "bonelord", "spinewalker"
    };

    /** 可动部件在动画中的角色。 */
    private enum Role {
        /** 腿：行走时前后交替摆动。 */
        LEG,
        /** 臂/爪/刃：与同侧腿反相摆动，攻击时前挥。 */
        ARM,
        /** 翼：上下扇动。 */
        WING,
        /** 尾：左右轻摆。 */
        TAIL
    }

    /** {@code 形态:部件 -> 角色}，由 {@link #limb} 在建层时登记（键见 {@link #key}）。 */
    private static final Map<String, Role> LIMB_ROLE = new HashMap<>();
    /** {@code 形态:部件 -> 静止姿态旋转}，用于每帧复位。 */
    private static final Map<String, float[]> LIMB_BASE = new HashMap<>();
    /** {@code 形态 -> 可动部件名列表}，按登记顺序。 */
    private static final Map<String, List<String>> FORM_LIMBS = new HashMap<>();
    /** 正在构建的形态名，供 {@link #limb} 生成键。 */
    private static String currentForm = "";

    /** burster（地雷怪）在贴图上的跳跃幅度（模型 +Y 朝下，故取负号向上）。 */
    private static final float BURSTER_HOP = 3.0F;

    private final ModelPart root;
    private final ModelPart[] forms = new ModelPart[FORM_NAMES.length];

    public SixtySecondsMobModelV2(ModelPart root) {
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

    private static void cube(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz),
                PartPose.ZERO);
    }

    private static void cube(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float ox, float oy, float oz, float rx, float ry, float rz) {
        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, dx, dy, dz),
                PartPose.offsetAndRotation(ox, oy, oz, rx, ry, rz));
    }

    /**
     * 可动肢体：把部件原点（旋转枢轴）迁到 {@code (0, pivotY, 0)} —— 即肢体与躯干
     * 相连的关节，同时<b>保持静止姿态与迁移前完全一致</b>。
     *
     * <p>普通 {@link #cube} 的部件原点在实体原点，给这类部件施加旋转会让整条肢体
     * 绕实体原点画出一个巨大的弧线（腿会被甩到身后十几格）。这里把原点上移到关节，
     * 并用逆旋转补偿盒体偏移：</p>
     * <pre>
     *   世界坐标 = T(0, pivotY, 0) · R · v'
     *   欲使其等于迁移前的 R · v，需  v' = v − R⁻¹ · (0, pivotY, 0)
     * </pre>
     * 其中 {@code R = Rz·Ry·Rx}，与 {@code ModelPart.translateAndRotate} 的
     * 乘法顺序一致。
     *
     * <p>把同一条肢体上的多个部件（如手臂与手）设成<b>相同的 pivotY</b>，它们就会
     * 共享枢轴，动画时保持刚性连接而不会脱节。</p>
     */
    private static void limb(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float pivotY, Role role) {
        limb(parent, name, u, v, x, y, z, dx, dy, dz, pivotY, role, 0.0F, 0.0F, 0.0F);
    }

    private static void limb(PartDefinition parent, String name, int u, int v,
                             float x, float y, float z, float dx, float dy, float dz,
                             float pivotY, Role role, float rx, float ry, float rz) {
        // v' = v − R⁻¹·(0, pivotY, 0)，R = Rz(rz)·Ry(ry)·Rx(rx)
        float sz = (float) Math.sin(rz);
        float cz = (float) Math.cos(rz);
        float sy = (float) Math.sin(ry);
        float cy = (float) Math.cos(ry);
        float sx = (float) Math.sin(rx);
        float cx = (float) Math.cos(rx);
        float ox = pivotY * sz * cy;
        float oy = pivotY * cz * cx + pivotY * sz * sy * sx;
        float oz = -pivotY * cz * sx + pivotY * sz * sy * cx;

        parent.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(u, v)
                        .addBox(x - ox, y - oy, z - oz, dx, dy, dz),
                PartPose.offsetAndRotation(0, pivotY, 0, rx, ry, rz));
        LIMB_ROLE.put(key(name), role);
        LIMB_BASE.put(key(name), new float[]{rx, ry, rz});
        FORM_LIMBS.computeIfAbsent(currentForm, k -> new ArrayList<>()).add(name);
    }

    private static String key(String partName) {
        return currentForm + ":" + partName;
    }

    private static PartDefinition form(PartDefinition root, String name) {
        currentForm = name;
        return root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
    }

    private static void addShambler(PartDefinition root) {
        PartDefinition p = form(root, "shambler");
        cube(p, "torso", 0, 109, -5, 7, -3, 10, 12, 7);
        cube(p, "hunched_back", 104, 116, -4, 5, 2, 8, 8, 4, 0, 0, 0, -0.22F, 0, 0);
        cube(p, "split_skull", 34, 114, -4, 0, -4, 8, 7, 7);
        cube(p, "jaw", 78, 102, -3, 6, -5, 6, 3, 5);
        limb(p, "dragging_arm", 64, 110, -10, 10, -1, 5, 13, 5, 10, Role.ARM, -0.75F, 0, -0.12F);
        limb(p, "hook_hand", 54, 101, -11, 21, -2, 6, 3, 6, 10, Role.ARM, -0.25F, 0, 0);
        limb(p, "short_arm", 34, 100, 5, 10, -1, 5, 9, 5, 10, Role.ARM, -0.40F, 0, 0.18F);
        limb(p, "left_leg", 104, 102, -4, 18, -2, 5, 9, 5, 18, Role.LEG, 0.35F, 0, -0.12F);
        limb(p, "right_leg", 84, 113, 1, 18, -2, 5, 10, 5, 18, Role.LEG, -0.58F, 0, 0.16F);
        cube(p, "tendril", 0, 96, 2, 21, 3, 3, 10, 3, 0, 0, 0, -0.72F, 0.18F, 0);
    }

    private static void addRunner(PartDefinition root) {
        PartDefinition p = form(root, "runner");
        // 中央躯干：加宽加厚，作为所有肢体的连接核心
        cube(p, "torso", 58, 115, -4, 8, -3, 8, 11, 6);
        cube(p, "neck", 12, 103, -2, 2, -2, 4, 7, 4, 0, 0, 0, -0.3F, 0, 0);
        cube(p, "head", 30, 114, -3, -3, -5, 6, 6, 9);
        cube(p, "snout", 78, 118, -2, 0, -12, 4, 3, 8);
        limb(p, "tail", 0, 114, -2, 12, 3, 5, 3, 13, 12, Role.TAIL, 0.42F, 0, 0);
        limb(p, "leg_l", 98, 104, -4, 16, -2, 4, 11, 4, 16, Role.LEG, -0.72F, 0, -0.1F);
        limb(p, "leg_r", 110, 104, 0, 16, -2, 4, 11, 4, 16, Role.LEG, -0.28F, 0, 0.1F);
        limb(p, "ankle_l", 100, 118, -5, 25, -5, 3, 3, 7, 16, Role.LEG, 0.92F, 0, 0);
        limb(p, "ankle_r", 78, 108, 2, 25, -5, 3, 3, 7, 16, Role.LEG, 0.92F, 0, 0);
        // 前臂根部贴躯干侧面，避免悬空
        limb(p, "arm_l", 58, 102, -5, 10, -2, 3, 10, 3, 10, Role.ARM, -0.95F, 0, 0.1F);
        limb(p, "arm_r", 0, 101, 2, 10, -2, 3, 10, 3, 10, Role.ARM, -0.95F, 0, -0.1F);
    }

    private static void addBrute(PartDefinition root) {
        PartDefinition p = form(root, "brute");
        cube(p, "barrel_chest", 0, 107, -7, 6, -4, 14, 13, 8);
        cube(p, "neck", 0, 83, -4, 1, -3, 8, 6, 6);
        cube(p, "ram_head", 44, 111, -5, -5, -5, 10, 8, 9);
        cube(p, "horn_l", 106, 118, -8, -5, -3, 3, 7, 3, 0, 0, 0, 0, 0, -0.45F);
        cube(p, "horn_r", 116, 108, 5, -5, -3, 3, 7, 3, 0, 0, 0, 0, 0, 0.45F);
        limb(p, "fist_l", 82, 111, -12, 11, -4, 5, 10, 7, 11, Role.ARM, -0.18F, 0, -0.22F);
        limb(p, "fist_r", 44, 94, 7, 11, -4, 5, 10, 7, 11, Role.ARM, -0.18F, 0, 0.22F);
        limb(p, "thigh_l", 68, 95, -6, 18, -3, 6, 10, 6, 18, Role.LEG, 0.12F, 0, -0.08F);
        limb(p, "thigh_r", 92, 95, 0, 18, -3, 6, 10, 6, 18, Role.LEG, 0.12F, 0, 0.08F);
        cube(p, "back_plate", 0, 95, -6, 8, 3, 12, 9, 3, 0, 0, 0, -0.16F, 0, 0);
    }

    private static void addSpitter(PartDefinition root) {
        PartDefinition p = form(root, "spitter");
        cube(p, "acid_sack", 0, 111, -6, 9, -3, 12, 10, 7);
        cube(p, "sack_ridge", 72, 116, -4, 3, 0, 8, 7, 5, 0, 0, 0, -0.35F, 0, 0);
        cube(p, "open_muzzle", 38, 112, -4, -3, -9, 8, 7, 9);
        cube(p, "lower_mouth", 98, 118, -3, 4, -8, 6, 3, 7);
        cube(p, "tube_l", 114, 104, -8, 4, 1, 3, 11, 3, 0, 0, 0, -0.35F, 0, -0.25F);
        cube(p, "tube_r", 0, 97, 5, 4, 1, 3, 11, 3, 0, 0, 0, -0.35F, 0, 0.25F);
        limb(p, "acid_arm_l", 98, 105, -10, 11, -2, 4, 9, 4, 11, Role.ARM, -0.48F, 0, 0);
        limb(p, "acid_arm_r", 72, 103, 6, 11, -2, 4, 9, 4, 11, Role.ARM, -0.48F, 0, 0);
        limb(p, "acid_leg_l", 38, 99, -5, 18, -1, 4, 9, 4, 18, Role.LEG, 0.15F, 0, 0);
        limb(p, "acid_leg_r", 54, 99, 1, 18, -1, 4, 9, 4, 18, Role.LEG, 0.15F, 0, 0);
    }

    private static void addStalker(PartDefinition root) {
        PartDefinition p = form(root, "stalker");
        cube(p, "low_shell", 36, 114, -6, 12, -4, 12, 6, 8);
        cube(p, "needle_head", 0, 112, -4, 6, -10, 8, 6, 10);
        cube(p, "eye_shroud", 0, 105, -3, 8, -12, 6, 3, 4);
        limb(p, "leg_fl", 76, 102, -9, 14, -3, 3, 3, 10, 14, Role.LEG, 0.72F, 0, -0.32F);
        limb(p, "leg_fr", 102, 102, 6, 14, -3, 3, 3, 10, 14, Role.LEG, 0.72F, 0, 0.32F);
        limb(p, "leg_bl", 76, 115, -9, 14, 1, 3, 3, 10, 14, Role.LEG, -0.72F, 0, -0.32F);
        limb(p, "leg_br", 102, 115, 6, 14, 1, 3, 3, 10, 14, Role.LEG, -0.72F, 0, 0.32F);
        limb(p, "blade_l", 36, 101, -10, 7, -1, 3, 10, 3, 7, Role.ARM, -0.35F, 0, -0.2F);
        limb(p, "blade_r", 48, 101, 7, 7, -1, 3, 10, 3, 7, Role.ARM, -0.35F, 0, 0.2F);
    }

    private static void addHowler(PartDefinition root) {
        PartDefinition p = form(root, "howler");
        cube(p, "resonator_chest", 0, 109, -6, 8, -3, 12, 13, 6);
        cube(p, "throat", 104, 115, -3, 5, -8, 6, 8, 5);
        cube(p, "howling_jaw", 36, 112, -5, -3, -9, 10, 8, 8);
        cube(p, "horn_l", 52, 101, -8, -5, -2, 3, 8, 3, 0, 0, 0, 0, 0, -0.35F);
        cube(p, "horn_r", 64, 99, 5, -5, -2, 3, 8, 3, 0, 0, 0, 0, 0, 0.35F);
        limb(p, "arm_l", 72, 110, -11, 10, -1, 4, 14, 4, 10, Role.ARM, -0.25F, 0, -0.1F);
        limb(p, "arm_r", 88, 110, 7, 10, -1, 4, 14, 4, 10, Role.ARM, -0.25F, 0, 0.1F);
        limb(p, "leg_l", 104, 103, -5, 20, -2, 4, 8, 4, 20, Role.LEG, 0.08F, 0, 0);
        limb(p, "leg_r", 36, 100, 1, 20, -2, 4, 8, 4, 20, Role.LEG, 0.08F, 0, 0);
    }

    private static void addBloater(PartDefinition root) {
        PartDefinition p = form(root, "bloater");
        cube(p, "inflated_body", 0, 101, -8, 7, -6, 16, 15, 12);
        cube(p, "belly_lobe", 90, 115, -6, 13, -9, 12, 8, 5, 0, 0, 0, 0.12F, 0, 0);
        cube(p, "one_eyed_face", 56, 113, -5, 1, -10, 10, 8, 7);
        cube(p, "lip", 90, 106, -4, 8, -12, 8, 3, 6);
        cube(p, "pustule_l", 0, 92, -11, 10, -3, 4, 5, 4, 0, 0, 0, 0, 0, -0.2F);
        cube(p, "pustule_r", 16, 92, 7, 12, -2, 4, 5, 4, 0, 0, 0, 0, 0, 0.2F);
        cube(p, "pustule_back", 96, 96, -3, 8, 5, 6, 6, 4, 0, 0, 0, -0.15F, 0, 0);
        limb(p, "dangling_leg_l", 56, 101, -6, 20, -2, 5, 7, 5, 20, Role.LEG, 0.22F, 0, 0);
        limb(p, "dangling_leg_r", 76, 94, 1, 20, -2, 5, 7, 5, 20, Role.LEG, 0.22F, 0, 0);
    }

    private static void addJuggernaut(PartDefinition root) {
        PartDefinition p = form(root, "juggernaut");
        cube(p, "tower_body", 0, 104, -8, 6, -4, 16, 16, 8);
        cube(p, "iron_helmet", 48, 109, -6, -4, -5, 12, 10, 9);
        cube(p, "visor", 22, 80, -5, 1, -10, 10, 3, 5);
        cube(p, "shoulder_l", 90, 110, -13, 7, -3, 5, 9, 9, 0, 0, 0, 0, 0, -0.1F);
        cube(p, "shoulder_r", 90, 92, 8, 7, -3, 5, 9, 9, 0, 0, 0, 0, 0, 0.1F);
        limb(p, "hammer_arm_l", 48, 91, -14, 13, -2, 5, 12, 6, 13, Role.ARM, -0.12F, 0, 0);
        limb(p, "hammer_arm_r", 0, 86, 9, 13, -2, 5, 12, 6, 13, Role.ARM, -0.12F, 0, 0);
        limb(p, "pillar_leg_l", 70, 77, -6, 21, -3, 6, 9, 6, 21, Role.LEG, 0.08F, 0, 0);
        limb(p, "pillar_leg_r", 94, 77, 0, 21, -3, 6, 9, 6, 21, Role.LEG, 0.08F, 0, 0);
        cube(p, "back_banner", 22, 88, -5, 9, 4, 10, 14, 2, 0, 0, 0, 0.2F, 0, 0);
    }

    private static void addCinderling(PartDefinition root) {
        PartDefinition p = form(root, "cinderling");
        cube(p, "charred_core", 0, 110, -5, 7, -4, 10, 12, 8);
        cube(p, "ember_face", 36, 115, -4, 3, -8, 8, 7, 6);
        cube(p, "flame_crown", 64, 116, -3, -4, -2, 6, 8, 4, 0, 0, 0, -0.15F, 0, 0);
        // 尾根部移到 charred_core 内（z 起 1），与身体相连
        limb(p, "flame_tail", 84, 116, -3, 16, 1, 6, 9, 3, 16, Role.TAIL, 0.5F, 0, 0);
        limb(p, "coal_arm_l", 102, 115, -9, 10, -2, 4, 9, 4, 10, Role.ARM, -0.55F, 0, -0.2F);
        limb(p, "coal_arm_r", 64, 103, 5, 10, -2, 4, 9, 4, 10, Role.ARM, -0.55F, 0, 0.2F);
        // 腿加长，使脚接近地面模型 y=27
        limb(p, "coal_leg_l", 80, 105, -5, 18, -1, 4, 9, 4, 18, Role.LEG, 0.2F, 0, 0);
        limb(p, "coal_leg_r", 36, 104, 1, 18, -1, 4, 9, 4, 18, Role.LEG, 0.2F, 0, 0);
    }

    private static void addFrostling(PartDefinition root) {
        PartDefinition p = form(root, "frostling");
        cube(p, "crystal_body", 0, 109, -5, 8, -4, 10, 11, 8);
        cube(p, "ice_mask", 36, 114, -4, 0, -8, 8, 8, 6);
        cube(p, "crown", 96, 116, -2, -7, -2, 4, 8, 4, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "spike_l", 112, 102, -8, 6, -1, 3, 11, 3, 0, 0, 0, 0, 0, -0.42F);
        cube(p, "spike_r", 64, 101, 5, 6, -1, 3, 11, 3, 0, 0, 0, 0, 0, 0.42F);
        limb(p, "ice_arm_l", 64, 115, -9, 11, -2, 4, 9, 4, 11, Role.ARM, -0.45F, 0, -0.12F);
        limb(p, "ice_arm_r", 80, 115, 5, 11, -2, 4, 9, 4, 11, Role.ARM, -0.45F, 0, 0.12F);
        limb(p, "ice_leg_l", 112, 116, -5, 19, -1, 4, 8, 4, 19, Role.LEG, 0.12F, 0, 0);
        limb(p, "ice_leg_r", 96, 104, 1, 19, -1, 4, 8, 4, 19, Role.LEG, 0.12F, 0, 0);
    }

    private static void addHuskbrute(PartDefinition root) {
        PartDefinition p = form(root, "huskbrute");
        cube(p, "dried_torso", 0, 106, -7, 7, -4, 14, 14, 8);
        cube(p, "shell_head", 44, 111, -5, -4, -4, 10, 9, 8);
        cube(p, "shell_back", 80, 112, -7, 7, 3, 14, 13, 3, 0, 0, 0, -0.12F, 0, 0);
        cube(p, "jaw", 44, 100, -4, 4, -9, 8, 4, 7);
        limb(p, "arm_l", 80, 93, -12, 9, -2, 5, 14, 5, 9, Role.ARM, -0.18F, 0, -0.15F);
        limb(p, "arm_r", 100, 93, 7, 9, -2, 5, 14, 5, 9, Role.ARM, -0.18F, 0, 0.15F);
        limb(p, "leg_l", 0, 92, -6, 20, -2, 6, 9, 5, 20, Role.LEG, 0.08F, 0, 0);
        limb(p, "leg_r", 22, 92, 0, 20, -2, 6, 9, 5, 20, Role.LEG, 0.08F, 0, 0);
    }

    private static void addRavenor(PartDefinition root) {
        PartDefinition p = form(root, "ravenor");
        // 加长躯干向下覆盖脚枢轴，并让腿延伸到地面模型 y≈27
        cube(p, "bird_body", 0, 110, -4, 4, -3, 8, 18, 6, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "beaked_head", 28, 113, -4, 1, -8, 8, 7, 8);
        cube(p, "beak", 104, 118, -2, 4, -14, 4, 3, 7);
        limb(p, "wing_l", 60, 113, -12, 6, 0, 8, 12, 3, 6, Role.WING, 0.2F, 0, -0.35F);
        limb(p, "wing_r", 82, 113, 4, 6, 0, 8, 12, 3, 6, Role.WING, 0.2F, 0, 0.35F);
        limb(p, "talon_l", 28, 101, -6, 19, -2, 4, 11, 4, 19, Role.LEG, 0.72F, 0, -0.18F);
        limb(p, "talon_r", 44, 101, 2, 19, -2, 4, 11, 4, 19, Role.LEG, 0.72F, 0, 0.18F);
        limb(p, "tail_feather", 104, 103, -2, 16, 3, 4, 12, 3, 16, Role.TAIL, 0.72F, 0, 0);
    }

    private static void addWailer(PartDefinition root) {
        PartDefinition p = form(root, "wailer");
        cube(p, "gaunt_body", 0, 106, -4, 6, -3, 8, 16, 6);
        cube(p, "long_neck", 84, 112, -2, -4, -2, 4, 12, 4);
        cube(p, "split_face", 28, 112, -4, -11, -5, 8, 9, 7);
        cube(p, "jaw", 58, 117, -3, -3, -9, 6, 4, 7);
        limb(p, "ribbon_l", 100, 109, -8, 9, 0, 3, 16, 3, 9, Role.ARM, 0.12F, 0, -0.15F);
        limb(p, "ribbon_r", 112, 109, 5, 9, 0, 3, 16, 3, 9, Role.ARM, 0.12F, 0, 0.15F);
        limb(p, "thin_arm_l", 58, 98, -6, 8, -1, 3, 16, 3, 8, Role.ARM, -0.18F, 0, 0);
        limb(p, "thin_arm_r", 70, 98, 5, 8, -1, 3, 16, 3, 8, Role.ARM, -0.18F, 0, 0);
        limb(p, "needle_leg_l", 28, 101, -4, 21, -1, 3, 8, 3, 21, Role.LEG, 0.1F, 0, 0);
        limb(p, "needle_leg_r", 40, 101, 1, 21, -1, 3, 8, 3, 21, Role.LEG, 0.1F, 0, 0);
    }

    /**
     * 爆碎者。
     *
     * <p>注：原来 mine_body 用 texOffs(96,16)、central_eye 用 (112,16)，
     * 按 14×12×14 / 8×8×5 展开需要 152 / 138 像素宽，超出 128 贴图宽度，
     * SOUTH 面整块落在贴图外、EAST 与 DOWN 被裁掉大半。这里把全部部件重新排布
     * 到 128×128 内（见各 texOffs），保证六个面都有正确的采样区域。</p>
     */
    private static void addBurster(PartDefinition root) {
        PartDefinition p = form(root, "burster");
        cube(p, "mine_body", 0, 102, -7, 9, -7, 14, 12, 14);
        cube(p, "central_eye", 56, 115, -4, 5, -11, 8, 8, 5);
        cube(p, "fuse", 92, 102, -2, 1, -2, 4, 8, 4, 0, 0, 0, -0.35F, 0, 0);
        cube(p, "spike_front", 100, 114, -2, 12, -14, 4, 9, 5, 0, 0, 0, 0.8F, 0, 0);
        cube(p, "spike_back", 82, 114, -2, 12, 7, 4, 9, 5, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_l", 56, 103, -14, 12, -2, 5, 8, 4, 0, 0, 0, 0, 0, -0.8F);
        cube(p, "spike_r", 74, 102, 9, 12, -2, 5, 8, 4, 0, 0, 0, 0, 0, 0.8F);
        cube(p, "stub_l", 108, 105, -6, 20, -5, 4, 7, 4, 0, 0, 0, 0.28F, 0, 0);
        cube(p, "stub_r", 108, 96, 2, 20, -5, 4, 7, 4, 0, 0, 0, 0.28F, 0, 0);
    }

    private static void addGorehound(PartDefinition root) {
        PartDefinition p = form(root, "gorehound");
        cube(p, "canine_body", 38, 114, -5, 10, -3, 10, 8, 6, 0, 0, 0, 0.1F, 0, 0);
        cube(p, "hound_neck", 70, 113, -4, 3, -4, 8, 9, 6, 0, 0, 0, -0.28F, 0, 0);
        cube(p, "hound_head", 0, 111, -5, -3, -9, 10, 8, 9);
        cube(p, "muzzle", 38, 102, -3, 2, -15, 6, 4, 8);
        cube(p, "ear_l", 114, 105, -7, -5, -3, 4, 6, 3, 0, 0, 0, 0, 0, -0.35F);
        cube(p, "ear_r", 16, 102, 3, -5, -3, 4, 6, 3, 0, 0, 0, 0, 0, 0.35F);
        limb(p, "leg_fl", 82, 99, -4, 16, -1, 4, 11, 4, 16, Role.LEG, 0.52F, 0, 0);
        limb(p, "leg_fr", 0, 97, 3, 16, -1, 4, 11, 4, 16, Role.LEG, 0.52F, 0, 0);
        limb(p, "leg_bl", 98, 100, -4, 16, 0, 4, 11, 4, 16, Role.LEG, -0.52F, 0, 0);
        limb(p, "leg_br", 66, 99, 3, 16, 0, 4, 11, 4, 16, Role.LEG, -0.52F, 0, 0);
        limb(p, "tail", 98, 114, 1, 10, 3, 4, 3, 11, 10, Role.TAIL, -0.55F, 0, 0);
    }

    private static void addShadowmute(PartDefinition root) {
        PartDefinition p = form(root, "shadowmute");
        // 披风加宽 z 覆盖触手根部，消除与身体的缝隙
        cube(p, "floating_cloak", 0, 107, -6, 8, -4, 12, 15, 7);
        cube(p, "hood", 36, 111, -5, -3, -4, 10, 10, 7);
        cube(p, "face_void", 70, 116, -4, 1, -9, 8, 7, 5);
        // 触手根部落回披风内（x 贴身侧）
        limb(p, "tendril_l", 114, 112, -7, 12, -1, 3, 13, 3, 12, Role.ARM, 0.32F, 0, -0.2F);
        limb(p, "tendril_r", 70, 100, 6, 12, -1, 3, 13, 3, 12, Role.ARM, 0.32F, 0, 0.2F);
        limb(p, "cloak_tail", 96, 115, -3, 20, 1, 6, 10, 3, 20, Role.TAIL, 0.4F, 0, 0);
        // 手移近身体，不再悬空
        limb(p, "floating_hand_l", 82, 104, -7, 12, -2, 4, 7, 4, 12, Role.ARM, -0.5F, 0, 0);
        limb(p, "floating_hand_r", 98, 104, 6, 12, -2, 4, 7, 4, 12, Role.ARM, -0.5F, 0, 0);
    }

    private static void addBonelord(PartDefinition root) {
        PartDefinition p = form(root, "bonelord");
        cube(p, "rib_cage", 0, 108, -5, 7, -3, 10, 14, 6);
        cube(p, "skull", 32, 111, -5, -5, -5, 10, 9, 8);
        cube(p, "crown_l", 32, 100, -8, -10, -2, 4, 8, 3, 0, 0, 0, 0, 0, -0.28F);
        cube(p, "crown_r", 46, 100, 4, -10, -2, 4, 8, 3, 0, 0, 0, 0, 0, 0.28F);
        limb(p, "bone_arm_l", 92, 109, -10, 8, -1, 3, 16, 3, 8, Role.ARM, -0.22F, 0, -0.12F);
        limb(p, "bone_arm_r", 104, 109, 7, 8, -1, 3, 16, 3, 8, Role.ARM, -0.22F, 0, 0.12F);
        limb(p, "bone_leg_l", 116, 116, -4, 20, -1, 3, 9, 3, 20, Role.LEG, 0.1F, 0, 0);
        limb(p, "bone_leg_r", 116, 104, 1, 20, -1, 3, 9, 3, 20, Role.LEG, 0.1F, 0, 0);
        cube(p, "vertebrae_cape", 68, 110, -5, 10, 1, 10, 16, 2, 0, 0, 0, 0.2F, 0, 0);
    }

    private static void addSpinewalker(PartDefinition root) {
        PartDefinition p = form(root, "spinewalker");
        // 重建：加厚中央躯干，让四肢根部都落在躯干内
        cube(p, "vertebra_body", 32, 110, -5, 8, -4, 10, 12, 8);
        cube(p, "spine_neck", 60, 113, -3, -1, -4, 6, 11, 4, 0, 0, 0, -0.3F, 0, 0);
        cube(p, "spine_head", 0, 112, -4, -9, -6, 8, 8, 8);
        cube(p, "jaw", 80, 118, -3, -2, -11, 6, 4, 6);
        cube(p, "spike_0", 24, 99, -2, 1, 4, 4, 7, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_1", 96, 103, -2, 6, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_2", 112, 103, -2, 11, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_3", 60, 101, -2, 16, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        // 四肢根部紧贴加厚后的躯干（x 在 ±5 内），不再悬空
        limb(p, "leg_l", 104, 115, -6, 19, -2, 4, 9, 4, 19, Role.LEG, 0.18F, 0, 0);
        limb(p, "leg_r", 80, 105, 2, 19, -2, 4, 9, 4, 19, Role.LEG, 0.18F, 0, 0);
        limb(p, "arm_l", 0, 97, -6, 9, -2, 3, 12, 3, 9, Role.ARM, -0.38F, 0, 0);
        limb(p, "arm_r", 12, 97, 3, 9, -2, 3, 12, 3, 9, Role.ARM, -0.38F, 0, 0);
    }

    /**
     * 取攻击动画进度。
     *
     * <p>原版 {@code EntityModel} 的 {@code attackTime} 由各模型自己维护，
     * 本模型不是人形模型、没有走原版的装配流程，故在此显式取实体的挥击进度。
     * 实体的 {@code attackAnim} 在 {@code doHurtTarget} 调用 swing 后置 1 并逐 tick 衰减。</p>
     */
    @Override
    public void prepareMobModel(SixtySecondsMonsterEntity entity, float limbSwing,
                                float limbSwingAmount, float partialTick) {
        this.attackTime = entity.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(SixtySecondsMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart form : forms) {
            form.visible = false;
            form.xRot = 0.0F;
            form.yRot = 0.0F;
            form.zRot = 0.0F;
            form.x = 0.0F;
            form.y = 0.0F;
            form.z = 0.0F;
        }
        int id = Mth.clamp(entity.getVariant().id, 0, forms.length - 1);
        ModelPart active = forms[id];
        active.visible = true;
        String form = FORM_NAMES[id];

        List<String> limbs = FORM_LIMBS.getOrDefault(form, List.of());

        // 复位所有可动部件的静止姿态（上一帧的动画量必须清掉）
        for (String name : limbs) {
            float[] base = LIMB_BASE.get(form + ":" + name);
            if (base == null) {
                continue;
            }
            ModelPart part = active.getChild(name);
            part.xRot = base[0];
            part.yRot = base[1];
            part.zRot = base[2];
        }

        float walk = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float swing = Mth.cos(limbSwing * 0.6662F);
        float swingOpposite = Mth.cos(limbSwing * 0.6662F + (float) Math.PI);

        // 攻击挥击曲线：attackTime 1→0 时 punch 走 0→1→0
        float punch = 0.0F;
        if (this.attackTime > 0.0F) {
            float t = 1.0F - this.attackTime;
            punch = Mth.sin((1.0F - t * t * t) * (float) Math.PI);
        }

        for (String name : limbs) {
            Role role = LIMB_ROLE.get(form + ":" + name);
            if (role == null) {
                continue;
            }
            ModelPart part = active.getChild(name);
            boolean leftSide = isLeftSide(name);
            float own = leftSide ? swing : swingOpposite;
            float opposite = leftSide ? swingOpposite : swing;
            switch (role) {
                case LEG -> part.xRot += own * 0.75F * walk;
                case ARM -> {
                    // 与同侧腿反相；攻击时整条手臂前挥
                    part.xRot += opposite * 0.5F * walk - punch * 1.3F;
                    part.zRot += punch * (leftSide ? 0.25F : -0.25F);
                }
                case WING -> part.zRot += Mth.sin(ageInTicks * 0.55F)
                        * (0.12F + walk * 0.55F) * (leftSide ? 1.0F : -1.0F);
                case TAIL -> {
                    part.yRot += Mth.sin(ageInTicks * 0.18F + id) * 0.18F;
                    part.xRot += Mth.sin(ageInTicks * 0.25F + id) * 0.07F;
                }
            }
        }

        // 整体：呼吸 / 悬浮 / 弹跳，以及攻击时前倾
        if (id == 15) {
            // 暗默：悬浮
            active.y = Mth.sin(ageInTicks * 0.08F + id) * 0.55F;
        } else if (id == 13) {
            // 爆碎者：像地雷一样一蹦一蹦（模型 +Y 朝下，故取负号向上）
            float hop = Math.abs(Mth.cos(limbSwing * 0.6662F)) * walk;
            active.y = Mth.sin(ageInTicks * 0.08F + id) * 0.08F - hop * BURSTER_HOP;
            active.zRot = Mth.cos(limbSwing * 0.6662F) * walk * 0.18F;
        } else {
            active.y = Mth.sin(ageInTicks * 0.08F + id) * 0.08F;
        }
        active.xRot = headPitch * Mth.DEG_TO_RAD * 0.03F - punch * 0.18F;
        active.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.03F;
    }

    /** 判断部件属于左侧还是右侧，用于决定摆动相位（四肢对角线交替）。 */
    private static boolean isLeftSide(String name) {
        return name.endsWith("_l") || name.startsWith("left")
                || name.endsWith("_fl") || name.endsWith("_bl");
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
