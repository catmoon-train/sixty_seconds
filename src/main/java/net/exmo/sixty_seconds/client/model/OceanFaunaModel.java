package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.OceanFaunaEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 海洋生物群系模型（第二批次，10 变体独立几何）。
 *
 * <p>每个变体拥有独立 PartGroup：manta_ray / jellyfish / giant_squid / pufferfish /
 * starfish / seahorse / lionfish / iron_crab / nautilus / barracuda。
 * {@code setupAnim} 按实体变体切换可见性，并播放各自的循环动画（滑翔、伞盖搏动、
 * 触手摆动、膨胀、腕足蠕动、尾鳍摆动等）。
 *
 * <p><b>UV 约定</b>：每个变体使用<b>各自独立的一张 128×128 贴图</b>
 * （{@code ocean_<name>.png}），因此各变体的 UV 区域可以复用同一片坐标空间，
 * 互不冲突。贴图由 {@code tools/gen_ocean_fauna.py} 直接解析本文件生成，改模型后重跑即可。
 *
 * <p>模型空间 y=24 为触地基准；本族为游泳生物，重心约在 y=12~16。
 */
public class OceanFaunaModel extends EntityModel<OceanFaunaEntity> {

    /** 各变体的附肢数量（动画循环用；几何本身已展开为字面量书写）。 */
    private static final int JELLY_TENTACLES = 6;
    private static final int SQUID_ARMS = 8;
    private static final int PUFFER_SPIKES = 12;
    private static final int STAR_ARMS = 5;
    private static final int LION_SPINES = 8;
    private static final int CRAB_LEG_PAIRS = 3;
    private static final int NAUTILUS_TENTACLES = 6;

    private final ModelPart root;
    private final ModelPart mantaRay;
    private final ModelPart jellyfish;
    private final ModelPart giantSquid;
    private final ModelPart pufferfish;
    private final ModelPart starfish;
    private final ModelPart seahorse;
    private final ModelPart lionfish;
    private final ModelPart ironCrab;
    private final ModelPart nautilus;
    private final ModelPart barracuda;

    // 动画用子部件缓存
    private final ModelPart mantaWingL;
    private final ModelPart mantaWingR;
    private final ModelPart mantaTail;
    private final ModelPart[] jellyTentacles = new ModelPart[JELLY_TENTACLES];
    private final ModelPart jellyBell;
    private final ModelPart[] squidArms = new ModelPart[SQUID_ARMS];
    private final ModelPart squidMantle;
    private final ModelPart[] pufferSpikes = new ModelPart[PUFFER_SPIKES];
    private final ModelPart pufferBody;
    private final ModelPart[] starArms = new ModelPart[STAR_ARMS];
    private final ModelPart starDisc;
    private final ModelPart seaHorseHead;
    private final ModelPart seaHorseTail;
    private final ModelPart[] lionSpines = new ModelPart[LION_SPINES];
    private final ModelPart lionPecL;
    private final ModelPart lionPecR;
    private final ModelPart[] crabLegsL = new ModelPart[CRAB_LEG_PAIRS];
    private final ModelPart[] crabLegsR = new ModelPart[CRAB_LEG_PAIRS];
    private final ModelPart crabClawL;
    private final ModelPart crabClawR;
    private final ModelPart nautilusShell;
    private final ModelPart[] nautilusTentacles = new ModelPart[NAUTILUS_TENTACLES];
    private final ModelPart barracudaJaw;
    private final ModelPart barracudaTail;
    private final ModelPart barracudaDorsal;

    public OceanFaunaModel(ModelPart root) {
        this.root = root;
        this.mantaRay = root.getChild("manta_ray");
        this.jellyfish = root.getChild("jellyfish");
        this.giantSquid = root.getChild("giant_squid");
        this.pufferfish = root.getChild("pufferfish");
        this.starfish = root.getChild("starfish");
        this.seahorse = root.getChild("seahorse");
        this.lionfish = root.getChild("lionfish");
        this.ironCrab = root.getChild("iron_crab");
        this.nautilus = root.getChild("nautilus");
        this.barracuda = root.getChild("barracuda");

        this.mantaWingL = mantaRay.getChild("wing_l");
        this.mantaWingR = mantaRay.getChild("wing_r");
        this.mantaTail = mantaRay.getChild("tail");

        this.jellyBell = jellyfish.getChild("bell");
        for (int i = 0; i < JELLY_TENTACLES; i++) {
            jellyTentacles[i] = jellyfish.getChild("tentacle_" + i);
        }

        this.squidMantle = giantSquid.getChild("mantle");
        for (int i = 0; i < SQUID_ARMS; i++) {
            squidArms[i] = giantSquid.getChild("arm_" + i);
        }

        this.pufferBody = pufferfish.getChild("body");
        for (int i = 0; i < PUFFER_SPIKES; i++) {
            pufferSpikes[i] = pufferBody.getChild("spike_" + i);
        }

        this.starDisc = starfish.getChild("disc");
        for (int i = 0; i < STAR_ARMS; i++) {
            starArms[i] = starfish.getChild("arm_" + i);
        }

        this.seaHorseHead = seahorse.getChild("head");
        this.seaHorseTail = seahorse.getChild("tail0");
        // 卷曲尾：缩放沿父子链累乘 → 自然锥形
        ModelPart shTail1 = seaHorseTail.getChild("tail1");
        ModelPart shTail2 = shTail1.getChild("tail2");
        shTail1.xScale = shTail1.yScale = 0.9F;
        shTail2.xScale = shTail2.yScale = 0.85F;

        for (int i = 0; i < LION_SPINES; i++) {
            lionSpines[i] = lionfish.getChild("spine_" + i);
        }
        this.lionPecL = lionfish.getChild("pec_l");
        this.lionPecR = lionfish.getChild("pec_r");

        ModelPart crabBody = ironCrab.getChild("carapace");
        for (int i = 0; i < CRAB_LEG_PAIRS; i++) {
            crabLegsL[i] = crabBody.getChild("leg_l_" + i);
            crabLegsR[i] = crabBody.getChild("leg_r_" + i);
        }
        this.crabClawL = crabBody.getChild("claw_l");
        this.crabClawR = crabBody.getChild("claw_r");

        this.nautilusShell = nautilus.getChild("shell");
        // 螺旋壳：逐节缩小 → 螺旋收束感
        ModelPart nw1 = nautilusShell.getChild("whorl1");
        ModelPart nw2 = nw1.getChild("whorl2");
        nw1.xScale = nw1.yScale = 0.95F;
        nw2.xScale = nw2.yScale = 0.92F;
        for (int i = 0; i < NAUTILUS_TENTACLES; i++) {
            nautilusTentacles[i] = nautilus.getChild("tentacle_" + i);
        }

        this.barracudaJaw = barracuda.getChild("jaw");
        this.barracudaTail = barracuda.getChild("tail");
        this.barracudaDorsal = barracuda.getChild("dorsal");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ════════════════════════════════════════════════════════════
        // 1. MANTA_RAY 蝠鲼：扁平菱形体 + 双大翼 + 长尾
        // ════════════════════════════════════════════════════════════
        PartDefinition mr = root.addOrReplaceChild("manta_ray", CubeListBuilder.create(), PartPose.ZERO);
        mr.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -2.0F, -7.0F, 12.0F, 4.0F, 14.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        mr.addOrReplaceChild("wing_l",
                CubeListBuilder.create().texOffs(0, 20).addBox(-10.0F, -1.0F, -5.5F, 10.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(-5.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.22F));
        mr.addOrReplaceChild("wing_r",
                CubeListBuilder.create().texOffs(0, 36).addBox(0.0F, -1.0F, -5.5F, 10.0F, 2.0F, 11.0F),
                PartPose.offsetAndRotation(5.0F, 14.0F, 0.0F, 0.0F, 0.0F, -0.22F));
        mr.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 52).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 14.0F),
                PartPose.offset(0.0F, 14.0F, 7.0F));
        mr.addOrReplaceChild("ceph_l",
                CubeListBuilder.create().texOffs(0, 70).addBox(-1.0F, -1.0F, -2.0F, 1.0F, 2.0F, 4.0F),
                PartPose.offset(-3.5F, 13.5F, -7.0F));
        mr.addOrReplaceChild("ceph_r",
                CubeListBuilder.create().texOffs(0, 78).addBox(0.0F, -1.0F, -2.0F, 1.0F, 2.0F, 4.0F),
                PartPose.offset(3.5F, 13.5F, -7.0F));

        // ════════════════════════════════════════════════════════════
        // 2. JELLYFISH 水母：半球伞盖 + 内伞 + 6 条触手
        // ════════════════════════════════════════════════════════════
        PartDefinition jf = root.addOrReplaceChild("jellyfish", CubeListBuilder.create(), PartPose.ZERO);
        jf.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -8.0F, -6.0F, 12.0F, 8.0F, 12.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));
        jf.addOrReplaceChild("inner",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4.0F, -2.5F, -4.0F, 8.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        // 6 条触手：texOffs 必须是字面量（贴图脚本靠正则解析本文件），故逐条展开
        jf.addOrReplaceChild("tentacle_0",
                CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(3.6F, 12.0F, 0.0F));
        jf.addOrReplaceChild("tentacle_1",
                CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(1.8F, 12.0F, 3.12F));
        jf.addOrReplaceChild("tentacle_2",
                CubeListBuilder.create().texOffs(68, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(-1.8F, 12.0F, 3.12F));
        jf.addOrReplaceChild("tentacle_3",
                CubeListBuilder.create().texOffs(76, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(-3.6F, 12.0F, 0.0F));
        jf.addOrReplaceChild("tentacle_4",
                CubeListBuilder.create().texOffs(84, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(-1.8F, 12.0F, -3.12F));
        jf.addOrReplaceChild("tentacle_5",
                CubeListBuilder.create().texOffs(92, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offset(1.8F, 12.0F, -3.12F));

        // ════════════════════════════════════════════════════════════
        // 3. GIANT_SQUID 巨型乌贼：锥形外套膜 + 头 + 8 腕 + 双鳍
        // ════════════════════════════════════════════════════════════
        PartDefinition gs = root.addOrReplaceChild("giant_squid", CubeListBuilder.create(), PartPose.ZERO);
        gs.addOrReplaceChild("mantle",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -14.0F, -4.0F, 8.0F, 14.0F, 8.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        gs.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-3.5F, -3.0F, -3.5F, 7.0F, 5.0F, 7.0F),
                PartPose.offset(0.0F, 9.0F, 0.0F));
        // 8 条腕：texOffs 必须是字面量，故逐条展开（角度按 45° 递增）
        gs.addOrReplaceChild("arm_0",
                CubeListBuilder.create().texOffs(34, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(2.4F, 11.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        gs.addOrReplaceChild("arm_1",
                CubeListBuilder.create().texOffs(41, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(1.70F, 11.0F, 1.70F, 0.0F, -0.7854F, 0.0F));
        gs.addOrReplaceChild("arm_2",
                CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, 2.4F, 0.0F, -1.5708F, 0.0F));
        gs.addOrReplaceChild("arm_3",
                CubeListBuilder.create().texOffs(55, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-1.70F, 11.0F, 1.70F, 0.0F, -2.3562F, 0.0F));
        gs.addOrReplaceChild("arm_4",
                CubeListBuilder.create().texOffs(62, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-2.4F, 11.0F, 0.0F, 0.0F, -3.1416F, 0.0F));
        gs.addOrReplaceChild("arm_5",
                CubeListBuilder.create().texOffs(69, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(-1.70F, 11.0F, -1.70F, 0.0F, -3.9270F, 0.0F));
        gs.addOrReplaceChild("arm_6",
                CubeListBuilder.create().texOffs(76, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 11.0F, -2.4F, 0.0F, -4.7124F, 0.0F));
        gs.addOrReplaceChild("arm_7",
                CubeListBuilder.create().texOffs(83, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F),
                PartPose.offsetAndRotation(1.70F, 11.0F, -1.70F, 0.0F, -5.4978F, 0.0F));
        gs.addOrReplaceChild("fin_l",
                CubeListBuilder.create().texOffs(0, 38).addBox(-5.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(-3.5F, 7.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        gs.addOrReplaceChild("fin_r",
                CubeListBuilder.create().texOffs(0, 45).addBox(0.0F, 0.0F, -2.0F, 5.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(3.5F, 7.0F, 0.0F, 0.0F, 0.0F, -0.35F));

        // ════════════════════════════════════════════════════════════
        // 4. PUFFERFISH 河豚：球形身体 + 12 根棘刺 + 尾鳍/胸鳍
        // ════════════════════════════════════════════════════════════
        PartDefinition pf = root.addOrReplaceChild("pufferfish", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition pfBody = pf.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // 12 根棘刺：上/下/左/右/前/后各 2 根
        float[][] spikePos = {
                {-2.0F, -5.0F, -2.0F}, { 2.0F, -5.0F,  2.0F},
                {-2.0F,  5.0F,  2.0F}, { 2.0F,  5.0F, -2.0F},
                {-5.0F, -2.0F,  2.0F}, {-5.0F,  2.0F, -2.0F},
                { 5.0F, -2.0F, -2.0F}, { 5.0F,  2.0F,  2.0F},
                {-2.0F, -2.0F, -5.0F}, { 2.0F,  2.0F, -5.0F},
                {-2.0F,  2.0F,  5.0F}, { 2.0F, -2.0F,  5.0F},
        };
        // 12 根棘刺：texOffs 必须是字面量，故逐根展开（2×2×2 小方块，UV 占 8×4）
        pfBody.addOrReplaceChild("spike_0",
                CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-2.0F, -5.0F, -2.0F));
        pfBody.addOrReplaceChild("spike_1",
                CubeListBuilder.create().texOffs(53, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, -5.0F, 2.0F));
        pfBody.addOrReplaceChild("spike_2",
                CubeListBuilder.create().texOffs(62, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-2.0F, 5.0F, 2.0F));
        pfBody.addOrReplaceChild("spike_3",
                CubeListBuilder.create().texOffs(71, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, 5.0F, -2.0F));
        pfBody.addOrReplaceChild("spike_4",
                CubeListBuilder.create().texOffs(80, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, -2.0F, 2.0F));
        pfBody.addOrReplaceChild("spike_5",
                CubeListBuilder.create().texOffs(89, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-5.0F, 2.0F, -2.0F));
        pfBody.addOrReplaceChild("spike_6",
                CubeListBuilder.create().texOffs(98, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(5.0F, -2.0F, -2.0F));
        pfBody.addOrReplaceChild("spike_7",
                CubeListBuilder.create().texOffs(107, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(5.0F, 2.0F, 2.0F));
        pfBody.addOrReplaceChild("spike_8",
                CubeListBuilder.create().texOffs(44, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-2.0F, -2.0F, -5.0F));
        pfBody.addOrReplaceChild("spike_9",
                CubeListBuilder.create().texOffs(53, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, 2.0F, -5.0F));
        pfBody.addOrReplaceChild("spike_10",
                CubeListBuilder.create().texOffs(62, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(-2.0F, 2.0F, 5.0F));
        pfBody.addOrReplaceChild("spike_11",
                CubeListBuilder.create().texOffs(71, 6).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(2.0F, -2.0F, 5.0F));
        pf.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 24).addBox(0.0F, -3.0F, 0.0F, 1.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 14.0F, 5.0F));
        pf.addOrReplaceChild("pec_l",
                CubeListBuilder.create().texOffs(0, 38).addBox(-4.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(-4.5F, 14.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        pf.addOrReplaceChild("pec_r",
                CubeListBuilder.create().texOffs(0, 45).addBox(0.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(4.5F, 14.0F, 0.0F, 0.0F, 0.0F, -0.5F));

        // ════════════════════════════════════════════════════════════
        // 5. STARFISH 海星：中央盘 + 5 条放射腕
        // ════════════════════════════════════════════════════════════
        PartDefinition sf = root.addOrReplaceChild("starfish", CubeListBuilder.create(), PartPose.ZERO);
        sf.addOrReplaceChild("disc",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        // 5 条放射腕：texOffs 必须是字面量，故逐条展开（按 72° 递增）
        sf.addOrReplaceChild("arm_0",
                CubeListBuilder.create().texOffs(0, 12).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        sf.addOrReplaceChild("arm_1",
                CubeListBuilder.create().texOffs(0, 26).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, -1.2566F, 0.0F));
        sf.addOrReplaceChild("arm_2",
                CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, -2.5133F, 0.0F));
        sf.addOrReplaceChild("arm_3",
                CubeListBuilder.create().texOffs(0, 54).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, -3.7699F, 0.0F));
        sf.addOrReplaceChild("arm_4",
                CubeListBuilder.create().texOffs(0, 68).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 2.0F, 9.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.0F, -5.0265F, 0.0F));

        // ════════════════════════════════════════════════════════════
        // 6. SEAHORSE 海马：马首 + 长吻 + 直立躯干 + 卷曲尾 + 背鳍
        // ════════════════════════════════════════════════════════════
        PartDefinition sh = root.addOrReplaceChild("seahorse", CubeListBuilder.create(), PartPose.ZERO);
        sh.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        PartDefinition shHead = sh.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -3.0F, -2.5F, 3.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        shHead.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, -0.5F, -2.0F));
        sh.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 32).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 9.5F, -0.5F));
        // 卷曲尾：三节逐节缩小并旋转
        PartDefinition t0 = sh.addOrReplaceChild("tail0",
                CubeListBuilder.create().texOffs(0, 38).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 22.0F, 0.0F, 0.5F, 0.0F, 0.0F));
        PartDefinition t1 = t0.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.8F, 0.0F, 0.0F));
        t1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(0, 52).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 1.0F, 0.0F, 0.0F));
        sh.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(0, 58).addBox(0.0F, -3.0F, 0.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, 2.0F));

        // ════════════════════════════════════════════════════════════
        // 7. LIONFISH 狮子鱼：侧扁体 + 8 根长毒棘 + 大胸鳍 + 尾鳍
        // ════════════════════════════════════════════════════════════
        PartDefinition lf = root.addOrReplaceChild("lionfish", CubeListBuilder.create(), PartPose.ZERO);
        lf.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.0F, -4.5F, 5.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        // 8 根长毒棘：沿背部两列排布
        // 8 根毒棘：texOffs 必须是字面量，故逐根展开（左右两列各 4 根）
        lf.addOrReplaceChild("spine_0",
                CubeListBuilder.create().texOffs(30, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, 11.5F, -3.5F, 0.0F, 0.0F, 0.35F));
        lf.addOrReplaceChild("spine_1",
                CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, 11.5F, -3.5F, 0.0F, 0.0F, -0.35F));
        lf.addOrReplaceChild("spine_2",
                CubeListBuilder.create().texOffs(42, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, 11.5F, -1.2F, 0.0F, 0.0F, 0.35F));
        lf.addOrReplaceChild("spine_3",
                CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, 11.5F, -1.2F, 0.0F, 0.0F, -0.35F));
        lf.addOrReplaceChild("spine_4",
                CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, 11.5F, 1.1F, 0.0F, 0.0F, 0.35F));
        lf.addOrReplaceChild("spine_5",
                CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, 11.5F, 1.1F, 0.0F, 0.0F, -0.35F));
        lf.addOrReplaceChild("spine_6",
                CubeListBuilder.create().texOffs(66, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(-1.8F, 11.5F, 3.4F, 0.0F, 0.0F, 0.35F));
        lf.addOrReplaceChild("spine_7",
                CubeListBuilder.create().texOffs(72, 0).addBox(-0.5F, -7.0F, -0.5F, 1.0F, 7.0F, 1.0F),
                PartPose.offsetAndRotation(1.8F, 11.5F, 3.4F, 0.0F, 0.0F, -0.35F));
        lf.addOrReplaceChild("pec_l",
                CubeListBuilder.create().texOffs(0, 22).addBox(-7.0F, -3.0F, -2.0F, 7.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 15.0F, -1.0F, 0.0F, 0.5F, 0.0F));
        lf.addOrReplaceChild("pec_r",
                CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -3.0F, -2.0F, 7.0F, 6.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 15.0F, -1.0F, 0.0F, -0.5F, 0.0F));
        lf.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 42).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 1.0F),
                PartPose.offset(0.0F, 15.0F, 4.5F));

        // ════════════════════════════════════════════════════════════
        // 8. IRON_CRAB 铁甲蟹：宽甲壳 + 双螯 + 6 足 + 装甲板
        // ════════════════════════════════════════════════════════════
        PartDefinition ic = root.addOrReplaceChild("iron_crab", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition icBody = ic.addOrReplaceChild("carapace",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -3.0F, -6.0F, 14.0F, 6.0F, 12.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        icBody.addOrReplaceChild("plate",
                CubeListBuilder.create().texOffs(0, 20).addBox(-5.0F, -1.5F, -4.5F, 10.0F, 2.0F, 9.0F),
                PartPose.offset(0.0F, -3.0F, 0.0F));
        icBody.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(0, 34).addBox(-4.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(-7.0F, 0.0F, -4.0F, 0.0F, 0.35F, 0.0F));
        icBody.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(0, 46).addBox(0.0F, -1.5F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(7.0F, 0.0F, -4.0F, 0.0F, -0.35F, 0.0F));
        // 6 条步足：texOffs 必须是字面量，故逐条展开（左右各 3 条）
        icBody.addOrReplaceChild("leg_l_0",
                CubeListBuilder.create().texOffs(20, 34).addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-6.5F, 2.0F, -3.5F, 0.0F, 0.0F, -0.35F));
        icBody.addOrReplaceChild("leg_l_1",
                CubeListBuilder.create().texOffs(28, 34).addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-6.5F, 2.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        icBody.addOrReplaceChild("leg_l_2",
                CubeListBuilder.create().texOffs(36, 34).addBox(-5.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-6.5F, 2.0F, 3.5F, 0.0F, 0.0F, -0.35F));
        icBody.addOrReplaceChild("leg_r_0",
                CubeListBuilder.create().texOffs(20, 46).addBox(0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(6.5F, 2.0F, -3.5F, 0.0F, 0.0F, 0.35F));
        icBody.addOrReplaceChild("leg_r_1",
                CubeListBuilder.create().texOffs(28, 46).addBox(0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(6.5F, 2.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        icBody.addOrReplaceChild("leg_r_2",
                CubeListBuilder.create().texOffs(36, 46).addBox(0.0F, -0.5F, -0.5F, 5.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(6.5F, 2.0F, 3.5F, 0.0F, 0.0F, 0.35F));

        // ════════════════════════════════════════════════════════════
        // 9. NAUTILUS 鹦鹉螺：螺旋壳（逐节缩小）+ 兜帽 + 6 触手
        // ════════════════════════════════════════════════════════════
        PartDefinition na = root.addOrReplaceChild("nautilus", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition shell = na.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, 0.0F, 10.0F, 10.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        // 螺旋：逐节缩小 + 旋转
        PartDefinition w1 = shell.addOrReplaceChild("whorl1",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.4F));
        PartDefinition w2 = w1.addOrReplaceChild("whorl2",
                CubeListBuilder.create().texOffs(0, 29).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.5F));
        w2.addOrReplaceChild("whorl3",
                CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.0F, 0.0F, 0.6F));
        na.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 48).addBox(-4.0F, -1.0F, -3.0F, 8.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 11.0F, -0.5F));
        // 6 条触手：texOffs 必须是字面量，故逐条展开（绕壳口一圈，按 60° 递增）
        na.addOrReplaceChild("tentacle_0",
                CubeListBuilder.create().texOffs(30, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(2.6F, 11.5F, -2.5F));
        na.addOrReplaceChild("tentacle_1",
                CubeListBuilder.create().texOffs(36, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(1.3F, 13.23F, -2.5F));
        na.addOrReplaceChild("tentacle_2",
                CubeListBuilder.create().texOffs(42, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(-1.3F, 13.23F, -2.5F));
        na.addOrReplaceChild("tentacle_3",
                CubeListBuilder.create().texOffs(48, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(-2.6F, 11.5F, -2.5F));
        na.addOrReplaceChild("tentacle_4",
                CubeListBuilder.create().texOffs(54, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(-1.3F, 9.77F, -2.5F));
        na.addOrReplaceChild("tentacle_5",
                CubeListBuilder.create().texOffs(60, 48).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F),
                PartPose.offset(1.3F, 9.77F, -2.5F));

        // ════════════════════════════════════════════════════════════
        // 10. BARRACUDA 梭鱼：流线长体 + 尖吻利颌 + 背鳍/尾鳍
        // ════════════════════════════════════════════════════════════
        PartDefinition ba = root.addOrReplaceChild("barracuda", CubeListBuilder.create(), PartPose.ZERO);
        ba.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -3.5F, -8.0F, 5.0F, 7.0F, 16.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        ba.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 14.0F, -8.0F));
        ba.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 34).addBox(-1.5F, 0.0F, -3.5F, 3.0F, 1.5F, 4.0F),
                PartPose.offset(0.0F, 15.5F, -7.5F));
        ba.addOrReplaceChild("dorsal",
                CubeListBuilder.create().texOffs(0, 42).addBox(0.0F, -3.0F, -1.5F, 1.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 11.0F, 0.0F));
        ba.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 52).addBox(0.0F, -5.0F, 0.0F, 1.0F, 10.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, 8.0F));
        ba.addOrReplaceChild("pec_l",
                CubeListBuilder.create().texOffs(0, 70).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(-2.0F, 15.0F, -3.0F, 0.0F, 0.0F, 0.4F));
        ba.addOrReplaceChild("pec_r",
                CubeListBuilder.create().texOffs(0, 78).addBox(0.0F, -1.0F, -1.0F, 4.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(2.0F, 15.0F, -3.0F, 0.0F, 0.0F, -0.4F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(OceanFaunaEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 默认隐藏所有变体组
        mantaRay.visible = false;
        jellyfish.visible = false;
        giantSquid.visible = false;
        pufferfish.visible = false;
        starfish.visible = false;
        seahorse.visible = false;
        lionfish.visible = false;
        ironCrab.visible = false;
        nautilus.visible = false;
        barracuda.visible = false;

        float t = ageInTicks * 0.12F;
        OceanFaunaEntity.Variant v = entity.getVariant();

        switch (v) {
            case MANTA_RAY -> {
                mantaRay.visible = true;
                // 双翼上下扇动（滑翔）
                float flap = Mth.sin(t * 1.1F) * 0.35F;
                mantaWingL.zRot = 0.22F + flap;
                mantaWingR.zRot = -0.22F - flap;
                mantaTail.yRot = Mth.sin(t * 0.8F) * 0.25F;
            }
            case JELLYFISH -> {
                jellyfish.visible = true;
                // 伞盖搏动（缩放）+ 触手飘动
                float pulse = 1.0F + Mth.sin(t * 1.3F) * 0.12F;
                jellyBell.xScale = pulse;
                jellyBell.zScale = pulse;
                for (int i = 0; i < JELLY_TENTACLES; i++) {
                    jellyTentacles[i].xRot = Mth.sin(t * 1.0F + i) * 0.22F;
                    jellyTentacles[i].zRot = Mth.cos(t * 0.9F + i) * 0.18F;
                }
            }
            case GIANT_SQUID -> {
                giantSquid.visible = true;
                squidMantle.xScale = 1.0F + Mth.sin(t * 1.2F) * 0.06F;
                for (int i = 0; i < SQUID_ARMS; i++) {
                    squidArms[i].xRot = Mth.sin(t * 1.4F + i * 0.7F) * 0.35F;
                    squidArms[i].zRot = Mth.cos(t * 1.1F + i * 0.5F) * 0.25F;
                }
            }
            case PUFFERFISH -> {
                pufferfish.visible = true;
                // 膨胀：整体缩放呼吸
                float puff = 1.0F + Mth.sin(t * 0.9F) * 0.10F;
                pufferBody.xScale = pufferBody.yScale = pufferBody.zScale = puff;
                for (int i = 0; i < PUFFER_SPIKES; i++) {
                    pufferSpikes[i].xScale = pufferSpikes[i].yScale = pufferSpikes[i].zScale =
                            1.0F + Mth.sin(t * 0.9F + i * 0.4F) * 0.15F;
                }
            }
            case STARFISH -> {
                starfish.visible = true;
                for (int i = 0; i < STAR_ARMS; i++) {
                    starArms[i].xRot = Mth.sin(t * 0.7F + i * 1.2F) * 0.14F;
                }
            }
            case SEAHORSE -> {
                seahorse.visible = true;
                seaHorseHead.xRot = Mth.sin(t * 0.8F) * 0.12F;
                seaHorseTail.xRot = 0.5F + Mth.sin(t * 0.9F) * 0.18F;
            }
            case LIONFISH -> {
                lionfish.visible = true;
                for (int i = 0; i < LION_SPINES; i++) {
                    lionSpines[i].zRot = ((i % 2 == 0) ? 0.35F : -0.35F)
                            + Mth.sin(t * 1.0F + i * 0.5F) * 0.10F;
                }
                lionPecL.yRot = 0.5F + Mth.sin(t * 1.6F) * 0.18F;
                lionPecR.yRot = -0.5F - Mth.sin(t * 1.6F) * 0.18F;
            }
            case IRON_CRAB -> {
                ironCrab.visible = true;
                for (int i = 0; i < CRAB_LEG_PAIRS; i++) {
                    float ph = t * 1.5F + i * 0.9F;
                    crabLegsL[i].zRot = -0.35F + Mth.sin(ph) * 0.18F;
                    crabLegsR[i].zRot = 0.35F - Mth.sin(ph) * 0.18F;
                }
                crabClawL.yRot = 0.35F + Mth.sin(t * 1.2F) * 0.12F;
                crabClawR.yRot = -0.35F - Mth.sin(t * 1.2F) * 0.12F;
            }
            case NAUTILUS -> {
                nautilus.visible = true;
                nautilusShell.zRot = Mth.sin(t * 0.5F) * 0.06F;
                for (int i = 0; i < NAUTILUS_TENTACLES; i++) {
                    nautilusTentacles[i].xRot = Mth.sin(t * 1.5F + i * 0.8F) * 0.30F;
                }
            }
            case BARRACUDA -> {
                barracuda.visible = true;
                // 身体摆尾游动
                barracudaTail.yRot = Mth.sin(t * 2.2F) * 0.45F;
                barracudaDorsal.zRot = Mth.sin(t * 2.2F) * 0.08F;
                barracudaJaw.xRot = 0.12F + Mth.sin(t * 0.7F) * 0.10F;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
