package net.exmo.sixty_seconds.entity;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.exmo.sixty_seconds.entity.OceanCreatureEntity.OceanMeleeAttackGoal;
import net.exmo.sixty_seconds.entity.OceanCreatureEntity.OceanSwimGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * 海洋生物群系（第二批次，10 种独立建模生物）。
 *
 * <p>与 {@link OceanFloorMonsterEntity}（海底六小怪）平行的一套独立族群，各自拥有：
 * 专属盒体几何（见 {@code OceanFaunaModel} 的同名 PartGroup）、专属贴图、专属数值与
 * 专属命中效果（毒/盲/缓慢/击退/污染等）。
 *
 * <p>刷新与回收策略沿用海底小怪：贴近玩家生成、远离 30 秒自散、生成上限由
 * {@code OceanCreatureSpawner} 控制。
 */
public class OceanFaunaEntity extends OceanCreatureEntity {

    public enum Variant {
        /** 蝠鲼：滑翔巡游，中速中血，撞击附带击退。 */
        MANTA_RAY(0, 45.0, 0.34, 7, 2.4F, "ocean_manta_ray"),
        /** 水母：漂浮慢速，刺细胞附带中毒。 */
        JELLYFISH(1, 24.0, 0.14, 6, 1.6F, "ocean_jellyfish"),
        /** 巨型乌贼：高血中速，墨汁致盲。 */
        GIANT_SQUID(2, 80.0, 0.26, 12, 3.0F, "ocean_giant_squid"),
        /** 河豚：慢速，膨胀反伤 + 剧毒。 */
        PUFFERFISH(3, 30.0, 0.18, 8, 1.4F, "ocean_pufferfish"),
        /** 海星：极慢高血，断肢再生（持续回血）。 */
        STARFISH(4, 40.0, 0.10, 5, 1.5F, "ocean_starfish"),
        /** 海马：极小极快，血薄伤害低。 */
        SEAHORSE(5, 16.0, 0.30, 4, 0.9F, "ocean_seahorse"),
        /** 狮子鱼：中速，毒棘造成长时间中毒。 */
        LIONFISH(6, 28.0, 0.24, 9, 1.5F, "ocean_lionfish"),
        /** 铁甲蟹：极高血坦克，移速极慢，破门好手。 */
        IRON_CRAB(7, 110.0, 0.14, 15, 2.2F, "ocean_iron_crab"),
        /** 鹦鹉螺：装甲壳体，命中强击退。 */
        NAUTILUS(8, 60.0, 0.16, 10, 2.0F, "ocean_nautilus"),
        /** 梭鱼：最快，突袭型高伤低血。 */
        BARRACUDA(9, 50.0, 0.42, 13, 2.0F, "ocean_barracuda");

        public final int id;
        public final double health;
        public final double speed;
        /** 近战命中玩家扣的健康值。 */
        public final int injury;
        public final float scale;
        public final String textureName;

        Variant(int id, double health, double speed, int injury, float scale, String textureName) {
            this.id = id;
            this.health = health;
            this.speed = speed;
            this.injury = injury;
            this.scale = scale;
            this.textureName = textureName;
        }

        public static Variant byId(int id) {
            for (Variant v : values()) if (v.id == id) return v;
            return MANTA_RAY;
        }

        public String nameKey() {
            return "entity.sixty_seconds." + textureName;
        }
    }

    private int lonelyTicks = 0;

    public OceanFaunaEntity(EntityType<? extends OceanCreatureEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new OceanSwimGoal(this, 0.6, 30));
        this.goalSelector.addGoal(1, new OceanMeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, ServerPlayer.class,
                10, true, true, p -> isValidOceanPrey((ServerPlayer) p)));
    }

    /** 生成时按变体装配。 */
    public void applyVariant(Variant variant) {
        applyVariant(variant.id, variant.health, variant.speed, variant.scale, variant.nameKey());
        setCustomNameVisible(false);
        // 避免非 60s 模式下被 OceanCreatureEntity 的失活清理删掉
        setPersistenceRequired();
    }

    public Variant getVariant() {
        return Variant.byId(getVariantId());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        if (spawnType == MobSpawnType.NATURAL && getVariant() == Variant.MANTA_RAY) {
            int id = ((ServerLevel) level).getRandom().nextInt(Variant.values().length);
            applyVariant(Variant.byId(id));
        }
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    public ResourceLocation textureLocation() {
        return ResourceLocation.fromNamespaceAndPath("sixty_seconds",
                "textures/entity/" + getVariant().textureName + ".png");
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(level() instanceof ServerLevel serverLevel)) return super.doHurtTarget(target);
        if (!(target instanceof ServerPlayer player) || !isValidOceanPrey(player)) {
            setTarget(null);
            return false;
        }
        player.invulnerableTime = 10;
        int injury = net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.scaleInjury(this, getVariant().injury);
        switch (getVariant()) {
            case JELLYFISH -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, 0));
            case LIONFISH -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 8, 1));
            case PUFFERFISH -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 6, 1));
            case GIANT_SQUID -> player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 5, 0));
            case STARFISH -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, 0));
            case SEAHORSE -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 2, 0));
            case MANTA_RAY, NAUTILUS, IRON_CRAB -> {
                // 强击退：把玩家推开
                net.minecraft.world.phys.Vec3 away = player.position().subtract(position()).normalize();
                double power = getVariant() == Variant.NAUTILUS ? 1.2 : 0.8;
                player.setDeltaMovement(away.x * power, 0.45, away.z * power);
                player.hurtMarked = true;
            }
            default -> { }
        }
        // 所有海洋生物攻击附带少量污染
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        stats.pollution = Math.min(100, stats.pollution
                + net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.scalePollutionGain(serverLevel, 3));
        stats.sync();
        SixtySecondsHealthSystem.applyInjury(player, null, injury);
        playSound(net.minecraft.sounds.SoundEvents.COD_HURT, 0.4F, 0.5F);
        return true;
    }

    /**
     * 海星断肢再生：水中持续回血（比基类 baseTick 的自然恢复更快）。
     * 远离玩家过久（>30s）自散，防止海底生物越攒越多。
     */
    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;
        if (tickCount % 20 == 0) {
            // 海星专属：再生
            if (getVariant() == Variant.STARFISH && isInWater() && getHealth() < getMaxHealth()) {
                heal(2.0F);
            }
            Player nearest = serverLevel.getNearestPlayer(this, 48);
            lonelyTicks = nearest == null ? lonelyTicks + 20 : 0;
            if (lonelyTicks >= 600) {
                discard();
            }
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.COD_HURT;
    }
}
