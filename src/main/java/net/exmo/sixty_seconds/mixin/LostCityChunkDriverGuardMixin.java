package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.ChunkDriver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.LongAdder;

/**
 * ChunkDriver 两项防护：
 *
 * <p><b>1. 空方块状态守卫。</b>
 * {@code correct(BlockState)} 在异常路径下可能收到 null，直接解引用会打断整个区块的生成。
 * 这里在方法入口拦截：null 直接返回 null，让后续写入退化为“什么都不做”而不是崩溃。</p>
 *
 * <p><b>2. 陈旧方块实体清理。</b>
 * ChunkDriver 把 SectionCache 里的方块直接批量写进区块 Section，绕过了
 * {@code WorldGenRegion#setBlock} 的正常路径。结构生成挂到区块上的方块实体 NBT
 * 在最终方块落定为空气（或普通方块）后会残留在区块里；服务器线程随后反序列化这些
 * 孤儿数据时会产生成百上千条 “Failed to create block entity” 警告，
 * 并伴随一次明显的区块提升（promotion）卡顿。
 * <p>
 * 在 {@code actuallyGenerate} 返回时做一次有界清扫：
 * {@code getBlockEntitiesPos()} 返回的是防御性副本，遍历成本只与区块里的
 * 方块实体数量成正比，不会扫描普通方块位置。凡方块状态已不含方块实体的位置，
 * 直接移除其残留 NBT。任何异常都吞掉并计数——世界生成必须 fail-open，
 * 计数器让映射/运行时不兼容可见，同时不污染热路径日志。</p>
 */
@Mixin(value = ChunkDriver.class, remap = false)
public class LostCityChunkDriverGuardMixin {

    private static final LongAdder SS_POSITIONS_CHECKED = new LongAdder();
    private static final LongAdder SS_STALE_REMOVED = new LongAdder();
    private static final LongAdder SS_FAILURES = new LongAdder();

    /** 累计统计（诊断用）。Mixin 类内的辅助方法必须为 private。 */
    private static String ss$diagnostics() {
        return "checked=" + SS_POSITIONS_CHECKED.sum()
            + ", removed=" + SS_STALE_REMOVED.sum()
            + ", failures=" + SS_FAILURES.sum();
    }

    @Inject(method = "correct", at = @At("HEAD"), cancellable = true, require = 0)
    private void ss$nullGuard(BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (state == null) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "actuallyGenerate", at = @At("RETURN"), require = 0)
    private void ss$removeStalePendingBlockEntities(ChunkAccess chunk, CallbackInfo ci) {
        if (chunk == null) {
            return;
        }
        try {
            for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                SS_POSITIONS_CHECKED.increment();
                if (!chunk.getBlockState(pos).hasBlockEntity()) {
                    chunk.removeBlockEntity(pos);
                    SS_STALE_REMOVED.increment();
                }
            }
        } catch (Throwable ignored) {
            SS_FAILURES.increment();
        }
    }
}
