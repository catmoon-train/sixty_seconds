package net.exmo.sixty_seconds.lostcities;

import net.exmo.sixty_seconds.bridge.stubs.SubtitleCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
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
    /** 星图扫描半径（区块）。玩家处于建筑内，洪泛填充会从玩家所在区块扩展至整栋，故半径无需很大。 */
    private static final int STAR_TITLE_SCAN_RADIUS = 3;
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
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                LAST_BUILDING.remove(player);
                continue;
            }
            BlockPos pos = player.blockPosition();
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            // 与星图完全相同的判定：取玩家附近会被星图显示出来的建筑区域
            List<SixtySecondsLostCitiesStarMap.BuildingRegion> regions =
                    SixtySecondsLostCitiesStarMap.buildingStarRegions(level, cx, cz, STAR_TITLE_SCAN_RADIUS);
            SixtySecondsLostCitiesStarMap.BuildingRegion hit = null;
            for (SixtySecondsLostCitiesStarMap.BuildingRegion region : regions) {
                if (pos.getX() >= region.minX && pos.getX() <= region.maxX
                        && pos.getZ() >= region.minZ && pos.getZ() <= region.maxZ) {
                    hit = region;
                    break;
                }
            }
            if (hit == null) {
                // 不在任何星图建筑区域内（或星图信息暂不可用）：清除记录以便再次进入时重新提示
                LAST_BUILDING.remove(player);
                continue;
            }
            String id = hit.id;
            String last = LAST_BUILDING.get(player);
            if (id.equals(last)) {
                continue; // 同一建筑内移动：不重复提示
            }
            LAST_BUILDING.put(player, id);
            // 标题（建筑名）随危险度上色：越高越红，并加粗
            ChatFormatting nameColor = hit.star >= 4 ? ChatFormatting.RED
                    : hit.star >= 3 ? ChatFormatting.GOLD : ChatFormatting.AQUA;
            Component title = Component.translatable(hit.displayName).withStyle(nameColor, ChatFormatting.BOLD);
            // 副标题里的「星级」按各自星级对应的颜色逐颗上色（与星图 StarRegion.STAR_COLORS 完全一致）
            MutableComponent stars = Component.literal("");
            for (int i = 0; i < hit.star && i < STAR_COLORS.length; i++) {
                stars.append(Component.literal("★").withColor(TextColor.fromRgb(STAR_COLORS[i])));
            }
            Component subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.danger",
                    stars, hit.star);
            SubtitleCommand.sendToPlayerTop(player, title, subtitle, 70);
        }
    }
}
