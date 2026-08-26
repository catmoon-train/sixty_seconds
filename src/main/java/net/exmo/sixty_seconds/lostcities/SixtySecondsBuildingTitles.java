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
            ILostChunkInfo c = info.getChunkInfo(cx, cz);
            String id = (c != null && c.isCity() && c.getBuildingId() != null) ? c.getBuildingId().getPath() : null;
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
            Component title = Component.translatable(displayKey);
            Component subtitle;
            if (star >= 1 && star <= 5) {
                subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.danger",
                        "★".repeat(star), star);
            } else if (star == SixtySecondsLostCitiesStarMap.SAFE_STAR) {
                subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.safe_zone");
            } else if (star == SixtySecondsLostCitiesStarMap.UNGRADED) {
                subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.evacuation");
            } else {
                subtitle = Component.translatable("building.sixty_seconds.sixty_seconds.unknown");
            }
            SubtitleCommand.sendToPlayerTop(player, title, subtitle, 70);
        }
    }
}
