package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.gen.Stuff;
import mcjty.lostcities.worldgen.lost.cityassets.CompiledPalette;
import mcjty.lostcities.worldgen.lost.cityassets.StuffObject;
import mcjty.lostcities.worldgen.lost.regassets.StuffSettingsRE;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 排查用诊断 mixin：定位 LostCities 在 {@code Stuff.actuallyGenerateStuff} 中
 * 因某个调色板字符缺失（{@code CompiledPalette.get(char)} 返回 null）而导致的 NullPointerException。
 *
 * <p>做法：
 * <ol>
 *   <li>在 {@code actuallyGenerateStuff} 方法入口预检当前 stuff 的 {@code column} 字符串里，
 *       每个字符是否都能在编译后的调色板中解析；若有缺失，打印<b>完整上下文</b>
 *       （cityStyle / building / tags / column / 调色板已定义字符），让开发者直接知道
 *       「具体是哪个 stuff 物件的哪一部分（哪个字符）出问题」。</li>
 *   <li>再用 {@code @Redirect} 拦截 {@code CompiledPalette.get(char)} 的返回，
 *       当确实得到 null 时静默返回一个占位方块，避免 {@code ChunkDriver.correct} 内 NPE 使游戏崩溃，
 *       保证排查期间世界能继续生成。</li>
 * </ol>
 *
 * <p>这是临时性排查工具，定位并修复对应数据包的调色板后应当移除此 mixin（或置为不注册）。
 */
@Mixin(Stuff.class)
public class LostCityStuffDebugMixin {

    private static final Logger LOGGER = LogManager.getLogger(LostCityStuffDebugMixin.class);

    /** 已报告的「stuff 上下文 + 缺失字符」去重表，避免刷屏。 */
    private static final Set<String> LOGGED_MISSING = new HashSet<>();

    /** 已报告的 rubble 物件 id 去重表，避免刷屏。 */
    private static final Set<ResourceLocation> LOGGED_STUFF = new HashSet<>();

    @Inject(method = "actuallyGenerateStuff", at = @At("HEAD"))
    private static void sixtySecondsDebugStuffContext(
            LostCityTerrainFeature feature, BuildingInfo info, StuffSettingsRE settings, CompiledPalette palette, boolean inBuilding, CallbackInfo ci) {
        String column = settings.getColumn();
        if (column == null || column.isEmpty()) {
            return;
        }
        List<Character> missing = new ArrayList<>();
        for (int i = 0; i < column.length(); i++) {
            char ch = column.charAt(i);
            if (palette.get(ch) == null) {
                missing.add(ch);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        String key = column + "|" + info.getCityStyle().getName() + "|" + info.getBuildingType();
        if (!LOGGED_MISSING.add(key)) {
            return;
        }
        LOGGER.error(
                "LostCities STUFF DEBUG: stuff column references character(s) {} that are NOT defined in the compiled palette!\n" +
                "    cityStyle = {}\n" +
                "    building  = {}\n" +
                "    tags      = {}\n" +
                "    column    = '{}' (length {})\n" +
                "    defined palette characters = {}",
                missing,
                info.getCityStyle().getName(),
                info.getBuildingType(),
                settings.getTags(),
                column, column.length(),
                palette.getCharacters());
    }

    @Inject(method = "generateStuff",
            at = @At(value = "INVOKE",
                    target = "Lmcjty/lostcities/worldgen/gen/Stuff;actuallyGenerateStuff(Lmcjty/lostcities/worldgen/LostCityTerrainFeature;Lmcjty/lostcities/worldgen/lost/BuildingInfo;Lmcjty/lostcities/worldgen/lost/regassets/StuffSettingsRE;Lmcjty/lostcities/worldgen/lost/cityassets/CompiledPalette;Z)V"))
    private static void sixtySecondsDebugResolveStuffId(LostCityTerrainFeature feature, BuildingInfo info, CallbackInfo ci,
            @Local StuffObject stuff, @Local StuffSettingsRE settings) {
        ResourceLocation id = stuff.getId();
        if (!LOGGED_STUFF.add(id)) {
            return;
        }
        LOGGER.error(
                "LostCities STUFF DEBUG [resolve] stuff id={} (name={}), tags={}, column='{}', buildingType={}\n" +
                "    -> expected file: data/{}/lostcities/stuff/{}.json",
                id, stuff.getName(), settings.getTags(), settings.getColumn(), info.getBuildingType(),
                id.getNamespace(), id.getPath());
    }

    @Redirect(method = "actuallyGenerateStuff",
            at = @At(value = "INVOKE",
                    target = "Lmcjty/lostcities/worldgen/lost/cityassets/CompiledPalette;get(C)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState sixtySecondsDebugPaletteGet(CompiledPalette instance, char c) {
        BlockState result = instance.get(c);
        if (result == null) {
            // 兜底：确实缺失时不要返回 null（否则 ChunkDriver.correct 内会 NPE 崩溃）。
            // 上下文信息已由上面的 @Inject(HEAD) 打印，这里仅做安全占位（空气，不污染世界）。
            result = Blocks.AIR.defaultBlockState();
        }
        return result;
    }
}
