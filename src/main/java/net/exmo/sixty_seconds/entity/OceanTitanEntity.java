package net.exmo.sixty_seconds.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.exmo.sixty_seconds.SixtySecondsBalance;

/**
 * 海洋霸主（10 个<b>独立建模</b>的 Boss，不使用僵尸人形）。
 *
 * <p>与 {@link SixtySecondsBossEntity}（复用僵尸人形 + 特征层）不同，本族每个 Boss 在
 * {@code OceanTitanModel} 中拥有各自完全独立的盒体几何（鲸、电鳗、藤壶巨怪、
 * 鮟鱇、巨型螃蟹、幽灵水母、深渊之喉、珊瑚巨偶、沉船怨灵、海皇三叉戟）。
 *
 * <p>每个 Boss 同时拥有<b>独立招式</b>：由 {@code TitanMove} 定义，
 * 在 {@link #aiStep()} 中按冷却轮流释放（冲撞 / 雷击 / 酸雨 / 诱捕灯 /
 * 漩涡 / 毒刺 / 吞噬 / 尖啸 / 召唤 / 三叉戟等）。
 *
 * <p>生成方式与既有 Boss 一致：通过 {@code SixtySecondsPveSystem.spawnBoss}
 * 风格的指令/自然刷新入口调用 {@link #spawn}，并自带血条 Boss 事件与全服公告。
 */
public class OceanTitanEntity extends OceanCreatureEntity {

    /** Boss 招式集合。 */
    public enum TitanMove {
        /** 巨兽冲撞：短暂加速冲向目标，命中击飞。 */
        RAM,
        /** 雷霆放电：以自身为中心对范围内玩家放电。 */
        DISCHARGE,
        /** 腐蚀酸雨：在目标上空降下持续伤害区域。 */
        ACID_RAIN,
        /** 诱捕灯笼：致盲并吸引周围玩家。 */
        LURE,
        /** 深渊漩涡：把附近玩家拉向自己。 */
        WHIRLPOOL,
        /** 毒棘齐射：扇形发射减速毒棘。 */
        SPINE_VOLLEY,
        /** 吞噬：对近身目标高额伤害并回复自身。 */
        DEVOUR,
        /** 灵魂尖啸：范围恐惧 + 虚弱。 */
        SHRIEK,
        /** 藤壶召唤：召唤小型海洋生物助战。 */
        SUMMON,
        /** 三叉戟审判：直线穿透伤害。 */
        TRIDENT_JUDGEMENT,
    }

    public enum Variant {
        /** 深渊巨鲸：高血冲撞型，撞击附带强击退。 */
        ABYSS_WHALE(   0, 420.0, 0.26, 26, 3.2F, "ocean_titan_abyss_whale",
                new TitanMove[]{TitanMove.RAM, TitanMove.WHIRLPOOL, TitanMove.SHRIEK}),
        /** 雷暴电鳗：放电 + 麻痹。 */
        TEMPEST_EEL(   1, 340.0, 0.32, 22, 2.6F, "ocean_titan_tempest_eel",
                new TitanMove[]{TitanMove.DISCHARGE, TitanMove.RAM, TitanMove.WHIRLPOOL}),
        /** 藤壶巨怪：召唤小怪 + 酸雨。 */
        BARNACLE_TITAN(2, 460.0, 0.16, 24, 3.0F, "ocean_titan_barnacle_titan",
                new TitanMove[]{TitanMove.ACID_RAIN, TitanMove.SUMMON, TitanMove.RAM}),
        /** 深海鮟鱇：诱捕灯笼 + 吞噬。 */
        ANGLER_LORD(   3, 380.0, 0.22, 28, 2.8F, "ocean_titan_angler_lord",
                new TitanMove[]{TitanMove.LURE, TitanMove.DEVOUR, TitanMove.WHIRLPOOL}),
        /** 碎壳巨蟹：高防高伤，钳击与冲撞。 */
        CARAPACE_KING( 4, 500.0, 0.18, 30, 3.0F, "ocean_titan_carapace_king",
                new TitanMove[]{TitanMove.RAM, TitanMove.SPINE_VOLLEY, TitanMove.DEVOUR}),
        /** 幽灵水母后：致盲 + 毒棘齐射。 */
        GHOST_MEDUSA(  5, 330.0, 0.20, 20, 2.6F, "ocean_titan_ghost_medusa",
                new TitanMove[]{TitanMove.SPINE_VOLLEY, TitanMove.LURE, TitanMove.SHRIEK}),
        /** 深渊之喉：吞噬 + 漩涡。 */
        ABYSS_MAW(     6, 440.0, 0.24, 32, 3.4F, "ocean_titan_abyss_maw",
                new TitanMove[]{TitanMove.DEVOUR, TitanMove.WHIRLPOOL, TitanMove.RAM}),
        /** 珊瑚巨偶：极高血，酸雨与尖啸。 */
        CORAL_COLOSSUS(7, 520.0, 0.14, 26, 3.2F, "ocean_titan_coral_colossus",
                new TitanMove[]{TitanMove.ACID_RAIN, TitanMove.SHRIEK, TitanMove.SPINE_VOLLEY}),
        /** 沉船怨灵：尖啸恐惧 + 召唤。 */
        WRECK_WRAITH(  8, 360.0, 0.28, 24, 2.8F, "ocean_titan_wreck_wraith",
                new TitanMove[]{TitanMove.SHRIEK, TitanMove.SUMMON, TitanMove.LURE}),
        /** 海皇三叉戟：远程审判 + 放电。 */
        TRIDENT_SOVEREIGN(9, 400.0, 0.24, 28, 3.0F, "ocean_titan_trident_sovereign",
                new TitanMove[]{TitanMove.TRIDENT_JUDGEMENT, TitanMove.DISCHARGE, TitanMove.WHIRLPOOL});

        public final int id;
        public final double health;
        public final double speed;
        public final int injury;
        public final float scale;
        public final String textureName;
        /** 该 Boss 的专属招式循环。 */
        public final TitanMove[] moves;

        Variant(int id, double health, double speed, int injury, float scale,
                String textureName, TitanMove[] moves) {
            this.id = id;
            this.health = health;
            this.speed = speed;
            this.injury = injury;
            this.scale = scale;
            this.textureName = textureName;
            this.moves = moves;
        }

        public static Variant byId(int id) {
            for (Variant v : values()) if (v.id == id) return v;
            return ABYSS_WHALE;
        }

        public String nameKey() {
            return "entity.sixty_seconds." + textureName;
        }
    }

    // 招式冷却（按招式序号分别计时）
    private int moveCooldown = 60;
    private int moveIndex = 0;
    private int ramTicks = 0;

    // 自管理 Boss 血条 + 等级（与 SixtySecondsBossEntity 一致）
    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.literal(""), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> BOSS_LEVEL =
            net.minecraft.network.syncher.SynchedEntityData.defineId(OceanTitanEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOSS_LEVEL, 1);
    }

    public int bossLevel() { return this.entityData.get(BOSS_LEVEL); }
    public void setBossLevel(int lvl) {
        this.entityData.set(BOSS_LEVEL, Mth.clamp(lvl, 1, SixtySecondsBalance.BOSS_MAX_LEVEL));
    }

    /** 伤害随 Boss 等级缩放（与 SixtySecondsBossEntity 一致）。 */
    private float lvlDmg(float base) {
        return base * (1.0F + 0.18F * (bossLevel() - 1));
    }

    public OceanTitanEntity(EntityType<? extends OceanCreatureEntity> entityType, Level level) {
        super(entityType, level);
        setPersistenceRequired();
    }

    public void applyVariant(Variant variant) {
        applyVariant(variant, 1);
    }

    public void applyVariant(Variant variant, int level) {
        setBossLevel(level);
        double hp = variant.health + SixtySecondsBalance.BOSS_HEALTH_PER_LEVEL * (bossLevel() - 1);
        applyVariant(variant.id, hp, variant.speed, variant.scale, variant.nameKey());
        setCustomNameVisible(false);
        setPersistenceRequired();
        // Boss 级：提高基础属性与击退抗性
        var attr = getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr != null) attr.setBaseValue(0.85);
        bossEvent.setName(Component.translatable(variant.nameKey())
                .append(Component.literal(" Lv." + bossLevel())));
    }

    public Variant getVariant() {
        return Variant.byId(getVariantId());
    }

    /** 按变体取专属贴图。 */
    public net.minecraft.resources.ResourceLocation textureLocation() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sixty_seconds",
                "textures/entity/" + getVariant().textureName + ".png");
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    // ══════════════════════ 招式系统 ══════════════════════

    @Override
    public void aiStep() {
        super.aiStep();
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;

        Variant v = getVariant();
        if (v.moves.length == 0) return;

        // 冲撞状态推进
        if (ramTicks > 0) {
            ramTicks--;
            LivingEntity t = getTarget();
            if (t != null) {
                Vec3 dir = t.position().subtract(position()).normalize();
                setDeltaMovement(dir.x * 0.55, getDeltaMovement().y * 0.5 + 0.02, dir.z * 0.55);
                if (distanceToSqr(t) < 9.0) {
                    doHurtTarget(t);
                    Vec3 away = t.position().subtract(position()).normalize();
                    t.setDeltaMovement(away.x * 1.4, 0.7, away.z * 1.4);
                    t.hurtMarked = true;
                    ramTicks = 0;
                }
            }
            return;
        }

        if (moveCooldown > 0) { moveCooldown--; return; }

        // 需要目标才释放大部分招式
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            moveCooldown = 20;
            return;
        }

        TitanMove move = v.moves[moveIndex % v.moves.length];
        moveIndex++;
        castMove(serverLevel, move, target);
        moveCooldown = 100 + random.nextInt(60);
    }

    /** 释放一个招式。 */
    private void castMove(ServerLevel level, TitanMove move, LivingEntity target) {
        switch (move) {
            case RAM -> {
                ramTicks = 30;
                playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.4F, 0.5F);
            }
            case DISCHARGE -> {
                // 雷霆放电：范围内玩家受伤 + 麻痹（缓慢）
                for (ServerPlayer p : radiusPlayers(level, 12)) {
                    p.hurt(damageSources().mobAttack(this), lvlDmg(8.0F));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 1));
                }
                playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 0.8F);
            }
            case ACID_RAIN -> {
                // 腐蚀酸雨：目标周围降下持续伤害区
                BlockPos c = target.blockPosition();
                for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                        null, new AABB(c).inflate(8))) {
                    pl.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 6, 1));
                    pl.hurt(damageSources().mobAttack(this), lvlDmg(5.0F));
                }
                playSound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.6F);
            }
            case LURE -> {
                // 诱捕灯笼：致盲 + 拉向自己
                for (ServerPlayer p : radiusPlayers(level, 16)) {
                    p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 5, 0));
                    pull(p, 0.35);
                }
                playSound(SoundEvents.END_PORTAL_FRAME_FILL, 1.0F, 0.7F);
            }
            case WHIRLPOOL -> {
                // 深渊漩涡：强吸引
                for (ServerPlayer p : radiusPlayers(level, 18)) {
                    pull(p, 0.75);
                }
                playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2F, 0.6F);
            }
            case SPINE_VOLLEY -> {
                // 毒棘齐射：范围伤害 + 中毒
                for (ServerPlayer p : radiusPlayers(level, 14)) {
                    p.hurt(damageSources().mobAttack(this), lvlDmg(7.0F));
                    p.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, 0));
                }
                playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 0.9F);
            }
            case DEVOUR -> {
                // 吞噬：近身高额伤害并回血
                if (distanceToSqr(target) < 16.0) {
                    target.hurt(damageSources().mobAttack(this), lvlDmg(18.0F));
                    heal(20.0F);
                }
                playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.6F);
            }
            case SHRIEK -> {
                // 灵魂尖啸：虚弱 + 恐惧（缓慢）
                for (ServerPlayer p : radiusPlayers(level, 18)) {
                    p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 1));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 0));
                }
                playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
            }
            case SUMMON -> {
                // 藤壶召唤：召唤若干海洋生物助战
                for (int i = 0; i < 3; i++) {
                    net.exmo.sixty_seconds.logic.OceanCreatureSpawner.spawnOceanFauna(
                            level, blockPosition().offset(random.nextInt(9) - 4, 1, random.nextInt(9) - 4),
                            random);
                }
                playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
            }
            case TRIDENT_JUDGEMENT -> {
                // 三叉戟审判：直线穿透伤害
                Vec3 dir = target.position().subtract(position()).normalize();
                for (int step = 1; step <= 12; step++) {
                    BlockPos at = blockPosition().offset(
                            (int) (dir.x * step * 1.5), 0, (int) (dir.z * step * 1.5));
                    for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                            net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                            null, new AABB(at).inflate(1.5))) {
                        pl.hurt(damageSources().mobAttack(this), lvlDmg(12.0F));
                    }
                }
                playSound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.7F);
            }
        }
    }

    /** 把玩家拉向自己。 */
    private void pull(ServerPlayer player, double power) {
        Vec3 toward = position().subtract(player.position()).normalize();
        player.setDeltaMovement(toward.x * power, toward.y * power * 0.4, toward.z * power);
        player.hurtMarked = true;
    }

    /** 取范围内的<b>服务器玩家</b>（{@code getNearbyPlayers} 返回 Player，需过滤转型）。 */
    private java.util.List<ServerPlayer> radiusPlayers(ServerLevel level, double radius) {
        java.util.List<ServerPlayer> out = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.player.Player p : level.getNearbyPlayers(
                net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                this, getBoundingBox().inflate(radius))) {
            if (p instanceof ServerPlayer sp) out.add(sp);
        }
        return out;
    }

    // ══════════════════════ 其它 ══════════════════════

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!(level() instanceof ServerLevel serverLevel) || !(target instanceof ServerPlayer player)) {
            return super.doHurtTarget(target);
        }
        int injury = (int) (net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.scaleInjury(this, getVariant().injury)
                * (1.0 + 0.18 * (bossLevel() - 1)));
        net.exmo.sixty_seconds.component.SixtySecondsStatsComponent stats =
                net.exmo.sixty_seconds.component.SixtySecondsStatsComponent.KEY.get(player);
        stats.pollution = Math.min(100, stats.pollution
                + net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.scalePollutionGain(serverLevel, 8));
        stats.sync();
        net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem.applyInjury(player, null, injury);
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("TitanMoveIndex", moveIndex);
        tag.putInt("TitanMoveCooldown", moveCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        moveIndex = tag.getInt("TitanMoveIndex");
        moveCooldown = tag.getInt("TitanMoveCooldown");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.bossEvent.removeAllPlayers();
        if (level() instanceof ServerLevel sl) {
            net.exmo.sixty_seconds.logic.SixtySecondsPveSystem.onOceanTitanDied(sl, this, this.bossLevel(), source);
        }
    }
}
