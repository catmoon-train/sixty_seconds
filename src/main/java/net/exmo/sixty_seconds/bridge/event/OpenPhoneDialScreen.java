package net.exmo.sixty_seconds.bridge.event;

import net.exmo.sixty_seconds.bridge.fabric.Event;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端打开电话拨号界面的钩子。
 * <p>
 * {@code SixtySecondsPhoneItem} 是公共类（专用服务端也会加载），不能直接引用
 * {@code PhoneDialScreen} / {@code Minecraft} 等客户端类——否则服务端加载该物品类时
 * 会触发 {@code RuntimeDistCleaner} 崩溃。因此界面打开逻辑由客户端
 * （{@code SixtySecondsClient}）注册到这里，公共类只调用 invoker。
 * 未注册监听时 invoker 为无操作 lambda，服务端调用安全。
 */
public interface OpenPhoneDialScreen {
    Event<OpenPhoneDialScreen> EVENT = new Event<>(listeners -> (stack, hand) -> {
        for (OpenPhoneDialScreen listener : listeners) {
            listener.open(stack, hand);
        }
    });

    void open(ItemStack stack, InteractionHand hand);
}
