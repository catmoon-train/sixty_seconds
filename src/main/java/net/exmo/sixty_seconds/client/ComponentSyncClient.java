package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.network.ComponentSyncS2CPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * {@link ComponentSyncS2CPacket} 的客户端接收逻辑。
 * <p>
 * 从 payload 类中移出：payload 在专用服务端注册时也会被加载，
 * 内嵌客户端接收逻辑（引用 {@code Minecraft} 等客户端类）会导致
 * {@code RuntimeDistCleaner} dist 崩溃。接收器由 {@code SixtySecondsClient}
 * 通过 {@code ClientPlayNetworking.registerGlobalReceiver} 注册。
 */
public final class ComponentSyncClient {
    private ComponentSyncClient() {
    }

    public static void handle(ComponentSyncS2CPacket payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        ComponentKey<?> key = ComponentKey.byId(ResourceLocation.parse(payload.keyId()));
        if (key == null) {
            return;
        }
        // 世界组件（如 SixtySecGameWorldComponent / AreasWorldComponent）挂在 Level 上，
        // 同步包里携带的是收包玩家的 UUID，但客户端必须挂到 ClientLevel，不能挂到玩家。
        Object provider;
        if (key.isWorldAttached()) {
            provider = mc.level;
        } else {
            // 玩家/实体组件：目标玩家不在本客户端世界里时<b>直接跳过</b>。
            // 旧实现此处回退到 mc.level（ClientLevel），而这类组件并未在 ClientLevel 注册，
            // CCA 会抛 IllegalStateException: Cannot create ... for ClientLevel；
            // 异常冒泡到 NetworkRegistry 就变成每收到一个同步包刷一条 ERROR。
            Player target = mc.level.getPlayerByUUID(payload.entityId());
            if (target == null) {
                return;
            }
            provider = target;
        }
        try {
            key.applyFromPacket(provider, payload.data(), mc.player);
        } catch (Exception | LinkageError e) {
            // 兜底：组件未在客户端注册（仅服务端组件等）时静默跳过，不让异常冒泡刷 ERROR
            net.exmo.sixty_seconds.SixtySeconds.LOGGER.debug(
                    "[60s] component_sync not applied for {}: {}", payload.keyId(), e);
        }
    }
}
