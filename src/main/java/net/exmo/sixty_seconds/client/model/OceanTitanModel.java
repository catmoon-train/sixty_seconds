package net.exmo.sixty_seconds.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.exmo.sixty_seconds.entity.OceanTitanEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * 海洋霸主模型（10 个<b>完全独立</b>的 Boss 几何，非僵尸人形）。
 *
 * <p>每个 Boss 拥有独立 PartGroup：abyss_whale / tempest_eel / barnacle_titan /
 * angler_lord / carapace_king / ghost_medusa / abyss_maw / coral_colossus /
 * wreck_wraith / trident_sovereign。{@code setupAnim} 按变体切换可见性并播放
 * 各自循环动画（鲸尾摆、鳗身波、藤壶开合、灯笼摇晃、蟹钳开合、伞盖搏动、
 * 巨口张合、珊瑚摇曳、怨灵飘浮、三叉戟旋转）。
 *
 * <p>⚠ <b>texOffs 必须为字面量</b>：贴图由 {@code tools/gen_ocean_titan.py}
 * 正则解析本文件生成，表达式会导致该盒体解析不到、贴图缺面。
 */
public class OceanTitanModel extends EntityModel<OceanTitanEntity> {

    private final ModelPart root;
    private final ModelPart[] groups = new ModelPart[10];

    // 动画用子部件
    private final ModelPart whaleTail, whaleFlukeL, whaleFlukeR, whaleJaw;
    private final ModelPart[] eelSegs = new ModelPart[5];
    private final ModelPart[] barnacles = new ModelPart[4];
    private final ModelPart anglerLure, anglerRod, anglerJaw;
    private final ModelPart crabClawL, crabClawR;
    private final ModelPart[] medusaTentacles = new ModelPart[6];
    private final ModelPart mawUpper, mawLower;
    private final ModelPart[] coralArms = new ModelPart[4];
    private final ModelPart wraithBody, wraithHood;
    private final ModelPart trident, tridentHead;

    public OceanTitanModel(ModelPart root) {
        this.root = root;
        String[] names = {"abyss_whale", "tempest_eel", "barnacle_titan", "angler_lord",
                "carapace_king", "ghost_medusa", "abyss_maw", "coral_colossus",
                "wreck_wraith", "trident_sovereign"};
        for (int i = 0; i < names.length; i++) {
            groups[i] = root.getChild(names[i]);
        }

        ModelPart whale = groups[0];
        this.whaleTail = whale.getChild("tail");
        this.whaleFlukeL = whale.getChild("fluke_l");
        this.whaleFlukeR = whale.getChild("fluke_r");
        this.whaleJaw = whale.getChild("jaw");

        ModelPart eel = groups[1];
        for (int i = 0; i < 5; i++) eelSegs[i] = eel.getChild("seg" + i);

        ModelPart barn = groups[2];
        for (int i = 0; i < 4; i++) barnacles[i] = barn.getChild("barnacle" + i);

        ModelPart angler = groups[3];
        this.anglerRod = angler.getChild("rod");
        this.anglerLure = angler.getChild("lamp");
        this.anglerJaw = angler.getChild("jaw");

        ModelPart crab = groups[4];
        this.crabClawL = crab.getChild("claw_l");
        this.crabClawR = crab.getChild("claw_r");

        ModelPart medusa = groups[5];
        for (int i = 0; i < 6; i++) medusaTentacles[i] = medusa.getChild("tentacle" + i);

        ModelPart maw = groups[6];
        this.mawUpper = maw.getChild("upper");
        this.mawLower = maw.getChild("lower");

        ModelPart coral = groups[7];
        for (int i = 0; i < 4; i++) coralArms[i] = coral.getChild("arm" + i);

        ModelPart wraith = groups[8];
        this.wraithBody = wraith.getChild("body");
        this.wraithHood = wraith.getChild("hood");

        ModelPart sov = groups[9];
        this.trident = sov.getChild("trident");
        this.tridentHead = sov.getChild("trident_head");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ══════ 0. ABYSS_WHALE 深渊巨鲸：庞大流线体 + 尾鳍 + 巨口 ══════
        PartDefinition wh = root.addOrReplaceChild("abyss_whale", CubeListBuilder.create(), PartPose.ZERO);
        wh.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -10.0F, -16.0F, 16.0F, 20.0F, 32.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        wh.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 54).addBox(-7.0F, -7.0F, -10.0F, 14.0F, 14.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, -16.0F));
        wh.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 80).addBox(-6.0F, 0.0F, -9.0F, 12.0F, 5.0F, 9.0F),
                PartPose.offset(0.0F, 17.0F, -16.0F));
        wh.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offset(0.0F, 12.0F, 16.0F));
        wh.addOrReplaceChild("fluke_l",
                CubeListBuilder.create().texOffs(64, 26).addBox(-12.0F, -1.0F, -3.0F, 12.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(-3.0F, 12.0F, 30.0F, 0.0F, 0.0F, 0.30F));
        wh.addOrReplaceChild("fluke_r",
                CubeListBuilder.create().texOffs(64, 38).addBox(0.0F, -1.0F, -3.0F, 12.0F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(3.0F, 12.0F, 30.0F, 0.0F, 0.0F, -0.30F));
        wh.addOrReplaceChild("fin_l",
                CubeListBuilder.create().texOffs(64, 50).addBox(-8.0F, -1.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(-7.0F, 16.0F, -6.0F, 0.0F, 0.0F, 0.35F));
        wh.addOrReplaceChild("fin_r",
                CubeListBuilder.create().texOffs(64, 60).addBox(0.0F, -1.0F, -3.0F, 8.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(7.0F, 16.0F, -6.0F, 0.0F, 0.0F, -0.35F));

        // ══════ 1. TEMPEST_EEL 雷暴电鳗：分段蛇形躯体 + 电弧鳍 ══════
        PartDefinition ee = root.addOrReplaceChild("tempest_eel", CubeListBuilder.create(), PartPose.ZERO);
        ee.addOrReplaceChild("seg0",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -5.0F, 0.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, -8.0F));
        ee.addOrReplaceChild("seg1",
                CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, -4.5F, 0.0F, 9.0F, 9.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 2.0F));
        ee.addOrReplaceChild("seg2",
                CubeListBuilder.create().texOffs(0, 46).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 12.0F));
        ee.addOrReplaceChild("seg3",
                CubeListBuilder.create().texOffs(0, 66).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 22.0F));
        ee.addOrReplaceChild("seg4",
                CubeListBuilder.create().texOffs(0, 84).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 32.0F));
        ee.addOrReplaceChild("crest",
                CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -8.0F, 0.0F, 1.0F, 8.0F, 30.0F),
                PartPose.offset(0.0F, 8.0F, -6.0F));

        // ══════ 2. BARNACLE_TITAN 藤壶巨怪：岩质躯干 + 4 组藤壶 + 巨臂 ══════
        PartDefinition bt = root.addOrReplaceChild("barnacle_titan", CubeListBuilder.create(), PartPose.ZERO);
        bt.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -12.0F, -7.0F, 18.0F, 24.0F, 14.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        bt.addOrReplaceChild("barnacle0",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(-8.0F, 2.0F, -4.0F));
        bt.addOrReplaceChild("barnacle1",
                CubeListBuilder.create().texOffs(0, 56).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F),
                PartPose.offset(8.0F, 0.0F, -4.0F));
        bt.addOrReplaceChild("barnacle2",
                CubeListBuilder.create().texOffs(0, 72).addBox(-2.5F, -3.5F, -2.5F, 5.0F, 7.0F, 5.0F),
                PartPose.offset(0.0F, -10.0F, 6.0F));
        bt.addOrReplaceChild("barnacle3",
                CubeListBuilder.create().texOffs(0, 86).addBox(-2.0F, -3.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offset(0.0F, 6.0F, 7.0F));
        bt.addOrReplaceChild("arm_l",
                CubeListBuilder.create().texOffs(64, 0).addBox(-6.0F, -3.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(-9.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.30F));
        bt.addOrReplaceChild("arm_r",
                CubeListBuilder.create().texOffs(64, 26).addBox(0.0F, -3.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offsetAndRotation(9.0F, -6.0F, 0.0F, 0.0F, 0.0F, -0.30F));

        // ══════ 3. ANGLER_LORD 深海鮟鱇：巨口 + 灯笼诱饵 ══════
        PartDefinition al = root.addOrReplaceChild("angler_lord", CubeListBuilder.create(), PartPose.ZERO);
        al.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -8.0F, -8.0F, 18.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        al.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 34).addBox(-8.0F, 0.0F, -7.0F, 16.0F, 6.0F, 14.0F),
                PartPose.offset(0.0F, 18.0F, -1.0F));
        al.addOrReplaceChild("rod",
                CubeListBuilder.create().texOffs(0, 56).addBox(-0.5F, -12.0F, -0.5F, 1.0F, 12.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, -6.0F, -0.6F, 0.0F, 0.0F));
        al.addOrReplaceChild("lamp",
                CubeListBuilder.create().texOffs(0, 70).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -8.0F, -10.0F));
        al.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 80).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 10.0F, 8.0F));
        al.addOrReplaceChild("fin_l",
                CubeListBuilder.create().texOffs(36, 56).addBox(-6.0F, -1.0F, -2.0F, 6.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(-8.0F, 14.0F, 2.0F, 0.0F, 0.0F, 0.40F));
        al.addOrReplaceChild("fin_r",
                CubeListBuilder.create().texOffs(36, 66).addBox(0.0F, -1.0F, -2.0F, 6.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(8.0F, 14.0F, 2.0F, 0.0F, 0.0F, -0.40F));

        // ══════ 4. CARAPACE_KING 碎壳巨蟹：宽甲壳 + 双巨螯 + 6 足 ══════
        PartDefinition ck = root.addOrReplaceChild("carapace_king", CubeListBuilder.create(), PartPose.ZERO);
        ck.addOrReplaceChild("shell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -5.0F, -9.0F, 22.0F, 10.0F, 18.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        ck.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 30).addBox(-8.0F, -2.0F, -6.0F, 16.0F, 3.0F, 12.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        ck.addOrReplaceChild("claw_l",
                CubeListBuilder.create().texOffs(0, 47).addBox(-7.0F, -3.0F, -5.0F, 7.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(-11.0F, 14.0F, -6.0F, 0.0F, 0.35F, 0.0F));
        ck.addOrReplaceChild("claw_r",
                CubeListBuilder.create().texOffs(0, 65).addBox(0.0F, -3.0F, -5.0F, 7.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(11.0F, 14.0F, -6.0F, 0.0F, -0.35F, 0.0F));
        ck.addOrReplaceChild("leg_l0",
                CubeListBuilder.create().texOffs(36, 47).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-9.0F, 18.0F, -4.0F, 0.0F, 0.0F, -0.35F));
        ck.addOrReplaceChild("leg_l1",
                CubeListBuilder.create().texOffs(36, 53).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-9.0F, 18.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        ck.addOrReplaceChild("leg_l2",
                CubeListBuilder.create().texOffs(36, 59).addBox(-9.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-9.0F, 18.0F, 4.0F, 0.0F, 0.0F, -0.35F));
        ck.addOrReplaceChild("leg_r0",
                CubeListBuilder.create().texOffs(36, 65).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(9.0F, 18.0F, -4.0F, 0.0F, 0.0F, 0.35F));
        ck.addOrReplaceChild("leg_r1",
                CubeListBuilder.create().texOffs(36, 71).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(9.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        ck.addOrReplaceChild("leg_r2",
                CubeListBuilder.create().texOffs(36, 77).addBox(0.0F, -1.0F, -1.0F, 9.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(9.0F, 18.0F, 4.0F, 0.0F, 0.0F, 0.35F));

        // ══════ 5. GHOST_MEDUSA 幽灵水母后：巨伞盖 + 6 长触手 ══════
        PartDefinition gm = root.addOrReplaceChild("ghost_medusa", CubeListBuilder.create(), PartPose.ZERO);
        gm.addOrReplaceChild("bell",
                CubeListBuilder.create().texOffs(0, 0).addBox(-11.0F, -12.0F, -11.0F, 22.0F, 14.0F, 22.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        gm.addOrReplaceChild("inner",
                CubeListBuilder.create().texOffs(0, 38).addBox(-7.0F, -4.0F, -7.0F, 14.0F, 8.0F, 14.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        gm.addOrReplaceChild("tentacle0",
                CubeListBuilder.create().texOffs(64, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(7.0F, 16.0F, 0.0F));
        gm.addOrReplaceChild("tentacle1",
                CubeListBuilder.create().texOffs(72, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(3.5F, 16.0F, 6.06F));
        gm.addOrReplaceChild("tentacle2",
                CubeListBuilder.create().texOffs(80, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(-3.5F, 16.0F, 6.06F));
        gm.addOrReplaceChild("tentacle3",
                CubeListBuilder.create().texOffs(88, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(-7.0F, 16.0F, 0.0F));
        gm.addOrReplaceChild("tentacle4",
                CubeListBuilder.create().texOffs(96, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(-3.5F, 16.0F, -6.06F));
        gm.addOrReplaceChild("tentacle5",
                CubeListBuilder.create().texOffs(104, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                PartPose.offset(3.5F, 16.0F, -6.06F));

        // ══════ 6. ABYSS_MAW 深渊之喉：环形巨口（上下颚）+ 触须 ══════
        PartDefinition am = root.addOrReplaceChild("abyss_maw", CubeListBuilder.create(), PartPose.ZERO);
        am.addOrReplaceChild("upper",
                CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, -8.0F, -10.0F, 20.0F, 8.0F, 20.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        am.addOrReplaceChild("lower",
                CubeListBuilder.create().texOffs(0, 30).addBox(-10.0F, 0.0F, -10.0F, 20.0F, 8.0F, 20.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        am.addOrReplaceChild("throat",
                CubeListBuilder.create().texOffs(0, 60).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, 12.0F, 6.0F));
        am.addOrReplaceChild("fringe0",
                CubeListBuilder.create().texOffs(44, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(8.0F, 16.0F, -6.0F));
        am.addOrReplaceChild("fringe1",
                CubeListBuilder.create().texOffs(52, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-8.0F, 16.0F, -6.0F));
        am.addOrReplaceChild("fringe2",
                CubeListBuilder.create().texOffs(60, 60).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 16.0F, -9.0F));

        // ══════ 7. CORAL_COLOSSUS 珊瑚巨偶：岩石躯干 + 4 条珊瑚臂 ══════
        PartDefinition cc = root.addOrReplaceChild("coral_colossus", CubeListBuilder.create(), PartPose.ZERO);
        cc.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -12.0F, -8.0F, 16.0F, 24.0F, 16.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        cc.addOrReplaceChild("arm0",
                CubeListBuilder.create().texOffs(0, 42).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        cc.addOrReplaceChild("arm1",
                CubeListBuilder.create().texOffs(0, 60).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 1.5708F, 0.0F));
        cc.addOrReplaceChild("arm2",
                CubeListBuilder.create().texOffs(0, 78).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 3.1416F, 0.0F));
        cc.addOrReplaceChild("arm3",
                CubeListBuilder.create().texOffs(0, 96).addBox(-2.0F, -12.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.0F, 4.7124F, 0.0F));
        cc.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(44, 42).addBox(-10.0F, -3.0F, -10.0F, 20.0F, 3.0F, 20.0F),
                PartPose.offset(0.0F, 20.0F, 0.0F));

        // ══════ 8. WRECK_WRAITH 沉船怨灵：破帆残骸 + 兜帽 + 飘带 ══════
        PartDefinition ww = root.addOrReplaceChild("wreck_wraith", CubeListBuilder.create(), PartPose.ZERO);
        ww.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -12.0F, -4.0F, 12.0F, 24.0F, 8.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        ww.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.0F, -6.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        ww.addOrReplaceChild("sail",
                CubeListBuilder.create().texOffs(0, 54).addBox(-9.0F, -10.0F, -0.5F, 18.0F, 20.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 4.0F, 0.0F, 0.0F, 0.10F));
        ww.addOrReplaceChild("mast",
                CubeListBuilder.create().texOffs(0, 78).addBox(-0.5F, -20.0F, -0.5F, 1.0F, 24.0F, 1.0F),
                PartPose.offset(0.0F, 4.0F, 4.0F));
        ww.addOrReplaceChild("wisp_l",
                CubeListBuilder.create().texOffs(44, 0).addBox(-4.0F, 0.0F, -0.5F, 4.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(-5.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.20F));
        ww.addOrReplaceChild("wisp_r",
                CubeListBuilder.create().texOffs(44, 20).addBox(0.0F, 0.0F, -0.5F, 4.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(5.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.20F));

        // ══════ 9. TRIDENT_SOVEREIGN 海皇三叉戟：人形轮廓 + 王冠 + 三叉戟 ══════
        PartDefinition ts = root.addOrReplaceChild("trident_sovereign", CubeListBuilder.create(), PartPose.ZERO);
        ts.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -12.0F, -4.0F, 12.0F, 20.0F, 8.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        ts.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 30).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));
        ts.addOrReplaceChild("crown",
                CubeListBuilder.create().texOffs(0, 48).addBox(-5.0F, -2.0F, -5.0F, 10.0F, 2.0F, 10.0F),
                PartPose.offset(0.0F, -11.0F, 0.0F));
        ts.addOrReplaceChild("leg_l",
                CubeListBuilder.create().texOffs(0, 62).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offset(-3.0F, 18.0F, 0.0F));
        ts.addOrReplaceChild("leg_r",
                CubeListBuilder.create().texOffs(0, 84).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 14.0F, 6.0F),
                PartPose.offset(3.0F, 18.0F, 0.0F));
        ts.addOrReplaceChild("trident",
                CubeListBuilder.create().texOffs(40, 20).addBox(-0.5F, -18.0F, -0.5F, 1.0F, 36.0F, 1.0F),
                PartPose.offset(7.0F, 4.0F, 0.0F));
        ts.addOrReplaceChild("trident_head",
                CubeListBuilder.create().texOffs(40, 60).addBox(-3.0F, -4.0F, -0.5F, 6.0F, 4.0F, 1.0F),
                PartPose.offset(7.0F, -14.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(OceanTitanEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        for (ModelPart g : groups) g.visible = false;

        float t = ageInTicks * 0.12F;
        OceanTitanEntity.Variant v = entity.getVariant();

        switch (v) {
            case ABYSS_WHALE -> {
                groups[0].visible = true;
                whaleTail.yRot = Mth.sin(t * 0.9F) * 0.30F;
                whaleFlukeL.zRot = 0.30F + Mth.sin(t * 0.9F) * 0.18F;
                whaleFlukeR.zRot = -0.30F - Mth.sin(t * 0.9F) * 0.18F;
                whaleJaw.xRot = 0.10F + Mth.sin(t * 0.6F) * 0.08F;
            }
            case TEMPEST_EEL -> {
                groups[1].visible = true;
                for (int i = 0; i < 5; i++) {
                    eelSegs[i].yRot = Mth.sin(t * 1.6F - i * 0.7F) * 0.35F;
                }
            }
            case BARNACLE_TITAN -> {
                groups[2].visible = true;
                for (int i = 0; i < 4; i++) {
                    barnacles[i].yScale = 1.0F + Mth.sin(t * 1.1F + i) * 0.12F;
                }
            }
            case ANGLER_LORD -> {
                groups[3].visible = true;
                anglerRod.xRot = -0.6F + Mth.sin(t * 0.8F) * 0.15F;
                anglerLure.xScale = anglerLure.yScale = anglerLure.zScale =
                        1.0F + Mth.sin(t * 2.0F) * 0.15F;   // 灯笼脉动发光感
                anglerJaw.xRot = 0.12F + Mth.sin(t * 0.7F) * 0.10F;
            }
            case CARAPACE_KING -> {
                groups[4].visible = true;
                crabClawL.yRot = 0.35F + Mth.sin(t * 1.3F) * 0.14F;
                crabClawR.yRot = -0.35F - Mth.sin(t * 1.3F) * 0.14F;
            }
            case GHOST_MEDUSA -> {
                groups[5].visible = true;
                for (int i = 0; i < 6; i++) {
                    medusaTentacles[i].xRot = Mth.sin(t * 1.0F + i) * 0.24F;
                    medusaTentacles[i].zRot = Mth.cos(t * 0.9F + i) * 0.18F;
                }
            }
            case ABYSS_MAW -> {
                groups[6].visible = true;
                float bite = 0.18F + Mth.sin(t * 0.8F) * 0.16F;
                mawUpper.xRot = -bite;
                mawLower.xRot = bite;
            }
            case CORAL_COLOSSUS -> {
                groups[7].visible = true;
                for (int i = 0; i < 4; i++) {
                    coralArms[i].zRot = Mth.sin(t * 0.8F + i * 0.8F) * 0.10F;
                }
            }
            case WRECK_WRAITH -> {
                groups[8].visible = true;
                wraithBody.y = 8.0F + Mth.sin(t * 0.7F) * 0.8F;   // 整体漂浮起伏
                wraithHood.yRot = Mth.sin(t * 0.5F) * 0.12F;
            }
            case TRIDENT_SOVEREIGN -> {
                groups[9].visible = true;
                trident.yRot = Mth.sin(t * 0.6F) * 0.10F;
                tridentHead.yRot = Mth.sin(t * 0.6F) * 0.10F;
            }
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
