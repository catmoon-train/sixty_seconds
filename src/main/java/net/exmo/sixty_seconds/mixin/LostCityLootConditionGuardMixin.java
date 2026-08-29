package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import mcjty.lostcities.worldgen.lost.cityassets.AssetRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拦截 LostCities 的 {@code LostCityTerrainFeature#createLoot}，避免因为数据包里引用了
 * 未定义的 loot condition 而让整块区块生成失败。
 */
@Mixin(LostCityTerrainFeature.class)
public class LostCityLootConditionGuardMixin {

    private static final Set<String> KNOWN_PRESENT = ConcurrentHashMap.newKeySet();
    private static final Set<String> KNOWN_MISSING = ConcurrentHashMap.newKeySet();

    @Inject(method = "createLoot", at = @At("HEAD"), cancellable = true)
    private static void sixtySecondsSkipMissingLootCondition(
            BuildingInfo info,
            RandomSource random,
            LevelAccessor world,
            BlockPos pos,
            BuildingInfo.ConditionTodo todo,
            IDimensionInfo diminfo,
            CallbackInfo ci
    ) {
        if (todo == null) {
            return;
        }
        String name = todo.getCondition();
        if (name == null || name.isEmpty()) {
            return;
        }

        if (KNOWN_PRESENT.contains(name)) {
            return; // 该 condition 已确认存在，不介入
        }
        if (!KNOWN_MISSING.contains(name)) {
            if (conditionExists(world, name)) {
                KNOWN_PRESENT.add(name);
                return;
            }
            KNOWN_MISSING.add(name);
        }

        random.nextFloat();
        ci.cancel();
    }

    private static boolean conditionExists(LevelAccessor world, String name) {
        try {
            return AssetRegistries.CONDITIONS.get(world, name) != null;
        } catch (Exception e) {
            // get() 在资源不存在时会包一层 RuntimeException 抛出，这里当作“不存在”
            return false;
        }
    }
}
