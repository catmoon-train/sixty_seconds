package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInformation;
import net.exmo.sixty_seconds.bridge.stubs.SubtitleCommand;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.WeakHashMap;

/**
 * 进入建筑区域时，向玩家展示「建筑名称（title）+ 危险星级（subtitle）」，类似海洋模式登岛时的报幕。
 * 仅在玩家所在建筑的 id 发生变化时提示一次；离开建筑后重置，因此同一建筑内走动不会反复刷屏。
 * 每 10 游戏刻（0.5 秒）检查一次，且仅查询已加载区块（{@code hasChunk}）以避免主线程触发生成阻塞。
 */
public final class SixtySecondsBuildingTitles {
    private static final WeakHashMap<ServerPlayer, String> LAST_BUILDING = new WeakHashMap<>();

    private SixtySecondsBuildingTitles() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        ILostCityInformation info = SixtySecondsLostCitiesStarMap.cityInfo(level);
        if (info == null) {
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
            if (!level.hasChunk(cx, cz)) {
                continue; // 区块未加载时不查询，避免触发生成阻塞主线程
            }
            // 先查玩家所在区块；取不到建筑时（站在建筑边缘的街道格）向周围 8 个区块兜底，
            // 优先取玩家所在区块，其次最近的建筑区块。
            String id = null;
            ILostChunkInfo self = info.getChunkInfo(cx, cz);
            if (self != null && self.isCity() && self.getBuildingId() != null) {
                id = self.getBuildingId().getPath();
            } else {
                int[][] offs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                for (int[] o : offs) {
                    if (!level.hasChunk(cx + o[0], cz + o[1])) {
                        continue;
                    }
                    ILostChunkInfo n = info.getChunkInfo(cx + o[0], cz + o[1]);
                    if (n != null && n.isCity() && n.getBuildingId() != null) {
                        id = n.getBuildingId().getPath();
                        break;
                    }
                }
            }
            String last = LAST_BUILDING.get(player);
            if (id == null) {
                if (last != null) {
                    LAST_BUILDING.remove(player); // 离开建筑：允许再次进入时重新提示
                }
                continue;
            }
            if (id.equals(last)) {
                continue; // 同一建筑内移动：不重复提示
            }
            LAST_BUILDING.put(player, id);
            int star = SixtySecondsLostCitiesStarMap.starForBuildingName(id);
            String displayKey = SixtySecondsLostCitiesStarMap.buildingDisplayKey(id);
            // 服务端按玩家自己的语言解析翻译键，避免客户端资源较旧时把原始键渲染出来
            Component title = Component.literal(SixtySecondsLostCitiesStarMap.resolveFor(player, displayKey));
            Component subtitle;
            if (star >= 1 && star <= 5) {
                String tmpl = SixtySecondsLostCitiesStarMap.resolveFor(player,
                        "building.sixty_seconds.sixty_seconds.danger");
                subtitle = Component.literal(String.format(tmpl, "★".repeat(star), star));
            } else if (star == SixtySecondsLostCitiesStarMap.SAFE_STAR) {
                subtitle = Component.literal(SixtySecondsLostCitiesStarMap.resolveFor(player,
                        "building.sixty_seconds.sixty_seconds.safe_zone"));
            } else if (star == SixtySecondsLostCitiesStarMap.UNGRADED) {
                subtitle = Component.literal(SixtySecondsLostCitiesStarMap.resolveFor(player,
                        "building.sixty_seconds.sixty_seconds.evacuation"));
            } else {
                subtitle = Component.literal(SixtySecondsLostCitiesStarMap.resolveFor(player,
                        "building.sixty_seconds.sixty_seconds.unknown"));
            }
            SubtitleCommand.sendToPlayerTop(player, title, subtitle, 70);
        }
    }
}
