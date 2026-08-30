package net.exmo.sixty_seconds.content.entity;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 潜水艇载具。继承 {@link Mob}（与飞行载具同源），实现水下三维可控移动。
 * <p>
 * 控制（玩家骑乘时）：
 * <ul>
 *   <li>W 前进 / S 后退（沿当前朝向，含俯仰角）</li>
 *   <li>A 向左转 / D 向右转</li>
 *   <li>空格：抬头并上浮</li>
 *   <li>左 Ctrl：低头并下潜</li>
 * </ul>
 * 上浮/下潜意图由客户端按键写入 {@link #DATA_ASCEND}/{@link #DATA_DESCEND}，服务端据此驱动。
 */
public class SixtySecondsSubmarineEntity extends Mob {

    private static final EntityDataAccessor<Boolean> DATA_ASCEND = SynchedEntityData.defineId(
            SixtySecondsSubmarineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DESCEND = SynchedEntityData.defineId(
            SixtySecondsSubmarineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(
            SixtySecondsSubmarineEntity.class, EntityDataSerializers.FLOAT);

    /** 当前俯仰角（弧度，负值抬头、正值低头）。 */
    private float pitch = 0.0F;
    private static final float MAX_PITCH = 0.6F;
    private static final double SPEED = 0.32D;
    private static final double LIFT = 0.09D;
    private static final float TURN_RATE = 3.0F;

    public SixtySecondsSubmarineEntity(EntityType<? extends SixtySecondsSubmarineEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ASCEND, false);
        builder.define(DATA_DESCEND, false);
        builder.define(DATA_PITCH, 0.0F);
    }

    public boolean isAscending() {
        return this.entityData.get(DATA_ASCEND);
    }

    public void setAscending(boolean value) {
        this.entityData.set(DATA_ASCEND, value);
    }

    public boolean isDescending() {
        return this.entityData.get(DATA_DESCEND);
    }

    public void setDescending(boolean value) {
        this.entityData.set(DATA_DESCEND, value);
    }

    /** 渲染用俯仰角（弧度）。 */
    public float getPitchAngle() {
        return this.entityData.get(DATA_PITCH);
    }

    @Override
    public void tick() {
        Entity rider = this.getFirstPassenger();
        if (rider instanceof Player) {
            float forward = ((Player) rider).zza;   // W = +1 前进, S = -1 后退（沿艇艏方向）
            float steer = ((Player) rider).xxa;      // A = +1, D = -1
            boolean ascend = this.entityData.get(DATA_ASCEND);
            boolean descend = this.entityData.get(DATA_DESCEND);

            float target = 0.0F;
            if (ascend) target -= 1.0F;
            if (descend) target += 1.0F;
            this.pitch += (target * MAX_PITCH - this.pitch) * 0.15F;
            this.pitch = Mth.clamp(this.pitch, -MAX_PITCH, MAX_PITCH);
            this.entityData.set(DATA_PITCH, this.pitch);
            this.setXRot(this.pitch * (180.0F / (float) Math.PI));
            this.setYHeadRot(this.getYRot());

            this.setYRot(this.getYRot() - steer * TURN_RATE);

            Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            // 渲染以 (180 - yaw) 摆放模型，dir 实际指向艇艉，故水平分量取反得到艇艏前进方向；
            // 竖直分量保持 dir.y，使俯仰与上下潜一致（按空格/左 Ctrl 前进即斜向上/下）。
            double vx = -dir.x * forward * SPEED;
            double vz = -dir.z * forward * SPEED;
            double vy = dir.y * forward * SPEED;
            if (ascend) vy += LIFT;
            if (descend) vy -= LIFT;
            this.setDeltaMovement(vx, vy, vz);
            // 阻止 Mob 自带 travel 再次施力：清空其输入
            this.zza = 0.0F;
            this.xxa = 0.0F;
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 0.85, 0.85));
            this.pitch *= 0.95F;
            this.entityData.set(DATA_PITCH, this.pitch);
            this.zza = 0.0F;
            this.xxa = 0.0F;
        }
        // 载具自身不会溺水；乘员在水下也不会溺水
        this.setAirSupply(this.getMaxAirSupply());
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player playerRider) {
                playerRider.setAirSupply(playerRider.getMaxAirSupply());
            }
        }
        super.tick();
    }

    @Override
    public void positionRider(Entity rider, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(rider)) {
            int index = this.getPassengers().indexOf(rider);
            // 0 号乘客（驾驶员）在前方 +1 格，1 号乘客在其后一格（中心）
            double along = (1 - index) * 1.0D;
            Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(-1.0D);
            double x = this.getX() + fwd.x * along;
            double z = this.getZ() + fwd.z * along;
            double y = this.getY() + this.getPassengersRidingOffset();
            moveFunction.accept(rider, x, y, z);
        }
    }

    public double getPassengersRidingOffset() {
        return 1.5D;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.getPassengers().size() < 2) {
            player.startRiding(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isAffectedByFluids() {
        // 关闭水浮力/水流推动，使潜水艇能真正潜入水下而不浮到水面
        return false;
    }

    @Override
    public boolean canAddPassenger(Entity entity) {
        return this.getPassengers().size() < 2;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.pitch = tag.getFloat("pitch");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("pitch", this.pitch);
    }
}
