package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 海怪模型（128×128 贴图，多套几何按变体切换）。
 *
 * <ul>
 *   <li><b>classic</b>（KRAKEN / SERPENT / LEVIATHAN）：头足类轮廓——椭球套体 + 头冠 + 双眼 + 喙 + 8 条三节触手。</li>
 *   <li><b>abyss_kraken</b>（深渊克拉肯）：加高套体 + 背脊甲 + 头冠板 + 6 条四节长触手（末节带倒钩）。</li>
 *   <li><b>trench_serpent</b>（海沟巨蛇）：长头 + 双角 + 8 节逐级收窄蛇躯 + 背鳍带 + 胸鳍 + 竖尾鳍。</li>
 *   <li><b>sunken_leviathan</b>（沉没利维坦）：巨口鲸兽 —— 巨头 + 下颌 + 骨脊板 + 外露肋骨 + 双鳍 + 尾鳍。</li>
 * </ul>
 *
 * 模型空间 y=24 基线；{@code texOffs}/尺寸与 {@code tools/gen_ocean_entities.py}
 * 逐面上色的矩形一一对应——改模型须同步改脚本。
 *
 * <h3>UV 布局（各变体独立贴图，故区域可重叠复用）</h3>
 * <pre>
 * classic:            body(0,0) 14×18×14 | crest(0,34) 8×4×2 | eye(60,0) 4×4×4
 *                     beak(60,12) 6×3×3  | tent1(0,44) 4×8×4 | tent2(20,44) 3×7×3 | tent3(36,44) 2×6×2
 * abyss_kraken:       mantle(0,0) 16×22×16 | ridge(0,40) 4×6×18 | eye(66,0) 5×5×5 | glow(66,12) 3×3×1
 *                     beak(88,0) 8×4×4 | tentA(48,40) 5×10×5 | tentB(70,40) 4×9×4
 *                     tentC(88,40) 3×8×3 | tentD(0,66) 2×8×2 | barb(12,66) 2×4×2 | crown(24,66) 10×5×3
 * trench_serpent:     head(0,0) 12×11×16 | jaw(58,0) 12×4×14 | horn(58,20) 2×9×2 | eye(68,20) 3×3×2
 *                     seg(0,30) 11×11×10 | dorsal(44,30) 1×7×10 | tailFin(0,54) 1×14×10
 *                     pect(24,54) 9×2×5 | tailTip(54,54) 5×5×8 | fang(82,54) 1×4×1
 * sunken_leviathan:   head(0,0) 14×12×14 | maw(58,0) 14×4×12 | teeth(58,18) 14×2×1 | eye(90,18) 3×3×2
 *                     torso(0,28) 16×14×20 | spine(74,28) 4×5×14 | tail(0,64) 8×8×12
 *                     fluke(42,64) 14×1×8 | flipper(88,64) 10×2×6 | rib(0,86) 2×8×2
 *                     spike(12,86) 2×7×2 | barnacle(24,86) 3×2×3
 * </pre>
 */
public class OceanSeaMonsterModel extends EntityModel<OceanSeaMonsterEntity> {

    private static final int TEX_W = 128;
    private static final int TEX_H = 128;

    private static final int CLASSIC_TENTACLES = 8;
    private static final int ABYSS_TENTACLES = 6;
    private static final int ABYSS_SEGMENTS = 4;
    private static final int SERPENT_SEGMENTS = 8;
    /** 克拉肯腕足数量（独立建模）。 */
    private static final int KRAKEN_ARMS = 8;
    /** 海蛇躯干节数（独立建模）。 */
    private static final int SS_SEGMENTS = 7;

    private final ModelPart root;

    // ── classic（克拉肯 / 海蛇 / 利维坦，沿用旧几何与旧贴图）────────────
    private final ModelPart classic;
    private final ModelPart body;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart[] tentacles = new ModelPart[CLASSIC_TENTACLES];

    // ── abyss_kraken ────────────────────────────────────────────────
    private final ModelPart abyss;
    private final ModelPart abyssMantle;
    private final ModelPart abyssEyeL;
    private final ModelPart abyssEyeR;
    private final ModelPart[] abyssTentacles = new ModelPart[ABYSS_TENTACLES];
    private final ModelPart[][] abyssSegs = new ModelPart[ABYSS_TENTACLES][ABYSS_SEGMENTS];

    // ── trench_serpent ──────────────────────────────────────────────
    private final ModelPart serpent;
    private final ModelPart serpentHead;
    private final ModelPart serpentJaw;
    private final ModelPart[] serpentSegs = new ModelPart[SERPENT_SEGMENTS];
    private final ModelPart serpentTailFin;
    private final ModelPart serpentPectL;
    private final ModelPart serpentPectR;

    // ── kraken（克拉肯，独立建模，不再复用 classic 章鱼几何）──────────
    private final ModelPart kraken;
    private final ModelPart krakenHead;
    private final ModelPart krakenMantle;
    private final ModelPart[] krakenArms = new ModelPart[KRAKEN_ARMS];

    // ── sea_serpent（海蛇，独立建模，不再复用 classic 章鱼几何）────────
    private final ModelPart seaSerpent;
    private final ModelPart ssHead;
    private final ModelPart ssJaw;
    private final ModelPart[] ssSegs = new ModelPart[SS_SEGMENTS];
    private final ModelPart ssFin;
    private final ModelPart ssPectL;
    private final ModelPart ssPectR;

    // ── sunken_leviathan ────────────────────────────────────────────
    private final ModelPart leviathan;
    private final ModelPart leviHead;
    private final ModelPart leviMaw;
    private final ModelPart leviTorso;
    private final ModelPart leviTail;
    private final ModelPart leviFluke;
    private final ModelPart leviFlipperL;
    private final ModelPart leviFlipperR;

    public OceanSeaMonsterModel(ModelPart root) {
        this.root = root;

        this.classic = root.getChild("classic");
        this.body = classic.getChild("body");
        this.leftEye = body.getChild("left_eye");
        this.rightEye = body.getChild("right_eye");
        for (int i = 0; i < CLASSIC_TENTACLES; i++) {
            tentacles[i] = classic.getChild("tentacle_" + i);
        }

        this.abyss = root.getChild("abyss_kraken");
        this.abyssMantle = abyss.getChild("mantle");
        this.abyssEyeL = abyssMantle.getChild("eye_l");
        this.abyssEyeR = abyssMantle.getChild("eye_r");
        for (int i = 0; i < ABYSS_TENTACLES; i++) {
            abyssTentacles[i] = abyss.getChild("arm_" + i);
            ModelPart seg = abyssTentacles[i];
            for (int k = 0; k < ABYSS_SEGMENTS; k++) {
                seg = seg.getChild("seg_" + k);
                abyssSegs[i][k] = seg;
                // 逐节缩窄（ModelPart 的 scale 会向子节点传递，天然形成锥形）
                seg.xScale = seg.zScale = 0.9F;
            }
        }

        this.serpent = root.getChild("trench_serpent");
        this.serpentHead = serpent.getChild("head");
        this.serpentJaw = serpentHead.getChild("jaw");
        ModelPart sSeg = serpent;
        for (int k = 0; k < SERPENT_SEGMENTS; k++) {
            sSeg = sSeg.getChild("seg_" + k);
            serpentSegs[k] = sSeg;
            if (k > 0) {
                sSeg.xScale = sSeg.yScale = sSeg.zScale = 0.93F;
            }
        }
        this.serpentTailFin = serpentSegs[SERPENT_SEGMENTS - 1].getChild("tail_tip").getChild("tail_fin");
        this.serpentPectL = serpent.getChild("pect_l");
        this.serpentPectR = serpent.getChild("pect_r");

        // ── kraken（克拉肯，独立几何）──
        this.kraken = root.getChild("kraken");
        this.krakenHead = kraken.getChild("head");
        this.krakenMantle = kraken.getChild("mantle");
        for (int i = 0; i < KRAKEN_ARMS; i++) {
            krakenArms[i] = kraken.getChild("arm_" + i);
        }

        // ── sea_serpent（海蛇，独立几何）──
        this.seaSerpent = root.getChild("sea_serpent");
        this.ssHead = seaSerpent.getChild("head");
        this.ssJaw = ssHead.getChild("jaw");
        ModelPart ssSeg = seaSerpent;
        for (int k = 0; k < SS_SEGMENTS; k++) {
            ssSeg = ssSeg.getChild("seg_" + k);
            ssSegs[k] = ssSeg;
            if (k > 0) {
                ssSeg.xScale = ssSeg.yScale = ssSeg.zScale = 0.94F;
            }
        }
        this.ssFin = ssSegs[SS_SEGMENTS - 1].getChild("fin");
        this.ssPectL = seaSerpent.getChild("pect_l");
        this.ssPectR = seaSerpent.getChild("pect_r");

        this.leviathan = root.getChild("sunken_leviathan");
        this.leviHead = leviathan.getChild("head");
        this.leviMaw = leviHead.getChild("maw");
        this.leviTorso = leviathan.getChild("torso");
        this.leviTail = leviTorso.getChild("tail");
        this.leviFluke = leviTail.getChild("fluke");
        this.leviFlipperL = leviathan.getChild("flipper_l");
        this.leviFlipperR = leviathan.getChild("flipper_r");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        buildClassic(part.addOrReplaceChild("classic", CubeListBuilder.create(), PartPose.ZERO));
        buildAbyssKraken(part.addOrReplaceChild("abyss_kraken", CubeListBuilder.create(), PartPose.ZERO));
        buildTrenchSerpent(part.addOrReplaceChild("trench_serpent", CubeListBuilder.create(), PartPose.ZERO));
        buildSunkenLeviathan(part.addOrReplaceChild("sunken_leviathan", CubeListBuilder.create(), PartPose.ZERO));
        // 独立建模：克拉肯 / 海蛇（不再复用 classic 章鱼几何）
        buildKraken(part.addOrReplaceChild("kraken", CubeListBuilder.create(), PartPose.ZERO));
        buildSeaSerpent(part.addOrReplaceChild("sea_serpent", CubeListBuilder.create(), PartPose.ZERO));

        return LayerDefinition.create(mesh, TEX_W, TEX_H);
    }

    // ════════════════════════════════════════════════════════════════
    // KRAKEN 克拉肯（独立建模·重制）：锥形外套膜 + 顶部脊 + 巨眼头 + 喙
    //   + 眼柄发光巨眼 + 大侧鳍 + 8 条三节渐细腕足（带吸盘）
    // 贴图：ocean_kraken.png
    // ════════════════════════════════════════════════════════════════
    private static void buildKraken(PartDefinition part) {
        // 锥形外套膜 + 顶部脊
        part.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.0F, -16.0F, -7.0F, 14.0F, 16.0F, 14.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        part.addOrReplaceChild("mantle_ridge",
                CubeListBuilder.create().texOffs(44, 0)
                        .addBox(-2.0F, -4.0F, -7.0F, 4.0F, 4.0F, 14.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // 头部 + 喙
        part.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 46)
                        .addBox(-5.0F, -4.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, -2.0F));
        part.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(32, 46)
                        .addBox(-2.5F, 0.0F, -3.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, 10.0F, -4.0F));

        // 眼柄 + 巨眼 + 发光瞳（左右共享 UV）
        part.addOrReplaceChild("eye_stalk_l",
                CubeListBuilder.create().texOffs(50, 46)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(3.5F, 13.0F, -5.0F, 0.0F, 0.0F, -0.25F));
        part.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(56, 46)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(4.5F, 15.0F, -5.0F));
        part.addOrReplaceChild("pupil_l",
                CubeListBuilder.create().texOffs(70, 46)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(4.5F, 15.0F, -7.0F));
        part.addOrReplaceChild("eye_stalk_r",
                CubeListBuilder.create().texOffs(50, 46)
                        .addBox(-0.5F, -3.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-3.5F, 13.0F, -5.0F, 0.0F, 0.0F, 0.25F));
        part.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(56, 46)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(-4.5F, 15.0F, -5.0F));
        part.addOrReplaceChild("pupil_r",
                CubeListBuilder.create().texOffs(70, 46)
                        .addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-4.5F, 15.0F, -7.0F));

        // 大侧鳍（左右共享 UV）
        part.addOrReplaceChild("fin_l",
                CubeListBuilder.create().texOffs(0, 76)
                        .addBox(-7.0F, 0.0F, -3.0F, 7.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(-6.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.40F));
        part.addOrReplaceChild("fin_r",
                CubeListBuilder.create().texOffs(0, 76)
                        .addBox(0.0F, 0.0F, -3.0F, 7.0F, 1.0F, 6.0F),
                PartPose.offsetAndRotation(6.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.40F));

        // 8 条三节渐细腕足（共享 UV），逐条展开按 45° 分布
        float[][] armDefs = {
                { 3.5F, 8.0F,  3.5F, 0.0F, 0.0F,    0.0F},
                { 0.0F, 8.0F,  5.0F, 0.0F, 0.7854F, 0.0F},
                {-3.5F, 8.0F,  3.5F, 0.0F, 1.5708F, 0.0F},
                {-5.0F, 8.0F,  0.0F, 0.0F, 2.3562F, 0.0F},
                {-3.5F, 8.0F, -3.5F, 0.0F, 3.1416F, 0.0F},
                { 0.0F, 8.0F, -5.0F, 0.0F, 3.9270F, 0.0F},
                { 3.5F, 8.0F, -3.5F, 0.0F, 4.7124F, 0.0F},
                { 5.0F, 8.0F,  0.0F, 0.0F, 5.4978F, 0.0F},
        };
        for (int i = 0; i < 8; i++) {
            float[] a = armDefs[i];
            PartDefinition arm = part.addOrReplaceChild("arm_" + i,
                    CubeListBuilder.create(),
                    PartPose.offsetAndRotation(a[0], a[1], a[2], a[3], a[4], a[5]));
            arm.addOrReplaceChild("seg0",
                    CubeListBuilder.create().texOffs(20, 76)
                            .addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                    PartPose.ZERO);
            arm.addOrReplaceChild("suck0",
                    CubeListBuilder.create().texOffs(56, 76)
                            .addBox(-1.2F, -4.0F, 1.6F, 2.4F, 1.0F, 1.0F),
                    PartPose.ZERO);
            PartDefinition k1 = arm.addOrReplaceChild("seg1",
                    CubeListBuilder.create().texOffs(34, 76)
                            .addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.12F, 0.0F, 0.0F));
            k1.addOrReplaceChild("suck1",
                    CubeListBuilder.create().texOffs(56, 76)
                            .addBox(-1.0F, -3.0F, 1.2F, 2.0F, 1.0F, 1.0F),
                    PartPose.ZERO);
            PartDefinition k2 = k1.addOrReplaceChild("seg2",
                    CubeListBuilder.create().texOffs(46, 76)
                            .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.18F, 0.0F, 0.0F));
            k2.addOrReplaceChild("suck2",
                    CubeListBuilder.create().texOffs(56, 76)
                            .addBox(-0.6F, -3.0F, 0.8F, 1.2F, 1.0F, 1.0F),
                    PartPose.ZERO);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // SEA_SERPENT 海蛇（独立建模）：7 节渐细蛇躯 + 长头利颌 + 背鳍带 + 尾鳍
    // 贴图：ocean_serpent.png
    // ════════════════════════════════════════════════════════════════
    private static void buildSeaSerpent(PartDefinition part) {
        part.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -3.5F, -8.0F, 7.0F, 7.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        part.getChild("head").addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-3.0F, 0.0F, -7.0F, 6.0F, 2.5F, 7.0F),
                PartPose.offset(0.0F, 3.5F, 0.0F));
        part.getChild("head").addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(0, 26)
                        .addBox(0.0F, -4.0F, -1.0F, 1.5F, 4.0F, 1.5F),
                PartPose.offsetAndRotation(2.0F, -3.0F, -4.0F, 0.0F, 0.0F, -0.30F));
        part.getChild("head").addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(10, 26)
                        .addBox(-1.5F, -4.0F, -1.0F, 1.5F, 4.0F, 1.5F),
                PartPose.offsetAndRotation(-2.0F, -3.0F, -4.0F, 0.0F, 0.0F, 0.30F));
        // 7 节躯干：父子链逐节嵌套（逐节缩小由构造器 scale 完成）
        // texOffs / addBox 必须为字面量（贴图脚本正则解析），故逐节展开书写
        PartDefinition s0 = part.addOrReplaceChild("seg_0",
                CubeListBuilder.create().texOffs(24, 34)
                        .addBox(-5.0F, -2.5F, 0.0F, 10.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        PartDefinition s1 = s0.addOrReplaceChild("seg_1",
                CubeListBuilder.create().texOffs(24, 48)
                        .addBox(-4.65F, -4.65F, -4.65F, 9.3F, 9.3F, 7.6F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition s2 = s1.addOrReplaceChild("seg_2",
                CubeListBuilder.create().texOffs(24, 66)
                        .addBox(-4.3F, -4.3F, -4.3F, 8.6F, 8.6F, 7.2F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition s3 = s2.addOrReplaceChild("seg_3",
                CubeListBuilder.create().texOffs(24, 84)
                        .addBox(-3.95F, -3.95F, -3.95F, 7.9F, 7.9F, 6.8F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition s4 = s3.addOrReplaceChild("seg_4",
                CubeListBuilder.create().texOffs(24, 100)
                        .addBox(-3.6F, -3.6F, -3.6F, 7.2F, 7.2F, 6.4F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition s5 = s4.addOrReplaceChild("seg_5",
                CubeListBuilder.create().texOffs(24, 114)
                        .addBox(-3.25F, -3.25F, -3.25F, 6.5F, 6.5F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        PartDefinition s6 = s5.addOrReplaceChild("seg_6",
                CubeListBuilder.create().texOffs(64, 66)
                        .addBox(-2.9F, -2.9F, -2.9F, 5.8F, 5.8F, 5.6F),
                PartPose.offset(0.0F, 0.0F, 7.0F));
        s6.addOrReplaceChild("fin",
                CubeListBuilder.create().texOffs(64, 80)
                        .addBox(-0.5F, -4.0F, 0.0F, 1.0F, 8.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        part.addOrReplaceChild("pect_l",
                CubeListBuilder.create().texOffs(24, 26)
                        .addBox(-5.0F, -1.0F, -2.0F, 5.0F, 1.5F, 4.0F),
                PartPose.offsetAndRotation(-3.0F, 17.0F, -2.0F, 0.0F, 0.0F, 0.35F));
        part.addOrReplaceChild("pect_r",
                CubeListBuilder.create().texOffs(24, 34)
                        .addBox(0.0F, -1.0F, -2.0F, 5.0F, 1.5F, 4.0F),
                PartPose.offsetAndRotation(3.0F, 17.0F, -2.0F, 0.0F, 0.0F, -0.35F));
    }

    // ════════════════════════════════════════════════════════════════
    // classic（保持原几何，勿改 UV：ocean_kraken/serpent/leviathan 贴图依赖它）
    // 现在仅 LEVIATHAN 利维坦继续使用此章鱼几何
    // ════════════════════════════════════════════════════════════════
    private static void buildClassic(PartDefinition part) {
        PartDefinition bodyDef = part.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.0F, -9.0F, -7.0F, 14.0F, 18.0F, 14.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bodyDef.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(0, 34)
                        .addBox(-4.0F, -4.0F, -1.0F, 8.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -9.0F, 0.0F));

        bodyDef.addOrReplaceChild("left_eye",
                CubeListBuilder.create().texOffs(60, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(5.0F, -3.0F, -7.0F));
        bodyDef.addOrReplaceChild("right_eye",
                CubeListBuilder.create().texOffs(60, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(-5.0F, -3.0F, -7.0F));

        bodyDef.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(60, 12)
                        .addBox(-3.0F, 0.0F, -2.0F, 6.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 5.0F, -6.0F));

        for (int i = 0; i < CLASSIC_TENTACLES; i++) {
            double angle = (2 * Math.PI / CLASSIC_TENTACLES) * i;
            float x = (float) (Math.cos(angle) * 6.0);
            float z = (float) (Math.sin(angle) * 6.0);
            PartDefinition tentDef = part.addOrReplaceChild("tentacle_" + i,
                    CubeListBuilder.create(),
                    PartPose.offsetAndRotation(x, 24.0F + 7.0F, z, 0.3F, (float) angle, 0.0F));
            PartDefinition s1 = tentDef.addOrReplaceChild("seg1",
                    CubeListBuilder.create().texOffs(0, 44)
                            .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 8.0F, 4.0F),
                    PartPose.ZERO);
            PartDefinition s2 = s1.addOrReplaceChild("seg2",
                    CubeListBuilder.create().texOffs(20, 44)
                            .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F),
                    PartPose.offset(0.0F, 8.0F, 0.0F));
            s2.addOrReplaceChild("seg3",
                    CubeListBuilder.create().texOffs(36, 44)
                            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                    PartPose.offset(0.0F, 7.0F, 0.0F));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // ABYSS_KRAKEN 深渊克拉肯：装甲化头足类，6 条四节长臂
    // ════════════════════════════════════════════════════════════════
    private static void buildAbyssKraken(PartDefinition part) {
        PartDefinition mantle = part.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -11.0F, -8.0F, 16.0F, 22.0F, 16.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        mantle.addOrReplaceChild("ridge",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-2.0F, -6.0F, -9.0F, 4.0F, 6.0F, 18.0F),
                PartPose.offset(0.0F, -10.0F, 1.0F));
        mantle.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(24, 66)
                        .addBox(-5.0F, -5.0F, -1.5F, 10.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -10.0F, -6.0F, -0.4F, 0.0F, 0.0F));

        PartDefinition eyeL = mantle.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(66, 0)
                        .addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(5.5F, -3.0F, -7.5F));
        eyeL.addOrReplaceChild("glow",
                CubeListBuilder.create().texOffs(66, 12)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));
        PartDefinition eyeR = mantle.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(66, 0).mirror()
                        .addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(-5.5F, -3.0F, -7.5F));
        eyeR.addOrReplaceChild("glow",
                CubeListBuilder.create().texOffs(66, 12)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -3.0F));

        mantle.addOrReplaceChild("beak",
                CubeListBuilder.create().texOffs(88, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 7.0F, -7.0F));

        // 6 条四节长臂（UV 逐节独立；各臂共享同一组 UV）
        for (int i = 0; i < ABYSS_TENTACLES; i++) {
            double angle = (2 * Math.PI / ABYSS_TENTACLES) * i;
            float x = (float) (Math.cos(angle) * 6.5);
            float z = (float) (Math.sin(angle) * 6.5);
            PartDefinition arm = part.addOrReplaceChild("arm_" + i,
                    CubeListBuilder.create(),
                    PartPose.offsetAndRotation(x, 2.0F, z, 0.25F, (float) angle, 0.0F));
            PartDefinition s0 = arm.addOrReplaceChild("seg_0",
                    CubeListBuilder.create().texOffs(48, 40)
                            .addBox(-2.5F, -10.0F, -2.5F, 5.0F, 10.0F, 5.0F),
                    PartPose.ZERO);
            PartDefinition s1 = s0.addOrReplaceChild("seg_1",
                    CubeListBuilder.create().texOffs(70, 40)
                            .addBox(-2.0F, -9.0F, -2.0F, 4.0F, 9.0F, 4.0F),
                    PartPose.offset(0.0F, -9.5F, 0.0F));
            PartDefinition s2 = s1.addOrReplaceChild("seg_2",
                    CubeListBuilder.create().texOffs(88, 40)
                            .addBox(-1.5F, -8.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                    PartPose.offset(0.0F, -8.5F, 0.0F));
            PartDefinition s3 = s2.addOrReplaceChild("seg_3",
                    CubeListBuilder.create().texOffs(0, 66)
                            .addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                    PartPose.offset(0.0F, -7.5F, 0.0F));
            // 末节倒钩
            s3.addOrReplaceChild("barb",
                    CubeListBuilder.create().texOffs(12, 66)
                            .addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, 0.5F, 0.0F, 0.0F));
        }
    }

    // ════════════════════════════════════════════════════════════════
    // TRENCH_SERPENT 海沟巨蛇：长头 + 双角 + 8 节蛇躯 + 背鳍带
    // ════════════════════════════════════════════════════════════════
    private static void buildTrenchSerpent(PartDefinition part) {
        PartDefinition head = part.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-6.0F, -5.5F, -16.0F, 12.0F, 11.0F, 16.0F),
                PartPose.offset(0.0F, 16.0F, -6.0F));

        PartDefinition jaw = head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(58, 0)
                        .addBox(-6.0F, 0.0F, -14.0F, 12.0F, 4.0F, 14.0F),
                PartPose.offsetAndRotation(0.0F, 5.5F, -1.0F, 0.15F, 0.0F, 0.0F));
        jaw.addOrReplaceChild("fang_l",
                CubeListBuilder.create().texOffs(82, 54)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(4.0F, 0.0F, -12.0F));
        jaw.addOrReplaceChild("fang_r",
                CubeListBuilder.create().texOffs(82, 54)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(-4.0F, 0.0F, -12.0F));

        head.addOrReplaceChild("horn_l",
                CubeListBuilder.create().texOffs(58, 20)
                        .addBox(-1.0F, -9.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(3.5F, -5.0F, -4.0F, -0.45F, 0.0F, 0.25F));
        head.addOrReplaceChild("horn_r",
                CubeListBuilder.create().texOffs(58, 20).mirror()
                        .addBox(-1.0F, -9.0F, -1.0F, 2.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(-3.5F, -5.0F, -4.0F, -0.45F, 0.0F, -0.25F));
        head.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(68, 20)
                        .addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(5.2F, -2.0F, -11.0F, 0.0F, 1.5708F, 0.0F));
        head.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(68, 20)
                        .addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-5.2F, -2.0F, -11.0F, 0.0F, -1.5708F, 0.0F));

        // 8 节蛇躯逐节嵌套（父节缩放向子节传递，形成自然收窄）
        PartDefinition cur = part;
        for (int k = 0; k < SERPENT_SEGMENTS; k++) {
            PartPose pose = (k == 0)
                    ? PartPose.offset(0.0F, 16.0F, -6.0F)
                    : PartPose.offset(0.0F, 0.0F, 9.5F);
            cur = cur.addOrReplaceChild("seg_" + k,
                    CubeListBuilder.create().texOffs(0, 30)
                            .addBox(-5.5F, -5.5F, 0.0F, 11.0F, 11.0F, 10.0F),
                    pose);
            if (k < 6) {
                cur.addOrReplaceChild("dorsal",
                        CubeListBuilder.create().texOffs(44, 30)
                                .addBox(0.0F, -7.0F, 0.0F, 0.0F, 7.0F, 10.0F),
                        PartPose.offset(0.0F, -5.5F, 0.0F));
            }
        }
        PartDefinition tip = cur.addOrReplaceChild("tail_tip",
                CubeListBuilder.create().texOffs(54, 54)
                        .addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 9.5F));
        tip.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 54)
                        .addBox(0.0F, -7.0F, 0.0F, 0.0F, 14.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 7.0F));

        part.addOrReplaceChild("pect_l",
                CubeListBuilder.create().texOffs(24, 54)
                        .addBox(0.0F, -1.0F, -2.5F, 9.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, 18.0F, 2.0F, 0.0F, 0.0F, 0.35F));
        part.addOrReplaceChild("pect_r",
                CubeListBuilder.create().texOffs(24, 54).mirror()
                        .addBox(-9.0F, -1.0F, -2.5F, 9.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, 18.0F, 2.0F, 0.0F, 0.0F, -0.35F));
    }

    // ════════════════════════════════════════════════════════════════
    // SUNKEN_LEVIATHAN 沉没利维坦：巨口鲸兽 + 骨脊 + 外露肋骨
    // ════════════════════════════════════════════════════════════════
    private static void buildSunkenLeviathan(PartDefinition part) {
        PartDefinition head = part.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-7.0F, -6.0F, -14.0F, 14.0F, 12.0F, 14.0F),
                PartPose.offset(0.0F, 14.0F, -8.0F));

        PartDefinition maw = head.addOrReplaceChild("maw",
                CubeListBuilder.create().texOffs(58, 0)
                        .addBox(-7.0F, 0.0F, -12.0F, 14.0F, 4.0F, 12.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, -1.0F, 0.25F, 0.0F, 0.0F));
        maw.addOrReplaceChild("teeth",
                CubeListBuilder.create().texOffs(58, 18)
                        .addBox(-7.0F, -2.0F, 0.0F, 14.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, -12.0F));
        head.addOrReplaceChild("eye_l",
                CubeListBuilder.create().texOffs(90, 18)
                        .addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(7.2F, -2.0F, -9.0F, 0.0F, 1.5708F, 0.0F));
        head.addOrReplaceChild("eye_r",
                CubeListBuilder.create().texOffs(90, 18)
                        .addBox(-1.5F, -1.5F, -1.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-7.2F, -2.0F, -9.0F, 0.0F, -1.5708F, 0.0F));
        head.addOrReplaceChild("barnacle_a",
                CubeListBuilder.create().texOffs(24, 86)
                        .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(4.0F, -6.5F, -6.0F));
        head.addOrReplaceChild("barnacle_b",
                CubeListBuilder.create().texOffs(24, 86)
                        .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F),
                PartPose.offset(-3.0F, -6.5F, -10.0F));

        PartDefinition torso = part.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 28)
                        .addBox(-8.0F, -7.0F, 0.0F, 16.0F, 14.0F, 20.0F),
                PartPose.offset(0.0F, 14.0F, -8.0F));

        torso.addOrReplaceChild("spine_plate",
                CubeListBuilder.create().texOffs(74, 28)
                        .addBox(-2.0F, -5.0F, 0.0F, 4.0F, 5.0F, 14.0F),
                PartPose.offset(0.0F, -7.0F, 3.0F));
        for (int k = 0; k < 4; k++) {
            torso.addOrReplaceChild("spike_" + k,
                    CubeListBuilder.create().texOffs(12, 86)
                            .addBox(-1.0F, -7.0F, -1.0F, 2.0F, 7.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -11.0F, 2.0F + k * 4.0F, -0.15F, 0.0F, 0.0F));
            torso.addOrReplaceChild("rib_l_" + k,
                    CubeListBuilder.create().texOffs(0, 86)
                            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                    PartPose.offsetAndRotation(8.0F, -2.0F, 3.0F + k * 4.0F, 0.0F, 0.0F, -0.45F));
            torso.addOrReplaceChild("rib_r_" + k,
                    CubeListBuilder.create().texOffs(0, 86).mirror()
                            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                    PartPose.offsetAndRotation(-8.0F, -2.0F, 3.0F + k * 4.0F, 0.0F, 0.0F, 0.45F));
        }

        PartDefinition tail = torso.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 64)
                        .addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, 0.0F, 20.0F));
        tail.addOrReplaceChild("fluke",
                CubeListBuilder.create().texOffs(42, 64)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 12.0F));

        part.addOrReplaceChild("flipper_l",
                CubeListBuilder.create().texOffs(88, 64)
                        .addBox(0.0F, -1.0F, -3.0F, 10.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(8.0F, 19.0F, 2.0F, 0.0F, 0.0F, 0.3F));
        part.addOrReplaceChild("flipper_r",
                CubeListBuilder.create().texOffs(88, 64).mirror()
                        .addBox(-10.0F, -1.0F, -3.0F, 10.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(-8.0F, 19.0F, 2.0F, 0.0F, 0.0F, -0.3F));
    }

    @Override
    public void setupAnim(OceanSeaMonsterEntity entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        classic.visible = false;
        abyss.visible = false;
        serpent.visible = false;
        leviathan.visible = false;
        kraken.visible = false;
        seaSerpent.visible = false;

        switch (entity.getVariant()) {
            case KRAKEN -> {
                kraken.visible = true;
                animKraken(ageInTicks, netHeadYaw, headPitch);
            }
            case SERPENT -> {
                seaSerpent.visible = true;
                animSeaSerpent(ageInTicks, limbSwingAmount, netHeadYaw, headPitch);
            }
            case ABYSS_KRAKEN -> {
                abyss.visible = true;
                animAbyss(ageInTicks, netHeadYaw, headPitch);
            }
            case TRENCH_SERPENT -> {
                serpent.visible = true;
                animSerpent(ageInTicks, limbSwingAmount, netHeadYaw, headPitch);
            }
            case SUNKEN_LEVIATHAN -> {
                leviathan.visible = true;
                animLeviathan(ageInTicks, limbSwingAmount, netHeadYaw, headPitch);
            }
            default -> {
                classic.visible = true;
                animClassic(ageInTicks);
            }
        }

        // 受伤全身抖动（各变体共用根节点抖动）
        if (entity.hurtTime > 0) {
            float shake = Mth.sin(entity.hurtTime * 0.9F) * 0.08F;
            root.xRot = shake;
            root.zRot = shake;
        } else {
            root.xRot = 0.0F;
            root.zRot = 0.0F;
        }
    }

    /** 克拉肯：外套膜呼吸 + 8 腕足波浪摆动 + 头部朝向。 */
    private void animKraken(float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks * 0.12F;
        krakenMantle.xScale = 1.0F + Mth.sin(t * 1.1F) * 0.06F;
        krakenMantle.zScale = 1.0F + Mth.sin(t * 1.1F) * 0.06F;
        for (int i = 0; i < KRAKEN_ARMS; i++) {
            krakenArms[i].xRot = Mth.sin(t * 1.3F + i * 0.8F) * 0.32F;
            krakenArms[i].zRot = Mth.cos(t * 1.1F + i * 0.6F) * 0.24F;
        }
        krakenHead.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        krakenHead.xRot = headPitch * Mth.DEG_TO_RAD;
    }

    /** 海蛇：7 节正弦波动 + 张颌 + 胸鳍扇动。 */
    private void animSeaSerpent(float ageInTicks, float limbSwingAmount, float netHeadYaw, float headPitch) {
        float t = ageInTicks * 0.14F;
        for (int k = 0; k < SS_SEGMENTS; k++) {
            ssSegs[k].yRot = Mth.sin(t * 1.5F - k * 0.65F) * (0.22F + limbSwingAmount * 0.15F);
        }
        ssHead.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        ssHead.xRot = headPitch * Mth.DEG_TO_RAD;
        ssJaw.xRot = 0.12F + Mth.sin(t * 0.8F) * 0.10F;
        ssPectL.zRot = 0.35F + Mth.sin(t * 1.6F) * 0.16F;
        ssPectR.zRot = -0.35F - Mth.sin(t * 1.6F) * 0.16F;
        if (ssFin != null) ssFin.yRot = Mth.sin(t * 1.5F) * 0.20F;
    }

    private void animClassic(float ageInTicks) {
        body.y = 24.0F + Mth.sin(ageInTicks * 0.08F) * 1.0F;

        float eyeGlow = Mth.sin(ageInTicks * 0.15F) * 0.05F;
        leftEye.zScale = leftEye.xScale = 1.0F + eyeGlow;
        rightEye.zScale = rightEye.xScale = 1.0F + eyeGlow;

        for (int i = 0; i < CLASSIC_TENTACLES; i++) {
            float phase = i * 0.785F;
            float swing = Mth.sin(ageInTicks * 0.12F + phase) * 0.3F;
            float swing2 = Mth.cos(ageInTicks * 0.1F + phase) * 0.25F;
            tentacles[i].xRot = 0.3F + swing;
            tentacles[i].zRot = swing2;
            ModelPart seg1 = tentacles[i].getChild("seg1");
            seg1.xRot = swing * 0.5F;
            ModelPart seg2 = seg1.getChild("seg2");
            seg2.xRot = swing * 0.3F;
            seg2.getChild("seg3").xRot = swing * 0.2F;
        }
    }

    private void animAbyss(float ageInTicks, float netHeadYaw, float headPitch) {
        abyssMantle.y = 13.0F + Mth.sin(ageInTicks * 0.06F) * 1.4F;
        abyssMantle.yRot = netHeadYaw * 0.004F;
        abyssMantle.xRot = headPitch * 0.004F;

        float pulse = 1.0F + Mth.sin(ageInTicks * 0.2F) * 0.08F;
        abyssEyeL.xScale = abyssEyeL.yScale = pulse;
        abyssEyeR.xScale = abyssEyeR.yScale = pulse;

        for (int i = 0; i < ABYSS_TENTACLES; i++) {
            float phase = i * 1.047F; // π/3
            float swing = Mth.sin(ageInTicks * 0.09F + phase) * 0.35F;
            abyssTentacles[i].xRot = 0.25F + swing;
            abyssTentacles[i].zRot = Mth.cos(ageInTicks * 0.07F + phase) * 0.28F;
            for (int k = 0; k < ABYSS_SEGMENTS; k++) {
                abyssSegs[i][k].xRot = Mth.sin(ageInTicks * 0.09F + phase - (k + 1) * 0.5F) * (0.16F + k * 0.05F);
            }
        }
    }

    private void animSerpent(float ageInTicks, float limbSwingAmount, float netHeadYaw, float headPitch) {
        float speed = 0.09F + limbSwingAmount * 0.06F;
        serpentHead.yRot = Mth.sin(ageInTicks * speed) * 0.18F + netHeadYaw * 0.005F;
        serpentHead.xRot = headPitch * 0.005F;
        serpentJaw.xRot = 0.15F + Mth.sin(ageInTicks * 0.12F) * 0.18F;

        for (int k = 0; k < SERPENT_SEGMENTS; k++) {
            float wave = Mth.sin(ageInTicks * speed - k * 0.55F);
            serpentSegs[k].yRot = wave * (0.16F + k * 0.015F);
            serpentSegs[k].xRot = Mth.cos(ageInTicks * speed * 0.6F - k * 0.4F) * 0.05F;
        }
        serpentTailFin.yRot = Mth.sin(ageInTicks * speed - SERPENT_SEGMENTS * 0.55F) * 0.3F;
        serpentPectL.zRot = 0.35F + Mth.sin(ageInTicks * 0.1F) * 0.18F;
        serpentPectR.zRot = -0.35F - Mth.sin(ageInTicks * 0.1F) * 0.18F;
    }

    private void animLeviathan(float ageInTicks, float limbSwingAmount, float netHeadYaw, float headPitch) {
        float sway = Mth.sin(ageInTicks * 0.05F);
        leviTorso.yRot = sway * 0.06F;
        leviTorso.y = 14.0F + sway * 1.2F;
        leviHead.y = 14.0F + sway * 1.2F;
        leviHead.yRot = netHeadYaw * 0.004F + sway * 0.05F;
        leviHead.xRot = headPitch * 0.004F;

        // 巨口周期性张合（吞噬预警）
        float open = Mth.sin(ageInTicks * 0.055F);
        leviMaw.xRot = 0.25F + Math.max(0.0F, open) * 0.55F;

        leviTail.yRot = Mth.sin(ageInTicks * (0.07F + limbSwingAmount * 0.05F) - 0.8F) * 0.22F;
        leviFluke.xRot = Mth.sin(ageInTicks * 0.09F - 1.2F) * 0.25F;
        leviFlipperL.zRot = 0.3F + Mth.sin(ageInTicks * 0.06F) * 0.22F;
        leviFlipperR.zRot = -0.3F - Mth.sin(ageInTicks * 0.06F) * 0.22F;
    }

    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack,
            com.mojang.blaze3d.vertex.VertexConsumer buffer, int packedLight,
            int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
