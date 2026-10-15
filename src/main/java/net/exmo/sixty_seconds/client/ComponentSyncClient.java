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
            Player target = mc.level.getPlayerByUUID(payload.entityId());
            provider = target != null ? target : mc.level;
        }
        key.applyFromPacket(provider, payload.data(), mc.player);
    }
}
