package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.exmo.sixty_seconds.SixtySeconds;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端→客户端：星图星级区域同步。
 * <p>
 * 把服务端 {@link SixtySecondsConfig#areaLevelOverrides}（管理员用魔杖/命令划定的星级区域）
 * 推送给客户端，客户端据此调用 {@code StarMapManager.setStarRegions(...)}，让全屏星图与
 * HUD 小地图能绘制星级边框、标签与所在区域指示。
 * <p>
 * 与海图不同：星级区域是静态配置，不需要逐秒推送。客户端打开星图或首次手持星图时
 * 发 {@link SixtySecondsStarMapRequestC2SPacket} 请求，服务端收到后回本包；服务端也会在
 * 玩家加入时主动推一次，保证 HUD 在不打开全屏图的情况下也有数据。
 * <p>
 * 家居位置暂不同步（{@code StarMapManager.homePos} 保持 null），未来如需可在此包扩展。
 */
public record SixtySecondsStarMapS2CPacket(List<RegionEntry> regions) implements CustomPacketPayload {

    /** 单条星级区域：世界坐标盒（两角含端点，写入时已取正序）+ 等级 1..5 + 可选名字。 */
    public record RegionEntry(int minX, int minZ, int maxX, int maxZ, int level, String name) {
    }

    public static final Type<SixtySecondsStarMapS2CPacket> ID =
            new Type<>(SixtySeconds.id("sixty_seconds_star_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SixtySecondsStarMapS2CPacket> CODEC =
            StreamCodec.ofMember((packet, buf) -> {
                buf.writeVarInt(packet.regions().size());
                for (RegionEntry r : packet.regions()) {
                    buf.writeVarInt(r.minX());
                    buf.writeVarInt(r.minZ());
                    buf.writeVarInt(r.maxX());
                    buf.writeVarInt(r.maxZ());
                    buf.writeVarInt(r.level());
                    buf.writeUtf(r.name() == null ? "" : r.name());
                }
            }, buf -> {
                int count = buf.readVarInt();
                List<RegionEntry> regions = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int minX = buf.readVarInt();
                    int minZ = buf.readVarInt();
                    int maxX = buf.readVarInt();
                    int maxZ = buf.readVarInt();
                    int level = buf.readVarInt();
                    String name = buf.readUtf();
                    regions.add(new RegionEntry(minX, minZ, maxX, maxZ, level,
                            name == null || name.isEmpty() ? null : name));
                }
                return new SixtySecondsStarMapS2CPacket(regions);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /**
     * 打包并发送给指定玩家。参考海图加载方式：服务端从 LostCities 世界生成数据<b>动态计算</b>玩家附近的
     * 「建筑星级区域」并下发（而非仅读取静态配置）。这样一来星图能真实反映哪一块城区属于什么星级。
     * 管理员在配置里手写的 {@code areaLevelOverrides} 仍作为覆盖层叠加。
     */
    public static void send(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos p = player.blockPosition();
        int pcx = p.getX() >> 4;
        int pcz = p.getZ() >> 4;
        List<RegionEntry> entries = new ArrayList<>();
        // 1) 动态生成：扫描玩家周围已加载建筑区块，按连通同名建筑聚合成星级区域
        for (SixtySecondsLostCitiesStarMap.BuildingRegion br
                : SixtySecondsLostCitiesStarMap.buildingStarRegions(level, pcx, pcz, SixtySecondsLostCitiesStarMap.STAR_MAP_SCAN_RADIUS_CHUNKS)) {
            // 下发翻译键，由客户端 Component.translatable 按玩家语言渲染（语言文件随模组下发，客户端必含）
            entries.add(new RegionEntry(br.minX, br.minZ, br.maxX, br.maxZ, br.star,
                    br.displayName));
        }
        // 2) 管理员覆盖层（若存在）
        SixtySecondsConfigStore.current(level).ifPresent(config -> {
            if (config.areaLevelOverrides != null) {
                for (SixtySecondsConfig.LevelRegion lr : config.areaLevelOverrides) {
                    if (lr == null || lr.min == null || lr.max == null) {
                        continue;
                    }
                    // 两角取正序，保证 minX<=maxX
                    int minX = Math.min(lr.min.x, lr.max.x);
                    int maxX = Math.max(lr.min.x, lr.max.x);
                    int minZ = Math.min(lr.min.z, lr.max.z);
                    int maxZ = Math.max(lr.min.z, lr.max.z);
                    int lvl = Math.max(1, Math.min(5, lr.level));
                    entries.add(new RegionEntry(minX, minZ, maxX, maxZ, lvl, lr.name));
                }
            }
        });
        ServerPlayNetworking.send(player, new SixtySecondsStarMapS2CPacket(entries));
    }
}
