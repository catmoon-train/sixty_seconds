package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanTitanEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    private static final Map<ServerLevel, Set<UUID>> ACTIVE = new WeakHashMap<>();

    private SixtySecondsBossOutlineSystem() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }

        Set<UUID> shouldGlow = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            AABB search = player.getBoundingBox().inflate(OUTLINE_RADIUS);
            collect(level, search, SixtySecondsBossEntity.class, shouldGlow);
            collect(level, search, OceanSeaMonsterEntity.class, shouldGlow);
            collect(level, search, OceanTitanEntity.class, shouldGlow);
        }

        Set<UUID> previous = ACTIVE.get(level);
        if (previous != null) {
            for (UUID id : previous) {
                if (!shouldGlow.contains(id)) {
                    Entity entity = level.getEntity(id);
                    if (isBoss(entity)) {
                        entity.setGlowingTag(false);
                    }
                }
            }
        }
        for (UUID id : shouldGlow) {
            Entity entity = level.getEntity(id);
            if (isBoss(entity)) {
                entity.setGlowingTag(true);
            }
        }
        ACTIVE.put(level, shouldGlow);
    }

    private static <T extends Entity> void collect(ServerLevel level, AABB box, Class<T> type,
            Set<UUID> result) {
        for (T entity : level.getEntitiesOfClass(type, box, Entity::isAlive)) {
            if (!entity.isRemoved()) {
                result.add(entity.getUUID());
            }
        }
    }

    private static boolean isBoss(Entity entity) {
        return entity instanceof SixtySecondsBossEntity
                || entity instanceof OceanSeaMonsterEntity
                || entity instanceof OceanTitanEntity;
    }
}
