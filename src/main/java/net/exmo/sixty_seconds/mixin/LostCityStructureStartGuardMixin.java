package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.IDimensionInfo;
import net.exmo.sixty_seconds.lostcities.CityStructureProtection;
import net.exmo.sixty_seconds.lostcities.LostCitiesConcurrency;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 阻止原版结构把方块写进 Lost Cities 的城市区域。
 *
 * <p>钩子选在 {@code StructureStart#placeInChunk} 而不是结构发现阶段：
 * <ul>
 *   <li>发现阶段（{@code ChunkGenerator#createStructures}）保持原样，不干预，
 *       因此 {@code /locate}、结构引用、战利品表等元数据都照常存在；</li>
 *   <li>落地方阶段才做判定并取消写入——结构"被发现了"但不会在城市上生成任何方块，
 *       城市建筑因此不会被村庄、神殿、要塞之类覆盖。</li>
 * </ul>
 *
 * <p>判定基于结构的<b>完整包围盒</b>（{@code StructureStart#getBoundingBox}），
 * 而不是当前区块：大结构跨数十个区块，只看当前区块会漏掉绝大部分重叠区域。
 * 判定结果按 {@code StructureStart} 实例缓存——同一个结构会被多个区块重复调用
 * {@code placeInChunk}，用 {@code WeakHashMap} 可以让缓存随结构回收而释放。
 *
 * <p>判定未完备（信息不足）时不缓存，下次重新判定；判定过程抛异常时一律放行。
 */
@Mixin(StructureStart.class)
public class LostCityStructureStartGuardMixin {

    /** 弱键缓存：随 StructureStart 实例生命周期自动释放。 */
    private static final Map<StructureStart, CityStructureProtection.StructureDecision> DECISIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(method = "placeInChunk", at = @At("HEAD"), cancellable = true)
    private void sixtySeconds$rejectStructuresOverCity(WorldGenLevel level,
                                                       StructureManager structureManager,
                                                       ChunkGenerator generator,
                                                       RandomSource random,
                                                       BoundingBox box,
                                                       ChunkPos chunkPos,
                                                       CallbackInfo ci) {
        if (!CityStructureProtection.isEnabled() || level == null) {
            return;
        }

        IDimensionInfo dimensionInfo = LostCitiesConcurrency.dimensionInfo(level);
        if (dimensionInfo == null) {
            return; // 非 Lost Cities 维度
        }

        StructureStart start = (StructureStart) (Object) this;

        CityStructureProtection.StructureDecision decision;
        synchronized (DECISIONS) {
            decision = DECISIONS.get(start);
        }

        if (decision == null || !decision.complete()) {
            // 判定可能触发 Lost Cities 的冷计算，刻意放在锁外，避免阻塞其它工作线程
            decision = CityStructureProtection.inspectStructureBox(
                    dimensionInfo,
                    start.getBoundingBox(),
                    CityStructureProtection.bufferChunks());
            if (decision.complete()) {
                synchronized (DECISIONS) {
                    DECISIONS.put(start, decision);
                }
            }
        }

        if (decision.reject()) {
            ci.cancel();
        }
    }
}
