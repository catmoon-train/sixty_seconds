package net.exmo.sixty_seconds.item;

import mcjty.lostcities.api.ILostChunkInfo;
import mcjty.lostcities.api.ILostCityInformation;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.island.SixtySecondsIsland;
import net.exmo.sixty_seconds.island.SixtySecondsIslands;
import net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * 撤离点指南针。右键后通过 actionbar 提示距离最近的撤离点的方位，
 * 兼容普通模式下的撤离点建筑（LostCities evacuationpoint）与海洋模式下的撤离点海岛。
 */
public class EvacCompassItem extends Item {

    public EvacCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos evac = findNearestEvacuationPoint((ServerLevel) level, player);
        if (evac == null) {
            player.displayClientMessage(
                    Component.translatable(SixtySeconds.MOD_ID + ".evac_compass.no_evac").withStyle(ChatFormatting.RED),
                    true);
            return InteractionResultHolder.success(stack);
        }

        double dx = evac.getX() - player.getX();
        double dz = evac.getZ() - player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);

        // 方位：以 -Z 为北、+X 为东，atan2 返回 [-pi, pi]
        double angle = Math.atan2(dx, -dz);
        double deg = Math.toDegrees(angle);
        if (deg < 0) deg += 360.0;

        String dirKey;
        if (deg < 22.5 || deg >= 337.5) dirKey = "north";
        else if (deg < 67.5) dirKey = "northeast";
        else if (deg < 112.5) dirKey = "east";
        else if (deg < 157.5) dirKey = "southeast";
        else if (deg < 202.5) dirKey = "south";
        else if (deg < 247.5) dirKey = "southwest";
        else if (deg < 292.5) dirKey = "west";
        else dirKey = "northwest";

        player.displayClientMessage(
                Component.translatable(
                        SixtySeconds.MOD_ID + ".evac_compass.found",
                        dist,
                        Component.translatable(SixtySeconds.MOD_ID + ".direction." + dirKey)
                ).withStyle(ChatFormatting.AQUA),
                true);

        return InteractionResultHolder.success(stack);
    }

    /** 寻找距离玩家最近的撤离点（普通模式：撤离建筑；海洋模式：撤离海岛）。 */
    private static BlockPos findNearestEvacuationPoint(ServerLevel level, Player player) {
        BlockPos playerPos = player.blockPosition();
        if (SixtySeconds.isOcean(level)) {
            return nearestEvacIsland(level, playerPos);
        }
        return nearestEvacBuilding(level, playerPos);
    }

    private static BlockPos nearestEvacIsland(ServerLevel level, BlockPos playerPos) {
        SixtySecondsIslands.Data data = SixtySecondsIslands.get(level);
        SixtySecondsIsland nearest = null;
        double best = Double.MAX_VALUE;
        for (SixtySecondsIsland island : data.save.islands) {
            if (!island.isEvacuation && island.type != SixtySecondsIsland.Type.EVACUATION) continue;
            BlockPos c = new BlockPos(island.centerX, playerPos.getY(), island.centerZ);
            double d = c.distSqr(playerPos);
            if (d < best) { best = d; nearest = island; }
        }
        return nearest == null ? null : new BlockPos(nearest.centerX, playerPos.getY(), nearest.centerZ);
    }

    private static BlockPos nearestEvacBuilding(ServerLevel level, BlockPos playerPos) {
        ILostCityInformation info = SixtySecondsLostCitiesStarMap.cityInfo(level);
        if (info == null) return null;
        int pcx = playerPos.getX() >> 4;
        int pcz = playerPos.getZ() >> 4;
        int radius = 48; // 扫描 48 个区块半径，足以覆盖整座城市
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                ILostChunkInfo chunk = info.getChunkInfo(cx, cz);
                if (chunk == null || !chunk.isCity() || chunk.getBuildingId() == null) continue;
                if (!chunk.getBuildingId().getPath().toLowerCase(Locale.ROOT).contains("evac")) continue;
                BlockPos c = new BlockPos(cx * 16 + 8, playerPos.getY(), cz * 16 + 8);
                double d = c.distSqr(playerPos);
                if (d < bestSqr) { bestSqr = d; best = c; }
            }
        }
        return best;
    }
}
