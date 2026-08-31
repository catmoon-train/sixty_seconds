package net.exmo.sixty_seconds.entity;

import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsDifficulty;
import net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

/**
 * 尸潮领主（60s PVE Boss）：带<b>等级 1..5</b> 与 <b>变体</b>，属性/技能随等级与变体增强；
 * 头顶 {@link ServerBossEvent} 血条。技能（服务端 tick 驱动，冷却用 gameTime 时间戳）。
 *
 * <h3>Boss 变体</h3>
 * <ul>
 *   <li><b>破坏者 RAVAGER</b> — 均衡型：震地猛击 / 骇人咆哮 / 尸潮召唤 / 猛冲 / 酸雨（终焉）</li>
 *   <li><b>巨像 COLOSSUS</b> — 重装坦克：强化震地 / 铁壁 / 裂地震波 / 猛冲 / 狂怒被动（低血狂暴）</li>
 *   <li><b>亡灵术士 NECROMANCER</b> — 召唤大师：骨矛 / 生命汲取 / 强化尸潮召唤 / 骨墙 / 亡者大军（终焉）</li>
 *   <li><b>疫病者 PLAGUEBEARER</b> — 毒疫专精：酸液喷吐 / 毒息 / 酸雨齐射 / 腐化光环 / 剧毒新星（终焉）</li>
 *   <li><b>鬼魅 SPECTER</b> — 暗杀刺客：暗影突袭 / 潜行 / 鬼魅瞬击 / 幻影分身 / 死亡标记（终焉）</li>
 * </ul>
 */
public class SixtySecondsBossEntity extends SixtySecondsMonsterEntity {

    // ── Boss 变体枚举 ─────────────────────────────────────────────────
    public enum BossVariant {
        /** 破坏者：均衡型，默认变体，拥有基础技能组 */
        RAVAGER(0, 1.0, 1.0, "sixty_seconds_boss_ravager"),
        /** 巨像：重型坦克，高血量低移速，附带铁壁+狂怒 */
        COLOSSUS(1, 1.5, 0.65, "sixty_seconds_boss_colossus"),
        /** 亡灵术士：召唤专精，本体低血量，强化召唤+生命汲取 */
        NECROMANCER(2, 0.75, 0.9, "sixty_seconds_boss_necromancer"),
        /** 疫病者：毒疫大师，中程酸液+毒息+腐化光环 */
        PLAGUEBEARER(3, 1.05, 0.82, "sixty_seconds_boss_plaguebearer"),
        /** 鬼魅：刺客型，高速度低血量，瞬移+暗影打击 */
        SPECTER(4, 0.7, 1.25, "sixty_seconds_boss_specter"),
        /** 熔渊暴君：火系重装，熔岩震地+烈焰冲撞+火山喷发 */
        INFERNO(5, 1.4, 0.8, "sixty_seconds_boss_inferno"),
        /** 霜噬守望：冰控法师，冰锥+霜息+暴雪新星 */
        FROSTBITE(6, 1.1, 0.9, "sixty_seconds_boss_frostbite"),
        /** 虫潮之主：召唤流，虫群+酸液弹幕+腐蚀新星 */
        SWARMKEEPER(7, 0.9, 1.0, "sixty_seconds_boss_swarmkeeper"),
        /** 雷霆传令：机动刺客，瞬电冲撞+雷瞬击+雷暴弹幕 */
        STORMHERALD(8, 0.85, 1.3, "sixty_seconds_boss_stormherald"),
        /** 虚空织者：暗影法师，生命汲取+暗影突袭+虚空新星 */
        VOIDWEAVER(9, 0.95, 1.05, "sixty_seconds_boss_voidweaver");

        public final int id;
        public final double healthMult;
        public final double speedMult;
        public final String textureName;

        BossVariant(int id, double healthMult, double speedMult, String textureName) {
            this.id = id;
            this.healthMult = healthMult;
            this.speedMult = speedMult;
            this.textureName = textureName;
        }

        public static BossVariant byId(int id) {
            for (BossVariant v : values()) {
                if (v.id == id) return v;
            }
            return RAVAGER;
        }

        public String nameKey() {
            return "entity.sixty_seconds." + textureName;
        }
    }

    private static final EntityDataAccessor<Integer> BOSS_LEVEL =
            SynchedEntityData.defineId(SixtySecondsBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> APEX =
            SynchedEntityData.defineId(SixtySecondsBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BOSS_VARIANT =
            SynchedEntityData.defineId(SixtySecondsBossEntity.class, EntityDataSerializers.INT);
    /**
     * 驻守岛 id：炼狱岛固定驻守 Boss 写入所属岛屿 id（≥0）；
     * 普通/夜晚 Boss 为 -1。用于「玩家远离则消失、靠近则去重重生」逻辑。
     */
    private static final EntityDataAccessor<Integer> HOME_ISLAND_ID =
            SynchedEntityData.defineId(SixtySecondsBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.sixty_seconds.sixty_seconds_boss"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    // ── 技能冷却（gameTime 时间戳）─────────────────────────────────────
    private long nextSlamTick = 0;
    private long nextRoarTick = 0;
    private long nextSummonTick = 0;
    private long nextChargeTick = 0;
    private long nextBarrageTick = 0;
    // 新增技能冷却
    private long nextDrainTick = 0;       // 生命汲取
    private long nextBreathTick = 0;      // 毒息
    private long nextShadowTick = 0;      // 暗影突袭/鬼魅瞬击
    private long nextSpearTick = 0;       // 骨矛
    private long nextSkinTick = 0;        // 铁壁
    private long nextNovaTick = 0;        // 剧毒新星

    /** 狂怒已激活标记（巨像低血被动） */
    private boolean frenzied = false;
    /** 潜行冷却 / 持续时间管理（鬼魅） */
    private int vanishCooldown = 0;

    public SixtySecondsBossEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_LEVEL, 1);
        builder.define(APEX, false);
        builder.define(BOSS_VARIANT, BossVariant.RAVAGER.id);
        builder.define(HOME_ISLAND_ID, -1);
    }

    /** 陆地 Boss 体型相对原有尺寸的放大倍数（基础为原有尺寸的 2.5 倍）。 */
    private static final float BOSS_SCALE_MULTIPLIER = 2.5F;

    /** 按 Boss 等级装配（普通尸潮领主）。 */
    public void applyBossLevel(int level) {
        applyBossLevel(level, false, BossVariant.RAVAGER);
    }

    public void applyBossLevel(int level, boolean apex) {
        applyBossLevel(level, apex, BossVariant.RAVAGER);
    }

    /**
     * 按 Boss 等级与变体装配：血量/移速/体型/击退抗性 + 血条标题。
     * {@code apex=true} 为终焉之王终极形态。
     */
    public void applyBossLevel(int level, boolean apex, BossVariant variant) {
        int lvl = Mth.clamp(level, 1, SixtySecondsBalance.BOSS_MAX_LEVEL);
        this.entityData.set(BOSS_LEVEL, lvl);
        this.entityData.set(APEX, apex);
        this.entityData.set(BOSS_VARIANT, variant.id);
        addTag(PVE_TAG);
        double baseHealth = (SixtySecondsBalance.BOSS_BASE_HEALTH
                + SixtySecondsBalance.BOSS_HEALTH_PER_LEVEL * (lvl - 1)) * variant.healthMult;
        double apexMult = apex ? 1.8 : 1.0;
        setAttr(Attributes.MAX_HEALTH, baseHealth * apexMult);
        setAttr(Attributes.MOVEMENT_SPEED, (apex ? 0.27 : 0.24) * variant.speedMult);
        setAttr(Attributes.KNOCKBACK_RESISTANCE, 1.0);
        // 陆地 Boss 体型：基础为原有尺寸的 2.5 倍，并保留按等级/apex 变化
        float baseScale = (apex ? 1.7F : 1.35F) + 0.15F * (lvl - 1);
        setAttr(Attributes.SCALE, baseScale * BOSS_SCALE_MULTIPLIER);
        setHealth(getMaxHealth());
        // 名称
        Component name;
        String baseNameKey = variant == BossVariant.RAVAGER
                ? (apex ? "entity.sixty_seconds.sixty_seconds_boss_apex_leveled"
                       : "entity.sixty_seconds.sixty_seconds_boss_leveled")
                : (apex ? "entity.sixty_seconds.sixty_seconds_boss_variant_apex"
                       : "entity.sixty_seconds.sixty_seconds_boss_variant_leveled");
        if (variant == BossVariant.RAVAGER) {
            name = Component.translatable(baseNameKey, lvl)
                    .withStyle(apex ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_RED);
        } else {
            name = Component.translatable(baseNameKey,
                    Component.translatable(variant.nameKey()), lvl)
                    .withStyle(apex ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_RED);
        }
        setCustomName(name);
        setCustomNameVisible(false);
        bossEvent.setName(name);
        bossEvent.setColor(apex ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED);
        setPersistenceRequired();
        setBattleMob(true);
        // 难度：定级会重设 base 属性，故以新 base 为基准重新施加难度缩放
        SixtySecondsDifficulty.reapply(this);
    }

    public boolean isApex() {
        return this.entityData.get(APEX);
    }

    public BossVariant getBossVariant() {
        return BossVariant.byId(this.entityData.get(BOSS_VARIANT));
    }

    /** 设置/读取驻守岛 id（炼狱岛固定 Boss 用；-1 表示非驻守 Boss）。 */
    public void setHomeIslandId(int id) {
        this.entityData.set(HOME_ISLAND_ID, id);
    }

    public int getHomeIslandId() {
        return this.entityData.get(HOME_ISLAND_ID);
    }

    /** 是否为某炼狱岛的驻守 Boss。 */
    public boolean isGarrisonBoss() {
        return this.entityData.get(HOME_ISLAND_ID) >= 0;
    }

    private void setAttr(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
            double value) {
        var instance = getAttribute(attr);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public int bossLevel() {
        return this.entityData.get(BOSS_LEVEL);
    }

    /** 「伤害 Boss」实体 tag（每局仅一只；近战命中固定伤害、护甲不减免）。由区域 Boss 系统挂载。 */
    public static final String DAMAGE_BOSS_TAG = "sixty_seconds_damage_boss";

    @Override
    protected int meleeInjury() {
        // 伤害 Boss：固定高额伤害，无视等级/狂怒倍率
        if (getTags().contains(DAMAGE_BOSS_TAG)) {
            return SixtySecondsBalance.DAMAGE_BOSS_MELEE_INJURY;
        }
        int base = SixtySecondsBalance.BOSS_MELEE_INJURY + 4 * (bossLevel() - 1);
        if (frenzied) base = (int)(base * 1.4);
        return base;
    }

    @Override
    public ResourceLocation textureLocation() {
        if (isApex()) {
            return ResourceLocation.fromNamespaceAndPath("sixty_seconds",
                    "textures/entity/sixty_seconds_boss_apex.png");
        }
        // 每个 Boss 变体使用独立纹理（注意取 BossVariant，而非父类的小怪 Variant）
        return ResourceLocation.fromNamespaceAndPath("sixty_seconds",
                "textures/entity/" + getBossVariant().textureName + ".png");
    }

    // ── Boss 血条 ─────────────────────────────────────────────────────
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    /** 单次受击封顶；巨像额外 30% 减伤。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        float capped = Math.min(amount, SixtySecondsBalance.BOSS_MAX_SINGLE_HIT);
        if (getBossVariant() == BossVariant.COLOSSUS) {
            capped *= 0.7F;
        }
        return super.hurt(source, capped);
    }

    // ══════════════════════════════════════════════════════════════════
    //  主 tick：按变体分支
    // ══════════════════════════════════════════════════════════════════
    /** 客户端局部粒子：仅在渲染该实体的客户端生成（无网络开销），数量受控以保性能。 */
    private void clientParticle(ParticleOptions pt, double x, double y, double z, int n) {
        if (level().isClientSide()) {
            for (int i = 0; i < n; i++) {
                double ox = (level().random.nextDouble() - 0.5) * 0.6;
                double oy = level().random.nextDouble() * 0.8;
                double oz = (level().random.nextDouble() - 0.5) * 0.6;
                level().addParticle(pt, x + ox, y + oy, z + oz, 0, 0.05, 0);
            }
        }
    }

    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target == null || tickCount % 2 != 0) {
            return;
        }
        long now = serverLevel.getGameTime();
        int lvl = bossLevel();
        boolean apex = isApex();
        BossVariant variant = getBossVariant();

        // 巨像狂怒被动
        if (variant == BossVariant.COLOSSUS) {
            tickFrenzy();
        }
        // 疫病者腐化光环
        if (variant == BossVariant.PLAGUEBEARER && tickCount % 20 == 0) {
            tickCorruptionAura(serverLevel);
        }
        // 鬼魅潜行冷却
        if (vanishCooldown > 0) vanishCooldown--;

        double distSqr = distanceToSqr(target);

        switch (variant) {
            case RAVAGER -> tickRavager(serverLevel, target, now, lvl, apex, distSqr);
            case COLOSSUS -> tickColossus(serverLevel, target, now, lvl, apex, distSqr);
            case NECROMANCER -> tickNecromancer(serverLevel, target, now, lvl, apex, distSqr);
            case PLAGUEBEARER -> tickPlaguebearer(serverLevel, target, now, lvl, apex, distSqr);
            case SPECTER -> tickSpecter(serverLevel, target, now, lvl, apex, distSqr);
            case INFERNO -> tickInferno(serverLevel, target, now, lvl, apex, distSqr);
            case FROSTBITE -> tickFrostbite(serverLevel, target, now, lvl, apex, distSqr);
            case SWARMKEEPER -> tickSwarmkeeper(serverLevel, target, now, lvl, apex, distSqr);
            case STORMHERALD -> tickStormherald(serverLevel, target, now, lvl, apex, distSqr);
            case VOIDWEAVER -> tickVoidweaver(serverLevel, target, now, lvl, apex, distSqr);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  熔渊暴君 INFERNO（火系重装）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickInferno(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextSlamTick && distSqr <= 5 * 5) {
            infernoSlam(serverLevel, now, lvl);              // 熔岩震地
        } else if (now >= nextChargeTick && distSqr >= 7 * 7 && distSqr <= 22 * 22) {
            infernoCharge(serverLevel, now, target);        // 烈焰冲撞
        } else if (lvl >= 2 && now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 28 * 28 && hasLineOfSight(target)) {
            infernoMeteor(serverLevel, now, target, lvl);   // 火球弹幕
        } else if (lvl >= 3 && now >= nextSummonTick) {
            infernoErupt(serverLevel, now, lvl);            // 火山喷发（召唤火元素）
        } else if (lvl >= 4 && now >= nextRoarTick && distSqr <= 12 * 12) {
            infernoRoar(serverLevel, now, lvl);             // 烈焰咆哮
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  霜噬守望 FROSTBITE（冰控法师）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickFrostbite(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextSpearTick && distSqr >= 4 * 4 && distSqr <= 22 * 22 && hasLineOfSight(target)) {
            frostLance(serverLevel, now, target, lvl);      // 冰锥
        } else if (now >= nextBreathTick && distSqr <= 10 * 10) {
            frostBreath(serverLevel, now, target, lvl);     // 霜息
        } else if (lvl >= 2 && now >= nextRoarTick && distSqr <= 14 * 14) {
            frostRoar(serverLevel, now, lvl);               // 寒霜咆哮
        } else if (lvl >= 3 && now >= nextNovaTick) {
            frostNova(serverLevel, now, lvl);               // 暴雪新星
        } else if (apex && now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 26 * 26 && hasLineOfSight(target)) {
            frostShardStorm(serverLevel, now, target, lvl);// 冰晶风暴
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  虫潮之主 SWARMKEEPER（召唤流）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickSwarmkeeper(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextSummonTick) {
            swarmSummon(serverLevel, now, lvl);            // 虫群
        } else if (lvl >= 2 && now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 30 * 30 && hasLineOfSight(target)) {
            swarmAcid(serverLevel, now, target, lvl);      // 酸液弹幕
        } else if (lvl >= 3 && now >= nextNovaTick) {
            swarmNova(serverLevel, now, lvl);              // 腐蚀新星
        } else if (apex && distSqr <= 12 * 12 && now >= nextSlamTick) {
            swarmCrush(serverLevel, now, lvl);            // 虫群碾压
        } else if (lvl >= 5 && now >= nextRoarTick) {
            swarmRoar(serverLevel, now, lvl);             // 万虫咆哮
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  雷霆传令 STORMHERALD（机动刺客）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickStormherald(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextChargeTick && distSqr >= 6 * 6 && distSqr <= 28 * 28) {
            stormCharge(serverLevel, now, target);        // 瞬电冲撞
        } else if (lvl >= 2 && now >= nextShadowTick && distSqr <= 64) {
            stormBlink(serverLevel, now, target, lvl, apex);   // 雷瞬击
        } else if (now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 30 * 30 && hasLineOfSight(target)) {
            stormBarrage(serverLevel, now, target, lvl); // 雷暴弹幕
        } else if (lvl >= 4 && apex && now >= nextRoarTick && distSqr <= 12 * 12) {
            stormRoar(serverLevel, now, lvl);             // 雷霆咆哮
        } else if (distSqr <= 5 * 5 && now >= nextSlamTick) {
            stormQuake(serverLevel, now, lvl);           // 落雷震地
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  虚空织者 VOIDWEAVER（暗影法师）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickVoidweaver(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextDrainTick && distSqr <= 8 * 8) {
            voidDrain(serverLevel, now, target, lvl);    // 生命汲取
        } else if (lvl >= 2 && now >= nextShadowTick && distSqr <= 64) {
            voidBlink(serverLevel, now, target, lvl, apex);   // 暗影突袭
        } else if (lvl >= 3 && now >= nextNovaTick) {
            voidNova(serverLevel, now, lvl);             // 虚空新星
        } else if (apex && now >= nextRoarTick && distSqr <= 12 * 12) {
            voidRoar(serverLevel, now, lvl);             // 虚空咆哮
        } else if (distSqr <= 6 * 6 && now >= nextSlamTick) {
            voidCrush(serverLevel, now, lvl);           // 虚空碾压
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  破坏者 RAVAGER（蛮力近战）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickRavager(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextSlamTick && distSqr <= 5 * 5) {
            ravagerQuake(serverLevel, now, lvl);        // 践踏震波
        } else if (lvl >= 2 && now >= nextRoarTick && distSqr <= 12 * 12) {
            ravagerRoar(serverLevel, now, lvl);         // 战吼
        } else if (apex && now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 30 * 30
                && hasLineOfSight(target)) {
            ravagerVolley(serverLevel, now, target, lvl); // 投掷巨石
        } else if (lvl >= 3 && now >= nextSummonTick) {
            ravagerCall(serverLevel, now, lvl);         // 召唤野猪兽
        } else if (lvl >= 4 && now >= nextChargeTick && distSqr >= 8 * 8 && distSqr <= 24 * 24) {
            ravagerCharge(serverLevel, now, target);   // 蛮力冲撞
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  巨像 COLOSSUS（重装坦克）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickColossus(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextSlamTick && distSqr <= 6.5 * 6.5) {
            colossusSmash(serverLevel, now, lvl);      // 巨拳重砸
        } else if (lvl >= 2 && now >= nextSkinTick && distSqr <= 16 * 16) {
            colossusBulwark(serverLevel, now, lvl, apex); // 铁壁
        } else if (lvl >= 3 && now >= nextSlamTick && distSqr >= 5 * 5 && distSqr <= 18 * 18) {
            colossusQuake(serverLevel, now, target, lvl);  // 裂地震波
            nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS / 2;
        } else if (lvl >= 4 && now >= nextChargeTick && distSqr >= 8 * 8 && distSqr <= 24 * 24) {
            colossusCharge(serverLevel, now, target);  // 碾压冲撞
        } else if (apex && now >= nextRoarTick && distSqr <= 10 * 10) {
            colossusStunRoar(serverLevel, now, lvl);   // 震慑咆哮
        } else if (now >= nextSlamTick && distSqr <= 5 * 5) {
            colossusSmash(serverLevel, now, lvl);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  亡灵术士 NECROMANCER（亡灵法师）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickNecromancer(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextDrainTick && distSqr <= 10 * 10 && hasLineOfSight(target)
                && getHealth() < getMaxHealth() * 0.7) {
            necroDrain(serverLevel, now, target, lvl);
        } else if (now >= nextSpearTick && distSqr >= 6 * 6 && distSqr <= 22 * 22
                && hasLineOfSight(target)) {
            necroSpear(serverLevel, now, target, lvl);
        } else if (lvl >= 2 && now >= nextSummonTick) {
            necroRaise(serverLevel, now, lvl);
        } else if (lvl >= 3 && now >= nextRoarTick && distSqr <= 12 * 12) {
            necroRoar(serverLevel, now, lvl);
        } else if (apex && now >= nextSummonTick && getHealth() < getMaxHealth() * 0.5) {
            necroLegion(serverLevel, now, lvl);        // 亡者大军
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  疫病者 PLAGUEBEARER（毒系）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickPlaguebearer(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextBreathTick && distSqr <= 7 * 7) {
            plagueBreath(serverLevel, now, target, lvl);
        } else if (now >= nextBarrageTick && distSqr >= 6 * 6 && distSqr <= 26 * 26
                && hasLineOfSight(target)) {
            plagueBolt(serverLevel, now, target, lvl);
        } else if (lvl >= 3 && now >= nextRoarTick && distSqr <= 14 * 14) {
            plagueRoar(serverLevel, now, lvl);         // 毒化咆哮：污染代替扣san
        } else if (apex && now >= nextNovaTick && distSqr <= 12 * 12) {
            plagueNova(serverLevel, now, lvl);
        } else if (lvl >= 4 && now >= nextSummonTick) {
            plagueSwarm(serverLevel, now, lvl);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  鬼魅 SPECTER（潜行刺客）—— 独立招式
    // ══════════════════════════════════════════════════════════════════
    private void tickSpecter(ServerLevel serverLevel, LivingEntity target, long now, int lvl, boolean apex, double distSqr) {
        if (now >= nextShadowTick && distSqr >= 5 * 5 && distSqr <= 16 * 16
                && hasLineOfSight(target)) {
            specterBlink(serverLevel, now, target, lvl, apex);
        } else if (lvl >= 2 && now >= nextSlamTick && distSqr <= 4 * 4) {
            specterFlurry(serverLevel, now, lvl);
        } else if (lvl >= 3 && vanishCooldown <= 0 && getHealth() < getMaxHealth() * 0.65) {
            specterVanish(serverLevel, now, lvl);
        } else if (apex && now >= nextChargeTick && distSqr >= 8 * 8 && distSqr <= 30 * 30) {
            specterMark(serverLevel, now, target, lvl);
        } else if (lvl >= 4 && now >= nextRoarTick && distSqr <= 10 * 10) {
            specterRoar(serverLevel, now, lvl);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  原有技能
    // ══════════════════════════════════════════════════════════════════

    /** 技能伤害：叠加难度加成（近战见 {@link #meleeInjury()}，在基类 doHurtTarget 处统一加成）。 */
    private int skillInjury(int base) {
        return SixtySecondsDifficulty.scaleInjury(this, base);
    }

    // ══════════════════════════════════════════════════════════════════
    //  破坏者 RAVAGER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 践踏震波：近身环形伤害 + 击飞。 */
    private void ravagerQuake(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 3);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 0.7F);
        int injury = skillInjury((SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1)) * (frenzied ? 2 : 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 5.5 * 5.5) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 0.9, 0.6, away.z * 0.9);
            p.hurtMarked = true;
        }
    }
    /** 战吼：自身加速 + 范围减速（压迫感）。 */
    private void ravagerRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
        clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 2);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 0));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 1));
            p.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.boss_roar")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }
    /** 投掷巨石：远程酸石弹幕。 */
    private void ravagerVolley(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 4 + lvl / 2;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 4.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 4.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.0F, 6.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 3);
    }
    /** 召唤野猪兽：召唤地面小怪助战。 */
    private void ravagerCall(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.ZOMBIE_AMBIENT, 1.4F, 0.5F);
        int count = 2 + lvl / 2;
        for (int i = 0; i < count; i++) {
            Variant v = sl.random.nextFloat() < 0.35F ? Variant.RUNNER : Variant.SHAMBLER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        swing(InteractionHand.MAIN_HAND);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.5, getZ(), 4);
    }
    /** 蛮力冲撞：冲向目标造成伤害 + 击退。 */
    private void ravagerCharge(ServerLevel sl, long now, LivingEntity target) {
        nextChargeTick = now + SixtySecondsBalance.BOSS_CHARGE_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.3F);
        Vec3 toward = target.position().subtract(position()).normalize();
        setDeltaMovement(toward.x * 1.6, 0.25, toward.z * 1.6);
        hurtMarked = true;
        clientParticle(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 3);
    }

    // ══════════════════════════════════════════════════════════════════
    //  巨像 COLOSSUS 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 巨拳重砸：范围伤害 + 强击退。 */
    private void colossusSmash(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 3);
        playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        int injury = skillInjury((SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1)) * 2 * (frenzied ? 2 : 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 6.5 * 6.5) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 1.15, 0.6, away.z * 1.15);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 10, 4));
        }
    }
    /** 铁壁：短暂大幅减伤 + 冲击波击退。 */
    private void colossusBulwark(ServerLevel sl, long now, int lvl, boolean apex) {
        nextSkinTick = now + (apex ? 20 * 25 : 20 * 35);
        playSound(SoundEvents.ANVIL_LAND, 0.6F, 1.5F);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getEyeY(), getZ(), 5);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 6, 2));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 8, 0));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 4 * 4) continue;
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 0.7, 0.35, away.z * 0.7);
            p.hurtMarked = true;
        }
    }
    /** 裂地震波：朝目标方向释放地面波。 */
    private void colossusQuake(ServerLevel sl, long now, LivingEntity target, int lvl) {
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.7F, 0.5F);
        Vec3 dir = target.position().subtract(position()).normalize();
        for (int i = 1; i <= 16; i++) {
            double x = getX() + dir.x * i, z = getZ() + dir.z * i;
            if (i % 2 == 0) {
                clientParticle(ParticleTypes.CLOUD, x, getY() + 0.1, z, 1);
                clientParticle(ParticleTypes.EXPLOSION, x, getY() + 0.1, z, 1);
            }
            for (ServerPlayer p : sl.players()) {
                if (!isValidPrey(p)) continue;
                if (p.distanceToSqr(new Vec3(x, p.getY(), z)) < 2 * 2) {
                    SixtySecondsHealthSystem.applyInjury(p, null, skillInjury(SixtySecondsBalance.BOSS_SLAM_INJURY + lvl * 3));
                    p.setDeltaMovement(dir.x * 0.6, 0.4, dir.z * 0.6);
                    p.hurtMarked = true;
                }
            }
        }
    }
    /** 碾压冲撞：高速冲撞。 */
    private void colossusCharge(ServerLevel sl, long now, LivingEntity target) {
        nextChargeTick = now + SixtySecondsBalance.BOSS_CHARGE_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.3F);
        Vec3 toward = target.position().subtract(position()).normalize();
        setDeltaMovement(toward.x * (frenzied ? 2.2 : 1.8), 0.25, toward.z * (frenzied ? 2.2 : 1.8));
        hurtMarked = true;
        clientParticle(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 3);
    }
    /** 震慑咆哮：范围虚弱 + 减速。 */
    private void colossusStunRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.6F, 0.7F);
        clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 10 * 10) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 1));
            p.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.boss_roar")
                    .withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  亡灵术士 NECROMANCER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 生命汲取：连线 + 自身治疗。 */
    private void necroDrain(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextDrainTick = now + 20 * 16;
        playSound(SoundEvents.WARDEN_HEARTBEAT, 0.5F, 0.3F);
        Vec3 from = new Vec3(getX(), getEyeY(), getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 step = to.subtract(from).normalize().scale(0.5);
        Vec3 pos = from;
        for (int i = 0; i < 12; i++) {
            pos = pos.add(step.scale(2.5));
            clientParticle(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 1);
        }
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            int drain = skillInjury(8 + lvl * 2);
            SixtySecondsHealthSystem.applyInjury(p, null, drain);
            heal(drain * 0.8F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
        }
    }
    /** 骨矛：远程穿透投射。 */
    private void necroSpear(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextSpearTick = now + 20 * 5;
        playSound(SoundEvents.SKELETON_SHOOT, 0.7F, 0.9F);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        SixtySecondsAcidSpitEntity spear = new SixtySecondsAcidSpitEntity(sl, this);
        double dx = target.getX() - getX(), dy = target.getY(0.4) - spear.getY(), dz = target.getZ() - getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        spear.shoot(dx, dy + h * 0.08, dz, 2.0F, 2.0F);
        spear.setNoGravity(true);
        sl.addFreshEntity(spear);
        clientParticle(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 2);
    }
    /** 亡者召唤。 */
    private void necroRaise(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.ZOMBIE_AMBIENT, 1.4F, 0.5F);
        int count = 2 + lvl / 2;
        for (int i = 0; i < count; i++) {
            float r = sl.random.nextFloat();
            Variant v = r < 0.2F ? Variant.BRUTE : r < 0.45F ? Variant.RUNNER : r < 0.6F ? Variant.STALKER : Variant.SHAMBLER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        swing(InteractionHand.MAIN_HAND);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.5, getZ(), 4);
    }
    /** 亡灵咆哮：范围虚弱。 */
    private void necroRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.6F);
        clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 0));
        }
    }
    /** 亡者大军：大量召唤 + 小怪强化光环。 */
    private void necroLegion(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.WITHER_SPAWN, 0.5F, 0.4F);
        clientParticle(ParticleTypes.PORTAL, getX(), getY() + 0.5, getZ(), 8);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.5, getZ(), 5);
        int count = 5 + lvl;
        for (int i = 0; i < count; i++) {
            float r = sl.random.nextFloat();
            Variant v = r < 0.3F ? Variant.BRUTE : r < 0.55F ? Variant.RUNNER : r < 0.75F ? Variant.BLOATER : Variant.STALKER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        for (SixtySecondsMonsterEntity mob : sl.getEntitiesOfClass(SixtySecondsMonsterEntity.class, getBoundingBox().inflate(16.0))) {
            if (mob == this) continue;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 12, 1));
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 12, 0));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  疫病者 PLAGUEBEARER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 毒息：前方锥形中毒 + 污染。 */
    private void plagueBreath(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBreathTick = now + 20 * 12;
        playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 0.3F, 0.4F);
        Vec3 facing = getLookAngle().normalize();
        for (int i = 0; i < 8; i++) {
            double spread = (sl.random.nextDouble() - 0.5) * 1.8;
            double dist = 1.0 + i * 2.5 / 8;
            clientParticle(ParticleTypes.ITEM_SLIME, getX() + facing.x * dist + spread, getEyeY() + spread * 0.5, getZ() + facing.z * dist + spread, 1);
        }
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p)) continue;
            Vec3 toP = p.position().subtract(position());
            if (toP.length() > 7 || toP.normalize().dot(facing) < 0.35) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, skillInjury(6 + lvl * 2));
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(p);
            stats.pollution = Math.min(100, stats.pollution + SixtySecondsDifficulty.scalePollutionGain(sl, 6 + lvl));
            stats.sync();
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, lvl > 2 ? 1 : 0));
        }
    }
    /** 毒弹：远程酸液弹幕。 */
    private void plagueBolt(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 4 + lvl / 2;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 4.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 4.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.0F, 6.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 3);
    }
    /** 毒化咆哮：污染代替扣 san。 */
    private void plagueRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
        clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 14 * 14) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 1));
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(p);
            stats.pollution = Math.min(100, stats.pollution + SixtySecondsDifficulty.scalePollutionGain(sl, 8 + lvl * 2));
            stats.sync();
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 3, 0));
        }
    }
    /** 疫病新星：环形毒弹 + 污染。 */
    private void plagueNova(ServerLevel sl, long now, int lvl) {
        nextNovaTick = now + 20 * 22;
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 0.7F, 1.2F);
        int shots = 12 + lvl;
        for (int i = 0; i < shots; i++) {
            double angle = (2 * Math.PI / shots) * i;
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            spit.shoot(Math.cos(angle), 0.1, Math.sin(angle), 0.7F, 8.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3);
    }
    /** 疫虫召唤。 */
    private void plagueSwarm(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.ZOMBIE_AMBIENT, 1.4F, 0.5F);
        int count = 2 + lvl / 2;
        for (int i = 0; i < count; i++) {
            Variant v = sl.random.nextFloat() < 0.35F ? Variant.RUNNER : Variant.SHAMBLER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        swing(InteractionHand.MAIN_HAND);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.5, getZ(), 4);
    }

    // ══════════════════════════════════════════════════════════════════
    //  鬼魅 SPECTER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 暗影突袭：瞬移至目标背后重击。 */
    private void specterBlink(ServerLevel sl, long now, LivingEntity target, int lvl, boolean apex) {
        nextShadowTick = now + (apex ? 20 * 6 : 20 * 10);
        playSound(SoundEvents.WARDEN_NEARBY_CLOSE, 0.6F, 1.6F);
        Vec3 behind = target.position().add(target.getLookAngle().scale(-2.0));
        teleportTo(behind.x, behind.y, behind.z);
        clientParticle(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 4);
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            int back = skillInjury((14 + lvl * 4) * (apex ? 2 : 1));
            SixtySecondsHealthSystem.applyInjury(p, null, back);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 + 10, 0));
        }
    }
    /** 暗影连斩：近身多段低伤。 */
    private void specterFlurry(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + 20 * 8;
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.8F);
        clientParticle(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 0.5, getZ(), 2);
        int injury = skillInjury(4 + lvl);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 4 * 4) continue;
            for (int i = 0; i < 3; i++) SixtySecondsHealthSystem.applyInjury(p, null, injury);
        }
    }
    /** 隐匿：隐身 + 加速。 */
    private void specterVanish(ServerLevel sl, long now, int lvl) {
        vanishCooldown = 20 * 30;
        playSound(SoundEvents.WARDEN_DIG, 0.5F, 1.0F);
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 5, 0, false, false, false));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 5, 2, false, false, false));
        clientParticle(ParticleTypes.SMOKE, getX(), getY() + 1.0, getZ(), 6);
    }
    /** 死亡标记：多次瞬击。 */
    private void specterMark(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextChargeTick = now + 20 * 20;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 0.8F);
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            p.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 4, 0));
            p.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.boss_death_mark")
                    .withStyle(ChatFormatting.DARK_RED), true);
        }
        for (int i = 0; i < 3; i++) {
            sl.getServer().tell(new net.minecraft.server.TickTask(i * 6, () -> {
                if (!isAlive() || target == null || !target.isAlive()) return;
                Vec3 behind = target.position().add(target.getLookAngle().scale(-2.0));
                teleportTo(behind.x, behind.y, behind.z);
                if (target instanceof ServerPlayer p && isValidPrey(p)) {
                    SixtySecondsHealthSystem.applyInjury(p, null, skillInjury(10 + lvl * 3));
                }
                clientParticle(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 3);
            }));
        }
    }
    /** 鬼魅咆哮：范围恐惧。 */
    private void specterRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
        clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 10 * 10) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 0));
        }
    }

    /** 腐化光环（疫病者被动）：每 1s 周围玩家获得污染。 */
    private void tickCorruptionAura(ServerLevel serverLevel) {
        double radius = isApex() ? 10.0 : 7.0;
        for (ServerPlayer player : serverLevel.players()) {
            if (!isValidPrey(player) || distanceToSqr(player) > radius * radius) continue;
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
            stats.pollution = Math.min(100, stats.pollution
                    + SixtySecondsDifficulty.scalePollutionGain(serverLevel, 1));
            stats.sync();
        }
        // 客户端局部粒子：仅渲染该实体的客户端产生（无网络开销），节流至每 10 tick 极少量以保性能
        if (level().isClientSide() && tickCount % 10 == 0) {
            clientParticle(ParticleTypes.MYCELIUM, getX(), getY() + 0.5, getZ(), 2);
            for (Player p : level().players()) {
                if (p.hasEffect(MobEffects.POISON) && p.distanceToSqr(this) < radius * radius) {
                    clientParticle(ParticleTypes.ITEM_SLIME, p.getX(), p.getY() + 0.5, p.getZ(), 1);
                }
            }
        }
        if (tickCount % 40 == 0) {
            playSound(SoundEvents.WITHER_AMBIENT, 0.12F, 1.4F);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  熔渊暴君 INFERNO 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 熔岩震地：更大范围 + 点燃感。 */
    private void infernoSlam(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 4);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 0.6F);
        int injury = skillInjury((SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1)) * 2 * (frenzied ? 2 : 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 6.0 * 6.0) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 1.0, 0.6, away.z * 1.0);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 10, 3));
        }
    }
    /** 烈焰冲撞。 */
    private void infernoCharge(ServerLevel sl, long now, LivingEntity target) {
        nextChargeTick = now + SixtySecondsBalance.BOSS_CHARGE_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.3F);
        Vec3 toward = target.position().subtract(position()).normalize();
        setDeltaMovement(toward.x * 1.7, 0.25, toward.z * 1.7);
        hurtMarked = true;
        clientParticle(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 3);
    }
    /** 火球弹幕：远程熔岩弹。 */
    private void infernoMeteor(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 4 + lvl / 2;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 4.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 4.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.0F, 6.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.FLAME, getX(), getEyeY(), getZ(), 3);
    }
    /** 火山喷发：召唤火元素小怪。 */
    private void infernoErupt(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.WITHER_SPAWN, 0.6F, 0.5F);
        int count = 2 + lvl / 2;
        for (int i = 0; i < count; i++) {
            Variant v = sl.random.nextFloat() < 0.4F ? Variant.BRUTE : Variant.STALKER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        clientParticle(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 6);
    }
    /** 烈焰咆哮：范围燃烧感减速。 */
    private void infernoRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.5F, 0.8F);
        clientParticle(ParticleTypes.FLAME, getX(), getEyeY(), getZ(), 3);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 2));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 3, 0));
            p.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.boss_roar")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  霜噬守望 FROSTBITE 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 冰锥：远程穿透。 */
    private void frostLance(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextSpearTick = now + 20 * 5;
        playSound(SoundEvents.SKELETON_SHOOT, 0.7F, 0.9F);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        SixtySecondsAcidSpitEntity spear = new SixtySecondsAcidSpitEntity(sl, this);
        double dx = target.getX() - getX(), dy = target.getY(0.4) - spear.getY(), dz = target.getZ() - getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        spear.shoot(dx, dy + h * 0.08, dz, 2.0F, 2.0F);
        spear.setNoGravity(true);
        sl.addFreshEntity(spear);
        clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 2);
    }
    /** 霜息：锥形冰冻 + 减速。 */
    private void frostBreath(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBreathTick = now + 20 * 12;
        playSound(SoundEvents.DRAGON_FIREBALL_EXPLODE, 0.3F, 0.4F);
        Vec3 facing = getLookAngle().normalize();
        for (int i = 0; i < 8; i++) {
            double spread = (sl.random.nextDouble() - 0.5) * 1.8;
            double dist = 1.0 + i * 2.5 / 8;
            clientParticle(ParticleTypes.SNOWFLAKE, getX() + facing.x * dist + spread, getEyeY() + spread * 0.5, getZ() + facing.z * dist + spread, 1);
        }
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p)) continue;
            Vec3 toP = p.position().subtract(position());
            if (toP.length() > 7 || toP.normalize().dot(facing) < 0.35) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, skillInjury(6 + lvl * 2));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, lvl > 2 ? 2 : 1));
        }
    }
    /** 寒霜咆哮：范围减速。 */
    private void frostRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
        clientParticle(ParticleTypes.SNOWFLAKE, getX(), getEyeY(), getZ(), 3);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 14 * 14) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 3, 0));
        }
    }
    /** 暴雪新星：环形冰弹。 */
    private void frostNova(ServerLevel sl, long now, int lvl) {
        nextNovaTick = now + 20 * 22;
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 0.7F, 1.2F);
        int shots = 12 + lvl;
        for (int i = 0; i < shots; i++) {
            double angle = (2 * Math.PI / shots) * i;
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            spit.shoot(Math.cos(angle), 0.1, Math.sin(angle), 0.7F, 8.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5, getZ(), 3);
    }
    /** 冰晶风暴：远程密集冰弹。 */
    private void frostShardStorm(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 6 + lvl;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 3.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 3.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.1F, 5.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.SNOWFLAKE, getX(), getEyeY(), getZ(), 4);
    }

    // ══════════════════════════════════════════════════════════════════
    //  虫潮之主 SWARMKEEPER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 虫群召唤。 */
    private void swarmSummon(ServerLevel sl, long now, int lvl) {
        nextSummonTick = now + SixtySecondsBalance.BOSS_SUMMON_COOLDOWN_TICKS;
        playSound(SoundEvents.ZOMBIE_AMBIENT, 1.4F, 0.5F);
        int count = 3 + lvl / 2;
        for (int i = 0; i < count; i++) {
            Variant v = sl.random.nextFloat() < 0.5F ? Variant.RUNNER : Variant.SHAMBLER;
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.spawnMinion(sl, blockPosition(), v);
        }
        swing(InteractionHand.MAIN_HAND);
        clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 0.5, getZ(), 4);
    }
    /** 酸液弹幕。 */
    private void swarmAcid(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 4 + lvl / 2;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 4.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 4.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.0F, 6.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 3);
    }
    /** 腐蚀新星。 */
    private void swarmNova(ServerLevel sl, long now, int lvl) {
        nextNovaTick = now + 20 * 22;
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 0.7F, 1.2F);
        int shots = 12 + lvl;
        for (int i = 0; i < shots; i++) {
            double angle = (2 * Math.PI / shots) * i;
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            spit.shoot(Math.cos(angle), 0.1, Math.sin(angle), 0.7F, 8.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 3);
    }
    /** 虫群碾压：近身范围。 */
    private void swarmCrush(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.2, getZ(), 4);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 0.9F);
        int injury = skillInjury(SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 5.5 * 5.5) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 0.7, 0.4, away.z * 0.7);
            p.hurtMarked = true;
        }
    }
    /** 万虫咆哮：范围污染 + 减速。 */
    private void swarmRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
        clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 5, 1));
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(p);
            stats.pollution = Math.min(100, stats.pollution + SixtySecondsDifficulty.scalePollutionGain(sl, 8 + lvl * 2));
            stats.sync();
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 3, 0));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  雷霆传令 STORMHERALD 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 瞬电冲撞。 */
    private void stormCharge(ServerLevel sl, long now, LivingEntity target) {
        nextChargeTick = now + SixtySecondsBalance.BOSS_CHARGE_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 1.3F);
        Vec3 toward = target.position().subtract(position()).normalize();
        setDeltaMovement(toward.x * 2.0, 0.25, toward.z * 2.0);
        hurtMarked = true;
        clientParticle(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 3);
    }
    /** 雷瞬击：瞬移至目标背后重击 + 麻痹。 */
    private void stormBlink(ServerLevel sl, long now, LivingEntity target, int lvl, boolean apex) {
        nextShadowTick = now + (apex ? 20 * 6 : 20 * 10);
        playSound(SoundEvents.WARDEN_NEARBY_CLOSE, 0.6F, 1.6F);
        Vec3 behind = target.position().add(target.getLookAngle().scale(-2.0));
        teleportTo(behind.x, behind.y, behind.z);
        clientParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 4);
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            int back = skillInjury((14 + lvl * 4) * (apex ? 2 : 1));
            SixtySecondsHealthSystem.applyInjury(p, null, back);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 10, 2));
        }
    }
    /** 雷暴弹幕：远程雷弹。 */
    private void stormBarrage(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextBarrageTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.LLAMA_SPIT, 1.6F, 0.5F);
        int shots = 4 + lvl / 2;
        for (int i = 0; i < shots; i++) {
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            double dx = target.getX() - getX() + (sl.random.nextDouble() - 0.5) * 4.0;
            double dy = target.getY(0.4) - spit.getY();
            double dz = target.getZ() - getZ() + (sl.random.nextDouble() - 0.5) * 4.0;
            double h = Math.sqrt(dx * dx + dz * dz);
            spit.shoot(dx, dy + h * 0.14, dz, 1.0F, 6.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY(), getZ(), 3);
    }
    /** 雷霆咆哮：范围虚弱。 */
    private void stormRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.6F);
        clientParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 0));
        }
    }
    /** 落雷震地：范围伤害 + 击退。 */
    private void stormQuake(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.2, getZ(), 4);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 0.7F);
        int injury = skillInjury(SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 5.0 * 5.0) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 0.8, 0.5, away.z * 0.8);
            p.hurtMarked = true;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  虚空织者 VOIDWEAVER 独立招式
    // ══════════════════════════════════════════════════════════════════
    /** 生命汲取：连线 + 自愈。 */
    private void voidDrain(ServerLevel sl, long now, LivingEntity target, int lvl) {
        nextDrainTick = now + 20 * 16;
        playSound(SoundEvents.WARDEN_HEARTBEAT, 0.5F, 0.3F);
        Vec3 from = new Vec3(getX(), getEyeY(), getZ());
        Vec3 to = new Vec3(target.getX(), target.getEyeY(), target.getZ());
        Vec3 step = to.subtract(from).normalize().scale(0.5);
        Vec3 pos = from;
        for (int i = 0; i < 12; i++) {
            pos = pos.add(step.scale(2.5));
            clientParticle(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 1);
        }
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            int drain = skillInjury(8 + lvl * 2);
            SixtySecondsHealthSystem.applyInjury(p, null, drain);
            heal(drain * 0.8F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
        }
    }
    /** 暗影突袭：瞬移重击。 */
    private void voidBlink(ServerLevel sl, long now, LivingEntity target, int lvl, boolean apex) {
        nextShadowTick = now + (apex ? 20 * 6 : 20 * 10);
        playSound(SoundEvents.WARDEN_NEARBY_CLOSE, 0.6F, 1.6F);
        Vec3 behind = target.position().add(target.getLookAngle().scale(-2.0));
        teleportTo(behind.x, behind.y, behind.z);
        clientParticle(ParticleTypes.PORTAL, getX(), getY() + 1.0, getZ(), 4);
        if (target instanceof ServerPlayer p && isValidPrey(p)) {
            int back = skillInjury((14 + lvl * 4) * (apex ? 2 : 1));
            SixtySecondsHealthSystem.applyInjury(p, null, back);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 + 10, 0));
        }
    }
    /** 虚空新星：环形暗影弹。 */
    private void voidNova(ServerLevel sl, long now, int lvl) {
        nextNovaTick = now + 20 * 22;
        playSound(SoundEvents.WITHER_BREAK_BLOCK, 0.7F, 1.2F);
        int shots = 12 + lvl;
        for (int i = 0; i < shots; i++) {
            double angle = (2 * Math.PI / shots) * i;
            SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(sl, this);
            spit.shoot(Math.cos(angle), 0.1, Math.sin(angle), 0.7F, 8.0F);
            sl.addFreshEntity(spit);
        }
        clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.5, getZ(), 3);
    }
    /** 虚空咆哮：范围恐惧。 */
    private void voidRoar(ServerLevel sl, long now, int lvl) {
        nextRoarTick = now + SixtySecondsBalance.BOSS_ROAR_COOLDOWN_TICKS;
        playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
        clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getEyeY(), getZ(), 2);
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 12 * 12) continue;
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 1));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 0));
        }
    }
    /** 虚空碾压：近身范围。 */
    private void voidCrush(ServerLevel sl, long now, int lvl) {
        nextSlamTick = now + SixtySecondsBalance.BOSS_SLAM_COOLDOWN_TICKS;
        swing(InteractionHand.MAIN_HAND, true);
        clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.2, getZ(), 4);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.7F);
        int injury = skillInjury(SixtySecondsBalance.BOSS_SLAM_INJURY + 4 * (lvl - 1));
        for (ServerPlayer p : sl.players()) {
            if (!isValidPrey(p) || distanceToSqr(p) > 6.0 * 6.0) continue;
            SixtySecondsHealthSystem.applyInjury(p, null, injury);
            Vec3 away = p.position().subtract(position()).normalize();
            p.setDeltaMovement(away.x * 0.9, 0.6, away.z * 0.9);
            p.hurtMarked = true;
        }
    }

    /** 狂怒被动（巨像）：低血量时激活，增加伤害和速度。 */
    private void tickFrenzy() {
        boolean shouldFrenzy = getHealth() < getMaxHealth() * 0.35;
        if (shouldFrenzy && !frenzied) {
            frenzied = true;
            playSound(SoundEvents.RAVAGER_ROAR, 1.8F, 0.5F);
            if (level() instanceof ServerLevel sl) {
                clientParticle(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY(), getZ(), 4);
            }
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
        }
        if (!shouldFrenzy && frenzied && getHealth() > getMaxHealth() * 0.5) {
            frenzied = false;
            removeEffect(MobEffects.DAMAGE_BOOST);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  死亡 / 存档
    // ══════════════════════════════════════════════════════════════════
    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (level() instanceof ServerLevel serverLevel) {
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.onBossDied(serverLevel, this, damageSource);
        }
        bossEvent.removeAllPlayers();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SreBossLevel", bossLevel());
        tag.putBoolean("SreBossApex", isApex());
        tag.putInt("SreBossVariant", getBossVariant().id);
        tag.putInt("SreBossHome", getHomeIslandId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SreBossLevel")) {
            BossVariant variant = tag.contains("SreBossVariant")
                    ? BossVariant.byId(tag.getInt("SreBossVariant")) : BossVariant.RAVAGER;
            applyBossLevel(tag.getInt("SreBossLevel"), tag.getBoolean("SreBossApex"), variant);
        }
        if (tag.contains("SreBossHome")) {
            setHomeIslandId(tag.getInt("SreBossHome"));
        }
    }
}
