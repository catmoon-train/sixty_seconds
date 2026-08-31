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
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;

/**
 * 海洋霸主（10 个<b>独立建模</b>的 Boss，不使用僵尸人形）。
 *
 * <p>与 {@link SixtySecondsBossEntity}（复用僵尸人形 + 特征层）不同，本族每个 Boss 在
 * {@code OceanTitanModel} 中拥有各自完全独立的盒体几何（鲸、电鳗、藤壶巨怪、
 * 鮟鱇、巨型螃蟹、幽灵水母、深渊之喉、珊瑚巨偶、沉船怨灵、海皇三叉戟）。
 *
 * <p>每个 Boss 拥有<b>完全独立的招式集</b>：由 {@code castVariantMove} 按变体分发，
 * 在 {@link #aiStep()} 中按冷却轮流释放（冲撞 / 雷击 / 酸雨 / 诱捕灯 /
 * 漩涡 / 毒刺 / 吞噬 / 尖啸 / 召唤 / 三叉戟等）。
 *
 * <p>生成方式与既有 Boss 一致：通过 {@code SixtySecondsPveSystem.spawnBoss}
 * 风格的指令/自然刷新入口调用 {@link #spawn}，并自带血条 Boss 事件与全服公告。
 */
public class OceanTitanEntity extends OceanCreatureEntity {

    /**
     * 海洋霸主招式设计原则：每个变体拥有<b>完全独立</b>的招式集合（不共享通用池）。
     * 招式在 {@link #castVariantMove(ServerLevel, Variant, LivingEntity)} 中按变体分发，
     * 每个变体循环释放自己的 3 个专属招式（见各 {@code tickXxx} 方法）。
     */

    public enum Variant {
        /** 深渊巨鲸：高血冲撞型，撞击附带强击退与溅水浪。 */
        ABYSS_WHALE(   0, 420.0, 0.26, 26, 3.2F, "ocean_titan_abyss_whale"),
        /** 雷暴电鳗：连锁闪电 + 麻痹。 */
        TEMPEST_EEL(   1, 340.0, 0.32, 22, 2.6F, "ocean_titan_tempest_eel"),
        /** 藤壶巨怪：甲壳猛击自强化 + 召唤小怪 + 酸雨。 */
        BARNACLE_TITAN(2, 460.0, 0.16, 24, 3.0F, "ocean_titan_barnacle_titan"),
        /** 深海鮟鱇：诱捕灯塔 + 深渊吞噬 + 磷光致盲弹。 */
        ANGLER_LORD(   3, 380.0, 0.22, 28, 2.8F, "ocean_titan_angler_lord"),
        /** 碎壳巨蟹：钳击连斩 + 甲壳冲锋 + 碎壳重压。 */
        CARAPACE_KING( 4, 500.0, 0.18, 30, 3.0F, "ocean_titan_carapace_king"),
        /** 幽灵水母后：毒刺齐射 + 幽光致盲 + 灵魂尖啸。 */
        GHOST_MEDUSA(  5, 330.0, 0.20, 20, 2.6F, "ocean_titan_ghost_medusa"),
        /** 深渊之喉：吞噬回血 + 深渊漩涡 + 喉部喷涌。 */
        ABYSS_MAW(     6, 440.0, 0.24, 32, 3.4F, "ocean_titan_abyss_maw"),
        /** 珊瑚巨偶：极高血，珊瑚酸雨 + 尖啸恐惧 + 棘刺爆发。 */
        CORAL_COLOSSUS(7, 520.0, 0.14, 26, 3.2F, "ocean_titan_coral_colossus"),
        /** 沉船怨灵：怨灵尖啸 + 召唤亡魂 + 鬼火诱捕。 */
        WRECK_WRAITH(  8, 360.0, 0.28, 24, 2.8F, "ocean_titan_wreck_wraith"),
        /** 海皇三叉戟：远程审判 + 海皇之怒弹幕 + 潮汐审判。 */
        TRIDENT_SOVEREIGN(9, 400.0, 0.24, 28, 3.0F, "ocean_titan_trident_sovereign");

        public final int id;
        public final double health;
        public final double speed;
        public final int injury;
        public final float scale;
        public final String textureName;

        Variant(int id, double health, double speed, int injury, float scale,
                String textureName) {
            this.id = id;
            this.health = health;
            this.speed = speed;
            this.injury = injury;
            this.scale = scale;
            this.textureName = textureName;
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

    // 寿命 / 远离消失（参考普通 Boss 寿命 与 普通海洋生物远离消失）
    private int titanSpawnDay = -1;
    private int awayTicks = 0;
    private static final int TITAN_DESPAWN_RADIUS = 128;   // 玩家远离超过此距离(格)持续过久则潜回深海

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

        // ── titan 寿命 / 远离自动消失（参考普通 Boss 寿命 与 普通海洋生物远离消失）──
        if (SixtySecondsMod.isActive(serverLevel)) {
            if (titanSpawnDay < 0) {
                titanSpawnDay = SixtySecondsState.get(serverLevel).dayNumber;
            } else if (SixtySecondsState.get(serverLevel).dayNumber - titanSpawnDay
                    >= SixtySecondsBalance.TITAN_MAX_LIFETIME_DAYS) {
                retreatToDeep(serverLevel);
                return;
            }
        }
        // 玩家远离一定距离持续过久 → 潜回深海（参考普通海洋生物远离消失；无玩家时不消失）
        double dSqr = nearestPlayerDistanceSqr();
        if (dSqr < Double.MAX_VALUE && dSqr > (double) TITAN_DESPAWN_RADIUS * TITAN_DESPAWN_RADIUS) {
            if (++awayTicks > 600) { retreatToDeep(serverLevel); return; }
        } else {
            awayTicks = 0;
        }

        Variant v = getVariant();

        // 冲撞状态推进（各变体的冲撞招式共用此帧推进，命中时按变体结算）
        if (ramTicks > 0) {
            ramTicks--;
            if (tickCount % 3 == 0) {
                clientParticle(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 1);
            }
            LivingEntity t = getTarget();
            if (t != null) {
                Vec3 dir = t.position().subtract(position()).normalize();
                setDeltaMovement(dir.x * 0.55, getDeltaMovement().y * 0.5 + 0.02, dir.z * 0.55);
                if (distanceToSqr(t) < 9.0) {
                    variantRamImpact(t);
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

        // 每变体独立招式集：按 0/1/2 槽循环释放自己的 3 个专属招式
        castVariantMove(serverLevel, v, target, moveIndex % 3);
        moveIndex++;
        moveCooldown = 100 + random.nextInt(60);
    }

    // ══════════════════════ 寿命 / 远离退场 ══════════════════════

    /** 最近存活玩家到本体的距离平方（旁观者不计）。 */
    private double nearestPlayerDistanceSqr() {
        double best = Double.MAX_VALUE;
        for (Player p : level().players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            best = Math.min(best, distanceToSqr(p));
        }
        return best;
    }

    /** titan 寿命到期 / 远离过久：潜回深海自动消失（参考 OceanSeaMonsterEntity 的退场）。 */
    private void retreatToDeep(ServerLevel sl) {
        Component msg = Component.translatable("message.sixty_seconds.ocean.monster_retreat",
                        getCustomName() != null ? getCustomName() : Component.literal("海洋霸主"))
                .withStyle(ChatFormatting.AQUA);
        sl.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY(), getZ(), 10);
        playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.0F, 0.5F);
        bossEvent.removeAllPlayers();
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (titanSpawnDay >= 0) tag.putInt("TitanSpawnDay", titanSpawnDay);
        tag.putInt("TitanMoveIndex", moveIndex);
        tag.putInt("TitanMoveCooldown", moveCooldown);
        // 注：键名保留 Titan* 前缀以兼容旧存档
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        titanSpawnDay = tag.contains("TitanSpawnDay") ? tag.getInt("TitanSpawnDay") : -1;
        moveIndex = tag.getInt("TitanMoveIndex");
        moveCooldown = tag.getInt("TitanMoveCooldown");
    }

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

    /** 按变体分发：释放该变体独立招式集中的第 slot(0/1/2) 招。 */
    private void castVariantMove(ServerLevel level, Variant v, LivingEntity target, int slot) {
        switch (v) {
            case ABYSS_WHALE     -> whaleMove(level, target, slot);
            case TEMPEST_EEL     -> eelMove(level, target, slot);
            case BARNACLE_TITAN  -> barnacleMove(level, target, slot);
            case ANGLER_LORD     -> anglerMove(level, target, slot);
            case CARAPACE_KING   -> crabMove(level, target, slot);
            case GHOST_MEDUSA    -> medusaMove(level, target, slot);
            case ABYSS_MAW       -> mawMove(level, target, slot);
            case CORAL_COLOSSUS  -> coralMove(level, target, slot);
            case WRECK_WRAITH    -> wraithMove(level, target, slot);
            case TRIDENT_SOVEREIGN -> tridentMove(level, target, slot);
        }
    }

    /** 冲撞命中结算：各变体的冲撞招式在此定义独特效果。 */
    private void variantRamImpact(LivingEntity t) {
        Vec3 away = t.position().subtract(position()).normalize();
        switch (getVariant()) {
            case ABYSS_WHALE -> {           // 巨尾拍击式俯冲：强击退 + 溅水浪
                t.hurt(damageSources().mobAttack(this), lvlDmg(16.0F));
                t.setDeltaMovement(away.x * 2.0, 0.9, away.z * 2.0);
                t.hurtMarked = true;
                clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, t.getX(), t.getY(), t.getZ(), 6);
                playSound(SoundEvents.DOLPHIN_SPLASH, 1.2F, 0.6F);
            }
            case TEMPEST_EEL -> {          // 电浆冲撞：伤害 + 麻痹
                t.hurt(damageSources().mobAttack(this), lvlDmg(12.0F));
                if (t instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, 1));
                clientParticle(ParticleTypes.SCULK_SOUL, t.getX(), t.getY() + 1.0, t.getZ(), 4);
                playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 1.0F);
            }
            case CARAPACE_KING -> {        // 甲壳冲锋：高额伤害 + 轻微击退
                t.hurt(damageSources().mobAttack(this), lvlDmg(20.0F));
                t.setDeltaMovement(away.x * 1.2, 0.5, away.z * 1.2);
                t.hurtMarked = true;
                clientParticle(ParticleTypes.CRIMSON_SPORE, t.getX(), t.getY() + 1.0, t.getZ(), 4);
                playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
            }
            default -> {
                t.hurt(damageSources().mobAttack(this), lvlDmg(12.0F));
                t.setDeltaMovement(away.x * 1.4, 0.7, away.z * 1.4);
                t.hurtMarked = true;
            }
        }
    }

    // ════════════════ 深渊巨鲸（高血冲撞型） ════════════════
    private void whaleMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 巨尾拍击：近身环形击退 + 伤害
            clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 0.5, getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 7)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(10.0F));
                Vec3 away = p.position().subtract(position()).normalize();
                p.setDeltaMovement(away.x * 1.6, 0.8, away.z * 1.6);
                p.hurtMarked = true;
            }
            playSound(SoundEvents.DOLPHIN_SPLASH, 1.4F, 0.5F);
        } else if (slot == 1) {            // 潮汐喷涌：朝目标直线冲击（逐格伤害 + 击退）
            Vec3 dir = target.position().subtract(position()).normalize();
            clientParticle(ParticleTypes.SPLASH, getX(), getEyeY(), getZ(), 4);
            for (int step = 1; step <= 14; step += 2) {
                BlockPos at = blockPosition().offset((int)(dir.x*step*1.5), 0, (int)(dir.z*step*1.5));
                clientParticle(ParticleTypes.BUBBLE, at.getX()+0.5, getY()+1.0, at.getZ()+0.5, 1);
                for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                        null, new AABB(at).inflate(1.5))) {
                    pl.hurt(damageSources().mobAttack(this), lvlDmg(7.0F));
                    Vec3 away = pl.position().subtract(position()).normalize();
                    pl.setDeltaMovement(away.x * 0.8, 0.3, away.z * 0.8);
                    pl.hurtMarked = true;
                }
            }
            playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.0F, 0.6F);
        } else {                            // 深渊俯冲：突进（走 ramTicks 框架）
            ramTicks = 32;
            playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.4F, 0.5F);
            clientParticle(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 4);
        }
    }

    // ════════════════ 雷暴电鳗（连锁闪电 + 麻痹） ════════════════
    private void eelMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 连锁闪电：跳 3 个目标，每跳伤害 + 缓慢
            java.util.List<ServerPlayer> near = radiusPlayers(level, 14);
            if (!near.isEmpty()) {
                ServerPlayer from = near.get(0);
                for (int jump = 0; jump < 3 && !near.isEmpty(); jump++) {
                    ServerPlayer next = nearestIn(from.position(), near);
                    clientParticle(ParticleTypes.SCULK_SOUL, from.getX(), from.getY()+1.0, from.getZ(), 3);
                    next.hurt(damageSources().mobAttack(this), lvlDmg(9.0F));
                    next.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*3, 1));
                    from = next;
                    near.remove(next);
                }
            }
            playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 0.8F);
        } else if (slot == 1) {            // 雷暴云：自身范围放电 + 缓慢
            clientParticle(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 12)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(7.0F));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*3, 1));
                clientParticle(ParticleTypes.SCULK_SOUL, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.9F, 1.0F);
        } else {                            // 电浆冲撞：突进（走 ramTicks 框架）
            ramTicks = 30;
            playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 0.7F);
            clientParticle(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0, getZ(), 4);
        }
    }

    private ServerPlayer nearestIn(Vec3 from, java.util.List<ServerPlayer> list) {
        ServerPlayer best = null; double bd = Double.MAX_VALUE;
        for (ServerPlayer p : list) {
            double d = p.position().distanceToSqr(from);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    // ════════════════ 藤壶巨怪（甲壳猛击 + 召唤 + 酸雨） ════════════════
    private void barnacleMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 甲壳猛击：近身高伤 + 自身减伤自强化
            clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 4);
            for (ServerPlayer p : radiusPlayers(level, 6)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(13.0F));
                clientParticle(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20*5, 1));
            playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        } else if (slot == 1) {            // 藤壶召唤：召唤小怪助战
            for (int i = 0; i < 3; i++) {
                net.exmo.sixty_seconds.logic.OceanCreatureSpawner.spawnOceanFauna(
                        level, blockPosition().offset(random.nextInt(9)-4, 1, random.nextInt(9)-4), random);
            }
            clientParticle(ParticleTypes.PORTAL, getX(), getY()+0.5, getZ(), 8);
            clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY()+0.5, getZ(), 4);
            playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
        } else {                            // 藤壶酸雨：目标上空持续伤害区
            BlockPos c = target.blockPosition();
            clientParticle(ParticleTypes.ITEM_SLIME, c.getX()+0.5, c.getY()+6.0, c.getZ()+0.5, 6);
            for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                    net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                    null, new AABB(c).inflate(8))) {
                pl.addEffect(new MobEffectInstance(MobEffects.POISON, 20*6, 1));
                pl.hurt(damageSources().mobAttack(this), lvlDmg(5.0F));
            }
            playSound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.6F);
        }
    }

    // ════════════════ 深海鮟鱇（诱捕 + 吞噬 + 磷光弹） ════════════════
    private void anglerMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 诱捕灯塔：致盲 + 拉向自己
            clientParticle(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 16)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20*5, 0));
                pull(p, 0.35);
                clientParticle(ParticleTypes.END_ROD, p.getX(), p.getY()+1.0, p.getZ(), 3);
            }
            playSound(SoundEvents.END_PORTAL_FRAME_FILL, 1.0F, 0.7F);
        } else if (slot == 1) {            // 深渊吞噬：近身高额伤害并回血
            if (distanceToSqr(target) < 16.0) {
                target.hurt(damageSources().mobAttack(this), lvlDmg(18.0F));
                heal(20.0F);
                clientParticle(ParticleTypes.CRIMSON_SPORE, target.getX(), target.getY()+1.0, target.getZ(), 5);
            }
            clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 4);
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.6F);
        } else {                            // 磷光致盲弹：朝目标发射致盲冲击波（范围致盲）
            Vec3 dir = target.position().subtract(position()).normalize();
            clientParticle(ParticleTypes.END_ROD, getX(), getEyeY(), getZ(), 4);
            for (int step = 1; step <= 10; step += 2) {
                BlockPos at = blockPosition().offset((int)(dir.x*step*2), 0, (int)(dir.z*step*2));
                clientParticle(ParticleTypes.END_ROD, at.getX()+0.5, getY()+1.0, at.getZ()+0.5, 1);
                for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                        null, new AABB(at).inflate(1.8))) {
                    pl.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20*3, 0));
                }
            }
            playSound(SoundEvents.END_PORTAL_FRAME_FILL, 0.8F, 1.0F);
        }
    }

    // ════════════════ 碎壳巨蟹（钳击 + 冲锋 + 重压） ════════════════
    private void crabMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 钳击连斩：近身两连击
            clientParticle(ParticleTypes.CRIMSON_SPORE, getX(), getEyeY(), getZ(), 4);
            for (ServerPlayer p : radiusPlayers(level, 5)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(9.0F));
                p.hurt(damageSources().mobAttack(this), lvlDmg(7.0F));
                clientParticle(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        } else if (slot == 1) {            // 甲壳冲锋：突进（走 ramTicks 框架）
            ramTicks = 30;
            playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 0.5F);
            clientParticle(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 4);
        } else {                            // 碎壳重压：范围压制 + 缓慢
            clientParticle(ParticleTypes.CRIMSON_SPORE, getX(), getY(), getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 8)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(11.0F));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*4, 0));
                Vec3 away = p.position().subtract(position()).normalize();
                p.setDeltaMovement(away.x*0.6, 0.2, away.z*0.6);
                p.hurtMarked = true;
                clientParticle(ParticleTypes.CRIMSON_SPORE, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.ANVIL_LAND, 1.2F, 0.5F);
        }
    }

    // ════════════════ 幽灵水母后（毒刺 + 致盲 + 尖啸） ════════════════
    private void medusaMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 毒刺齐射：范围伤害 + 中毒
            clientParticle(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 14)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(7.0F));
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 20*4, 0));
                clientParticle(ParticleTypes.ITEM_SLIME, p.getX(), p.getY()+1.0, p.getZ(), 3);
            }
            playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 0.9F);
        } else if (slot == 1) {            // 幽光致盲：范围致盲
            clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 16)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20*4, 0));
                clientParticle(ParticleTypes.SONIC_BOOM, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 0.6F);
        } else {                            // 灵魂尖啸：虚弱 + 缓慢（恐惧）
            clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 18)) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20*6, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*3, 0));
                clientParticle(ParticleTypes.SONIC_BOOM, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
        }
    }

    // ════════════════ 深渊之喉（吞噬 + 漩涡 + 喷涌） ════════════════
    private void mawMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 吞噬回血：近身高额 + 回血
            if (distanceToSqr(target) < 16.0) {
                target.hurt(damageSources().mobAttack(this), lvlDmg(18.0F));
                heal(20.0F);
                clientParticle(ParticleTypes.CRIMSON_SPORE, target.getX(), target.getY()+1.0, target.getZ(), 5);
            }
            clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 4);
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 0.6F);
        } else if (slot == 1) {            // 深渊漩涡：强吸引
            clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY()+1.0, getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 18)) {
                pull(p, 0.75);
                clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, p.getX(), p.getY(), p.getZ(), 2);
            }
            playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 1.2F, 0.6F);
        } else {                            // 喉部喷涌：朝目标直线伤害（毒 + 击退）
            Vec3 dir = target.position().subtract(position()).normalize();
            clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 4);
            for (int step = 1; step <= 12; step += 2) {
                BlockPos at = blockPosition().offset((int)(dir.x*step*1.5), 0, (int)(dir.z*step*1.5));
                clientParticle(ParticleTypes.ITEM_SLIME, at.getX()+0.5, getY()+1.0, at.getZ()+0.5, 1);
                for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                        null, new AABB(at).inflate(1.5))) {
                    pl.hurt(damageSources().mobAttack(this), lvlDmg(8.0F));
                    pl.addEffect(new MobEffectInstance(MobEffects.POISON, 20*3, 0));
                }
            }
            playSound(SoundEvents.SQUID_SQUIRT, 0.8F, 1.0F);
        }
    }

    // ════════════════ 珊瑚巨偶（酸雨 + 尖啸 + 棘刺） ════════════════
    private void coralMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 珊瑚酸雨：目标上空持续伤害区
            BlockPos c = target.blockPosition();
            clientParticle(ParticleTypes.ITEM_SLIME, c.getX()+0.5, c.getY()+6.0, c.getZ()+0.5, 6);
            for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                    net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                    null, new AABB(c).inflate(8))) {
                pl.addEffect(new MobEffectInstance(MobEffects.POISON, 20*6, 1));
                pl.hurt(damageSources().mobAttack(this), lvlDmg(5.0F));
            }
            playSound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.6F);
        } else if (slot == 1) {            // 尖啸恐惧：范围虚弱 + 缓慢
            clientParticle(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 18)) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20*6, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*3, 0));
                clientParticle(ParticleTypes.SONIC_BOOM, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
        } else {                            // 棘刺爆发：范围毒刺 + 中毒
            clientParticle(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 6);
            for (ServerPlayer p : radiusPlayers(level, 10)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(8.0F));
                p.addEffect(new MobEffectInstance(MobEffects.POISON, 20*4, 0));
                clientParticle(ParticleTypes.ITEM_SLIME, p.getX(), p.getY()+1.0, p.getZ(), 3);
            }
            playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 0.9F);
        }
    }

    // ════════════════ 沉船怨灵（尖啸 + 召唤 + 鬼火） ════════════════
    private void wraithMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 怨灵尖啸：范围虚弱 + 恐惧
            clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 18)) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20*6, 1));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20*3, 0));
                clientParticle(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 0.5F);
        } else if (slot == 1) {            // 召唤亡魂：召唤小怪助战
            for (int i = 0; i < 3; i++) {
                net.exmo.sixty_seconds.logic.OceanCreatureSpawner.spawnOceanFauna(
                        level, blockPosition().offset(random.nextInt(9)-4, 1, random.nextInt(9)-4), random);
            }
            clientParticle(ParticleTypes.PORTAL, getX(), getY()+0.5, getZ(), 8);
            clientParticle(ParticleTypes.ENCHANTED_HIT, getX(), getY()+0.5, getZ(), 4);
            playSound(SoundEvents.SLIME_SQUISH, 1.0F, 0.6F);
        } else {                            // 鬼火诱捕：致盲 + 拉向自己
            clientParticle(ParticleTypes.REVERSE_PORTAL, getX(), getEyeY(), getZ(), 5);
            for (ServerPlayer p : radiusPlayers(level, 16)) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20*5, 0));
                pull(p, 0.4);
                clientParticle(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.END_PORTAL_FRAME_FILL, 1.0F, 0.7F);
        }
    }

    // ════════════════ 海皇三叉戟（远程审判 + 弹幕 + 潮汐） ════════════════
    private void tridentMove(ServerLevel level, LivingEntity target, int slot) {
        if (slot == 0) {                   // 三叉戟审判：直线穿透伤害
            Vec3 dir = target.position().subtract(position()).normalize();
            clientParticle(ParticleTypes.SPLASH, getX(), getEyeY(), getZ(), 4);
            for (int step = 1; step <= 12; step += 2) {
                BlockPos at = blockPosition().offset((int)(dir.x*step*1.5), 0, (int)(dir.z*step*1.5));
                clientParticle(ParticleTypes.BUBBLE, at.getX()+0.5, getY()+1.0, at.getZ()+0.5, 1);
                for (net.minecraft.world.entity.player.Player pl : level.getNearbyPlayers(
                        net.minecraft.world.entity.ai.targeting.TargetingConditions.DEFAULT,
                        null, new AABB(at).inflate(1.5))) {
                    pl.hurt(damageSources().mobAttack(this), lvlDmg(12.0F));
                }
            }
            playSound(SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.7F);
        } else if (slot == 1) {            // 海皇之怒：远程酸液弹幕
            for (int i = 0; i < 3; i++) {
                Vec3 vel = target.position().subtract(position()).normalize()
                        .scale(1.1).add(new Vec3((random.nextDouble()-0.5)*0.3, 0.1, (random.nextDouble()-0.5)*0.3));
                SixtySecondsAcidSpitEntity spit = new SixtySecondsAcidSpitEntity(level, this);
                spit.moveTo(getX(), getEyeY(), getZ());
                spit.shoot(vel.x, vel.y, vel.z, 1.1F, 0.0F);
                level.addFreshEntity(spit);
            }
            clientParticle(ParticleTypes.ITEM_SLIME, getX(), getEyeY(), getZ(), 4);
            playSound(SoundEvents.SPLASH_POTION_THROW, 1.0F, 0.8F);
        } else {                            // 潮汐审判：范围 + 击退
            clientParticle(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY()+0.5, getZ(), 8);
            for (ServerPlayer p : radiusPlayers(level, 10)) {
                p.hurt(damageSources().mobAttack(this), lvlDmg(10.0F));
                Vec3 away = p.position().subtract(position()).normalize();
                p.setDeltaMovement(away.x * 1.5, 0.7, away.z * 1.5);
                p.hurtMarked = true;
                clientParticle(ParticleTypes.BUBBLE, p.getX(), p.getY()+1.0, p.getZ(), 2);
            }
            playSound(SoundEvents.DOLPHIN_SPLASH, 1.4F, 0.5F);
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
