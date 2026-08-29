package net.exmo.sixty_seconds.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mcjty.lostcities.config.LostCityProfile;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.MultiChunk;
import mcjty.lostcities.worldgen.lost.cityassets.CityStyle;
import mcjty.lostcities.worldgen.lost.cityassets.MultiBuilding;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 修复 LostCities {@code MultiChunk.calculateBuildings} 中的崩溃：
 *
 * <p>当一个 {@code MultiBuilding} 的 {@code dimX}/{@code dimZ} 大于当前 multichunk 的
 * {@code areasize} 时，{@code areasize - dimX + 1} 会变成 {@code <= 0}，
 * 导致 {@code Random.nextInt(nonPositive)} 抛出 {@code IllegalArgumentException: bound must be positive}
 * （见日志中的 {@code MultiChunk.calculateBuildings(MultiChunk.java:139)}）。
 *
 * <p>修复方式（仅通过 mixin 修改 LostCities 运行时类，不改动其源码）：
 * <ol>
 *   <li>{@code @Redirect} 拦截 {@code Random.nextInt(int)}，将非正的 bound 钳制为 1，避免抛异常；</li>
 *   <li>{@code @WrapOperation} 拦截 {@code canPlaceBuilding(...)}，当建筑尺寸超过 {@code areasize}
 *       时直接返回 {@code false}（这类建筑本来就放不进去），从而避免后续访问
 *       {@code buildingGrid[x+xx][z+zz]} 时越界，并让循环正常跳过该建筑。</li>
 * </ol>
 */
@Mixin(MultiChunk.class)
public class LostCityMultiChunkMixin {

    private static final Logger LOGGER = LogManager.getLogger(LostCityMultiChunkMixin.class);
    private static final Set<String> LOGGED_OVERSIZED = ConcurrentHashMap.newKeySet();

    @Shadow private int areasize;

    @Redirect(method = "calculateBuildings",
            at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int sixtySecondsSafeNextInt(Random rand, int bound) {
        // bound 必须为正，否则 nextInt 会抛 IllegalArgumentException。钳制到至少 1。
        return rand.nextInt(Math.max(bound, 1));
    }

    @WrapOperation(method = "calculateBuildings",
            at = @At(value = "INVOKE",
                    target = "Lmcjty/lostcities/worldgen/lost/MultiChunk;canPlaceBuilding" +
                            "(Lmcjty/lostcities/varia/ChunkCoord;" +
                            "Lmcjty/lostcities/worldgen/IDimensionInfo;" +
                            "Lmcjty/lostcities/config/LostCityProfile;" +
                            "Lmcjty/lostcities/worldgen/lost/cityassets/CityStyle;" +
                            "Lmcjty/lostcities/worldgen/lost/cityassets/MultiBuilding;IIII)Z"))
    private boolean sixtySecondsSkipOversizedBuilding(MultiChunk instance, ChunkCoord topleft, IDimensionInfo provider,
            LostCityProfile profile, CityStyle buildingCityStyle, MultiBuilding building,
            int cityLevel, int maxCellars, int x, int z, Operation<Boolean> original) {
        int dimX = building.getDimX();
        int dimZ = building.getDimZ();
        if (dimX > this.areasize || dimZ > this.areasize) {
            // 该 multibuilding 比 multichunk 区域还大，永远放不进，直接跳过。
            String key = building.getName() + "|" + this.areasize;
            if (LOGGED_OVERSIZED.add(key)) {
                LOGGER.warn(
                        "LostCities: 跳过超出 multichunk 尺寸的 MultiBuilding '{}' (dimX={}, dimZ={})，" +
                        "areasize={}。请检查其数据包配置。",
                        building.getName(), dimX, dimZ, this.areasize);
            }
            return false;
        }
        return original.call(instance, topleft, provider, profile, buildingCityStyle, building, cityLevel, maxCellars, x, z);
    }
}
