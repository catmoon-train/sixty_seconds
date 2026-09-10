package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanTitanEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 让 Boss 在玩家接近时显示轮廓。
 *
 * <p>实体的 glowing 标记是服务端同步的，因此这里采用「附近至少有一名玩家」
 * 的策略：没有玩家靠近时关闭轮廓，避免让整张地图的 Boss 永久高亮，也避免
 * 每个客户端额外维护一套实体描边状态。</p>
 */
public final class SixtySecondsBossOutlineSystem {
    private static final int CHECK_INTERVAL = 10;
    private static final double OUTLINE_RADIUS = 64.0D;
    // Entity 的共享标记字段固定使用 id 0；通过本地 accessor 读取/发送，
    // 避免直接访问 Entity 的 protected 常量，同时保持与原版同步协议一致。
    private static final EntityDataAccessor<Byte> SHARED_FLAGS =
            new EntityDataAccessor<>(0, EntityDataSerializers.BYTE);
    private static final Map<ServerLevel, Map<UUID, Set<Integer>>> ACTIVE = new WeakHashMap<>();

    private SixtySecondsBossOutlineSystem() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }

        Map<UUID, Set<Integer>> activeByPlayer = ACTIVE.computeIfAbsent(level, ignored -> new java.util.HashMap<>());
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                Set<Integer> previous = activeByPlayer.remove(player.getUUID());
                if (previous != null) {
                    for (int id : previous) {
                        Entity entity = level.getEntity(id);
                        if (isBoss(entity)) {
                            sendGlow(player, entity, false);
                        }
                    }
                }
                continue;
            }
            Set<Integer> shouldGlow = new HashSet<>();
            AABB search = player.getBoundingBox().inflate(OUTLINE_RADIUS);
            collect(level, search, SixtySecondsBossEntity.class, shouldGlow);
            collect(level, search, OceanSeaMonsterEntity.class, shouldGlow);
            collect(level, search, OceanTitanEntity.class, shouldGlow);

            Set<Integer> previous = activeByPlayer.getOrDefault(player.getUUID(), Set.of());
            for (int id : previous) {
                if (!shouldGlow.contains(id)) {
                    Entity entity = level.getEntity(id);
                    if (isBoss(entity)) {
                        sendGlow(player, entity, false);
                    }
                }
            }
            for (int id : shouldGlow) {
                Entity entity = level.getEntity(id);
                if (isBoss(entity)) {
                    sendGlow(player, entity, true);
                }
            }
            activeByPlayer.put(player.getUUID(), shouldGlow);
        }
        activeByPlayer.keySet().removeIf(uuid -> level.getServer().getPlayerList().getPlayer(uuid) == null);
    }

    private static <T extends Entity> void collect(ServerLevel level, AABB box, Class<T> type,
            Set<Integer> result) {
        for (T entity : level.getEntitiesOfClass(type, box, Entity::isAlive)) {
            if (!entity.isRemoved()) {
                result.add(entity.getId());
            }
        }
    }

    private static void sendGlow(ServerPlayer player, Entity entity, boolean glowing) {
        byte flags = entity.getEntityData().get(SHARED_FLAGS);
        byte packetFlags = glowing ? (byte) (flags | 0x40) : (byte) (flags & ~0x40);
        @SuppressWarnings({"rawtypes", "unchecked"})
        SynchedEntityData.DataValue value = new SynchedEntityData.DataValue(
                0, EntityDataSerializers.BYTE, packetFlags);
        player.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), java.util.List.of(value)));
    }

    private static boolean isBoss(Entity entity) {
        return entity instanceof SixtySecondsBossEntity
                || entity instanceof OceanSeaMonsterEntity
                || entity instanceof OceanTitanEntity;
    }
}
