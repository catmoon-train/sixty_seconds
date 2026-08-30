package net.exmo.sixty_seconds.entity;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.exmo.sixty_seconds.entity.OceanCreatureEntity.OceanMeleeAttackGoal;
import net.exmo.sixty_seconds.entity.OceanCreatureEntity.OceanSwimGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 海底小型怪物：贴近海床刷新、不溺死、有对玩家攻击，远离玩家后自动消失（刷新上限由 OceanCreatureSpawner 控制）。
 * 与海洋 Boss 不同，它们是普通刷怪、数量较多、单人威胁低，但成群时危险。
 */
public class OceanFloorMonsterEntity extends OceanCreatureEntity {

    public enum Variant {
        /** 寄居蟹：慢速近战，外壳坚硬。 */
        HERMIT_CRAB(0, 30.0, 0.22, 8, 1.6F, "ocean_hermit_crab"),
        /** 海胆刺客：血薄，近战附带中毒。 */
        URCHIN(1, 22.0, 0.18, 6, 1.2F, "ocean_urchin"),
        /** 潜伏电鳗：极快、血薄，近战附带虚弱。 */
        EEL(2, 26.0, 0.40, 9, 2.2F, "ocean_eel"),
        /** 沉船怨灵：中速，近战附带污染。 */
        WRAITH(3, 40.0, 0.24, 10, 2.4F, "ocean_wraith"),
        /** 深渊掠食者：大体型高伤，破门好手。 */
        LURKER(4, 60.0, 0.20, 14, 3.0F, "ocean_lurker"),
        /** 珊瑚守卫：极高血、移速极慢，守在遗迹附近。 */
        GUARDIAN(5, 90.0, 0.16, 16, 3.5F, "ocean_guardian");

        public final int id;
        public final double health;
        public final double speed;
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
            return HERMIT_CRAB;
        }

        public String nameKey() {
            return "entity.sixty_seconds." + textureName;
        }
    }

    private int lonelyTicks = 0;

    public OceanFloorMonsterEntity(EntityType<? extends OceanCreatureEntity> entityType, Level level) {
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
        setPersistenceRequired(); // 避免非 60s 模式下被 OceanCreatureEntity 的失活清理删掉
    }

    public Variant getVariant() {
        return Variant.byId(getVariantId());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        if (spawnType == MobSpawnType.NATURAL && getVariant() == Variant.HERMIT_CRAB) {
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
            case URCHIN -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 3, 0));
            case EEL -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 3, 0));
            case WRAITH -> {
                SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
                stats.pollution = Math.min(100, stats.pollution
                        + net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.scalePollutionGain(serverLevel, 4));
                stats.sync();
            }
            default -> { }
        }
        SixtySecondsHealthSystem.applyInjury(player, null, injury);
        playSound(net.minecraft.sounds.SoundEvents.COD_HURT, 0.4F, 0.5F);
        return true;
    }

    /** 远离玩家过久（>30s）自散，防止海底怪越攒越多。 */
    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) return;
        if (tickCount % 20 == 0) {
            Player nearest = serverLevel.getNearestPlayer(this, 48);
            lonelyTicks = nearest == null ? lonelyTicks + 20 : 0;
            if (lonelyTicks >= 600) {
                discard();
            }
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return net.minecraft.sounds.SoundEvents.COD_HURT;
    }
}
