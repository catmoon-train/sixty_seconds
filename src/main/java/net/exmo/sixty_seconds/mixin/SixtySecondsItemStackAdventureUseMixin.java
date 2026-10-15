package net.exmo.sixty_seconds.mixin;

import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复冒险模式下无法放置/使用本模组功能物品的问题。
 * <p>
 * 1.21.1 原版在 {@link ItemStack#useOn} 入口做冒险模式门禁：
 * <pre>
 * if (player != null &amp;&amp; !player.getAbilities().mayBuild
 *         &amp;&amp; !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(...))) {
 *     return InteractionResult.PASS;   // 直接 PASS，item.useOn / canPlace 永远走不到
 * }
 * </pre>
 * 冒险模式 {@code mayBuild == false}，而本模组物品没有 {@code can_place_on} 数据组件，
 * 于是 {@code SixtySecondsPlaceableBlockItem#canPlace} 里「白色混凝土上可放置」的适配
 * 在冒险模式下根本不会被执行。
 * <p>
 * 这里对实现了 {@link AdventureUsable} 的物品跳过该门禁（复刻原版 else 分支），
 * 是否真正允许放置/使用仍由物品自身逻辑决定：
 * <ul>
 *   <li>{@code SixtySecondsPlaceableBlockItem}：受 {@code SixtySecondsBuildRules}
 *       限制（白色混凝土标记上方 2 格内）</li>
 *   <li>扳手/火把等：各自的 {@code useOn} / {@code canPlace} 逻辑</li>
 * </ul>
 * 生存模式 {@code mayBuild == true} 不受影响；旁观模式服务端根本不会调用到此方法。
 */
@Mixin(ItemStack.class)
public abstract class SixtySecondsItemStackAdventureUseMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void sixtySeconds$allowAdventureUsableItems(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null || player.getAbilities().mayBuild) {
            return; // 非冒险模式（或玩家不存在）：走原版逻辑
        }
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof AdventureUsable)) {
            return; // 非本模组冒险可用物品：保持原版限制
        }
        // 复刻原版 else 分支：调用 item.useOn 并正常统计
        Item item = self.getItem();
        InteractionResult result = item.useOn(context);
        if (result.indicateItemUse()) {
            player.awardStat(Stats.ITEM_USED.get(item));
        }
        cir.setReturnValue(result);
    }
}
