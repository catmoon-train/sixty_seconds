package net.exmo.sixty_seconds.lostcities;

import net.exmo.sixty_seconds.bridge.stubs.SubtitleCommand;
import net.exmo.sixty_seconds.logic.SixtySecondsDailyEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.WeakHashMap;

/**
 * 进入建筑区域时，向玩家展示「建筑名称（title）+ 危险星级（subtitle）」，类似海洋模式登岛时的报幕。
 * <p>
 * 判定标准<b>完全复用星图</b>（{@link SixtySecondsLostCitiesStarMap#buildingStarRegions}）：
 * 当玩家所在区块落在星图会显示出来的某个建筑区域（星级 1~5）内时，展示一次 title。
 * 这保证标题触发条件与星图完全一致——星图上能看到的地方，走进去就会报幕。
 * <p>
 * 仅在玩家所在建筑 id 发生变化时提示一次；离开建筑后重置，因此同一建筑内走动不会反复刷屏。
 * 每 60 游戏刻（3 秒）检查一次。星图扫描只查询已加载区块（{@code hasChunk}），不触发生成阻塞主线程。
 */
public final class SixtySecondsBuildingTitles {
    private static final WeakHashMap<ServerPlayer, String> LAST_BUILDING = new WeakHashMap<>();
    /** 1~5 星对应的颜色，与星图 StarRegion.STAR_COLORS 保持一致（绿→青→金→橙→红）。 */
    private static final int[] STAR_COLORS = {
            0xFF55CC55, // ★ 绿
            0xFF4AB8C0, // ★★ 青
            0xFFFFD700, // ★★★ 金
            0xFFE07B39, // ★★★★ 橙
            0xFFD94040  // ★★★★★ 红
    };

    private SixtySecondsBuildingTitles() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 60 != 0) {
            return;
        }
        // 已发现建筑定期落盘（脏且到间隔才真正异步写盘，主线程仅做快照）
        if (level.getGameTime() % SixtySecondsDiscoveredBuildings.SAVE_INTERVAL_TICKS == 0) {
            SixtySecondsDiscoveredBuildings.saveIfDirty(level);
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                LAST_BUILDING.remove(player);
                continue;
            }
            // 在自家住宅（开局 60 秒的房子）或庇护所内时，不报幕建筑名 / 岛屿名 title 与星级副 title
            if (SixtySecondsDailyEvents.isPlayerInShelter(player)) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            // 与星图完全相同的建筑判定（id / 星级），但只查玩家所在的单个区块：
            // 报幕只需要「这一栋楼」的 id 与星级，不需要整栋楼的外接矩形，
            // 因此无需像星图那样做 7×7 区块的洪泛扫描（该扫描每 3 秒每玩家一次太重）。
            // 多区块建筑 getMultiBuildingInfo 返回父 id，与星图 regionKeyOf 的判定结果一致。
            mcjty.lostcities.api.ILostCityInformation info =
                    SixtySecondsLostCitiesStarMap.cityInfo(level);
            mcjty.lostcities.api.ILostChunkInfo chunk =
                    SixtySecondsLostCitiesStarMap.safeChunkInfo(level, info, cx, cz);
            String id = null;
            int star = 0;
            String displayKey = null;
            if (chunk != null && chunk.isCity()) {
                mcjty.lostcities.api.ILostChunkInfo.MultiBuildingInfo mb = chunk.getMultiBuildingInfo();
                if (mb != null) {
                    id = mb.buildingType().toString();
                    star = SixtySecondsLostCitiesStarMap.starForMultiBuilding(id);
                } else if (chunk.getBuildingId() != null) {
                    id = chunk.getBuildingId().toString();
                    star = SixtySecondsLostCitiesStarMap.starForBuildingName(id);
                }
                if (id != null && star >= 1 && star <= 5
                        && !SixtySecondsLostCitiesStarMap.isHiddenFromStarMap(id)) {
                    displayKey = SixtySecondsLostCitiesStarMap.buildingDisplayKey(id);
                } else {
                    // 安全区/撤离点/未登记建筑/空置地块（common_empty，星图隐藏建筑）：一律不报幕
                    id = null;
                }
            }
            if (id != null) {
                // 已发现建筑登记：与报幕同一判定点，复用上面已取到的区块信息（零额外区块查询）。
                // 星图下发时合并这些记录，让去过的建筑在离开后仍标在星图上（修复
                // 「进楼报幕正确、星图却不标记」——实时扫描只看当前位置 ±16 区块的已加载区块）。
                SixtySecondsDiscoveredBuildings.record(level, cx, cz, id, star);
            }
            if (id == null) {
                // 不在任何星图建筑区域内（或星图信息暂不可用）：清除记录以便再次进入时重新提示
                LAST_BUILDING.remove(player);
                continue;
            }
            String last = LAST_BUILDING.get(player);
            if (id.equals(last)) {
                continue; // 同一建筑内移动：不重复提示
            }
            LAST_BUILDING.put(player, id);
            // 标题（建筑名）随危险度上色：越高越红，并加粗
            ChatFormatting nameColor = star >= 4 ? ChatFormatting.RED
                    : star >= 3 ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            Component title = Component.translatable(displayKey).withStyle(nameColor, ChatFormatting.BOLD);
            // 副标题里的「星级」按各自星级对应的颜色逐颗上色（与星图 StarRegion.STAR_COLORS 完全一致）
            MutableComponent stars = Component.literal("");
            for (int i = 0; i < star && i < STAR_COLORS.length; i++) {
                stars.append(Component.literal("★").withColor(STAR_COLORS[i]));
            }
            Component subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.danger",
                    stars, star);
            SubtitleCommand.sendToPlayerTop(player, title, subtitle, 70);
        }
    }
}
