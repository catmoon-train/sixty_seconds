package net.exmo.sixty_seconds.network;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.island.SixtySecondsIsland;
import net.exmo.sixty_seconds.island.SixtySecondsIslands;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.exmo.sixty_seconds.SixtySeconds;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 服务端→客户端：海图数据（岛屿元数据 + 本队解锁迷雾）。客户端 SeaChartScreen 用岛屿 seed
 * 与服务端同一套形状函数重画轮廓，无需同步方块。{@code openScreen=true} 时客户端收到即打开海图。
 * 解锁状态<b>按接收者所在队</b>打包（创造模式全解锁），故必须逐人发送。
 * <p>
 * {@code teleportAllowed} = 按图开关 {@code seaChartTeleportEnabled}（创造/旁观恒为 true）：false 时客户端
 * 把「返回住所」置灰、点岛不再扬帆，但岛屿轮廓/迷雾照常画——海图退化成纯导航图，玩家得自己开船去。
 * 客户端只用它做<b>显示</b>，服务端 {@code SixtySecondsIslands.sail/requestReturn} 各自重校验，改包无用。
 * <p>
 * 庇护所与队友的<b>动态点位</b>不在本包里（它们每 tick 都在动）——见
 * {@link SixtySecondsSeaChartPositionsS2CPacket}，只在玩家开着海图时按秒推。
 */
public record SixtySecondsSeaChartS2CPacket(boolean enabled, boolean openScreen, boolean teleportAllowed,
        int seaY, int seaChartRadius, List<Entry> islands) implements CustomPacketPayload {

    /** 单岛条目：locked（未解锁）条目在客户端画成迷雾「未知海域」。typeOrd -1=旧存档无类型。 */
    public record Entry(int id, int level, int namePrefix, int nameSuffix, int centerX, int centerZ,
            int radius, long seed, boolean unlocked, boolean visited, int typeOrd, boolean evacuation,
            boolean hardcore) {
        public SixtySecondsIsland.Type type() {
            SixtySecondsIsland.Type[] values = SixtySecondsIsland.Type.values();
            return typeOrd >= 0 && typeOrd < values.length ? values[typeOrd] : null;
        }
    }

    public static final Type<SixtySecondsSeaChartS2CPacket> ID =
            new Type<>(SixtySeconds.id("sixty_seconds_sea_chart"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SixtySecondsSeaChartS2CPacket> CODEC =
            StreamCodec.ofMember((packet, buf) -> {
                buf.writeBoolean(packet.enabled());
                buf.writeBoolean(packet.openScreen());
                buf.writeBoolean(packet.teleportAllowed());
                buf.writeVarInt(packet.seaY());
                buf.writeVarInt(packet.seaChartRadius());
                buf.writeVarInt(packet.islands().size());
                for (Entry entry : packet.islands()) {
                    buf.writeVarInt(entry.id());
                    buf.writeVarInt(entry.level());
                    buf.writeVarInt(entry.namePrefix());
                    buf.writeVarInt(entry.nameSuffix());
                    buf.writeVarInt(entry.centerX());
                    buf.writeVarInt(entry.centerZ());
                    buf.writeVarInt(entry.radius());
                    buf.writeLong(entry.seed());
                    buf.writeBoolean(entry.unlocked());
                    buf.writeBoolean(entry.visited());
                    buf.writeVarInt(entry.typeOrd());
                    buf.writeBoolean(entry.evacuation());
                    buf.writeBoolean(entry.hardcore());
                }
            }, buf -> {
                boolean enabled = buf.readBoolean();
                boolean openScreen = buf.readBoolean();
                boolean teleportAllowed = buf.readBoolean();
                int seaY = buf.readVarInt();
                int seaChartRadius = buf.readVarInt();
                int count = buf.readVarInt();
                List<Entry> islands = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    islands.add(new Entry(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                            buf.readLong(), buf.readBoolean(),                             buf.readBoolean(), buf.readVarInt(),
                            buf.readBoolean(), buf.readBoolean()));
                }
                return new SixtySecondsSeaChartS2CPacket(enabled, openScreen, teleportAllowed, seaY,
                        seaChartRadius, islands);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** 按接收者视角（本队解锁集；创造=全解锁）打包并发送。 */
    public static void send(ServerPlayer player, boolean openScreen) {
        ServerLevel level = player.serverLevel();
        SixtySecondsIslands.Data data = SixtySecondsIslands.get(level);
        int teamId = SixtySecondsStatsComponent.KEY.get(player).teamId;
        Set<Integer> unlocked = data.teamUnlocked.get(teamId);
        Set<Integer> visited = data.teamVisited.get(teamId);
        boolean seeAll = player.isCreative() || player.isSpectator();
        // 海洋（海岛）维度内所有岛默认可达、扬帆/返航始终可用
        boolean ocean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        List<Entry> entries = new ArrayList<>();
        int seaY = 0;
        for (SixtySecondsIsland island : data.save.islands) {
            seaY = island.seaY;
            boolean isUnlocked = seeAll || ocean || island.level <= 1
                    || (unlocked != null && unlocked.contains(island.id));
            entries.add(new Entry(island.id, island.level, island.namePrefix, island.nameSuffix,
                    island.centerX, island.centerZ, island.radius, island.seed, isUnlocked,
                    visited != null && visited.contains(island.id),
                    island.type == null ? -1 : island.type.ordinal(),
                    island.isEvacuation || island.type == SixtySecondsIsland.Type.EVACUATION,
                    island.hardcore));
        }
        // 创造/旁观不受 sea_teleport 开关限制；海洋维度内扬帆/返航总是可用
        boolean teleportAllowed = seeAll || ocean || SixtySecondsIslands.teleportAllowed(level);
        int seaChartRadius = SixtySecondsConfigStore.current(level)
                .map(c -> c.seaChartRadius).orElse(1800);
        ServerPlayNetworking.send(player, new SixtySecondsSeaChartS2CPacket(data.save.enabled, openScreen,
                teleportAllowed, seaY, seaChartRadius, entries));
    }
}
