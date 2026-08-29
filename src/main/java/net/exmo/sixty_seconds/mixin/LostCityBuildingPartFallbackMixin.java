package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.lost.cityassets.Building;
import mcjty.lostcities.worldgen.lost.cityassets.ConditionContext;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 修复 LostCities {@code BuildingInfo} 构造时抛出的
 * {@code RuntimeException: Misconfiguration! Floor were generated for a building where no part condition matches!}
 * （见日志 {@code BuildingInfo.<init>(BuildingInfo.java:1047)}）。
 *
 * <p>成因：{@code BuildingInfo} 算出的楼层数超出了该建筑实际声明的 part 覆盖范围
 * （例如 profile 的 {@code BUILDING_MINFLOORS} 被抬得过高——注意
 * {@code getMinfloors()} 是在 {@code f} 被 clamp 到 {@code maxfloors} <b>之后</b>才应用的，
 * 所以会把只写了 {@code "maxfloors": N} 的建筑硬顶成更高楼层；
 * 或者建筑用 {@code "floor": n} 逐层列举 part 而列举层数不够；
 * 又或者地下室层数为负而没有任何 part 命中）。
 * 此时 {@code Building.getRandomPart} 返回 {@code null}，LostCities 直接抛异常，
 * 导致整个区块（以及依赖它的相邻区块）生成失败。
 *
 * <p>真正的修复是让楼层数落在建筑声明的范围内（已在 {@code LostCityProfileMixin} 中
 * 停止强制覆盖楼层数）。本 mixin 只作为<b>兜底安全网</b>：万一将来换了 profile 配置或数据包，
 * 某些建筑仍被算出超范围楼层，也不至于让整块区块生成失败。
 *
 * <p>修复方式（纯 mixin，不改 LostCities 源码）：当 {@code getRandomPart} 返回 {@code null} 时，
 * 沿着「离当前楼层最近、且确实有 part 命中」的楼层做回退搜索，用该楼层的 part 顶替。
 * 这样超出的楼层复用建筑最顶层/最底层的 part，地下层复用地面层 part，
 * 视觉上等价于把建筑「拉长」，但不会再让整块区块生成失败。
 *
 * <p>本 mixin 只在 {@code getRandomPart} 原本返回 {@code null} 时介入，
 * 正常情况完全不改变 LostCities 的随机序列与生成结果。
 */
@Mixin(Building.class)
public class LostCityBuildingPartFallbackMixin {

    private static final Logger LOGGER = LogManager.getLogger(LostCityBuildingPartFallbackMixin.class);

    /** 已回退过的「建筑 + 楼层」去重表，避免刷屏。 */
    private static final Set<String> LOGGED_FALLBACKS = ConcurrentHashMap.newKeySet();

    @Shadow
    @Final
    private List<Pair<Predicate<ConditionContext>, String>> parts;

    @Inject(method = "getRandomPart", at = @At("RETURN"), cancellable = true)
    private void sixtySecondsFallbackMissingPart(Random random, ConditionContext ctx, CallbackInfoReturnable<String> cir) {
        if (cir.getReturnValue() != null || parts == null || parts.isEmpty() || ctx == null) {
            return;
        }

        int floor = ctx.getFloor();
        int lowestFloor = -Math.max(0, ctx.getFloorsBelowGround());
        int highestFloor = ctx.getFloorsAboveGround();

        // 由近及远寻找「有 part 命中」的楼层
        int span = Math.max(highestFloor, floor) - Math.min(lowestFloor, floor) + 1;
        for (int d = 1; d <= span; d++) {
            int below = floor - d;
            int above = floor + d;
            // 优先向上找（地下层没有 part 时复用地面层），再向下找（超出楼顶时复用最顶层）
            if (above <= highestFloor) {
                String part = matchAtFloor(random, ctx, above);
                if (part != null) {
                    report((Building) (Object) this, ctx, floor, above, part);
                    cir.setReturnValue(part);
                    return;
                }
            }
            if (below >= lowestFloor) {
                String part = matchAtFloor(random, ctx, below);
                if (part != null) {
                    report((Building) (Object) this, ctx, floor, below, part);
                    cir.setReturnValue(part);
                    return;
                }
            }
        }
    }

    /** 在假定楼层为 {@code floor} 的前提下重新走一遍 part 条件筛选。 */
    private String matchAtFloor(Random random, ConditionContext ctx, int floor) {
        ConditionContext probe = atFloor(ctx, floor);
        List<String> matches = new ArrayList<>();
        for (Pair<Predicate<ConditionContext>, String> pair : parts) {
            if (pair.getLeft().test(probe)) {
                matches.add(pair.getRight());
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        return matches.get(random.nextInt(matches.size()));
    }

    /**
     * 构造一个与 {@code ctx} 完全等价、但楼层为 {@code floor} 的上下文。
     * {@code ConditionContext} 的字段是 private 且没有 setter，这里直接覆写所有读取方法，
     * 因此不需要访问其私有字段。
     */
    private static ConditionContext atFloor(ConditionContext ctx, int floor) {
        int levelOffset = floor - ctx.getFloor();
        int floorsAboveGround = ctx.getFloorsAboveGround();
        return new ConditionContext(0, 0, 0, 0, null, null, null, null) {
            @Override
            public int getLevel() {
                return ctx.getLevel() + levelOffset;
            }

            @Override
            public int getFloor() {
                return floor;
            }

            @Override
            public int getFloorsBelowGround() {
                return ctx.getFloorsBelowGround();
            }

            @Override
            public int getFloorsAboveGround() {
                return floorsAboveGround;
            }

            @Override
            public boolean isGroundFloor() {
                return floor == 0;
            }

            @Override
            public boolean isBuilding() {
                return ctx.isBuilding();
            }

            @Override
            public boolean isSphere() {
                return ctx.isSphere();
            }

            @Override
            public ResourceLocation getBiome() {
                return ctx.getBiome();
            }

            @Override
            public boolean isTopOfBuilding() {
                return floor >= floorsAboveGround;
            }

            @Override
            public boolean isCellar() {
                return floor < 0;
            }

            @Override
            public boolean isFloor(int l) {
                return floor == l;
            }

            @Override
            public boolean isRange(int l1, int l2) {
                return floor >= l1 && floor <= l2;
            }

            @Override
            public String getPart() {
                return ctx.getPart();
            }

            @Override
            public String getBuilding() {
                return ctx.getBuilding();
            }

            @Override
            public int getChunkX() {
                return ctx.getChunkX();
            }

            @Override
            public int getChunkZ() {
                return ctx.getChunkZ();
            }
        };
    }

    private static void report(Building building, ConditionContext ctx, int floor, int usedFloor, String part) {
        String key = building.getName() + "|" + floor + "|" + usedFloor + "|" + part;
        if (!LOGGED_FALLBACKS.add(key)) {
            return;
        }
        LOGGER.warn(
                "LostCities: building '{}' has no part matching floor {} (floors above ground = {}, cellars = {}). " +
                        "Falling back to the part of floor {} ('{}') to avoid aborting chunk generation.",
                building.getName(), floor, ctx.getFloorsAboveGround(), ctx.getFloorsBelowGround(), usedFloor, part);
    }
}
