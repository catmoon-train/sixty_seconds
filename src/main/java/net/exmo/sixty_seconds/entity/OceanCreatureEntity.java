package net.exmo.sixty_seconds.entity;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * 海洋生物基类：鲨鱼/海怪的共同骨架（水系寻路、水下呼吸、陆地窒息、变体同步）。
 * 继承 {@link PathfinderMob}，用自定义 {@link OceanMoveControl} 管理水中移动。
 *
 * <h3>设计决策</h3>
 * <ul>
 *   <li>不继承原版水生生物（如 {@code WaterAnimal}）：需要主动攻击玩家的战斗 AI；
 *       原版水生生物走 {@code Brain} 系统过于笨重</li>
 *   <li>碰撞箱固定基础尺寸，实际体型由 {@link Attributes#SCALE} 驱动（与 60s Boss 一致）</li>
 *   <li>60s 模式外不自毁：指令召唤的生物不依赖模式存活</li>
 * </ul>
 */
public abstract class OceanCreatureEntity extends PathfinderMob {

    public static final String OCEAN_TAG = "sixty_seconds_ocean_creature";

    private static final EntityDataAccessor<Integer> VARIANT_ID =
            SynchedEntityData.defineId(OceanCreatureEntity.class, EntityDataSerializers.INT);

    /** 陆地窒息计时：离开水面后逐 tick 递增，达上限开始伤害（给短暂搁浅挣扎窗口）。 */
    protected int outOfWaterTicks = 0;
    /** 变体的正常游速（{@link #applyVariant} 记下）：搁浅时移速被压到 {@link #LAND_SPEED}，回水后据此还原。 */
    protected double swimSpeed = 0.3;
    protected static final int MAX_OUT_OF_WATER = 200; // 10秒
    protected static final int BREATH_DAMAGE_INTERVAL = 20; // 每秒扣一次
    /** 搁浅时的爬行移速。 */
    protected static final double LAND_SPEED = 0.04;

    protected OceanCreatureEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new OceanMoveControl(this);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
        this.setPathfindingMalus(PathType.WALKABLE, 8.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT_ID, 0);
    }

    /** 变体装配：生命/移速/体型/名字（子类调完数据后调此方法完成初始装配）。 */
    protected void applyVariant(int variantId, double health, double speed, float scale, String nameKey) {
        this.entityData.set(VARIANT_ID, variantId);
        addTag(OCEAN_TAG);
        this.swimSpeed = speed;
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(health);
        var moveSpeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) moveSpeed.setBaseValue(speed);
        var scaleAttr = getAttribute(Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(scale);
        setHealth((float) health);
        setCustomName(net.minecraft.network.chat.Component.translatable(nameKey));
        setCustomNameVisible(false);
        // 难度：变体装配会重设 base 属性，故以新 base 为基准重新施加难度缩放
        net.exmo.sixty_seconds.logic.SixtySecondsDifficulty.reapply(this);
    }

    public int getVariantId() {
        return this.entityData.get(VARIANT_ID);
    }

    // ── 水下生物特性 ────────────────────────────────────────────────
    // canBreatheUnderwater() 在本版本（1.21.1）是 LivingEntity 的 <b>final</b> 方法，返回
    // getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER)——即由实体类型标签驱动，不能 @Override。
    // 「不淹死」的<b>主修法</b>是数据标签 data/minecraft/tags/entity_type/can_breathe_under_water.json
    // 收录 ocean_shark / ocean_sea_monster（原代码只留了这条注释却没建标签，也没做任何兜底 →
    // 鲨鱼/海怪在水里照样每秒扣气、气尽淹死，本次修的 BUG）。
    // 下面 decreaseAirSupply 覆写是<b>代码兜底</b>：即便数据包被覆盖/未加载，空气也永不下降，双保险。

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /**
     * 空气永不下降 = 永不淹死。{@code LivingEntity.baseTick} 只有在气量减到 -20 时才结算溺水伤害，
     * 这里让减气恒等于「不变」，气量卡在满值，溺水判定永远走不到。
     * <p>陆地窒息是本类在 {@link #tick} 里<b>显式</b>调 {@code hurt(drown, ...)} 造成的，不经这条路径，
     * 覆写它不影响搁浅惩罚。
     */
    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    /** 自然恢复只在水里触发。 */
    @Override
    public void baseTick() {
        super.baseTick();
        if (isInWater() && getHealth() < getMaxHealth() && tickCount % 20 == 0) {
            heal(1.0F);
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isEffectiveAi() && isInWater()) {
            moveRelative(getSpeed(), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        return level.isUnobstructed(this);
    }

    // ── 陆地惩罚 ─────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel)) return;

        // 陆地窒息
        if (!isInWater() && !isInWaterOrBubble()) {
            outOfWaterTicks++;
            if (outOfWaterTicks > MAX_OUT_OF_WATER && outOfWaterTicks % BREATH_DAMAGE_INTERVAL == 0) {
                hurt(damageSources().drown(), 2.0F);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.BUBBLE, getX(), getEyeY(), getZ(),
                            2, 0.3, 0.2, 0.3, 0);
                }
            }
            // 陆地极慢移速（爬行挣扎）
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getBaseValue() > LAND_SPEED) {
                speed.setBaseValue(LAND_SPEED);
            }
        } else {
            outOfWaterTicks = 0;
            // 回到水里：把被搁浅压低的移速还原成本变体的游速
            // （原代码只在出水时压低、回水时从不恢复，搁浅一次就永久变慢——本次一并修）
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getBaseValue() < swimSpeed) {
                speed.setBaseValue(swimSpeed);
            }
        }

        // 模式关闭时非持久生物自毁；管理员召唤实体豁免（只会被打死）
        if (!SixtySecondsMod.isActive((ServerLevel) level()) && !isPersistenceRequired()
                && !this.getTags().contains(net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity.ADMIN_SPAWN_TAG)) {
            discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 在水里受远程攻击时减少伤害（水阻）
        if (isInWater() && !source.isDirect() && source.getEntity() != null) {
            amount *= 0.8F;
        }
        return super.hurt(source, amount);
    }

    // ── 可攻击目标过滤 ──────────────────────────────────────────
    public static boolean isValidOceanPrey(ServerPlayer player) {
        return SixtySecondsMonsterEntity.isValidPrey(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OceanVariant", getVariantId());
        tag.putDouble("OceanSwimSpeed", swimSpeed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT_ID, tag.getInt("OceanVariant"));
        // 还原游速目标：缺字段（旧档）时退回当前移速 base，至少不会把它错误还原成默认 0.3
        if (tag.contains("OceanSwimSpeed")) {
            swimSpeed = tag.getDouble("OceanSwimSpeed");
        } else {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                swimSpeed = Math.max(swimSpeed, speed.getBaseValue());
            }
        }
    }

    // 普通海洋生物（鲨鱼等）允许原版「远离玩家后自然消失」：
    // requiresCustomPersistence 返回 false、removeWhenFarAway 返回 true。
    // 海洋 Boss（OceanSeaMonsterEntity）会覆写为永存，改由寿命机制统一回收。
    @Override
    public boolean requiresCustomPersistence() {
        return false;
    }

    /** 海洋生物（fauna/titan/shark/sea_monster/floor_monster）一律不显示实体悬浮名称牌。 */
    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    // ── 水系移动控制 ──────────────────────────────────────────────

    /** 水中移动控制器：模仿原版海豚/鱿鱼的游泳行为，同时支持攻击路径。 */
    protected static class OceanMoveControl extends MoveControl {
        private final OceanCreatureEntity creature;

        public OceanMoveControl(OceanCreatureEntity creature) {
            super(creature);
            this.creature = creature;
        }

        @Override
        public void tick() {
            if (!creature.isInWater()) {
                super.tick();
                return;
            }
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                double dx = this.wantedX - creature.getX();
                double dy = this.wantedY - creature.getY();
                double dz = this.wantedZ - creature.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 0.5) {
                    creature.setSpeed(0.0F);
                    return;
                }
                dy /= dist;
                float speed = (float) (creature.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.8);
                creature.setSpeed(Mth.lerp(0.125F, creature.getSpeed(), speed));
                creature.setDeltaMovement(creature.getDeltaMovement().add(
                        (dx / dist) * 0.03 * creature.getSpeed(),
                        dy * 0.03 * creature.getSpeed(),
                        (dz / dist) * 0.03 * creature.getSpeed()));
                // 朝向
                float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                creature.setYRot(rotlerp(creature.getYRot(), yaw, 10.0F));
            } else if (this.operation == MoveControl.Operation.WAIT) {
                creature.setSpeed(0.0F);
            }
        }
    }

    // ── 水中漫游目标 ──────────────────────────────────────────────

    /** 在水中随机游荡（无目标时）。 */
    protected static class OceanSwimGoal extends Goal {
        private final OceanCreatureEntity creature;
        private final double speed;
        private final int interval;

        public OceanSwimGoal(OceanCreatureEntity creature, double speed, int interval) {
            this.creature = creature;
            this.speed = speed;
            this.interval = interval;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return creature.isInWater() && creature.getTarget() == null
                    && creature.getRandom().nextInt(interval) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return creature.isInWater() && creature.getTarget() == null
                    && !creature.getNavigation().isDone();
        }

        @Override
        public void start() {
            BlockPos center = creature.blockPosition();
            for (int attempt = 0; attempt < 10; attempt++) {
                BlockPos target = center.offset(
                        creature.getRandom().nextInt(16) - 8,
                        creature.getRandom().nextInt(8) - 4,
                        creature.getRandom().nextInt(16) - 8);
                if (creature.level().getFluidState(target).is(FluidTags.WATER)) {
                    creature.getMoveControl().setWantedPosition(
                            target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, speed);
                    return;
                }
            }
        }
    }

    /**
     * 水中近战追击：直接用 {@link OceanMoveControl} 朝目标游去，<b>不做 A* 寻路</b>。
     * 原版 MeleeAttackGoal 内部用 getNavigation().moveTo 做水下路径规划，水生物数量一多、
     * 同时锁敌时会在同一 tick 内集体重算路径，造成 CPU 尖峰甚至触发服务器看门狗。
     * 海洋是开放水域，直线游动足够，去掉寻路即可消除该尖峰。
     */
    protected static class OceanMeleeAttackGoal extends Goal {
        private final PathfinderMob mob;
        private final double speed;
        private final boolean pauseWhenMobIdle;
        private int attackTimer = 0;

        public OceanMeleeAttackGoal(PathfinderMob mob, double speed, boolean pauseWhenMobIdle) {
            this.mob = mob;
            this.speed = speed;
            this.pauseWhenMobIdle = pauseWhenMobIdle;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private LivingEntity target() {
            return mob.getTarget();
        }

        @Override
        public boolean canUse() {
            LivingEntity t = target();
            return t != null && t.isAlive() && mob.isInWater()
                    && (!pauseWhenMobIdle || !mob.isPassenger());
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = target();
            return t != null && t.isAlive() && mob.isInWater();
        }

        @Override
        public void start() {
            this.attackTimer = 0;
        }

        @Override
        public void stop() {
            this.attackTimer = 0;
        }

        @Override
        public void tick() {
            LivingEntity t = target();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30.0F, 30.0F);
            double reach = mob.getBbWidth() * 0.5 + t.getBbWidth() * 0.5 + 1.0;
            double d2 = mob.distanceToSqr(t);
            if (d2 <= reach * reach) {
                // 进入攻击距离：直接造成伤害（攻击间隔与原版近战一致）
                if (attackTimer <= 0) {
                    attackTimer = 20;
                    mob.doHurtTarget(t);
                }
            } else {
                // 直接游向目标位置，不寻路
                mob.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), speed);
            }
            if (attackTimer > 0) attackTimer--;
        }
    }
}
