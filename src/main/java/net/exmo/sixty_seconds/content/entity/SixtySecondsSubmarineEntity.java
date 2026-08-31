package net.exmo.sixty_seconds.content.entity;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.exmo.sixty_seconds.content.entity.SixtySecondsVehicleEntity;
import net.exmo.sixty_seconds.registry.ModItems;

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
    private static final EntityDataAccessor<Integer> DATA_FUEL = SynchedEntityData.defineId(
            SixtySecondsSubmarineEntity.class, EntityDataSerializers.INT);

    /** 当前俯仰角（弧度，负值抬头、正值低头）。 */
    private float pitch = 0.0F;
    private static final float MAX_PITCH = 0.6F;
    private static final double SPEED = 0.32D;
    private static final double LIFT = 0.09D;
    private static final float TURN_RATE = 3.0F;
    /** 满油可存柴油罐数（与其它载具一致）。 */
    private static final int BASE_FUEL_CANS = 4;

    /**
     * 服务端持有的上浮 / 下潜输入态。
     *
     * <p>{@code SynchedEntityData} 是服务端权威：客户端 set 不会同步到服务端，
     * 因此按键必须经 {@link net.exmo.sixty_seconds.network.SubmarineControlC2SPacket}
     * 送到服务端并写入这里，服务端 {@link #tick()} 才能据此驱动升降。
     */
    private boolean serverAscend = false;
    private boolean serverDescend = false;

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
        builder.define(DATA_FUEL, 0);
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

    /**
     * 由服务端网络处理器调用，写入驾驶员的上浮 / 下潜意图。
     * 与 {@link #setAscending} 的区别：后者只是客户端本地数据，服务端读不到。
     */
    public void setServerInput(boolean ascend, boolean descend) {
        this.serverAscend = ascend;
        this.serverDescend = descend;
    }

    /** 当前剩余燃料（tick）。 */
    public int fuelTicks() {
        return this.entityData.get(DATA_FUEL);
    }

    /** 设置燃料并夹在 [0, maxFuelTicks()] 内。 */
    public void setFuelTicks(int ticks) {
        this.entityData.set(DATA_FUEL, Math.max(0, Math.min(maxFuelTicks(), ticks)));
    }

    /** 满油容量：BASE_FUEL_CANS 个柴油罐的量（与其它载具一致）。 */
    public int maxFuelTicks() {
        return BASE_FUEL_CANS * SixtySecondsVehicleEntity.FUEL_PER_CAN_TICKS;
    }

    /**
     * 不允许飞出水面：当潜水艇已贴近水面（艇体底部到达水柱水面）时，
     * 抬住位置到水面、并取消继续向上的速度，使其无法探出水面（仍可用前进/转向在水面航行）。
     */
    private void clampToWater() {
        if (this.level().isClientSide) return; // 仅服务端权威处理位移
        Level level = this.level();
        BlockPos c = this.blockPosition();
        int cx = c.getX();
        int cz = c.getZ();
        int baseY = c.getY();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int topWaterY = -1;
        for (int dy = -4; dy <= 4; dy++) {
            int y = baseY + dy;
            if (y < minY || y >= maxY) continue;
            BlockPos bp = new BlockPos(cx, y, cz);
            if (level.getFluidState(bp).is(FluidTags.WATER)
                    && !level.getFluidState(bp.above()).is(FluidTags.WATER)) {
                topWaterY = y;
                break;
            }
        }
        if (topWaterY < 0) return; // 不在水中，无需限制
        BlockPos top = new BlockPos(cx, topWaterY, cz);
        double surfaceY = topWaterY + level.getFluidState(top).getHeight(level, top) - 1.0D;
        if (surfaceY <= this.getY()) return; // 艇底仍在水面之下，无需限制
        // 艇底已到达/超过水面：贴住水面，并消去向上速度
        this.setPos(this.getX(), surfaceY, this.getZ());
        Vec3 v = this.getDeltaMovement();
        if (v.y > 0.0D) {
            this.setDeltaMovement(v.x, 0.0D, v.z);
        }
    }

    @Override
    public void tick() {
        // 先跑原版 LivingEntity/Mob 的 tick（其中 travel() 已被我们接管并短路，
        // 不会再次施加重力/阻力/骑乘输入），随后由本方法手动 setDeltaMovement 并 move()。
        super.tick();
        Entity rider = this.getFirstPassenger();
        if (rider instanceof Player) {
            float forward = ((Player) rider).zza;   // W = +1 前进, S = -1 后退（沿艇艏方向）
            float steer = ((Player) rider).xxa;      // A = +1, D = -1
            // 服务端：读网络包写入的输入态（客户端 set 的 entityData 服务端读不到）
            // 客户端：读同步下来的俯仰角，保持渲染姿态一致
            boolean ascend = this.level().isClientSide
                    ? this.entityData.get(DATA_ASCEND) : this.serverAscend;
            boolean descend = this.level().isClientSide
                    ? this.entityData.get(DATA_DESCEND) : this.serverDescend;

            float target = 0.0F;
            if (ascend) target -= 1.0F;
            if (descend) target += 1.0F;
            this.pitch += (target * MAX_PITCH - this.pitch) * 0.15F;
            this.pitch = Mth.clamp(this.pitch, -MAX_PITCH, MAX_PITCH);
            // 仅服务端写 DATA_PITCH：它会同步到所有客户端（含其他旁观玩家），
            // 客户端自身不写，避免与服务端权威值打架。
            if (!this.level().isClientSide) {
                this.entityData.set(DATA_PITCH, this.pitch);
            }
            this.setXRot(this.pitch * (180.0F / (float) Math.PI));
            this.setYHeadRot(this.getYRot());

            this.setYRot(this.getYRot() - steer * TURN_RATE);

            // 无燃料：无法驱动，保持姿态不动（仅刚上艇时提示一次）。
            if (this.fuelTicks() > 0) {
                Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
                // 渲染以 (180 - yaw) 摆放模型，dir 实际指向艇艉，故水平分量取反得到艇艏前进方向。
                double vx = -dir.x * forward * SPEED;
                double vz = -dir.z * forward * SPEED;
                // 竖直速度：升降按键独立生效（不依赖是否按 W），
                // 再叠加俯仰角带来的斜向分量（按 W 前进时即斜向上/下航行）。
                double vy = dir.y * forward * SPEED;
                if (ascend) vy += LIFT;
                if (descend) vy -= LIFT;
                this.setDeltaMovement(vx, vy, vz);
                // 由我们自己驱动位移，原版 travel() 不再插手
                this.move(MoverType.SELF, this.getDeltaMovement());
                // 服务端按输入消耗燃料
                if (!this.level().isClientSide
                        && (forward != 0.0F || steer != 0.0F || ascend || descend)) {
                    setFuelTicks(this.fuelTicks() - 1);
                    if (this.fuelTicks() == 0) {
                        ((Player) rider).displayClientMessage(Component.translatable(
                                "message.sixty_seconds.sixty_seconds.vehicle_no_fuel")
                                .withStyle(ChatFormatting.RED), true);
                    }
                }
            }
            // 阻止 Mob 自带 travel 再次施力：清空其输入
            this.zza = 0.0F;
            this.xxa = 0.0F;
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 0.85, 0.85));
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.pitch *= 0.95F;
            this.entityData.set(DATA_PITCH, this.pitch);
            this.zza = 0.0F;
            this.xxa = 0.0F;
        }
        // 不允许飞出水面：贴近水面时取消继续上浮
        this.clampToWater();
        // 载具自身不会溺水；乘员在水下也不会溺水
        this.setAirSupply(this.getMaxAirSupply());
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player playerRider) {
                playerRider.setAirSupply(playerRider.getMaxAirSupply());
            }
        }
    }

    /**
     * 接管移动：完全接管位移逻辑，不再调用原版 {@link Mob#travel}，
     * 避免其重新施加重力 / 阻力 / 骑乘输入而覆盖掉 {@link #tick()} 中 setDeltaMovement 的竖直速度。
     * 实际位移由 {@link #tick()} 内的 {@code move(MoverType.SELF, ...)} 完成。
     */
    @Override
    public void travel(Vec3 travelVector) {
        // 与 SixtySecondsFlyingVehicleEntity 一致：不调用 super.travel
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        // 骑乘时由 tick() 自己驱动位移，这里清空原版施加的速度
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return Vec3.ZERO;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return 0.0F;
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
        // 手持柴油罐/燃油罐右键加油（与其它载具一致）
        if (!this.level().isClientSide) {
            ItemStack held = player.getItemInHand(hand);
            int per;
            boolean diesel = held.is(ModItems.SIXTY_SECONDS_DIESEL_CAN);
            boolean petrol = held.is(ModItems.SIXTY_SECONDS_FUEL_CAN);
            if (diesel) per = SixtySecondsVehicleEntity.FUEL_PER_CAN_TICKS;
            else if (petrol) per = SixtySecondsVehicleEntity.FUEL_PER_CAN_TICKS / 2;
            else per = 0;
            if (per > 0 && this.fuelTicks() < this.maxFuelTicks()) {
                this.setFuelTicks(this.fuelTicks() + per);
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                int cans = Math.round((float) this.fuelTicks()
                        / (float) SixtySecondsVehicleEntity.FUEL_PER_CAN_TICKS);
                player.displayClientMessage(Component.translatable(
                                "message.sixty_seconds.sixty_seconds.vehicle_fuel_added", cans)
                        .withStyle(ChatFormatting.GREEN), true);
                return InteractionResult.CONSUME;
            }
        }
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
        this.setFuelTicks(tag.getInt("fuel"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("pitch", this.pitch);
        tag.putInt("fuel", this.fuelTicks());
    }
}
