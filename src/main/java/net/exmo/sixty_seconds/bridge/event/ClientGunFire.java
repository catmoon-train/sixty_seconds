package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.exmo.sixty_seconds.content.item.SixtySecondsGunItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 枪械客户端开火钩子：由 {@code SixtySecondsClient} 注册实现（计算准星命中并发送 C2S 包）。
 * <p>
 * {@code SixtySecondsGunItem} 是公共类（专用服务端也会加载），不能直接调用
 * {@code ClientPlayNetworking.send}（客户端发包桥）——服务端代码不应发客户端包。
 * 因此客户端开火逻辑注册到这里，公共物品类只调用 invoker；
 * 未注册监听时 invoker 为无操作 lambda，服务端调用安全。
 */
public interface ClientGunFire {
    Event<ClientGunFire> EVENT = new Event<>(listeners -> (gun, user, stack) -> {
        for (ClientGunFire listener : listeners) {
            listener.fire(gun, user, stack);
        }
    });

    void fire(SixtySecondsGunItem gun, Player user, ItemStack stack);
}
