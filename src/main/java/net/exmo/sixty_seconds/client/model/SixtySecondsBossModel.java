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
                PartPose.offset(0.0F, 16.0F, 0.0F));
        hum.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        hum.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(-5.0F, 18.0F, 0.0F));
        hum.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(40, 32).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offset(5.0F, 18.0F, 0.0F));
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
        // ══════════════════════════════════════════════════════
        PartDefinition ne = root.addOrReplaceChild("necromancer", CubeListBuilder.create(), PartPose.ZERO);
        ne.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -3.0F, 8.0F, 16.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        ne.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, 0.0F, -5.0F, 12.0F, 16.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        ne.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 52).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 1.0F, 0.0F));
        ne.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 72).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, -1.0F));
        ne.addOrReplaceChild("staff",
                CubeListBuilder.create().texOffs(0, 78).addBox(-0.5F, -18.0F, -0.5F, 1.0F, 28.0F, 1.0F),
                PartPose.offsetAndRotation(6.0F, 14.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        ne.addOrReplaceChild("orb",
                CubeListBuilder.create().texOffs(10, 78).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(6.0F, -5.0F, 0.0F));
        ne.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(24, 78).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.12F));

        // ══════════════════════════════════════════════════════
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
        // ══════════════════════════════════════════════════════
        PartDefinition sp = root.addOrReplaceChild("specter", CubeListBuilder.create(), PartPose.ZERO);
        sp.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -11.0F, -3.5F, 9.0F, 14.0F, 7.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        sp.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(0, 22).addBox(-6.5F, 0.0F, -5.0F, 13.0F, 18.0F, 10.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        sp.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 52).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 7.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        sp.addOrReplaceChild("glow",
                CubeListBuilder.create().texOffs(0, 70).addBox(-2.0F, -1.0F, -4.0F, 4.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 1.0F, -1.0F));
        sp.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 74).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        sp.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(8, 74).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        sp.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(16, 74).addBox(-1.0F, 12.0F, -2.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(5.0F, 3.0F, 0.0F));
        sp.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(28, 74).addBox(-1.0F, 12.0F, -2.0F, 2.0F, 3.0F, 3.0F),
                PartPose.offset(-5.0F, 3.0F, 0.0F));

        // ══════════════════════════════════════════════════════
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
        // ══════════════════════════════════════════════════════
        PartDefinition fz = root.addOrReplaceChild("frostbite", CubeListBuilder.create(), PartPose.ZERO);
        fz.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -12.0F, -4.5F, 14.0F, 18.0F, 9.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        fz.addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(0, 30).addBox(-6.0F, -8.0F, -5.5F, 12.0F, 10.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, -4.5F));
        fz.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 46).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, -4.0F, -1.0F));
        fz.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 62).addBox(-4.5F, -4.0F, -4.0F, 9.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, -7.0F, -1.0F));
        fz.addOrReplaceChild("shield_l",
                CubeListBuilder.create().texOffs(0, 76).addBox(-2.0F, -6.0F, -5.0F, 5.0F, 11.0F, 10.0F),
                PartPose.offsetAndRotation(8.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        fz.addOrReplaceChild("shield_r",
                CubeListBuilder.create().texOffs(32, 76).addBox(-3.0F, -6.0F, -5.0F, 5.0F, 11.0F, 10.0F),
                PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        fz.addOrReplaceChild("spike0",
                CubeListBuilder.create().texOffs(64, 76).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 4.0F, 3.5F, 0.4F, 0.0F, 0.25F));
        fz.addOrReplaceChild("spike1",
                CubeListBuilder.create().texOffs(74, 76).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 4.0F, 0.5F, 0.0F, 0.0F));
        fz.addOrReplaceChild("spike2",
                CubeListBuilder.create().texOffs(84, 76).addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 4.0F, 3.5F, 0.4F, 0.0F, -0.25F));
        fz.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(94, 76).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(3.5F, 18.0F, 0.0F));
        fz.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 94).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-3.5F, 18.0F, 0.0F));

        // ══════════════════════════════════════════════════════
        // SWARMKEEPER 虫潮之主：甲壳躯干 + 分节腹囊 + 双螯肢 + 6 足
        // ══════════════════════════════════════════════════════
        PartDefinition sk = root.addOrReplaceChild("swarmkeeper", CubeListBuilder.create(), PartPose.ZERO);
        sk.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        sk.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 26).addBox(-5.0F, -5.0F, 0.0F, 10.0F, 10.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, 15.0F, 4.0F, 0.15F, 0.0F, 0.0F));
        sk.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 52).addBox(-3.5F, -3.5F, -5.0F, 7.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, -7.0F));
        sk.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(0, 66).addBox(0.0F, -1.5F, -6.0F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(4.5F, 15.0F, -6.0F, 0.0F, 0.35F, 0.0F));
        sk.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(22, 66).addBox(-3.0F, -1.5F, -6.0F, 3.0F, 3.0F, 7.0F),
                PartPose.offsetAndRotation(-4.5F, 15.0F, -6.0F, 0.0F, -0.35F, 0.0F));
        sk.addOrReplaceChild("leg0",
                CubeListBuilder.create().texOffs(44, 66).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 18.0F, -4.0F, 0.0F, 0.5F, -0.35F));
        sk.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(60, 66).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 18.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        sk.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(76, 66).addBox(0.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 18.0F, 4.0F, 0.0F, -0.5F, -0.35F));
        sk.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(92, 66).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 18.0F, -4.0F, 0.0F, -0.5F, 0.35F));
        sk.addOrReplaceChild("leg4",
                CubeListBuilder.create().texOffs(0, 76).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        sk.addOrReplaceChild("leg5",
                CubeListBuilder.create().texOffs(16, 76).addBox(-7.0F, -0.5F, -0.5F, 7.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 18.0F, 4.0F, 0.0F, 0.5F, 0.35F));
        sk.addOrReplaceChild("horn",
                CubeListBuilder.create().texOffs(32, 76).addBox(-1.0F, -4.0F, -2.0F, 2.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 11.0F, -8.0F));

        // ══════════════════════════════════════════════════════
        // STORMHERALD 雷霆传令：带电躯干 + 双雷翼 + 雷角 + 悬浮下摆
        // ══════════════════════════════════════════════════════
        PartDefinition sh = root.addOrReplaceChild("stormherald", CubeListBuilder.create(), PartPose.ZERO);
        sh.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        sh.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 26).addBox(-3.5F, -4.0F, -4.0F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 1.0F, -1.0F));
        sh.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(0, 42).addBox(0.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offsetAndRotation(2.5F, -2.0F, 0.0F, -0.3F, 0.0F, -0.35F));
        sh.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(10, 42).addBox(-2.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offsetAndRotation(-2.5F, -2.0F, 0.0F, -0.3F, 0.0F, 0.35F));
        sh.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(0, 54).addBox(0.0F, -8.0F, -0.5F, 14.0F, 16.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 2.0F, 2.5F, 0.0F, 0.0F, -0.25F));
        sh.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(32, 54).addBox(-14.0F, -8.0F, -0.5F, 14.0F, 16.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 2.0F, 2.5F, 0.0F, 0.0F, 0.25F));
        sh.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(64, 54).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offsetAndRotation(6.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        sh.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(78, 54).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.offsetAndRotation(-6.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.15F));
        sh.addOrReplaceChild("skirt",
                CubeListBuilder.create().texOffs(92, 54).addBox(-5.5F, 0.0F, -4.5F, 11.0F, 12.0F, 9.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        sh.addOrReplaceChild("bolt",
                CubeListBuilder.create().texOffs(0, 74).addBox(-0.5F, -16.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(6.0F, 4.0F, -2.0F));

        // ══════════════════════════════════════════════════════
        // VOIDWEAVER 虚空织者：悬浮虚空长袍 + 无面兜帽 + 悬浮核心 + 4 触须
        // ══════════════════════════════════════════════════════
        PartDefinition vw = root.addOrReplaceChild("voidweaver", CubeListBuilder.create(), PartPose.ZERO);
        vw.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 15.0F, 8.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        vw.addOrReplaceChild("robe",
                CubeListBuilder.create().texOffs(0, 24).addBox(-7.0F, 0.0F, -5.5F, 14.0F, 16.0F, 11.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        vw.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 52).addBox(-5.5F, -6.0F, -5.5F, 11.0F, 8.0F, 11.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        vw.addOrReplaceChild("void_face",
                CubeListBuilder.create().texOffs(0, 72).addBox(-2.5F, -1.5F, -4.5F, 5.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 1.0F, -1.0F));
        vw.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 78).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, -5.0F));
        vw.addOrReplaceChild("tendril0",
                CubeListBuilder.create().texOffs(0, 88).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 8.0F, 2.0F, 0.3F, 0.0F, -0.30F));
        vw.addOrReplaceChild("tendril1",
                CubeListBuilder.create().texOffs(8, 88).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 8.0F, 2.0F, 0.3F, 0.0F, 0.30F));
        vw.addOrReplaceChild("tendril2",
                CubeListBuilder.create().texOffs(16, 88).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 6.0F, 4.0F, 0.5F, 0.0F, -0.20F));
        vw.addOrReplaceChild("tendril3",
                CubeListBuilder.create().texOffs(24, 88).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 6.0F, 4.0F, 0.5F, 0.0F, 0.20F));
        vw.addOrReplaceChild("ring",
                CubeListBuilder.create().texOffs(32, 88).addBox(-6.0F, -1.0F, -1.0F, 12.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -6.0F, 0.0F));

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
