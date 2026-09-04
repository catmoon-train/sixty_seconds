package net.exmo.sixty_seconds.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 无效悬挂实体的静默清理。
 * <p>
 * 废墟结构里生成的画/物品框常悬在已被替换成空气或被改建过的墙面上。
 * 原版 {@code tick} 会检测到 {@code survives()} 为 false 并丢弃实体，
 * 但在此之前还会打出一条 “Hanging entity at invalid position” 错误日志；
 * 在废墟密集的世界里这类日志会大量刷屏。
 * <p>
 * 这里在 tick 开头提前做同样的存活检查：服务端判定无法存活时直接
 * {@code discard()} 并取消原 tick——丢弃行为与原版一致，只是跳过了那条噪音日志。
 * {@code survives()} 自身抛异常时按“状态未知”处理，放行原版逻辑兜底。
 */
@Mixin(HangingEntity.class)
public abstract class SixtySecondsHangingEntityFixMixin extends Entity {

    protected SixtySecondsHangingEntityFixMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0, expect = 0)
    private void ss$skipInvalidHangingEntityLog(CallbackInfo ci) {
        handleInvalidHangingEntity(ci);
    }

    private void handleInvalidHangingEntity(CallbackInfo ci) {
        Level level = this.level();
        boolean survives;
        try {
            survives = ((HangingEntity) (Object) this).survives();
        } catch (Throwable ignored) {
            return;
        }
        if (level != null && !level.isClientSide && !survives) {
            this.discard();
            ci.cancel();
        }
    }
}
