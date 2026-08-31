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
 * 常规小怪模型（第一批次重构）：<b>3 个僵尸人形变体 + 15 个独立建模变体</b>。
 *
 * <h3>分组</h3>
 * <ul>
 *   <li><b>僵尸人形组</b> {@code humanoid}：仅 SHAMBLER / RUNNER / BRUTE 三个最基础小怪共用，
 *       通过各自贴图与体型缩放区分（保留原版僵尸观感）。</li>
 *   <li><b>15 个独立几何组</b>：spitter / stalker / howler / bloater / juggernaut /
 *       cinderling / frostling / huskbrute / ravenor / wailer / burster /
 *       gorehound / shadowmute / bonelord / spinewalker，各自完全独立建模。</li>
 * </ul>
 *
 * <p>⚠ <b>texOffs 必须为字面量</b>：贴图由 {@code tools/gen_creature_textures.py}
 * 正则解析本文件生成，表达式会导致该盒体解析不到、贴图缺面。
 *
 * <p>每个变体独占一张 128×128 贴图（{@code sixty_seconds_<name>.png}），
 * 因此各组 UV 可复用同一坐标空间；僵尸人形组沿用原版人形 UV 布局（绘制在贴图左上 64×64 区）。
 */
public class SixtySecondsCreatureModel extends EntityModel<SixtySecondsMonsterEntity> {

    private final ModelPart root;

    // ── 僵尸人形组（遗留：BRUTE/RUNNER/SHAMBLER 现已各自独立建模，此组不再被使用）──
    private final ModelPart humanoid;
    private final ModelPart hHead, hBody, hArmL, hArmR, hLegL, hLegR;

    // ── 原人形组三小怪的独立建模几何 ──
    private final ModelPart brute, runner, shambler;
    private final ModelPart brHead, brBody, brArmL, brArmR, brLegL, brLegR;
    private final ModelPart ruHead, ruBody, ruArmL, ruArmR, ruLegL, ruLegR;
    private final ModelPart ruHandL, ruHandR;
    private final ModelPart shHead, shBody, shArmL, shArmR, shLegL, shLegR;
    private final ModelPart shHandL, shHandR;

    // ── 15 个独立几何组 ──
    private final ModelPart spitter, stalker, howler, bloater, juggernaut;
    private final ModelPart cinderling, frostling, huskbrute, ravenor, wailer;
    private final ModelPart burster, gorehound, shadowmute, bonelord, spinewalker;

    // 动画用子部件
    private final ModelPart spitHump, spitHead;
    private final ModelPart[] stalkerLegs = new ModelPart[4];
    private final ModelPart stalkerBladeL, stalkerBladeR, stalkerHead;
    private final ModelPart howlerJaw, howlerChest;
    private final ModelPart[] bloaterPustules = new ModelPart[4];
    private final ModelPart jugShoulderL, jugShoulderR, jugHead;
    private final ModelPart cinderCore, cinderFlame;
    private final ModelPart frostCrown, frostSpikeL, frostSpikeR;
    private final ModelPart huskArmL, huskArmR;
    private final ModelPart ravenWingL, ravenWingR;
    private final ModelPart wailerJaw, wailerRibbon;
    private final ModelPart[] bursterSpikes = new ModelPart[6];
    private final ModelPart houndHead, houndTail, houndLegFL, houndLegFR, houndLegBL, houndLegBR;
    private final ModelPart shadowHood, shadowTail;
    private final ModelPart boneRibs, boneCape;
    private final ModelPart[] spineSpikes = new ModelPart[5];

    public SixtySecondsCreatureModel(ModelPart root) {
        this.root = root;
        this.humanoid = root.getChild("humanoid");
        this.hHead = humanoid.getChild("head");
        this.hBody = humanoid.getChild("body");
        this.hArmL = humanoid.getChild("arm_l");
        this.hArmR = humanoid.getChild("arm_r");
        this.hLegL = humanoid.getChild("leg_l");
        this.hLegR = humanoid.getChild("leg_r");

        this.brute = root.getChild("brute");
        this.brHead = brute.getChild("head");
        this.brBody = brute.getChild("body");
        this.brArmL = brute.getChild("arm_l");
        this.brArmR = brute.getChild("arm_r");
        this.brLegL = brute.getChild("leg_l");
        this.brLegR = brute.getChild("leg_r");

        this.runner = root.getChild("runner");
        this.ruHead = runner.getChild("head");
        this.ruBody = runner.getChild("body");
        this.ruArmL = runner.getChild("arm_l");
        this.ruArmR = runner.getChild("arm_r");
        this.ruLegL = runner.getChild("leg_l");
        this.ruLegR = runner.getChild("leg_r");
        this.ruHandL = runner.getChild("hand_l");
        this.ruHandR = runner.getChild("hand_r");

        this.shambler = root.getChild("shambler");
        this.shHead = shambler.getChild("head");
        this.shBody = shambler.getChild("body");
        this.shArmL = shambler.getChild("arm_l");
        this.shArmR = shambler.getChild("arm_r");
        this.shLegL = shambler.getChild("leg_l");
        this.shLegR = shambler.getChild("leg_r");
        this.shHandL = shambler.getChild("hand_l");
        this.shHandR = shambler.getChild("hand_r");

        this.spitter = root.getChild("spitter");
        this.spitHump = spitter.getChild("hump");
        this.spitHead = spitter.getChild("head");

        this.stalker = root.getChild("stalker");
        for (int i = 0; i < 4; i++) stalkerLegs[i] = stalker.getChild("leg" + i);
        this.stalkerBladeL = stalker.getChild("blade_l");
        this.stalkerBladeR = stalker.getChild("blade_r");
        this.stalkerHead = stalker.getChild("head");

        this.howler = root.getChild("howler");
        this.howlerJaw = howler.getChild("jaw");
        this.howlerChest = howler.getChild("chest");

        this.bloater = root.getChild("bloater");
        for (int i = 0; i < 4; i++) bloaterPustules[i] = bloater.getChild("pustule" + i);

        this.juggernaut = root.getChild("juggernaut");
        this.jugShoulderL = juggernaut.getChild("shoulder_l");
        this.jugShoulderR = juggernaut.getChild("shoulder_r");
        this.jugHead = juggernaut.getChild("head");

        this.cinderling = root.getChild("cinderling");
        this.cinderCore = cinderling.getChild("core");
        this.cinderFlame = cinderling.getChild("flame");

        this.frostling = root.getChild("frostling");
        this.frostCrown = frostling.getChild("crown");
        this.frostSpikeL = frostling.getChild("spike_l");
        this.frostSpikeR = frostling.getChild("spike_r");

        this.huskbrute = root.getChild("huskbrute");
        this.huskArmL = huskbrute.getChild("arm_l");
        this.huskArmR = huskbrute.getChild("arm_r");

        this.ravenor = root.getChild("ravenor");
        this.ravenWingL = ravenor.getChild("wing_l");
        this.ravenWingR = ravenor.getChild("wing_r");

        this.wailer = root.getChild("wailer");
        this.wailerJaw = wailer.getChild("jaw");
        this.wailerRibbon = wailer.getChild("ribbon");

        this.burster = root.getChild("burster");
        for (int i = 0; i < 6; i++) bursterSpikes[i] = burster.getChild("spike" + i);

        this.gorehound = root.getChild("gorehound");
        this.houndHead = gorehound.getChild("head");
        this.houndTail = gorehound.getChild("tail");
        this.houndLegFL = gorehound.getChild("leg_fl");
        this.houndLegFR = gorehound.getChild("leg_fr");
        this.houndLegBL = gorehound.getChild("leg_bl");
        this.houndLegBR = gorehound.getChild("leg_br");

        this.shadowmute = root.getChild("shadowmute");
        this.shadowHood = shadowmute.getChild("hood");
        this.shadowTail = shadowmute.getChild("tail");

        this.bonelord = root.getChild("bonelord");
        this.boneRibs = bonelord.getChild("ribs");
        this.boneCape = bonelord.getChild("cape");

        this.spinewalker = root.getChild("spinewalker");
        for (int i = 0; i < 5; i++) spineSpikes[i] = spinewalker.getChild("spike" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ══════════════════════════════════════════════════════════
        // 僵尸人形组（SHAMBLER / RUNNER / BRUTE 共用几何）
        // UV 沿用原版人形 64×64 布局：头(0,0) 身(16,16) 右臂(40,16) 左臂(40,32) 右腿(0,16) 左腿(0,32)
        // ══════════════════════════════════════════════════════════
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

        // ══════════════════════════════════════════════════════════
        // BRUTE 重锤兽：宽厚躯干 + 牛角 + 锤状双臂 + 粗壮双腿（独立建模）
        // ══════════════════════════════════════════════════════════
        PartDefinition br = root.addOrReplaceChild("brute", CubeListBuilder.create(), PartPose.ZERO);
        br.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -10.0F, -5.0F, 14.0F, 12.0F, 10.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition brHead = br.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 34).addBox(-4.0F, -22.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        brHead.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(24, 34).addBox(-3.0F, -20.0F, -5.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        br.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(24, 40).addBox(-6.0F, -24.0F, -2.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        br.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(30, 40).addBox(4.0F, -24.0F, -2.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        br.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(40, 0).addBox(-10.0F, -10.0F, -3.0F, 4.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        br.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(58, 0).addBox(6.0F, -10.0F, -3.0F, 4.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        br.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(40, 22).addBox(-6.0F, 0.0F, -4.0F, 5.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        br.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(62, 22).addBox(1.0F, 0.0F, -4.0F, 5.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // RUNNER 奔跑者：瘦长躯干 + 长腿 + 后掠双臂（独立建模）
        // ══════════════════════════════════════════════════════════
        PartDefinition ru = root.addOrReplaceChild("runner", CubeListBuilder.create(), PartPose.ZERO);
        ru.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition ruHead = ru.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, -19.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ruHead.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(20, 22).addBox(-2.0F, -17.0F, -4.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        ru.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(20, 0).addBox(-6.0F, -11.0F, -2.0F, 2.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ru.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(30, 0).addBox(4.0F, -11.0F, -2.0F, 2.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ru.addOrReplaceChild("hand_l",
                CubeListBuilder.create().texOffs(40, 0).addBox(-6.0F, -4.0F, -3.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ru.addOrReplaceChild("hand_r",
                CubeListBuilder.create().texOffs(46, 0).addBox(4.0F, -4.0F, -3.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ru.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(20, 16).addBox(-4.0F, -2.0F, -2.0F, 2.0F, 14.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ru.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(30, 16).addBox(2.0F, -2.0F, -2.0F, 2.0F, 14.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // SHAMBLER 拖行者：驼背躯干 + 一只巨大拖地臂 + 细弱双腿（独立建模）
        // ══════════════════════════════════════════════════════════
        PartDefinition sh = root.addOrReplaceChild("shambler", CubeListBuilder.create(), PartPose.ZERO);
        sh.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -8.0F, -3.0F, 10.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition shHead = sh.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 22).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        shHead.addOrReplaceChild("eyes",
                CubeListBuilder.create().texOffs(20, 22).addBox(-2.0F, -11.0F, -4.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        sh.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(24, 0).addBox(-10.0F, -6.0F, -2.0F, 4.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sh.addOrReplaceChild("hand_l",
                CubeListBuilder.create().texOffs(46, 0).addBox(-10.0F, 0.0F, -2.0F, 4.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sh.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(24, 24).addBox(5.0F, -6.0F, -2.0F, 2.0F, 7.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sh.addOrReplaceChild("hand_r",
                CubeListBuilder.create().texOffs(36, 24).addBox(5.0F, 0.0F, -2.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sh.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(44, 24).addBox(-4.0F, -1.0F, -2.0F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sh.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(54, 24).addBox(2.0F, -1.0F, -2.0F, 2.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // SPITTER 吐酸者：驼背躯干 + 背部巨毒囊 + 前伸喷口 + 细肢
        // ══════════════════════════════════════════════════════════
        PartDefinition sp = root.addOrReplaceChild("spitter", CubeListBuilder.create(), PartPose.ZERO);
        sp.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        sp.addOrReplaceChild("hump",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, 3.0F, 0.25F, 0.0F, 0.0F));
        sp.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, -3.0F));
        sp.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 50).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 12.0F, -9.0F));
        sp.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 12.0F, -1.0F, -0.3F, 0.0F, 0.2F));
        sp.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(10, 58).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 12.0F, -1.0F, -0.3F, 0.0F, -0.2F));
        sp.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(20, 58).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(2.5F, 22.0F, 0.0F));
        sp.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(32, 58).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(-2.5F, 22.0F, 0.0F));
        sp.addOrReplaceChild("sac_l",
                CubeListBuilder.create().texOffs(44, 58).addBox(0.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(4.0F, 17.0F, 2.0F));
        sp.addOrReplaceChild("sac_r",
                CubeListBuilder.create().texOffs(58, 58).addBox(-3.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(-4.0F, 17.0F, 2.0F));

        // ══════════════════════════════════════════════════════════
        // STALKER 潜袭者：瘦长低伏四足 + 双镰刀前肢 + 尖刺长头
        // ══════════════════════════════════════════════════════════
        PartDefinition st = root.addOrReplaceChild("stalker", CubeListBuilder.create(), PartPose.ZERO);
        st.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -6.0F, 5.0F, 5.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        st.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 17.5F, -6.0F));
        st.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(0, 28).addBox(-0.5F, -3.0F, -4.0F, 1.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 15.5F, -7.0F));
        st.addOrReplaceChild("blade_l",
                CubeListBuilder.create().texOffs(0, 36).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(2.5F, 18.0F, -5.0F, 0.0F, 0.35F, 0.0F));
        st.addOrReplaceChild("blade_r",
                CubeListBuilder.create().texOffs(0, 44).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(-2.5F, 18.0F, -5.0F, 0.0F, -0.35F, 0.0F));
        st.addOrReplaceChild("leg0",
                CubeListBuilder.create().texOffs(18, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 20.0F, -4.0F, 0.0F, 0.0F, 0.30F));
        st.addOrReplaceChild("leg1",
                CubeListBuilder.create().texOffs(24, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 20.0F, -4.0F, 0.0F, 0.0F, -0.30F));
        st.addOrReplaceChild("leg2",
                CubeListBuilder.create().texOffs(30, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 20.0F, 3.0F, 0.0F, 0.0F, 0.30F));
        st.addOrReplaceChild("leg3",
                CubeListBuilder.create().texOffs(36, 36).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 20.0F, 3.0F, 0.0F, 0.0F, -0.30F));
        st.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(42, 36).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 7.0F),
                PartPose.offset(0.0F, 17.5F, 6.0F));

        // ══════════════════════════════════════════════════════════
        // HOWLER 嚎叫者：夸张胸腔 + 喇叭状张口 + 粗壮双臂
        // ══════════════════════════════════════════════════════════
        PartDefinition ho = root.addOrReplaceChild("howler", CubeListBuilder.create(), PartPose.ZERO);
        ho.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -10.0F, -4.0F, 12.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        ho.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, -9.0F, -4.0F, 7.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, -3.0F));
        ho.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 36).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, -4.0F));
        ho.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(0, 46).addBox(0.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 4.0F, -3.0F, 0.0F, 0.0F, -0.5F));
        ho.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(10, 46).addBox(-2.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 4.0F, -3.0F, 0.0F, 0.0F, 0.5F));
        ho.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(20, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offsetAndRotation(6.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.25F));
        ho.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(32, 46).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offsetAndRotation(-6.0F, 10.0F, 0.0F, 0.0F, 0.0F, -0.25F));
        ho.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(44, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.offset(3.0F, 20.0F, 0.0F));
        ho.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(58, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
                PartPose.offset(-3.0F, 20.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // BLOATER 爆裂怪：膨胀球状躯干 + 4 脓包 + 短粗腿
        // ══════════════════════════════════════════════════════════
        PartDefinition bl = root.addOrReplaceChild("bloater", CubeListBuilder.create(), PartPose.ZERO);
        bl.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -8.0F, -6.0F, 14.0F, 14.0F, 12.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        bl.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 26).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 9.0F, -6.0F));
        bl.addOrReplaceChild("pustule0",
                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(-6.0F, 11.0F, -3.0F));
        bl.addOrReplaceChild("pustule1",
                CubeListBuilder.create().texOffs(14, 38).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(6.0F, 13.0F, -2.0F));
        bl.addOrReplaceChild("pustule2",
                CubeListBuilder.create().texOffs(28, 38).addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 8.0F, 4.0F));
        bl.addOrReplaceChild("pustule3",
                CubeListBuilder.create().texOffs(46, 38).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(4.0F, 19.0F, 4.0F));
        bl.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(0, 50).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(4.0F, 21.0F, 0.0F));
        bl.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(14, 50).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(-4.0F, 21.0F, 0.0F));
        bl.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(28, 50).addBox(0.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(6.0F, 16.0F, 0.0F));
        bl.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(44, 50).addBox(-4.0F, -1.0F, -1.5F, 4.0F, 3.0F, 3.0F),
                PartPose.offset(-6.0F, 16.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // JUGGERNAUT 装甲重锤：厚重躯干 + 双层巨肩 + 小头 + 粗腿 + 重锤臂
        // ══════════════════════════════════════════════════════════
        PartDefinition ju = root.addOrReplaceChild("juggernaut", CubeListBuilder.create(), PartPose.ZERO);
        ju.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -10.0F, -4.0F, 12.0F, 16.0F, 8.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        ju.addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(0, 26).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, 10.0F, -4.0F));
        ju.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 40).addBox(-2.5F, -3.0F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        ju.addOrReplaceChild("shoulder_l",
                CubeListBuilder.create().texOffs(0, 52).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 6.0F, 8.0F),
                PartPose.offset(7.0F, 4.0F, 0.0F));
        ju.addOrReplaceChild("shoulder_r",
                CubeListBuilder.create().texOffs(28, 52).addBox(-3.0F, -4.0F, -4.0F, 6.0F, 6.0F, 8.0F),
                PartPose.offset(-7.0F, 4.0F, 0.0F));
        ju.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(56, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(7.0F, 10.0F, 0.0F));
        ju.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(72, 52).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                PartPose.offset(-7.0F, 10.0F, 0.0F));
        ju.addOrReplaceChild("hammer",
                CubeListBuilder.create().texOffs(88, 52).addBox(-3.0F, 8.0F, -3.0F, 6.0F, 5.0F, 6.0F),
                PartPose.offset(7.0F, 10.0F, 0.0F));
        ju.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(0, 68).addBox(-3.0F, 0.0F, -3.0F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(3.5F, 18.0F, 0.0F));
        ju.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(24, 68).addBox(-2.0F, 0.0F, -3.0F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(-3.5F, 18.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // CINDERLING 余烬爬行者：焦黑小巧躯干 + 发光核心 + 头顶火苗 + 细长四肢
        // ══════════════════════════════════════════════════════════
        PartDefinition ci = root.addOrReplaceChild("cinderling", CubeListBuilder.create(), PartPose.ZERO);
        ci.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -2.5F, 6.0F, 10.0F, 5.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        ci.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 15.0F, -2.0F));
        ci.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-2.5F, -3.0F, -3.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 11.0F, -1.0F));
        ci.addOrReplaceChild("flame",
                CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 7.5F, -1.0F));
        ci.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 42).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.18F));
        ci.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(8, 42).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 8.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, 13.0F, 0.0F, 0.0F, 0.0F, -0.18F));
        ci.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(16, 42).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(1.8F, 22.0F, 0.0F));
        ci.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(26, 42).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-1.8F, 22.0F, 0.0F));
        ci.addOrReplaceChild("ember_l",
                CubeListBuilder.create().texOffs(36, 42).addBox(0.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(3.0F, 12.0F, 1.0F));
        ci.addOrReplaceChild("ember_r",
                CubeListBuilder.create().texOffs(46, 42).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-3.0F, 12.0F, 1.0F));

        // ══════════════════════════════════════════════════════════
        // FROSTLING 寒霜爬行者：冰晶棱块躯干 + 冰冠 + 双臂冰刺 + 细肢
        // ══════════════════════════════════════════════════════════
        PartDefinition fr = root.addOrReplaceChild("frostling", CubeListBuilder.create(), PartPose.ZERO);
        fr.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.0F, 7.0F, 11.0F, 6.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        fr.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, -3.0F, -3.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 10.0F, -1.0F));
        fr.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 28).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 7.0F, -1.0F));
        fr.addOrReplaceChild("spike_l",
                CubeListBuilder.create().texOffs(0, 38).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(3.5F, 15.0F, 0.0F, 0.0F, 0.0F, -0.60F));
        fr.addOrReplaceChild("spike_r",
                CubeListBuilder.create().texOffs(8, 38).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-3.5F, 15.0F, 0.0F, 0.0F, 0.0F, 0.60F));
        fr.addOrReplaceChild("shard0",
                CubeListBuilder.create().texOffs(16, 38).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 3.0F, 0.5F, 0.0F, 0.0F));
        fr.addOrReplaceChild("shard1",
                CubeListBuilder.create().texOffs(26, 38).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(2.5F, 19.0F, 2.5F, 0.7F, 0.6F, 0.0F));
        fr.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(36, 38).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(1.8F, 21.0F, 0.0F));
        fr.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(46, 38).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                PartPose.offset(-1.8F, 21.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // HUSKBRUTE 枯壳重锤：干裂木壳躯干 + 树瘤 + 粗大双臂 + 墩实双腿
        // ══════════════════════════════════════════════════════════
        PartDefinition hb = root.addOrReplaceChild("huskbrute", CubeListBuilder.create(), PartPose.ZERO);
        hb.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.5F, -9.0F, -4.0F, 11.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        hb.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, -7.0F, -2.0F, 12.0F, 9.0F, 3.0F),
                PartPose.offset(0.0F, 12.0F, 4.0F));
        hb.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 38).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 4.0F, -1.0F));
        hb.addOrReplaceChild("knot0",
                CubeListBuilder.create().texOffs(0, 52).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(-3.5F, 10.0F, -4.0F));
        hb.addOrReplaceChild("knot1",
                CubeListBuilder.create().texOffs(12, 52).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(3.5F, 15.0F, -4.0F));
        hb.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(24, 52).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(6.5F, 8.0F, 0.0F, 0.0F, 0.0F, 0.22F));
        hb.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(40, 52).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(-6.5F, 8.0F, 0.0F, 0.0F, 0.0F, -0.22F));
        hb.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(56, 52).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                PartPose.offset(3.0F, 19.0F, 0.0F));
        hb.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(76, 52).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F),
                PartPose.offset(-3.0F, 19.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // RAVENOR 掠影：极瘦高躯干 + 钩喙头 + 双翼膜 + 钩爪长腿
        // ══════════════════════════════════════════════════════════
        PartDefinition ra = root.addOrReplaceChild("ravenor", CubeListBuilder.create(), PartPose.ZERO);
        ra.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -8.0F, -2.0F, 5.0F, 13.0F, 4.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        ra.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 18).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, -1.0F));
        ra.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(0, 26).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, -2.0F));
        ra.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
        ra.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(0, 40).addBox(0.0F, -5.0F, -1.0F, 9.0F, 11.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 9.0F, 1.5F, 0.0F, 0.0F, -0.20F));
        ra.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(22, 40).addBox(-9.0F, -5.0F, -1.0F, 9.0F, 11.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 9.0F, 1.5F, 0.0F, 0.0F, 0.20F));
        ra.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(44, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(1.2F, 20.0F, 0.0F));
        ra.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(50, 40).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(-1.2F, 20.0F, 0.0F));
        ra.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(56, 40).addBox(-1.0F, 9.0F, -2.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(1.2F, 20.0F, 0.0F));
        ra.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(66, 40).addBox(-1.0F, 9.0F, -2.0F, 2.0F, 1.0F, 3.0F),
                PartPose.offset(-1.2F, 20.0F, 0.0F));
        ra.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(76, 40).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 19.0F, 2.0F, -0.25F, 0.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // WAILER 哀嚎者：高瘦佝偻躯干 + 长脸张口 + 垂落飘带 + 过膝长臂
        // ══════════════════════════════════════════════════════════
        PartDefinition wa = root.addOrReplaceChild("wailer", CubeListBuilder.create(), PartPose.ZERO);
        wa.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -9.0F, -2.5F, 6.0F, 14.0F, 5.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        wa.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 20).addBox(-2.5F, -6.0F, -3.0F, 5.0F, 6.0F, 5.0F),
                PartPose.offset(0.0F, 6.0F, -1.5F));
        wa.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        wa.addOrReplaceChild("ribbon",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 13.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 5.0F, 2.5F, 0.15F, 0.0F, 0.0F));
        wa.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(0, 55).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(3.5F, 7.0F, 0.0F, 0.0F, 0.0F, 0.12F));
        wa.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(10, 55).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 15.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, 7.0F, 0.0F, 0.0F, 0.0F, -0.12F));
        wa.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(20, 55).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(2.0F, 20.0F, 0.0F));
        wa.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(32, 55).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(-2.0F, 20.0F, 0.0F));
        wa.addOrReplaceChild("eye",
                CubeListBuilder.create().texOffs(44, 55).addBox(-1.5F, -3.0F, -3.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 4.0F, -1.5F));

        // ══════════════════════════════════════════════════════════
        // BURSTER 爆碎者：多刺膨胀体 + 6 根放射棘刺 + 短腿（texOffs 字面量展开）
        // ══════════════════════════════════════════════════════════
        PartDefinition bu = root.addOrReplaceChild("burster", CubeListBuilder.create(), PartPose.ZERO);
        bu.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 11.0F, 10.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        bu.addOrReplaceChild("spike0",
                CubeListBuilder.create().texOffs(0, 22).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.5F));
        bu.addOrReplaceChild("spike1",
                CubeListBuilder.create().texOffs(10, 22).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        bu.addOrReplaceChild("spike2",
                CubeListBuilder.create().texOffs(20, 22).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, 4.0F, 0.5F, 0.0F, 0.0F));
        bu.addOrReplaceChild("spike3",
                CubeListBuilder.create().texOffs(30, 22).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, -4.0F, -0.5F, 0.0F, 0.0F));
        bu.addOrReplaceChild("spike4",
                CubeListBuilder.create().texOffs(40, 22).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        bu.addOrReplaceChild("spike5",
                CubeListBuilder.create().texOffs(52, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        bu.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(64, 22).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(3.0F, 21.0F, 0.0F));
        bu.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(76, 22).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(-3.0F, 21.0F, 0.0F));
        bu.addOrReplaceChild("maw",
                CubeListBuilder.create().texOffs(88, 22).addBox(-2.0F, -1.0F, -6.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // GOREHOUND 嗜血猎犬：四足犬形 + 肌肉躯干 + 獠牙头 + 尾巴
        // ══════════════════════════════════════════════════════════
        PartDefinition gh = root.addOrReplaceChild("gorehound", CubeListBuilder.create(), PartPose.ZERO);
        gh.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.0F, -6.0F, 7.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        gh.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4.0F, -4.5F, -5.0F, 8.0F, 9.0F, 7.0F),
                PartPose.offset(0.0F, 17.5F, -3.0F));
        gh.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 14.0F, -6.0F));
        gh.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 54).addBox(-1.5F, -1.0F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 15.0F, -11.0F));
        gh.addOrReplaceChild("fang_l",
                CubeListBuilder.create().texOffs(0, 62).addBox(0.5F, 1.0F, -3.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -11.0F));
        gh.addOrReplaceChild("fang_r",
                CubeListBuilder.create().texOffs(8, 62).addBox(-1.5F, 1.0F, -3.0F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, -11.0F));
        gh.addOrReplaceChild("ear_l",
                CubeListBuilder.create().texOffs(16, 62).addBox(0.0F, -3.0F, -0.5F, 1.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(2.0F, 11.0F, -6.0F, 0.0F, 0.0F, -0.3F));
        gh.addOrReplaceChild("ear_r",
                CubeListBuilder.create().texOffs(24, 62).addBox(-1.0F, -3.0F, -0.5F, 1.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-2.0F, 11.0F, -6.0F, 0.0F, 0.0F, 0.3F));
        gh.addOrReplaceChild("leg_fl",
                CubeListBuilder.create().texOffs(32, 62).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(2.2F, 21.0F, -3.5F));
        gh.addOrReplaceChild("leg_fr",
                CubeListBuilder.create().texOffs(42, 62).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-2.2F, 21.0F, -3.5F));
        gh.addOrReplaceChild("leg_bl",
                CubeListBuilder.create().texOffs(52, 62).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(2.2F, 21.0F, 4.0F));
        gh.addOrReplaceChild("leg_br",
                CubeListBuilder.create().texOffs(62, 62).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-2.2F, 21.0F, 4.0F));
        gh.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(72, 62).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 6.0F, -0.5F, 0.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // SHADOWMUTE 暗默：无腿幽魂 + 兜帽 + 虚无下摆 + 发光眼
        // ══════════════════════════════════════════════════════════
        PartDefinition sm = root.addOrReplaceChild("shadowmute", CubeListBuilder.create(), PartPose.ZERO);
        sm.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -9.0F, -3.0F, 8.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        sm.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.5F, -5.0F, -4.5F, 9.0F, 6.0F, 9.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
        sm.addOrReplaceChild("face",
                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, -1.0F, -3.5F, 4.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 4.0F, -1.0F));
        sm.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 42).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 12.0F, 5.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        sm.addOrReplaceChild("wisp_l",
                CubeListBuilder.create().texOffs(0, 60).addBox(0.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(3.5F, 8.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        sm.addOrReplaceChild("wisp_r",
                CubeListBuilder.create().texOffs(16, 60).addBox(-5.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, 8.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        sm.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(32, 60).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.15F));
        sm.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(40, 60).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 7.0F, 0.0F, 0.0F, 0.0F, -0.15F));

        // ══════════════════════════════════════════════════════════
        // BONELORD 白骨领主：骷髅巨躯 + 外露肋骨 + 头骨 + 披风 + 骨刺
        // ══════════════════════════════════════════════════════════
        PartDefinition bo = root.addOrReplaceChild("bonelord", CubeListBuilder.create(), PartPose.ZERO);
        bo.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -10.0F, -3.0F, 9.0F, 15.0F, 6.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        bo.addOrReplaceChild("ribs",
                CubeListBuilder.create().texOffs(0, 22).addBox(-5.5F, -7.0F, -3.5F, 11.0F, 9.0F, 3.0F),
                PartPose.offset(0.0F, 11.0F, -3.0F));
        bo.addOrReplaceChild("skull",
                CubeListBuilder.create().texOffs(0, 34).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 2.0F, -0.5F));
        bo.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 48).addBox(-3.0F, 3.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 2.0F, -0.5F));
        bo.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(0, 56).addBox(-5.0F, 0.0F, -0.5F, 10.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 3.0F, 0.10F, 0.0F, 0.0F));
        bo.addOrReplaceChild("spine0",
                CubeListBuilder.create().texOffs(0, 72).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(-2.0F, 2.0F, 3.0F));
        bo.addOrReplaceChild("spine1",
                CubeListBuilder.create().texOffs(8, 72).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 1.0F, 3.5F));
        bo.addOrReplaceChild("spine2",
                CubeListBuilder.create().texOffs(16, 72).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(2.0F, 2.0F, 3.0F));
        bo.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(24, 72).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F),
                PartPose.offsetAndRotation(5.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.18F));
        bo.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(36, 72).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 13.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, 5.0F, 0.0F, 0.0F, 0.0F, -0.18F));
        bo.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(48, 72).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(2.5F, 18.0F, 0.0F));
        bo.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(60, 72).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(-2.5F, 18.0F, 0.0F));

        // ══════════════════════════════════════════════════════════
        // SPINEWALKER 棘行体：驼背长躯 + 5 根背脊骨刺 + 细长四肢
        // ══════════════════════════════════════════════════════════
        PartDefinition sw = root.addOrReplaceChild("spinewalker", CubeListBuilder.create(), PartPose.ZERO);
        sw.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -7.0F, 7.0F, 7.0F, 14.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        sw.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(0, 22).addBox(-1.5F, -3.0F, -4.0F, 3.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, -6.0F, 0.45F, 0.0F, 0.0F));
        sw.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 12.0F, -9.0F, 0.30F, 0.0F, 0.0F));
        sw.addOrReplaceChild("spike0",
                CubeListBuilder.create().texOffs(0, 42).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, -5.0F));
        sw.addOrReplaceChild("spike1",
                CubeListBuilder.create().texOffs(8, 42).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, -2.5F));
        sw.addOrReplaceChild("spike2",
                CubeListBuilder.create().texOffs(16, 42).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        sw.addOrReplaceChild("spike3",
                CubeListBuilder.create().texOffs(24, 42).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, 2.5F));
        sw.addOrReplaceChild("spike4",
                CubeListBuilder.create().texOffs(32, 42).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(0.0F, 12.0F, 5.0F));
        sw.addOrReplaceChild("leg_fl",
                CubeListBuilder.create().texOffs(40, 42).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 19.0F, -4.0F, 0.0F, 0.0F, 0.20F));
        sw.addOrReplaceChild("leg_fr",
                CubeListBuilder.create().texOffs(48, 42).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 19.0F, -4.0F, 0.0F, 0.0F, -0.20F));
        sw.addOrReplaceChild("leg_bl",
                CubeListBuilder.create().texOffs(56, 42).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(2.5F, 19.0F, 4.0F, 0.0F, 0.0F, 0.20F));
        sw.addOrReplaceChild("leg_br",
                CubeListBuilder.create().texOffs(64, 42).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 9.0F, 1.0F),
                PartPose.offsetAndRotation(-2.5F, 19.0F, 4.0F, 0.0F, 0.0F, -0.20F));
        sw.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(72, 42).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 7.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SixtySecondsMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 默认全部隐藏
        humanoid.visible = false;
        spitter.visible = false; stalker.visible = false; howler.visible = false;
        bloater.visible = false; juggernaut.visible = false; cinderling.visible = false;
        frostling.visible = false; huskbrute.visible = false; ravenor.visible = false;
        wailer.visible = false; burster.visible = false; gorehound.visible = false;
        shadowmute.visible = false; bonelord.visible = false; spinewalker.visible = false;

        float t = ageInTicks * 0.15F;
        SixtySecondsMonsterEntity.Variant v = entity.getVariant();

        switch (v) {
            // ── 原人形组三小怪（现已各自独立建模）──
            case BRUTE -> {
                brute.visible = true;
                setupBiped(brHead, brBody, brArmL, brArmR, brLegL, brLegR, null, null,
                        limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            }
            case RUNNER -> {
                runner.visible = true;
                setupBiped(ruHead, ruBody, ruArmL, ruArmR, ruLegL, ruLegR, ruHandL, ruHandR,
                        limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            }
            case SHAMBLER -> {
                shambler.visible = true;
                setupBiped(shHead, shBody, shArmL, shArmR, shLegL, shLegR, shHandL, shHandR,
                        limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            }
            // ── 其余独立建模小怪 ──
            default -> {
                // 独立组动画在 animateIndependent 中补齐
                animateIndependent(v, limbSwing, limbSwingAmount, t);
            }
        }
    }

    /** 僵尸人形组的标准走路动画（复用原版 HumanoidModel 逻辑）。 */
    private void setupHumanoidAnim(float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch) {
        hHead.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        hHead.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = limbSwing * 0.6662F;
        hArmR.xRot = Mth.cos(swing + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        hArmL.xRot = Mth.cos(swing) * 2.0F * limbSwingAmount * 0.5F;
        hArmR.zRot = 0.0F;
        hArmL.zRot = 0.0F;
        hLegR.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
        hLegL.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * limbSwingAmount;
        hLegR.yRot = 0.0F;
        hLegL.yRot = 0.0F;
    }

    /**
     * 通用双足走路动画，供独立建模的小怪（重锤兽/奔跑者/拖行者）复用。
     * handL/handR 可为 null（无独立手部件时）。
     */
    private void setupBiped(ModelPart head, ModelPart body, ModelPart armL, ModelPart armR,
                            ModelPart legL, ModelPart legR, ModelPart handL, ModelPart handR,
                            float limbSwing, float limbSwingAmount, float ageInTicks,
                            float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = limbSwing * 0.6662F;
        armR.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * limbSwingAmount;
        armL.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
        armR.zRot = 0.0F;
        armL.zRot = 0.0F;
        legR.xRot = Mth.cos(swing) * 1.4F * limbSwingAmount;
        legL.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * limbSwingAmount;
        legR.yRot = 0.0F;
        legL.yRot = 0.0F;
        if (handL != null) {
            handL.xRot = armL.xRot;
            handR.xRot = armR.xRot;
        }
    }

    /** 15 个独立小怪各自的循环动画（逐步补齐）。 */
    private void animateIndependent(SixtySecondsMonsterEntity.Variant v, float limbSwing,
                                    float limbSwingAmount, float t) {
        switch (v) {
            case SPITTER -> {
                spitter.visible = true;
                spitHump.xScale = spitHump.yScale = spitHump.zScale = 1.0F + Mth.sin(t * 0.9F) * 0.08F;
                spitHead.xRot = Mth.sin(t * 0.5F) * 0.10F;
            }
            case STALKER -> {
                stalker.visible = true;
                for (int i = 0; i < 4; i++) {
                    stalkerLegs[i].xRot = Mth.sin(t * 2.2F + i * 1.6F) * 0.45F * (0.3F + limbSwingAmount);
                }
                stalkerBladeL.zRot = 0.35F + Mth.sin(t * 1.2F) * 0.20F;
                stalkerBladeR.zRot = -0.35F - Mth.sin(t * 1.2F) * 0.20F;
                stalkerHead.xRot = Mth.sin(t * 0.6F) * 0.12F;
            }
            case HOWLER -> {
                howler.visible = true;
                float howl = 0.35F + Mth.sin(t * 0.7F) * 0.30F;
                howlerJaw.xRot = howl;
                howlerChest.xScale = howlerChest.yScale = 1.0F + Mth.sin(t * 0.7F) * 0.05F;
            }
            case BLOATER -> {
                bloater.visible = true;
                for (int i = 0; i < 4; i++) {
                    bloaterPustules[i].xScale = bloaterPustules[i].yScale = bloaterPustules[i].zScale =
                            1.0F + Mth.sin(t * 1.1F + i * 0.8F) * 0.14F;
                }
            }
            case JUGGERNAUT -> {
                juggernaut.visible = true;
                jugShoulderL.zRot = Mth.sin(t * 0.5F) * 0.05F;
                jugShoulderR.zRot = -Mth.sin(t * 0.5F) * 0.05F;
                jugHead.yRot = Mth.sin(t * 0.4F) * 0.10F;
            }
            case CINDERLING -> {
                cinderling.visible = true;
                cinderCore.xScale = cinderCore.yScale = cinderCore.zScale =
                        1.0F + Mth.sin(t * 2.2F) * 0.14F;                 // 核心脉动
                cinderFlame.yScale = 1.0F + Mth.sin(t * 2.6F) * 0.22F;    // 火苗跳动
            }
            case FROSTLING -> {
                frostling.visible = true;
                frostCrown.yRot = Mth.sin(t * 0.4F) * 0.08F;
                frostSpikeL.zRot = -0.60F + Mth.sin(t * 0.9F) * 0.10F;
                frostSpikeR.zRot = 0.60F - Mth.sin(t * 0.9F) * 0.10F;
            }
            case HUSKBRUTE -> {
                huskbrute.visible = true;
                float sw2 = limbSwing * 0.6F;
                huskArmL.xRot = Mth.cos(sw2) * 0.9F * limbSwingAmount;
                huskArmR.xRot = Mth.cos(sw2 + (float) Math.PI) * 0.9F * limbSwingAmount;
            }
            case RAVENOR -> {
                ravenor.visible = true;
                float flap = Mth.sin(t * 1.4F) * 0.35F;
                ravenWingL.zRot = -0.20F + flap;
                ravenWingR.zRot = 0.20F - flap;
            }
            case WAILER -> {
                wailer.visible = true;
                wailerJaw.xRot = 0.30F + Mth.sin(t * 0.8F) * 0.28F;
                wailerRibbon.xRot = 0.15F + Mth.sin(t * 0.6F) * 0.10F;
            }
            case BURSTER -> {
                burster.visible = true;
                for (int i = 0; i < 6; i++) {
                    bursterSpikes[i].xScale = bursterSpikes[i].yScale = bursterSpikes[i].zScale =
                            1.0F + Mth.sin(t * 1.3F + i * 0.7F) * 0.18F;
                }
            }
            case GOREHOUND -> {
                gorehound.visible = true;
                float gs = limbSwing * 0.9F;
                houndLegFL.xRot = Mth.cos(gs) * 0.8F * limbSwingAmount;
                houndLegFR.xRot = Mth.cos(gs + (float) Math.PI) * 0.8F * limbSwingAmount;
                houndLegBL.xRot = Mth.cos(gs + (float) Math.PI) * 0.8F * limbSwingAmount;
                houndLegBR.xRot = Mth.cos(gs) * 0.8F * limbSwingAmount;
                houndHead.xRot = Mth.sin(t * 0.5F) * 0.08F;
                houndTail.yRot = Mth.sin(t * 1.1F) * 0.35F;
            }
            case SHADOWMUTE -> {
                shadowmute.visible = true;
                // 幽魂上下漂浮 + 下摆飘动
                shadowmute.y = Mth.sin(t * 0.7F) * 0.8F;
                shadowHood.yRot = Mth.sin(t * 0.4F) * 0.10F;
                shadowTail.xRot = Mth.sin(t * 0.9F) * 0.12F;
            }
            case BONELORD -> {
                bonelord.visible = true;
                boneRibs.yScale = 1.0F + Mth.sin(t * 0.8F) * 0.06F;       // 肋骨呼吸
                boneCape.xRot = 0.10F + Mth.sin(t * 0.7F) * 0.09F;
            }
            case SPINEWALKER -> {
                spinewalker.visible = true;
                for (int i = 0; i < 5; i++) {
                    spineSpikes[i].yScale = 1.0F + Mth.sin(t * 1.0F + i * 0.6F) * 0.16F;
                }
            }
            default -> { }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
