package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.OceanFloorMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 海底小怪专属模型（6 变体独立几何）。
 *
 * 每个变体拥有独立 PartGroup：hermit_crab / urchin / eel / wraith / lurker / guardian。
 * setupAnim 阶段按实体变体切换可见性，并播放对应的简单循环动画。
 *
 * ⚠ 模型空间 y=24 为脚部/触地基准；贴图 128×128。
 * UV 坐标与 {@code tools/gen_ocean_entities.py} 一一对应，改模型需同步改脚本。
 */
public class OceanFloorMonsterModel extends EntityModel<OceanFloorMonsterEntity> {

    private static final int URCHIN_SPIKES = 16;
    private static final int CRAB_LEG_PAIRS = 3;
    private static final int EEL_SEGMENTS = 5;

    private final ModelPart root;
    private final ModelPart hermitCrab;
    private final ModelPart urchin;
    private final ModelPart eel;
    private final ModelPart wraith;
    private final ModelPart lurker;
    private final ModelPart guardian;

    // 缓存动画用子部件，避免每帧 getChild 递归查找
    private final ModelPart[] crabLegsL = new ModelPart[CRAB_LEG_PAIRS];
    private final ModelPart[] crabLegsR = new ModelPart[CRAB_LEG_PAIRS];
    private final ModelPart crabPincerL;
    private final ModelPart crabPincerR;
    private final ModelPart[] urchinSpikes = new ModelPart[URCHIN_SPIKES];
    private final ModelPart eelHead;
    private final ModelPart[] eelSegs = new ModelPart[EEL_SEGMENTS];
    private final ModelPart eelDorsal;
    private final ModelPart wraithHead;
    private final ModelPart wraithTorso;
    private final ModelPart wraithTail0;
    private final ModelPart wraithTail1;
    private final ModelPart wraithTail2;
    private final ModelPart wraithChain;
    private final ModelPart wraithArmL;
    private final ModelPart wraithArmR;
    private final ModelPart lurkerJaw;
    private final ModelPart lurkerLure;
    private final ModelPart lurkerTail;
    private final ModelPart lurkerTailFin;
    private final ModelPart lurkerPectL;
    private final ModelPart lurkerPectR;
    private final ModelPart guardHoverRune;
    private final ModelPart guardRuneRing;
    private final ModelPart guardShoulderL;
    private final ModelPart guardShoulderR;
    private final ModelPart guardCoralL;
    private final ModelPart guardCoralR;

    public OceanFloorMonsterModel(ModelPart root) {
        this.root = root;
        this.hermitCrab = root.getChild("hermit_crab");
        this.urchin = root.getChild("urchin");
        this.eel = root.getChild("eel");
        this.wraith = root.getChild("wraith");
        this.lurker = root.getChild("lurker");
        this.guardian = root.getChild("guardian");

        ModelPart crabBody = hermitCrab.getChild("body");
        for (int s = 0; s < CRAB_LEG_PAIRS; s++) {
            crabLegsL[s] = crabBody.getChild("leg_l_" + s);
            crabLegsR[s] = crabBody.getChild("leg_r_" + s);
        }
        this.crabPincerL = crabBody.getChild("pincer_left");
        this.crabPincerR = crabBody.getChild("pincer_right");

        for (int s = 0; s < URCHIN_SPIKES; s++) {
            urchinSpikes[s] = urchin.getChild("spike_" + s);
        }

        this.eelHead = eel.getChild("head");
        ModelPart seg = eel.getChild("seg0");
        eelSegs[0] = seg;
        for (int k = 1; k < EEL_SEGMENTS; k++) {
            seg = seg.getChild("seg" + k);
            eelSegs[k] = seg;
            // 缩放沿父子链累乘 → 自然锥形尾
            seg.xScale = seg.yScale = 0.9F;
        }
        this.eelDorsal = eel.getChild("dorsal");

        this.wraithHead = wraith.getChild("head");
        this.wraithTorso = wraith.getChild("torso");
        this.wraithTail0 = wraith.getChild("tail0");
        this.wraithTail1 = wraithTail0.getChild("tail1");
        this.wraithTail2 = wraithTail1.getChild("tail2");
        this.wraithChain = wraith.getChild("chain");
        this.wraithArmL = wraith.getChild("arm_l");
        this.wraithArmR = wraith.getChild("arm_r");

        ModelPart lurkerHead = lurker.getChild("head");
        this.lurkerJaw = lurkerHead.getChild("jaw");
        this.lurkerLure = lurker.getChild("lure_stem");
        this.lurkerTail = lurker.getChild("body").getChild("tail");
        this.lurkerTailFin = lurkerTail.getChild("tail_fin");
        this.lurkerPectL = lurker.getChild("pect_l");
        this.lurkerPectR = lurker.getChild("pect_r");

        this.guardHoverRune = guardian.getChild("hover_rune");
        this.guardRuneRing = guardian.getChild("core").getChild("rune_ring");
        this.guardShoulderL = guardian.getChild("shoulder_l");
        this.guardShoulderR = guardian.getChild("shoulder_r");
        this.guardCoralL = guardian.getChild("coral_arm_l");
        this.guardCoralR = guardian.getChild("coral_arm_r");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ════════════════════════════════════════════════════════════
        // 1. HERMIT_CRAB 寄居蟹（y=24 贴地，体宽约 1.2 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition hc = root.addOrReplaceChild("hermit_crab", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition hcBody = hc.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, -6.0F, 10.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        hc.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 20).addBox(-6.0F, -5.5F, -5.5F, 12.0F, 11.0F, 11.0F),
                PartPose.offset(0.0F, 16.0F, 6.0F));
        hc.addOrReplaceChild("spire",
                CubeListBuilder.create().texOffs(48, 20).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 11.0F, 10.0F));
        PartDefinition hcHead = hcBody.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(48, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 3.0F, -6.0F));
        hcHead.addOrReplaceChild("eye_left",
                CubeListBuilder.create().texOffs(70, 0).addBox(0.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -1.0F, -2.0F, 0.0F, 0.0F, 0.35F));
        hcHead.addOrReplaceChild("eye_right",
                CubeListBuilder.create().texOffs(70, 0).addBox(-2.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -1.0F, -2.0F, 0.0F, 0.0F, -0.35F));
        hcBody.addOrReplaceChild("claw_left",
                CubeListBuilder.create().texOffs(80, 0).addBox(0.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(5.0F, 3.0F, -4.0F, 0.0F, 0.0F, -0.15F));
        hcBody.addOrReplaceChild("pincer_left",
                CubeListBuilder.create().texOffs(80, 10).addBox(0.0F, -3.5F, -3.0F, 9.0F, 7.0F, 6.0F),
                PartPose.offsetAndRotation(12.0F, 3.0F, -4.0F, 0.0F, 0.0F, 0.25F));
        hcBody.addOrReplaceChild("claw_right",
                CubeListBuilder.create().texOffs(80, 0).mirror().addBox(-8.0F, -2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offsetAndRotation(-5.0F, 3.0F, -4.0F, 0.0F, 0.0F, 0.15F));
        hcBody.addOrReplaceChild("pincer_right",
                CubeListBuilder.create().texOffs(80, 10).mirror().addBox(-9.0F, -3.5F, -3.0F, 9.0F, 7.0F, 6.0F),
                PartPose.offsetAndRotation(-12.0F, 3.0F, -4.0F, 0.0F, 0.0F, -0.25F));
        for (int s = 0; s < CRAB_LEG_PAIRS; s++) {
            float z = -2.0F + s * 4.0F;
            PartDefinition legL = hcBody.addOrReplaceChild("leg_l_" + s,
                    CubeListBuilder.create().texOffs(0, 44).addBox(0.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(5.0F, 5.0F, z, 0.0F, 0.0F, 0.5F));
            legL.addOrReplaceChild("foot_l_" + s,
                    CubeListBuilder.create().texOffs(0, 50).addBox(0.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.95F));
            PartDefinition legR = hcBody.addOrReplaceChild("leg_r_" + s,
                    CubeListBuilder.create().texOffs(0, 44).mirror().addBox(-6.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(-5.0F, 5.0F, z, 0.0F, 0.0F, -0.5F));
            legR.addOrReplaceChild("foot_r_" + s,
                    CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-6.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F),
                    PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.95F));
        }
        hc.addOrReplaceChild("barnacle_a",
                CubeListBuilder.create().texOffs(20, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(3.5F, 11.0F, 4.0F));
        hc.addOrReplaceChild("barnacle_b",
                CubeListBuilder.create().texOffs(20, 44).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-4.5F, 14.0F, 9.5F));

        // ════════════════════════════════════════════════════════════
        // 2. URCHIN 海胆（近似球体 + 放射棘刺，直径约 1.2 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition ur = root.addOrReplaceChild("urchin", CubeListBuilder.create(), PartPose.ZERO);
        ur.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        for (int s = 0; s < 16; s++) {
            double a = s * 0.785;
            double b = (s % 3) * 0.45 + 0.35;
            float x = (float) (Math.cos(a) * Math.cos(b) * 5.8F);
            float y = (float) (Math.sin(b) * 5.8F);
            float z = (float) (Math.sin(a) * Math.cos(b) * 5.8F);
            PartPose pose = PartPose.offsetAndRotation(x, 18.0F + y, z,
                    (float) (-b + Math.PI / 2), (float) a, 0.0F);
            if (s % 2 == 0) {
                ur.addOrReplaceChild("spike_" + s,
                        CubeListBuilder.create().texOffs(50, 0)
                                .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 10.0F, 2.0F), pose);
            } else {
                ur.addOrReplaceChild("spike_" + s,
                        CubeListBuilder.create().texOffs(60, 0)
                                .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 6.0F, 2.0F), pose);
            }
        }
        ur.addOrReplaceChild("eye",
                CubeListBuilder.create().texOffs(96, 0).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 18.0F, -6.2F));
        ur.addOrReplaceChild("mouth",
                CubeListBuilder.create().texOffs(70, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // ════════════════════════════════════════════════════════════
        // 3. EEL 鳗鱼（长蛇形 6 节 + 背鳍 + 尾鳍，全长约 4 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition el = root.addOrReplaceChild("eel", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition eelHead = el.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -8.0F, 6.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 21.0F, -16.0F));
        eelHead.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(30, 0).addBox(-3.0F, 0.0F, -7.0F, 6.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -8.0F, 0.18F, 0.0F, 0.0F));
        eelHead.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(3.2F, -1.0F, -4.0F));
        eelHead.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(58, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-3.2F, -1.0F, -4.0F));
        PartDefinition seg0 = el.addOrReplaceChild("seg0",
                CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 7.0F),
                PartPose.offset(0.0F, 21.0F, -8.0F));
        PartDefinition prev = seg0;
        for (int k = 1; k < EEL_SEGMENTS; k++) {
            // 盒体尺寸统一，锥形收窄靠构造函数里的 xScale/yScale 逐节传递
            prev = prev.addOrReplaceChild("seg" + k,
                    CubeListBuilder.create().texOffs(0, 16)
                            .addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 7.0F),
                    PartPose.offset(0.0F, 0.0F, 6.5F));
        }
        prev.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(28, 16).addBox(0.0F, -7.0F, 0.0F, 0.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 6.5F));
        eelHead.addOrReplaceChild("gill_l",
                CubeListBuilder.create().texOffs(0, 34).addBox(0.0F, -2.0F, -1.5F, 0.0F, 4.0F, 3.0F),
                PartPose.offset(3.1F, 0.5F, -1.5F));
        eelHead.addOrReplaceChild("gill_r",
                CubeListBuilder.create().texOffs(0, 34).addBox(0.0F, -2.0F, -1.5F, 0.0F, 4.0F, 3.0F),
                PartPose.offset(-3.1F, 0.5F, -1.5F));
        el.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(48, 16).addBox(0.0F, -12.0F, 0.0F, 0.0F, 12.0F, 10.0F),
                PartPose.offset(0.0F, 18.0F, -8.0F));
        el.addOrReplaceChild("pect_l",
                CubeListBuilder.create().texOffs(72, 16).addBox(0.0F, 0.0F, -1.5F, 5.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(3.0F, 22.0F, -13.0F, 0.0F, 0.0F, 0.5F));
        el.addOrReplaceChild("pect_r",
                CubeListBuilder.create().texOffs(72, 16).mirror().addBox(-5.0F, 0.0F, -1.5F, 5.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(-3.0F, 22.0F, -13.0F, 0.0F, 0.0F, -0.5F));

        // ════════════════════════════════════════════════════════════
        // 4. WRAITH 沉船怨灵（人形 + 破斗篷 + 雾尾，高度约 2.5 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition wr = root.addOrReplaceChild("wraith", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition wrHead = wr.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        wrHead.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(30, 0).addBox(-4.5F, -8.0F, -4.5F, 9.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        wrHead.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(68, 0).addBox(0.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(0.5F, -3.0F, -3.6F));
        wrHead.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(68, 0).addBox(-2.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-0.5F, -3.0F, -3.6F));
        PartDefinition wrBody = wr.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, 0.0F, -2.5F, 9.0F, 12.0F, 5.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        wrBody.addOrReplaceChild("cloak",
                CubeListBuilder.create().texOffs(44, 18).addBox(-5.5F, 0.0F, -1.0F, 11.0F, 14.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 2.6F));
        wr.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(30, 18).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offsetAndRotation(5.0F, 14.0F, 0.0F, 0.0F, 0.0F, -0.08F));
        wr.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(30, 18).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offsetAndRotation(-5.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.08F));
        PartDefinition wrTail0 = wr.addOrReplaceChild("tail0",
                CubeListBuilder.create().texOffs(0, 36).addBox(-3.5F, 0.0F, -2.5F, 7.0F, 8.0F, 5.0F),
                PartPose.offset(0.0F, 25.0F, 0.0F));
        PartDefinition wrTail1 = wrTail0.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(26, 36).addBox(-2.5F, 0.0F, -2.0F, 5.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        wrTail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(46, 36).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        wr.addOrReplaceChild("chain",
                CubeListBuilder.create().texOffs(60, 36).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(3.5F, 18.0F, 2.0F, 0.2F, 0.0F, 0.15F));

        // ════════════════════════════════════════════════════════════
        // 5. LURKER 深渊掠食者（鮟鱇型，头部巨大，高约 2 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition lu = root.addOrReplaceChild("lurker", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition luHead = lu.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -6.0F, -8.0F, 14.0F, 12.0F, 12.0F),
                PartPose.offset(0.0F, 17.0F, 4.0F));
        PartDefinition luJaw = luHead.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(54, 0).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 4.0F, 11.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -7.0F, -0.35F, 0.0F, 0.0F));
        luJaw.addOrReplaceChild("teeth",
                CubeListBuilder.create().texOffs(54, 17).addBox(-7.0F, -2.0F, 0.0F, 14.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -7.0F));
        lu.addOrReplaceChild("lure_stem",
                CubeListBuilder.create().texOffs(0, 26).addBox(0.0F, -10.0F, 0.0F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F, -0.4F, 0.0F, 0.0F));
        lu.addOrReplaceChild("lure_bulb",
                CubeListBuilder.create().texOffs(6, 26).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -12.0F, -2.0F));
        PartDefinition luBody = lu.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(24, 26).addBox(-5.5F, -5.0F, 0.0F, 11.0F, 10.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 8.0F));
        PartDefinition luTail = luBody.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(72, 26).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 12.0F));
        luTail.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 50).addBox(0.0F, -6.0F, 0.0F, 0.0F, 12.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));
        lu.addOrReplaceChild("pect_l",
                CubeListBuilder.create().texOffs(20, 50).addBox(0.0F, -0.5F, -2.5F, 8.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, 19.0F, 12.0F, 0.0F, 0.0F, 0.35F));
        lu.addOrReplaceChild("pect_r",
                CubeListBuilder.create().texOffs(20, 50).mirror().addBox(-8.0F, -0.5F, -2.5F, 8.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, 19.0F, 12.0F, 0.0F, 0.0F, -0.35F));
        lu.addOrReplaceChild("spine_fin",
                CubeListBuilder.create().texOffs(48, 50).addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 11.0F, 10.0F));
        luHead.addOrReplaceChild("side_eye",
                CubeListBuilder.create().texOffs(56, 50).addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -8.2F));

        // ════════════════════════════════════════════════════════════
        // 6. GUARDIAN 珊瑚守卫（宽重型石构，高约 2.8 格）
        // ════════════════════════════════════════════════════════════
        PartDefinition gu = root.addOrReplaceChild("guardian", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition guCore = gu.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -7.0F, -7.0F, 14.0F, 14.0F, 14.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        guCore.addOrReplaceChild("eye",
                CubeListBuilder.create().texOffs(58, 0).addBox(-3.0F, -3.0F, -1.0F, 6.0F, 6.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -7.2F));
        guCore.addOrReplaceChild("rune_ring",
                CubeListBuilder.create().texOffs(58, 10).addBox(-5.0F, -5.0F, -0.5F, 10.0F, 10.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -7.6F));
        gu.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 30).addBox(-6.0F, -2.0F, -6.0F, 12.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        gu.addOrReplaceChild("shoulder_l",
                CubeListBuilder.create().texOffs(50, 30).addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offsetAndRotation(8.0F, 13.0F, 0.0F, 0.0F, 0.0F, -0.25F));
        gu.addOrReplaceChild("shoulder_r",
                CubeListBuilder.create().texOffs(50, 30).mirror().addBox(-1.5F, -5.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offsetAndRotation(-8.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.25F));
        gu.addOrReplaceChild("coral_arm_l",
                CubeListBuilder.create().texOffs(64, 30).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(9.5F, 16.0F, 2.0F, 0.15F, 0.0F, 0.15F));
        gu.addOrReplaceChild("coral_arm_r",
                CubeListBuilder.create().texOffs(64, 30).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(-9.5F, 16.0F, 2.0F, 0.15F, 0.0F, -0.15F));
        gu.addOrReplaceChild("base_plate",
                CubeListBuilder.create().texOffs(0, 48).addBox(-5.0F, -1.5F, -5.0F, 10.0F, 3.0F, 10.0F),
                PartPose.offset(0.0F, 23.5F, 0.0F));
        gu.addOrReplaceChild("back_spike",
                CubeListBuilder.create().texOffs(42, 48).addBox(-1.5F, -4.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 9.0F, 7.0F, 0.35F, 0.0F, 0.0F));
        gu.addOrReplaceChild("hover_rune",
                CubeListBuilder.create().texOffs(56, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(OceanFloorMonsterEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 默认隐藏所有变体组
        hermitCrab.visible = false;
        urchin.visible = false;
        eel.visible = false;
        wraith.visible = false;
        lurker.visible = false;
        guardian.visible = false;

        ModelPart active;
        switch (entity.getVariant()) {
            case URCHIN -> active = urchin;
            case EEL -> active = eel;
            case WRAITH -> active = wraith;
            case LURKER -> active = lurker;
            case GUARDIAN -> active = guardian;
            default -> active = hermitCrab;
        }
        active.visible = true;

        float t = ageInTicks * 0.1F;
        // 行走强度：陆行变体用 limbSwing 驱动，游动变体用时间驱动
        float walk = limbSwingAmount;

        if (active == hermitCrab) {
            // 寄居蟹：六足交替划动 + 大螯开合
            for (int s = 0; s < CRAB_LEG_PAIRS; s++) {
                float phase = limbSwing * 0.6F + s * 1.1F;
                float amp = 0.18F + walk * 0.5F;
                crabLegsL[s].zRot = 0.5F + Mth.sin(phase) * amp;
                crabLegsR[s].zRot = -0.5F - Mth.sin(phase + Mth.PI) * amp;
            }
            crabPincerL.zRot = 0.25F + Mth.sin(t * 0.7F) * 0.10F;
            crabPincerR.zRot = -0.25F - Mth.sin(t * 0.7F) * 0.10F;
        } else if (active == urchin) {
            // 海胆：整体缓慢摇滚 + 棘刺呼吸
            urchin.xRot = Mth.sin(t * 0.3F) * 0.06F;
            urchin.zRot = Mth.cos(t * 0.25F) * 0.05F;
            for (int s = 0; s < URCHIN_SPIKES; s++) {
                float sc = 1.0F + Mth.sin(t * 2.0F + s) * 0.05F;
                urchinSpikes[s].yScale = sc;
            }
        } else if (active == eel) {
            // 鳗鱼：整条身体正弦行波
            eelHead.yRot = Mth.sin(t * 0.8F) * 0.14F + netHeadYaw * 0.006F;
            eelHead.xRot = headPitch * 0.006F;
            for (int i = 0; i < EEL_SEGMENTS; i++) {
                eelSegs[i].yRot = Mth.sin(t * 0.8F - (i + 1) * 0.55F) * (0.12F + i * 0.02F);
            }
            eelDorsal.xRot = Mth.sin(t * 1.2F) * 0.08F;
        } else if (active == wraith) {
            // 怨灵：漂浮 + 雾尾垂摆 + 锁链摇晃
            float bob = Mth.sin(t * 0.6F) * 0.7F;
            wraithHead.y = 13.0F + bob;
            wraithTorso.y = 13.0F + bob;
            wraithHead.yRot = netHeadYaw * 0.0125F;
            wraithHead.xRot = headPitch * 0.0125F;
            wraithTail0.xRot = Mth.sin(t * 0.5F) * 0.08F;
            wraithTail1.xRot = Mth.sin(t * 0.5F - 0.4F) * 0.12F;
            wraithTail2.xRot = Mth.sin(t * 0.5F - 0.8F) * 0.18F;
            wraithChain.zRot = 0.15F + Mth.sin(t * 0.9F) * 0.10F;
            wraithArmL.zRot = -0.08F + Mth.sin(t * 0.4F) * 0.06F;
            wraithArmR.zRot = 0.08F - Mth.sin(t * 0.4F) * 0.06F;
        } else if (active == lurker) {
            // 潜伏者：巨口张合 + 诱饵灯摇曳 + 尾鳍推水
            lurkerJaw.xRot = -0.35F + Mth.sin(t * 0.9F) * 0.20F;
            lurkerLure.xRot = -0.4F + Mth.sin(t * 1.1F) * 0.14F;
            lurkerLure.zRot = Mth.cos(t * 0.9F) * 0.10F;
            lurkerTail.yRot = Mth.sin(t * 0.6F + walk * 3.0F) * 0.14F;
            lurkerTailFin.yRot = Mth.sin(t * 0.6F - 0.5F) * 0.22F;
            lurkerPectL.zRot = 0.35F + Mth.sin(t * 0.8F) * 0.14F;
            lurkerPectR.zRot = -0.35F - Mth.sin(t * 0.8F) * 0.14F;
        } else {
            // 守护者：悬浮符文自转 + 符文环反转 + 肩甲开合
            guardHoverRune.yRot = t * 0.5F;
            guardHoverRune.y = 8.0F + Mth.sin(t * 0.6F) * 0.5F;
            guardRuneRing.zRot = -t * 0.3F;
            guardShoulderL.zRot = -0.25F + Mth.sin(t * 0.5F) * 0.06F;
            guardShoulderR.zRot = 0.25F - Mth.sin(t * 0.5F) * 0.06F;
            guardCoralL.xRot = 0.15F + Mth.sin(t * 0.7F) * 0.08F;
            guardCoralR.xRot = 0.15F - Mth.sin(t * 0.7F) * 0.08F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
