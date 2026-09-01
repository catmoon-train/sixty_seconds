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

/**
 * 第二代普通 mob 模型。
 *
 * <p>每个变体拥有自己的根节点和自己的几何结构。这里不再保留旧模型中的
 * humanoid 兼容分支，也不通过缩放同一副人形骨架来伪装不同生物。</p>
 */
public class SixtySecondsMobModelV2 extends EntityModel<SixtySecondsMonsterEntity> {
    private static final String[] FORM_NAMES = {
            "shambler", "runner", "brute", "spitter", "stalker", "howler", "bloater",
            "juggernaut", "cinderling", "frostling", "huskbrute", "ravenor", "wailer",
            "burster", "gorehound", "shadowmute", "bonelord", "spinewalker"
    };

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

    private static PartDefinition form(PartDefinition root, String name) {
        return root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
    }

    private static void addShambler(PartDefinition root) {
        PartDefinition p = form(root, "shambler");
        cube(p, "torso", 0, 0, -5, 7, -3, 10, 12, 7);
        cube(p, "hunched_back", 0, 20, -4, 5, 2, 8, 8, 4, 0, 0, 0, -0.22F, 0, 0);
        cube(p, "split_skull", 32, 0, -4, 0, -4, 8, 7, 7);
        cube(p, "jaw", 32, 16, -3, 6, -5, 6, 3, 5);
        cube(p, "dragging_arm", 48, 0, -10, 10, -1, 5, 13, 5, 0, 0, 0, -0.75F, 0, -0.12F);
        cube(p, "hook_hand", 48, 20, -11, 21, -2, 6, 3, 6, 0, 0, 0, -0.25F, 0, 0);
        cube(p, "short_arm", 64, 0, 5, 10, -1, 5, 9, 5, 0, 0, 0, -0.40F, 0, 0.18F);
        cube(p, "left_leg", 64, 16, -4, 18, -2, 5, 9, 5, 0, 0, 0, 0.35F, 0, -0.12F);
        cube(p, "right_leg", 80, 16, 1, 18, -2, 5, 10, 5, 0, 0, 0, -0.58F, 0, 0.16F);
        cube(p, "tendril", 96, 0, 2, 21, 3, 3, 10, 3, 0, 0, 0, -0.72F, 0.18F, 0);
    }

    private static void addRunner(PartDefinition root) {
        PartDefinition p = form(root, "runner");
        cube(p, "rib_body", 0, 32, -3, 9, -2, 6, 9, 4);
        cube(p, "raptor_neck", 16, 32, -2, 3, -1, 4, 8, 3, 0, 0, 0, -0.35F, 0, 0);
        cube(p, "long_head", 16, 44, -3, -2, -4, 6, 6, 8);
        cube(p, "snout", 32, 44, -2, 0, -10, 4, 3, 7);
        cube(p, "tail", 32, 56, -2, 12, 2, 4, 3, 11, 0, 0, 0, 0.42F, 0, 0);
        cube(p, "leg_l", 48, 32, -3, 16, -1, 3, 11, 3, 0, 0, 0, -0.72F, 0, -0.12F);
        cube(p, "leg_r", 48, 48, 0, 16, -1, 3, 11, 3, 0, 0, 0, -0.28F, 0, 0.12F);
        cube(p, "ankle_l", 64, 32, -4, 24, -5, 3, 3, 7, 0, 0, 0, 0.92F, 0, 0);
        cube(p, "ankle_r", 64, 48, 1, 24, -5, 3, 3, 7, 0, 0, 0, 0.92F, 0, 0);
        cube(p, "forearm_l", 80, 32, -7, 9, -2, 3, 10, 3, 0, 0, 0, -0.95F, 0, 0.08F);
        cube(p, "forearm_r", 80, 48, 4, 9, -2, 3, 10, 3, 0, 0, 0, -0.95F, 0, -0.08F);
    }

    private static void addBrute(PartDefinition root) {
        PartDefinition p = form(root, "brute");
        cube(p, "barrel_chest", 0, 64, -7, 6, -4, 14, 13, 8);
        cube(p, "neck", 16, 64, -4, 1, -3, 8, 6, 6);
        cube(p, "ram_head", 16, 76, -5, -5, -5, 10, 8, 9);
        cube(p, "horn_l", 32, 64, -8, -5, -3, 3, 7, 3, 0, 0, 0, 0, 0, -0.45F);
        cube(p, "horn_r", 32, 76, 5, -5, -3, 3, 7, 3, 0, 0, 0, 0, 0, 0.45F);
        cube(p, "fist_l", 48, 64, -12, 11, -4, 5, 10, 7, 0, 0, 0, -0.18F, 0, -0.22F);
        cube(p, "fist_r", 48, 82, 7, 11, -4, 5, 10, 7, 0, 0, 0, -0.18F, 0, 0.22F);
        cube(p, "thigh_l", 64, 64, -6, 18, -3, 6, 10, 6, 0, 0, 0, 0.12F, 0, -0.08F);
        cube(p, "thigh_r", 80, 64, 0, 18, -3, 6, 10, 6, 0, 0, 0, 0.12F, 0, 0.08F);
        cube(p, "back_plate", 96, 64, -6, 8, 3, 12, 9, 3, 0, 0, 0, -0.16F, 0, 0);
    }

    private static void addSpitter(PartDefinition root) {
        PartDefinition p = form(root, "spitter");
        cube(p, "acid_sack", 0, 96, -6, 9, -3, 12, 10, 7);
        cube(p, "sack_ridge", 16, 96, -4, 3, 0, 8, 7, 5, 0, 0, 0, -0.35F, 0, 0);
        cube(p, "open_muzzle", 32, 96, -4, -3, -9, 8, 7, 9);
        cube(p, "lower_mouth", 48, 96, -3, 4, -8, 6, 3, 7);
        cube(p, "tube_l", 64, 96, -8, 4, 1, 3, 11, 3, 0, 0, 0, -0.35F, 0, -0.25F);
        cube(p, "tube_r", 80, 96, 5, 4, 1, 3, 11, 3, 0, 0, 0, -0.35F, 0, 0.25F);
        cube(p, "acid_arm_l", 96, 96, -10, 11, -2, 4, 9, 4, 0, 0, 0, -0.48F, 0, 0);
        cube(p, "acid_arm_r", 112, 96, 6, 11, -2, 4, 9, 4, 0, 0, 0, -0.48F, 0, 0);
        cube(p, "acid_leg_l", 0, 112, -5, 18, -1, 4, 9, 4, 0, 0, 0, 0.15F, 0, 0);
        cube(p, "acid_leg_r", 16, 112, 1, 18, -1, 4, 9, 4, 0, 0, 0, 0.15F, 0, 0);
    }

    private static void addStalker(PartDefinition root) {
        PartDefinition p = form(root, "stalker");
        cube(p, "low_shell", 32, 0, -6, 12, -4, 12, 6, 8);
        cube(p, "needle_head", 48, 0, -4, 6, -10, 8, 6, 10);
        cube(p, "eye_shroud", 64, 0, -3, 8, -12, 6, 3, 4);
        cube(p, "leg_fl", 80, 0, -9, 14, -3, 3, 3, 10, 0, 0, 0, 0.72F, 0, -0.32F);
        cube(p, "leg_fr", 96, 0, 6, 14, -3, 3, 3, 10, 0, 0, 0, 0.72F, 0, 0.32F);
        cube(p, "leg_bl", 80, 16, -9, 14, 1, 3, 3, 10, 0, 0, 0, -0.72F, 0, -0.32F);
        cube(p, "leg_br", 96, 16, 6, 14, 1, 3, 3, 10, 0, 0, 0, -0.72F, 0, 0.32F);
        cube(p, "blade_l", 112, 0, -10, 7, -1, 3, 10, 3, 0, 0, 0, -0.35F, 0, -0.2F);
        cube(p, "blade_r", 112, 16, 7, 7, -1, 3, 10, 3, 0, 0, 0, -0.35F, 0, 0.2F);
    }

    private static void addHowler(PartDefinition root) {
        PartDefinition p = form(root, "howler");
        cube(p, "resonator_chest", 0, 0, -6, 8, -3, 12, 13, 6);
        cube(p, "throat", 16, 0, -3, 5, -8, 6, 8, 5);
        cube(p, "howling_jaw", 32, 0, -5, -3, -9, 10, 8, 8);
        cube(p, "horn_l", 48, 0, -8, -5, -2, 3, 8, 3, 0, 0, 0, 0, 0, -0.35F);
        cube(p, "horn_r", 48, 16, 5, -5, -2, 3, 8, 3, 0, 0, 0, 0, 0, 0.35F);
        cube(p, "arm_l", 64, 0, -11, 10, -1, 4, 14, 4, 0, 0, 0, -0.25F, 0, -0.1F);
        cube(p, "arm_r", 80, 0, 7, 10, -1, 4, 14, 4, 0, 0, 0, -0.25F, 0, 0.1F);
        cube(p, "leg_l", 96, 0, -5, 20, -2, 4, 8, 4, 0, 0, 0, 0.08F, 0, 0);
        cube(p, "leg_r", 112, 0, 1, 20, -2, 4, 8, 4, 0, 0, 0, 0.08F, 0, 0);
    }

    private static void addBloater(PartDefinition root) {
        PartDefinition p = form(root, "bloater");
        cube(p, "inflated_body", 0, 32, -8, 7, -6, 16, 15, 12);
        cube(p, "belly_lobe", 16, 32, -6, 13, -9, 12, 8, 5, 0, 0, 0, 0.12F, 0, 0);
        cube(p, "one_eyed_face", 32, 32, -5, 1, -10, 10, 8, 7);
        cube(p, "lip", 48, 32, -4, 8, -12, 8, 3, 6);
        cube(p, "pustule_l", 64, 32, -11, 10, -3, 4, 5, 4, 0, 0, 0, 0, 0, -0.2F);
        cube(p, "pustule_r", 80, 32, 7, 12, -2, 4, 5, 4, 0, 0, 0, 0, 0, 0.2F);
        cube(p, "pustule_back", 96, 32, -3, 8, 5, 6, 6, 4, 0, 0, 0, -0.15F, 0, 0);
        cube(p, "dangling_leg_l", 112, 32, -6, 20, -2, 5, 7, 5, 0, 0, 0, 0.22F, 0, 0);
        cube(p, "dangling_leg_r", 0, 48, 1, 20, -2, 5, 7, 5, 0, 0, 0, 0.22F, 0, 0);
    }

    private static void addJuggernaut(PartDefinition root) {
        PartDefinition p = form(root, "juggernaut");
        cube(p, "tower_body", 16, 48, -8, 6, -4, 16, 16, 8);
        cube(p, "iron_helmet", 32, 48, -6, -4, -5, 12, 10, 9);
        cube(p, "visor", 48, 48, -5, 1, -10, 10, 3, 5);
        cube(p, "shoulder_l", 64, 48, -13, 7, -3, 5, 9, 9, 0, 0, 0, 0, 0, -0.1F);
        cube(p, "shoulder_r", 80, 48, 8, 7, -3, 5, 9, 9, 0, 0, 0, 0, 0, 0.1F);
        cube(p, "hammer_arm_l", 96, 48, -14, 13, -2, 5, 12, 6, 0, 0, 0, -0.12F, 0, 0);
        cube(p, "hammer_arm_r", 112, 48, 9, 13, -2, 5, 12, 6, 0, 0, 0, -0.12F, 0, 0);
        cube(p, "pillar_leg_l", 0, 64, -6, 21, -3, 6, 9, 6, 0, 0, 0, 0.08F, 0, 0);
        cube(p, "pillar_leg_r", 16, 64, 0, 21, -3, 6, 9, 6, 0, 0, 0, 0.08F, 0, 0);
        cube(p, "back_banner", 32, 64, -5, 9, 4, 10, 14, 2, 0, 0, 0, 0.2F, 0, 0);
    }

    private static void addCinderling(PartDefinition root) {
        PartDefinition p = form(root, "cinderling");
        cube(p, "charred_core", 48, 64, -5, 9, -4, 10, 10, 8);
        cube(p, "ember_face", 64, 64, -4, 3, -8, 8, 7, 6);
        cube(p, "flame_crown", 80, 64, -3, -4, -2, 6, 8, 4, 0, 0, 0, -0.15F, 0, 0);
        cube(p, "flame_tail", 96, 64, -3, 17, 3, 6, 9, 3, 0, 0, 0, 0.65F, 0, 0);
        cube(p, "coal_arm_l", 112, 64, -9, 10, -2, 4, 9, 4, 0, 0, 0, -0.55F, 0, -0.2F);
        cube(p, "coal_arm_r", 0, 80, 5, 10, -2, 4, 9, 4, 0, 0, 0, -0.55F, 0, 0.2F);
        cube(p, "coal_leg_l", 16, 80, -5, 18, -1, 4, 7, 4, 0, 0, 0, 0.2F, 0, 0);
        cube(p, "coal_leg_r", 32, 80, 1, 18, -1, 4, 7, 4, 0, 0, 0, 0.2F, 0, 0);
    }

    private static void addFrostling(PartDefinition root) {
        PartDefinition p = form(root, "frostling");
        cube(p, "crystal_body", 48, 80, -5, 8, -4, 10, 11, 8);
        cube(p, "ice_mask", 64, 80, -4, 0, -8, 8, 8, 6);
        cube(p, "crown", 80, 80, -2, -7, -2, 4, 8, 4, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "spike_l", 96, 80, -8, 6, -1, 3, 11, 3, 0, 0, 0, 0, 0, -0.42F);
        cube(p, "spike_r", 112, 80, 5, 6, -1, 3, 11, 3, 0, 0, 0, 0, 0, 0.42F);
        cube(p, "ice_arm_l", 0, 96, -9, 11, -2, 4, 9, 4, 0, 0, 0, -0.45F, 0, -0.12F);
        cube(p, "ice_arm_r", 16, 96, 5, 11, -2, 4, 9, 4, 0, 0, 0, -0.45F, 0, 0.12F);
        cube(p, "ice_leg_l", 32, 96, -5, 19, -1, 4, 8, 4, 0, 0, 0, 0.12F, 0, 0);
        cube(p, "ice_leg_r", 48, 96, 1, 19, -1, 4, 8, 4, 0, 0, 0, 0.12F, 0, 0);
    }

    private static void addHuskbrute(PartDefinition root) {
        PartDefinition p = form(root, "huskbrute");
        cube(p, "dried_torso", 64, 96, -7, 7, -4, 14, 14, 8);
        cube(p, "shell_head", 80, 96, -5, -4, -4, 10, 9, 8);
        cube(p, "shell_back", 96, 96, -7, 7, 3, 14, 13, 3, 0, 0, 0, -0.12F, 0, 0);
        cube(p, "jaw", 112, 96, -4, 4, -9, 8, 4, 7);
        cube(p, "arm_l", 0, 112, -12, 9, -2, 5, 14, 5, 0, 0, 0, -0.18F, 0, -0.15F);
        cube(p, "arm_r", 16, 112, 7, 9, -2, 5, 14, 5, 0, 0, 0, -0.18F, 0, 0.15F);
        cube(p, "leg_l", 32, 112, -6, 20, -2, 6, 9, 5, 0, 0, 0, 0.08F, 0, 0);
        cube(p, "leg_r", 48, 112, 0, 20, -2, 6, 9, 5, 0, 0, 0, 0.08F, 0, 0);
    }

    private static void addRavenor(PartDefinition root) {
        PartDefinition p = form(root, "ravenor");
        cube(p, "bird_body", 64, 112, -4, 8, -3, 8, 12, 6, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "beaked_head", 80, 112, -4, 1, -8, 8, 7, 8);
        cube(p, "beak", 96, 112, -2, 4, -14, 4, 3, 7);
        cube(p, "wing_l", 112, 112, -12, 6, 0, 8, 12, 3, 0, 0, 0, 0.2F, 0, -0.35F);
        cube(p, "wing_r", 0, 0, 4, 6, 0, 8, 12, 3, 0, 0, 0, 0.2F, 0, 0.35F);
        cube(p, "talon_l", 16, 0, -6, 19, -2, 4, 8, 4, 0, 0, 0, 0.72F, 0, -0.18F);
        cube(p, "talon_r", 32, 0, 2, 19, -2, 4, 8, 4, 0, 0, 0, 0.72F, 0, 0.18F);
        cube(p, "tail_feather", 48, 0, -2, 16, 3, 4, 12, 3, 0, 0, 0, 0.72F, 0, 0);
    }

    private static void addWailer(PartDefinition root) {
        PartDefinition p = form(root, "wailer");
        cube(p, "gaunt_body", 64, 0, -4, 6, -3, 8, 16, 6);
        cube(p, "long_neck", 80, 0, -2, -4, -2, 4, 12, 4);
        cube(p, "split_face", 96, 0, -4, -11, -5, 8, 9, 7);
        cube(p, "jaw", 112, 0, -3, -3, -9, 6, 4, 7);
        cube(p, "ribbon_l", 0, 16, -8, 9, 0, 3, 16, 3, 0, 0, 0, 0.12F, 0, -0.15F);
        cube(p, "ribbon_r", 16, 16, 5, 9, 0, 3, 16, 3, 0, 0, 0, 0.12F, 0, 0.15F);
        cube(p, "thin_arm_l", 32, 16, -9, 8, -1, 3, 16, 3, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "thin_arm_r", 48, 16, 6, 8, -1, 3, 16, 3, 0, 0, 0, -0.18F, 0, 0);
        cube(p, "needle_leg_l", 64, 16, -4, 21, -1, 3, 8, 3, 0, 0, 0, 0.1F, 0, 0);
        cube(p, "needle_leg_r", 80, 16, 1, 21, -1, 3, 8, 3, 0, 0, 0, 0.1F, 0, 0);
    }

    private static void addBurster(PartDefinition root) {
        PartDefinition p = form(root, "burster");
        cube(p, "mine_body", 96, 16, -7, 9, -7, 14, 12, 14);
        cube(p, "central_eye", 112, 16, -4, 5, -11, 8, 8, 5);
        cube(p, "fuse", 0, 32, -2, 0, -2, 4, 8, 4, 0, 0, 0, -0.35F, 0, 0);
        cube(p, "spike_front", 16, 32, -2, 12, -14, 4, 9, 5, 0, 0, 0, 0.8F, 0, 0);
        cube(p, "spike_back", 32, 32, -2, 12, 7, 4, 9, 5, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_l", 48, 32, -14, 12, -2, 5, 8, 4, 0, 0, 0, 0, 0, -0.8F);
        cube(p, "spike_r", 64, 32, 9, 12, -2, 5, 8, 4, 0, 0, 0, 0, 0, 0.8F);
        cube(p, "stub_l", 80, 32, -6, 20, -5, 4, 5, 4, 0, 0, 0, 0.28F, 0, 0);
        cube(p, "stub_r", 96, 32, 2, 20, -5, 4, 5, 4, 0, 0, 0, 0.28F, 0, 0);
    }

    private static void addGorehound(PartDefinition root) {
        PartDefinition p = form(root, "gorehound");
        cube(p, "canine_body", 112, 32, -5, 10, -3, 10, 8, 6, 0, 0, 0, 0.1F, 0, 0);
        cube(p, "hound_neck", 0, 48, -4, 3, -4, 8, 9, 6, 0, 0, 0, -0.28F, 0, 0);
        cube(p, "hound_head", 16, 48, -5, -3, -9, 10, 8, 9);
        cube(p, "muzzle", 32, 48, -3, 2, -15, 6, 4, 8);
        cube(p, "ear_l", 48, 48, -7, -5, -3, 4, 6, 3, 0, 0, 0, 0, 0, -0.35F);
        cube(p, "ear_r", 64, 48, 3, -5, -3, 4, 6, 3, 0, 0, 0, 0, 0, 0.35F);
        cube(p, "leg_fl", 80, 48, -6, 16, -2, 4, 10, 4, 0, 0, 0, 0.52F, 0, 0);
        cube(p, "leg_fr", 96, 48, 2, 16, -2, 4, 10, 4, 0, 0, 0, 0.52F, 0, 0);
        cube(p, "leg_bl", 112, 48, -6, 16, 1, 4, 10, 4, 0, 0, 0, -0.52F, 0, 0);
        cube(p, "leg_br", 0, 64, 2, 16, 1, 4, 10, 4, 0, 0, 0, -0.52F, 0, 0);
        cube(p, "tail", 16, 64, 1, 10, 3, 4, 3, 11, 0, 0, 0, -0.55F, 0, 0);
    }

    private static void addShadowmute(PartDefinition root) {
        PartDefinition p = form(root, "shadowmute");
        cube(p, "floating_cloak", 32, 64, -6, 8, -3, 12, 15, 6);
        cube(p, "hood", 48, 64, -5, -3, -4, 10, 10, 7);
        cube(p, "face_void", 64, 64, -4, 1, -9, 8, 7, 5);
        cube(p, "tendril_l", 80, 64, -9, 12, 0, 3, 13, 3, 0, 0, 0, 0.32F, 0, -0.2F);
        cube(p, "tendril_r", 96, 64, 6, 12, 0, 3, 13, 3, 0, 0, 0, 0.32F, 0, 0.2F);
        cube(p, "cloak_tail", 112, 64, -3, 20, 1, 6, 10, 3, 0, 0, 0, 0.4F, 0, 0);
        cube(p, "floating_hand_l", 0, 80, -11, 12, -2, 4, 7, 4, 0, 0, 0, -0.5F, 0, 0);
        cube(p, "floating_hand_r", 16, 80, 7, 12, -2, 4, 7, 4, 0, 0, 0, -0.5F, 0, 0);
    }

    private static void addBonelord(PartDefinition root) {
        PartDefinition p = form(root, "bonelord");
        cube(p, "rib_cage", 32, 80, -5, 7, -3, 10, 14, 6);
        cube(p, "skull", 48, 80, -5, -5, -5, 10, 9, 8);
        cube(p, "crown_l", 64, 80, -8, -10, -2, 4, 8, 3, 0, 0, 0, 0, 0, -0.28F);
        cube(p, "crown_r", 80, 80, 4, -10, -2, 4, 8, 3, 0, 0, 0, 0, 0, 0.28F);
        cube(p, "bone_arm_l", 96, 80, -10, 8, -1, 3, 16, 3, 0, 0, 0, -0.22F, 0, -0.12F);
        cube(p, "bone_arm_r", 112, 80, 7, 8, -1, 3, 16, 3, 0, 0, 0, -0.22F, 0, 0.12F);
        cube(p, "bone_leg_l", 0, 96, -4, 20, -1, 3, 9, 3, 0, 0, 0, 0.1F, 0, 0);
        cube(p, "bone_leg_r", 16, 96, 1, 20, -1, 3, 9, 3, 0, 0, 0, 0.1F, 0, 0);
        cube(p, "vertebrae_cape", 32, 96, -5, 10, 3, 10, 16, 2, 0, 0, 0, 0.2F, 0, 0);
    }

    private static void addSpinewalker(PartDefinition root) {
        PartDefinition p = form(root, "spinewalker");
        cube(p, "vertebra_body", 48, 96, -4, 8, -3, 8, 12, 6);
        cube(p, "spine_neck", 64, 96, -3, -2, 0, 6, 11, 4, 0, 0, 0, -0.3F, 0, 0);
        cube(p, "spine_head", 80, 96, -4, -9, -6, 8, 8, 8);
        cube(p, "jaw", 96, 96, -3, -2, -11, 6, 4, 6);
        cube(p, "spike_0", 112, 96, -2, 1, 4, 4, 7, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_1", 0, 112, -2, 6, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_2", 16, 112, -2, 11, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "spike_3", 32, 112, -2, 16, 4, 4, 8, 4, 0, 0, 0, -0.8F, 0, 0);
        cube(p, "leg_l", 48, 112, -5, 19, -1, 4, 9, 4, 0, 0, 0, 0.18F, 0, 0);
        cube(p, "leg_r", 64, 112, 1, 19, -1, 4, 9, 4, 0, 0, 0, 0.18F, 0, 0);
        cube(p, "arm_l", 80, 112, -9, 9, -1, 3, 12, 3, 0, 0, 0, -0.38F, 0, 0);
        cube(p, "arm_r", 96, 112, 6, 9, -1, 3, 12, 3, 0, 0, 0, -0.38F, 0, 0);
    }

    @Override
    public void setupAnim(SixtySecondsMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart form : forms) {
            form.visible = false;
            form.xRot = 0.0F;
            form.yRot = 0.0F;
            form.zRot = 0.0F;
            form.y = 0.0F;
        }
        int id = entity.getVariant().id;
        ModelPart active = forms[Math.max(0, Math.min(forms.length - 1, id))];
        active.visible = true;
        // 让新模型整体保持轻微呼吸和悬浮感；几何本身仍由每个变体独立定义。
        active.y = Mth.sin(ageInTicks * 0.08F + id) * (id == 15 ? 0.55F : 0.08F);
        active.xRot = headPitch * Mth.DEG_TO_RAD * 0.03F;
        active.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.03F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
