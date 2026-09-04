package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.lost.BuildingInfo;
import mcjty.lostcities.worldgen.lost.StructureAvoidance;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 暴露 {@link BuildingInfo} 的私有构造器与结构规避结果字段。
 * <p>
 * 并行化后的 {@code getBuildingInfo} 需要在单飞任务里构造 BuildingInfo
 * （构造器是 private 的，mixin 类无法直接调用），并复刻原版的
 * “仅当结构规避结果已知时才入缓存”语义（需要读取 private 的
 * {@code structureAvoidance} 字段）。
 */
@Mixin(value = BuildingInfo.class, remap = false)
public interface BuildingInfoInvoker {

    @Invoker("<init>")
    static BuildingInfo ss$createBuildingInfo(ChunkCoord key, IDimensionInfo provider) {
        throw new AssertionError();
    }

    @Accessor("structureAvoidance")
    StructureAvoidance.Result ss$getStructureAvoidance();
}
