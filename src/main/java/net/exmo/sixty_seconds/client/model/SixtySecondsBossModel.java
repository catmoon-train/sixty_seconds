package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 末日60秒 Boss 模型（重构后）：<b>1 个僵尸人形 Boss + 9 个独立建模 Boss</b>。
 *
 * <ul>
 *   <li><b>僵尸人形组</b> {@code humanoid}：仅 {@code RAVAGER} 破坏者保留（唯一仍为僵尸外观的 Boss），
 *       沿用原版人形 UV 布局。</li>
 *   <li><b>9 个独立几何组</b>：colossus / necromancer / plaguebearer / specter /
 *       inferno / frostbite / swarmkeeper / stormherald / voidweaver，各自完全独立建模，
 *       不再使用僵尸人形 + 特征层的做法。</li>
 * </ul>
 *
 * <p>⚠ <b>texOffs 必须为字面量</b>：贴图由 {@code tools/gen_boss_textures.py} 正则解析本文件生成。
 * <p>每个 Boss 独占一张 128×128 贴图（{@code sixty_seconds_boss_<name>.png}）。
 */
public class SixtySecondsBossModel extends EntityModel<SixtySecondsBossEntity> {

    private final ModelPart root;

    // 僵尸人形组（RAVAGER）
    private final ModelPart humanoid;
    private final ModelPart hHead, hArmL, hArmR, hLegL, hLegR;

    // 9 个独立几何组
    private final ModelPart colossus, necromancer, plaguebearer, specter, inferno;
    private final ModelPart frostbite, swarmkeeper, stormherald, voidweaver;

    // 动画子部件
    private final ModelPart colArmL, colArmR, colHead, colCore;
    private final ModelPart necHood, necStaff, necRobe;
    private final ModelPart plaHump, plaSacL, plaSacR, plaHead;
    private final ModelPart speRobe, speArmL, speArmR, speHood;
    private final ModelPart infCore, infArmL, infArmR, infMane;
    private final ModelPart froCrown, froShieldL, froShieldR;
    private final ModelPart swaAbdomen, swaClawL, swaClawR;
    private final ModelPart stoWingL, stoWingR, stoHornL, stoHornR;
    private final ModelPart voiCore, voiHood;
    private final ModelPart[] voiTendrils = new ModelPart[4];

    public SixtySecondsBossModel(ModelPart root) {
        this.root = root;
        this.humanoid = root.getChild("humanoid");
        this.hHead = humanoid.getChild("head");
        this.hArmL = humanoid.getChild("arm_l");
        this.hArmR = humanoid.getChild("arm_r");
        this.hLegL = humanoid.getChild("leg_l");
        this.hLegR = humanoid.getChild("leg_r");

        this.colossus = root.getChild("colossus");
        this.colArmL = colossus.getChild("arm_l");
        this.colArmR = colossus.getChild("arm_r");
        this.colHead = colossus.getChild("head");
        this.colCore = colossus.getChild("core");

        this.necromancer = root.getChild("necromancer");
        this.necHood = necromancer.getChild("hood");
        this.necStaff = necromancer.getChild("staff");
        this.necRobe = necromancer.getChild("robe");

        this.plaguebearer = root.getChild("plaguebearer");
        this.plaHump = plaguebearer.getChild("hump");
        this.plaSacL = plaguebearer.getChild("sac_l");
        this.plaSacR = plaguebearer.getChild("sac_r");
        this.plaHead = plaguebearer.getChild("head");

        this.specter = root.getChild("specter");
        this.speRobe = specter.getChild("robe");
        this.speArmL = specter.getChild("arm_l");
        this.speArmR = specter.getChild("arm_r");
        this.speHood = specter.getChild("hood");

        this.inferno = root.getChild("inferno");
        this.infCore = inferno.getChild("core");
        this.infArmL = inferno.getChild("arm_l");
        this.infArmR = inferno.getChild("arm_r");
        this.infMane = inferno.getChild("mane");

        this.frostbite = root.getChild("frostbite");
        this.froCrown = frostbite.getChild("crown");
        this.froShieldL = frostbite.getChild("shield_l");
        this.froShieldR = frostbite.getChild("shield_r");

        this.swarmkeeper = root.getChild("swarmkeeper");
        this.swaAbdomen = swarmkeeper.getChild("abdomen");
        this.swaClawL = swarmkeeper.getChild("claw_l");
        this.swaClawR = swarmkeeper.getChild("claw_r");

        this.stormherald = root.getChild("stormherald");
        this.stoWingL = stormherald.getChild("wing_l");
        this.stoWingR = stormherald.getChild("wing_r");
        this.stoHornL = stormherald.getChild("horn_l");
        this.stoHornR = stormherald.getChild("horn_r");

        this.voidweaver = root.getChild("voidweaver");
        this.voiCore = voidweaver.getChild("core");
        this.voiHood = voidweaver.getChild("hood");
        for (int i = 0; i < 4; i++) voiTendrils[i] = voidweaver.getChild("tendril" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ══════════════════════════════════════════════════════
        // 僵尸人形组（仅 RAVAGER 破坏者使用）
        // ══════════════════════════════════════════════════════
        PartDefinition hum = root.addOrReplaceChild("humanoid", CubeListBuilder.create(), PartPose.ZERO);
        hum.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        hum.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        hum.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 0.0F, 0.0F));
        hum.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(40, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 0.0F, 0.0F));
        hum.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        hum.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        // ══════════════════════════════════════════════════════
        // COLOSSUS 巨像：巨型岩石/金属躯干 + 巨肩 + 小头 + 熔核 + 粗腿
        // ══════════════════════════════════════════════════════
        PartDefinition co = root.addOrReplaceChild("colossus", CubeListBuilder.create(), PartPose.ZERO);
        co.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -14.0F, -6.0F, 18.0F, 22.0F, 12.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        co.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 3.0F),
                PartPose.offset(0.0F, 2.0F, -6.0F));
        co.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 46).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, -6.0F, -2.0F));
        co.addOrReplaceChild("shoulder_l",
                CubeListBuilder.create().texOffs(0, 62).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 9.0F, 12.0F),
                PartPose.offset(10.0F, -6.0F, 0.0F));
        co.addOrReplaceChild("shoulder_r",
                CubeListBuilder.create().texOffs(40, 62).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 9.0F, 12.0F),
                PartPose.offset(-10.0F, -6.0F, 0.0F));
        co.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(80, 62).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offsetAndRotation(10.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        co.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(0, 84).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offsetAndRotation(-10.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        co.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(30, 84).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offset(5.0F, 18.0F, 0.0F));
        co.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(60, 84).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offset(-5.0F, 18.0F, 0.0F));

        // ══════════════════════════════════════════════════════
        // NECROMANCER 亡灵术士：瘦高躯干 + 兜帽 + 法杖 + 长袍 + 亡灵光

        PartDefinition necromancer = root.addOrReplaceChild("necromancer", CubeListBuilder.create(), PartPose.ZERO);
        necromancer.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -3.0F, 8.0F, 16.0F, 6.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        necromancer.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(29, 0).addBox(-6.0F, 0.0F, -5.0F, 12.0F, 16.0F, 10.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        necromancer.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(74, 0).addBox(-6.5F, 0.0F, -5.5F, 13.0F, 2.0F, 11.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        necromancer.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 27).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 8.0F, 10.0F), PartPose.offset(0.0F, 1.0F, 0.0F));
        necromancer.addOrReplaceChild("face", CubeListBuilder.create().texOffs(41, 27).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 2.0F, -1.0F));
        necromancer.addOrReplaceChild("skullgem", CubeListBuilder.create().texOffs(52, 27).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 1.0F), PartPose.offset(0.0F, 4.0F, -4.5F));
        necromancer.addOrReplaceChild("pauldron_l", CubeListBuilder.create().texOffs(63, 27).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(6.0F, 4.0F, 0.0F));
        necromancer.addOrReplaceChild("pauldron_r", CubeListBuilder.create().texOffs(88, 27).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 5.0F, 6.0F), PartPose.offset(-6.0F, 4.0F, 0.0F));
        necromancer.addOrReplaceChild("staff", CubeListBuilder.create().texOffs(113, 27).addBox(-0.5F, -18.0F, -0.5F, 1.0F, 28.0F, 1.0F), PartPose.offsetAndRotation(6.0F, 14.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        necromancer.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 57).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(6.0F, -5.0F, 0.0F));
        necromancer.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(17, 57).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 12.0F, 2.0F), PartPose.offsetAndRotation(-5.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        // PLAGUEBEARER 疫病者：驼背巨囊 + 双侧毒腺 + 破布长袍 + 面罩
        // ══════════════════════════════════════════════════════
        PartDefinition pl = root.addOrReplaceChild("plaguebearer", CubeListBuilder.create(), PartPose.ZERO);
        pl.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -10.0F, -4.0F, 10.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        pl.addOrReplaceChild("hump",
                CubeListBuilder.create().texOffs(0, 26).addBox(-6.0F, -8.0F, -4.0F, 12.0F, 10.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 10.0F, 4.0F, 0.25F, 0.0F, 0.0F));
        pl.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 48).addBox(-3.5F, -4.0F, -4.0F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 3.0F, -2.0F));
        pl.addOrReplaceChild("mask",
                CubeListBuilder.create().texOffs(0, 64).addBox(-3.0F, -1.0F, -5.0F, 6.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 5.0F, -3.0F));
        pl.addOrReplaceChild("sac_l",
                CubeListBuilder.create().texOffs(0, 72).addBox(0.0F, -3.0F, -3.0F, 4.0F, 6.0F, 6.0F),
                PartPose.offset(5.0F, 8.0F, 2.0F));
        pl.addOrReplaceChild("sac_r",
                CubeListBuilder.create().texOffs(22, 72).addBox(-4.0F, -3.0F, -3.0F, 4.0F, 6.0F, 6.0F),
                PartPose.offset(-5.0F, 8.0F, 2.0F));
        pl.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(44, 72).addBox(-5.5F, 0.0F, -4.5F, 11.0F, 14.0F, 9.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        pl.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(76, 72).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offsetAndRotation(6.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.18F));
        pl.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(88, 72).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offsetAndRotation(-6.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.18F));

        // ══════════════════════════════════════════════════════
        // SPECTER 鬼魅：无腿幽魂 + 破烂长袍 + 兜帽 + 长爪臂

        PartDefinition specter = root.addOrReplaceChild("specter", CubeListBuilder.create(), PartPose.ZERO);
        specter.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -11.0F, -3.5F, 9.0F, 14.0F, 7.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        specter.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(33, 0).addBox(-6.5F, 0.0F, -5.0F, 13.0F, 18.0F, 10.0F), PartPose.offset(0.0F, 15.0F, 0.0F));
        specter.addOrReplaceChild("hem_l", CubeListBuilder.create().texOffs(80, 0).addBox(-3.0F, 14.0F, -3.0F, 3.0F, 4.0F, 6.0F), PartPose.offset(-4.0F, 13.0F, 0.0F));
        specter.addOrReplaceChild("hem_r", CubeListBuilder.create().texOffs(99, 0).addBox(-3.0F, 14.0F, -3.0F, 3.0F, 4.0F, 6.0F), PartPose.offset(4.0F, 13.0F, 0.0F));
        specter.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 29).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 7.0F, 10.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        specter.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(41, 29).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 2.0F, 1.0F), PartPose.offset(0.0F, 1.0F, -1.0F));
        specter.addOrReplaceChild("wisp_l", CubeListBuilder.create().texOffs(52, 29).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(-6.0F, 6.0F, 0.0F));
        specter.addOrReplaceChild("wisp_r", CubeListBuilder.create().texOffs(61, 29).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offset(6.0F, 6.0F, 0.0F));
        specter.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(70, 29).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        specter.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(75, 29).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 14.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        specter.addOrReplaceChild("claw_l", CubeListBuilder.create().texOffs(80, 29).addBox(-1.0F, 12.0F, -2.0F, 2.0F, 3.0F, 3.0F), PartPose.offset(5.0F, 3.0F, 0.0F));
        specter.addOrReplaceChild("claw_r", CubeListBuilder.create().texOffs(91, 29).addBox(-1.0F, 12.0F, -2.0F, 2.0F, 3.0F, 3.0F), PartPose.offset(-5.0F, 3.0F, 0.0F));
        // INFERNO 熔渊暴君：熔岩躯干 + 发光核心 + 火焰鬃毛 + 巨臂
        // ══════════════════════════════════════════════════════
        PartDefinition in = root.addOrReplaceChild("inferno", CubeListBuilder.create(), PartPose.ZERO);
        in.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -12.0F, -5.0F, 14.0F, 20.0F, 10.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        in.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 3.0F, -5.0F));
        in.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 42).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -3.0F, -2.0F));
        in.addOrReplaceChild("mane",
                CubeListBuilder.create().texOffs(0, 60).addBox(-6.0F, -5.0F, -1.0F, 12.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, -2.0F, 3.0F));
        in.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(0, 78).addBox(0.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -6.0F, -1.0F, 0.0F, 0.0F, -0.4F));
        in.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(10, 78).addBox(-2.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -6.0F, -1.0F, 0.0F, 0.0F, 0.4F));
        in.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(20, 78).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 15.0F, 5.0F),
                PartPose.offsetAndRotation(8.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.18F));
        in.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(42, 78).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 15.0F, 5.0F),
                PartPose.offsetAndRotation(-8.0F, 2.0F, 0.0F, 0.0F, 0.0F, -0.18F));
        in.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(64, 78).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F),
                PartPose.offset(4.0F, 19.0F, 0.0F));
        in.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(86, 78).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 10.0F, 6.0F),
                PartPose.offset(-4.0F, 19.0F, 0.0F));

        // ══════════════════════════════════════════════════════
        // FROSTBITE 霜噬守望：冰甲巨躯 + 冰晶头冠 + 双冰盾肩 + 冰刺

        PartDefinition frostbite = root.addOrReplaceChild("frostbite", CubeListBuilder.create(), PartPose.ZERO);
        frostbite.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -12.0F, -4.5F, 14.0F, 18.0F, 9.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        frostbite.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(47, 0).addBox(-6.0F, -8.0F, -5.5F, 12.0F, 10.0F, 3.0F), PartPose.offset(0.0F, 9.0F, -4.5F));
        frostbite.addOrReplaceChild("chestgem", CubeListBuilder.create().texOffs(78, 0).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F), PartPose.offset(0.0F, 6.0F, -5.6F));
        frostbite.addOrReplaceChild("head", CubeListBuilder.create().texOffs(91, 0).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 7.0F, 7.0F), PartPose.offset(0.0F, -4.0F, -1.0F));
        frostbite.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 28).addBox(-4.5F, -4.0F, -4.0F, 9.0F, 4.0F, 8.0F), PartPose.offset(0.0F, -7.0F, -1.0F));
        frostbite.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(35, 28).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 9.0F, 12.0F), PartPose.offset(10.0F, -6.0F, 0.0F));
        frostbite.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(76, 28).addBox(-4.0F, -5.0F, -6.0F, 8.0F, 9.0F, 12.0F), PartPose.offset(-10.0F, -6.0F, 0.0F));
        frostbite.addOrReplaceChild("shield_l", CubeListBuilder.create().texOffs(0, 50).addBox(-2.0F, -6.0F, -5.0F, 5.0F, 11.0F, 10.0F), PartPose.offsetAndRotation(8.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        frostbite.addOrReplaceChild("shield_r", CubeListBuilder.create().texOffs(31, 50).addBox(-3.0F, -6.0F, -5.0F, 5.0F, 11.0F, 10.0F), PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        frostbite.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(62, 50).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offsetAndRotation(9.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        frostbite.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(75, 50).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offsetAndRotation(-9.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        frostbite.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(88, 50).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F), PartPose.offset(3.5F, 18.0F, 0.0F));
        frostbite.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(0, 72).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F), PartPose.offset(-3.5F, 18.0F, 0.0F));
        frostbite.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(21, 72).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 4.0F, 3.5F, 0.4F, 0.0F, 0.25F));
        frostbite.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(30, 72).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 4.0F, 0.5F, 0.0F, 0.0F));
        frostbite.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(39, 72).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 4.0F, 3.5F, 0.4F, 0.0F, -0.25F));
        // SWARMKEEPER 虫潮之主：甲壳躯干 + 分节腹囊 + 双螯肢 + 6 足

        PartDefinition swarmkeeper = root.addOrReplaceChild("swarmkeeper", CubeListBuilder.create(), PartPose.ZERO);
        swarmkeeper.addOrReplaceChild("thorax", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 12.0F, 12.0F), PartPose.offset(0.0F, 15.0F, -2.0F));
        swarmkeeper.addOrReplaceChild("seg1", CubeListBuilder.create().texOffs(49, 0).addBox(-4.5F, -4.0F, 1.0F, 9.0F, 8.0F, 6.0F), PartPose.offset(0.0F, 15.0F, 2.0F));
        swarmkeeper.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 25).addBox(-5.0F, -5.0F, 0.0F, 10.0F, 10.0F, 14.0F), PartPose.offsetAndRotation(0.0F, 15.0F, 4.0F, 0.15F, 0.0F, 0.0F));
        swarmkeeper.addOrReplaceChild("seg2", CubeListBuilder.create().texOffs(49, 25).addBox(-3.5F, -3.0F, 9.0F, 7.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 15.0F, 9.0F, 0.3F, 0.0F, 0.0F));
        swarmkeeper.addOrReplaceChild("head", CubeListBuilder.create().texOffs(74, 25).addBox(-3.5F, -3.5F, -5.0F, 7.0F, 7.0F, 6.0F), PartPose.offset(0.0F, 15.0F, -7.0F));
        swarmkeeper.addOrReplaceChild("mandible_l", CubeListBuilder.create().texOffs(101, 25).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(-3.0F, 14.0F, -9.0F, 0.0F, 0.3F, 0.0F));
        swarmkeeper.addOrReplaceChild("mandible_r", CubeListBuilder.create().texOffs(114, 25).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(3.0F, 14.0F, -9.0F, 0.0F, -0.3F, 0.0F));
        swarmkeeper.addOrReplaceChild("claw_l", CubeListBuilder.create().texOffs(0, 50).addBox(0.0F, -1.5F, -6.0F, 3.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(4.5F, 15.0F, -6.0F, 0.0F, 0.35F, 0.0F));
        swarmkeeper.addOrReplaceChild("claw_r", CubeListBuilder.create().texOffs(21, 50).addBox(-3.0F, -1.5F, -6.0F, 3.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-4.5F, 15.0F, -6.0F, 0.0F, -0.35F, 0.0F));
        swarmkeeper.addOrReplaceChild("leg0", CubeListBuilder.create().texOffs(42, 50).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 18.0F, -4.0F, 0.0F, 0.5F, -0.35F));
        swarmkeeper.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(59, 50).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 18.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        swarmkeeper.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(76, 50).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 18.0F, 4.0F, 0.0F, -0.5F, -0.35F));
        swarmkeeper.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(93, 50).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 18.0F, -4.0F, 0.0F, -0.5F, 0.35F));
        swarmkeeper.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(110, 50).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        swarmkeeper.addOrReplaceChild("leg5", CubeListBuilder.create().texOffs(0, 61).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 18.0F, 4.0F, 0.0F, 0.5F, 0.35F));
        swarmkeeper.addOrReplaceChild("stinger", CubeListBuilder.create().texOffs(17, 61).addBox(-1.0F, -4.0F, -2.0F, 2.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 11.0F, -8.5F, 0.0F, 0.0F, 0.0F));
        // STORMHERALD 雷霆传令：带电躯干 + 双雷翼 + 雷角 + 悬浮下摆

        PartDefinition stormherald = root.addOrReplaceChild("stormherald", CubeListBuilder.create(), PartPose.ZERO);
        stormherald.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 16.0F, 8.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        stormherald.addOrReplaceChild("core", CubeListBuilder.create().texOffs(37, 0).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 3.0F), PartPose.offset(0.0F, 3.0F, -5.0F));
        stormherald.addOrReplaceChild("head", CubeListBuilder.create().texOffs(54, 0).addBox(-3.5F, -4.0F, -4.0F, 7.0F, 7.0F, 7.0F), PartPose.offset(0.0F, 1.0F, -1.0F));
        stormherald.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(83, 0).addBox(0.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(2.5F, -2.0F, 0.0F, -0.3F, 0.0F, -0.35F));
        stormherald.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(92, 0).addBox(-2.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(-2.5F, -2.0F, 0.0F, -0.3F, 0.0F, 0.35F));
        stormherald.addOrReplaceChild("wing_l", CubeListBuilder.create().texOffs(0, 25).addBox(0.0F, -8.0F, -0.5F, 14.0F, 16.0F, 1.0F), PartPose.offsetAndRotation(4.0F, 2.0F, 2.5F, 0.0F, 0.0F, -0.25F));
        stormherald.addOrReplaceChild("wing_r", CubeListBuilder.create().texOffs(31, 25).addBox(-14.0F, -8.0F, -0.5F, 14.0F, 16.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 2.0F, 2.5F, 0.0F, 0.0F, 0.25F));
        stormherald.addOrReplaceChild("shoulder_l", CubeListBuilder.create().texOffs(62, 25).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 7.0F, 8.0F), PartPose.offset(8.0F, 2.0F, 0.0F));
        stormherald.addOrReplaceChild("shoulder_r", CubeListBuilder.create().texOffs(95, 25).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 7.0F, 8.0F), PartPose.offset(-8.0F, 2.0F, 0.0F));
        stormherald.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(0, 43).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offsetAndRotation(6.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        stormherald.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(13, 43).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        stormherald.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(26, 43).addBox(-5.5F, 0.0F, -4.5F, 11.0F, 12.0F, 9.0F), PartPose.offset(0.0F, 18.0F, 0.0F));
        stormherald.addOrReplaceChild("bolt", CubeListBuilder.create().texOffs(67, 43).addBox(-0.5F, -16.0F, -0.5F, 1.0F, 6.0F, 1.0F), PartPose.offset(6.0F, 4.0F, -2.0F));
        // VOIDWEAVER 虚空织者：悬浮虚空长袍 + 无面兜帽 + 悬浮核心 + 4 触须

        PartDefinition voidweaver = root.addOrReplaceChild("voidweaver", CubeListBuilder.create(), PartPose.ZERO);
        voidweaver.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 15.0F, 8.0F), PartPose.offset(0.0F, 12.0F, 0.0F));
        voidweaver.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(37, 0).addBox(-7.0F, 0.0F, -5.5F, 14.0F, 16.0F, 11.0F), PartPose.offset(0.0F, 16.0F, 0.0F));
        voidweaver.addOrReplaceChild("hood", CubeListBuilder.create().texOffs(0, 28).addBox(-5.5F, -6.0F, -5.5F, 11.0F, 8.0F, 11.0F), PartPose.offset(0.0F, 0.0F, 0.0F));
        voidweaver.addOrReplaceChild("void_face", CubeListBuilder.create().texOffs(45, 28).addBox(-2.5F, -1.5F, -4.5F, 5.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 1.0F, -1.0F));
        voidweaver.addOrReplaceChild("core", CubeListBuilder.create().texOffs(58, 28).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offset(0.0F, 6.0F, -5.0F));
        voidweaver.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(75, 28).addBox(-6.0F, -1.0F, -1.0F, 12.0F, 1.0F, 1.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        voidweaver.addOrReplaceChild("mote_l", CubeListBuilder.create().texOffs(102, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(7.0F, 4.0F, 2.0F));
        voidweaver.addOrReplaceChild("mote_r", CubeListBuilder.create().texOffs(115, 28).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(-7.0F, 4.0F, 2.0F));
        voidweaver.addOrReplaceChild("tendril0", CubeListBuilder.create().texOffs(0, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(5.0F, 8.0F, 2.0F, 0.3F, 0.0F, -0.3F));
        voidweaver.addOrReplaceChild("tendril1", CubeListBuilder.create().texOffs(5, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, 8.0F, 2.0F, 0.3F, 0.0F, 0.3F));
        voidweaver.addOrReplaceChild("tendril2", CubeListBuilder.create().texOffs(10, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(3.0F, 6.0F, 4.0F, 0.5F, 0.0F, -0.2F));
        voidweaver.addOrReplaceChild("tendril3", CubeListBuilder.create().texOffs(15, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 6.0F, 4.0F, 0.5F, 0.0F, 0.2F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SixtySecondsBossEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        humanoid.visible = false;
        colossus.visible = false; necromancer.visible = false; plaguebearer.visible = false;
        specter.visible = false; inferno.visible = false; frostbite.visible = false;
        swarmkeeper.visible = false; stormherald.visible = false; voidweaver.visible = false;

        float t = ageInTicks * 0.13F;
        SixtySecondsBossEntity.BossVariant v = entity.getBossVariant();

        switch (v) {
            case RAVAGER -> {
                humanoid.visible = true;
                hHead.yRot = netHeadYaw * Mth.DEG_TO_RAD;
                hHead.xRot = headPitch * Mth.DEG_TO_RAD;
                float sw = limbSwing * 0.6662F;
                hArmR.xRot = Mth.cos(sw + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                hArmL.xRot = Mth.cos(sw) * 2.0F * limbSwingAmount * 0.5F;
                hLegR.xRot = Mth.cos(sw) * 1.4F * limbSwingAmount;
                hLegL.xRot = Mth.cos(sw + (float) Math.PI) * 1.4F * limbSwingAmount;
            }
            case COLOSSUS -> {
                colossus.visible = true;
                colHead.yRot = Mth.sin(t * 0.3F) * 0.10F;
                colArmL.xRot = Mth.cos(limbSwing * 0.5F) * 0.5F * limbSwingAmount;
                colArmR.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.5F * limbSwingAmount;
                colCore.xScale = colCore.yScale = colCore.zScale = 1.0F + Mth.sin(t * 1.6F) * 0.12F;
            }
            case NECROMANCER -> {
                necromancer.visible = true;
                necHood.yRot = Mth.sin(t * 0.4F) * 0.10F;
                necStaff.zRot = -0.12F + Mth.sin(t * 0.7F) * 0.06F;
                necRobe.xRot = Mth.sin(t * 0.5F) * 0.04F;
            }
            case PLAGUEBEARER -> {
                plaguebearer.visible = true;
                plaHump.xScale = plaHump.yScale = plaHump.zScale = 1.0F + Mth.sin(t * 0.9F) * 0.07F;
                plaSacL.yScale = 1.0F + Mth.sin(t * 1.1F) * 0.10F;
                plaSacR.yScale = 1.0F + Mth.sin(t * 1.1F + 1.5F) * 0.10F;
                plaHead.xRot = Mth.sin(t * 0.5F) * 0.08F;
            }
            case SPECTER -> {
                specter.visible = true;
                specter.y = Mth.sin(t * 0.6F) * 1.0F;              // 整体悬浮
                speHood.yRot = Mth.sin(t * 0.35F) * 0.12F;
                speArmL.zRot = 0.15F + Mth.sin(t * 0.8F) * 0.10F;
                speArmR.zRot = -0.15F - Mth.sin(t * 0.8F) * 0.10F;
                speRobe.xRot = Mth.sin(t * 0.7F) * 0.06F;
            }
            case INFERNO -> {
                inferno.visible = true;
                infCore.xScale = infCore.yScale = infCore.zScale = 1.0F + Mth.sin(t * 2.0F) * 0.16F;
                infMane.yScale = 1.0F + Mth.sin(t * 2.4F) * 0.14F;
                infArmL.xRot = Mth.cos(limbSwing * 0.55F) * 0.6F * limbSwingAmount;
                infArmR.xRot = Mth.cos(limbSwing * 0.55F + (float) Math.PI) * 0.6F * limbSwingAmount;
            }
            case FROSTBITE -> {
                frostbite.visible = true;
                froCrown.yRot = Mth.sin(t * 0.35F) * 0.08F;
                froShieldL.zRot = Mth.sin(t * 0.6F) * 0.05F;
                froShieldR.zRot = -Mth.sin(t * 0.6F) * 0.05F;
            }
            case SWARMKEEPER -> {
                swarmkeeper.visible = true;
                swaAbdomen.xScale = swaAbdomen.yScale = swaAbdomen.zScale = 1.0F + Mth.sin(t * 1.3F) * 0.08F;
                swaClawL.zRot = 0.25F + Mth.sin(t * 1.5F) * 0.14F;
                swaClawR.zRot = -0.25F - Mth.sin(t * 1.5F) * 0.14F;
            }
            case STORMHERALD -> {
                stormherald.visible = true;
                float flap = Mth.sin(t * 1.2F) * 0.30F;
                stoWingL.zRot = -0.25F + flap;
                stoWingR.zRot = 0.25F - flap;
                stoHornL.zRot = -0.35F + Mth.sin(t * 1.8F) * 0.06F;
                stoHornR.zRot = 0.35F - Mth.sin(t * 1.8F) * 0.06F;
            }
            case VOIDWEAVER -> {
                voidweaver.visible = true;
                voidweaver.y = Mth.sin(t * 0.5F) * 0.8F;
                voiCore.xScale = voiCore.yScale = voiCore.zScale = 1.0F + Mth.sin(t * 1.8F) * 0.14F;
                voiHood.yRot = Mth.sin(t * 0.3F) * 0.10F;
                for (int i = 0; i < 4; i++) {
                    voiTendrils[i].xRot = Mth.sin(t * 1.1F + i * 0.9F) * 0.28F;
                }
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
