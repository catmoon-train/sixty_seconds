package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.content.item.component.SixtySecWritableBookContent;
import net.exmo.sixty_seconds.index.SixtySecDataComponentTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TooltipContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * 便签：右键打开纸张书写页，可打字并保存内容（复用报纸的书写 GUI 与 WRITABLE_BOOK_CONTENT 组件）。
 */
public class SixtySecondsNoteItem extends Item {
    @Nullable
    public static BiFunction<ItemStack, InteractionHand, Boolean> runner;

    public SixtySecondsNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (usedHand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            var r = runner;
            if (r != null) {
                return r.apply(itemStack, usedHand) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipComponents, flag);
        SixtySecWritableBookContent content = stack.get(SixtySecDataComponentTypes.WRITABLE_BOOK_CONTENT);
        if (content != null && !content.pages().isEmpty()) {
            long chars = content.pages().stream()
                    .mapToLong(f -> {
                        String s = f.raw();
                        return s == null ? 0 : s.length();
                    })
                    .sum();
            if (chars > 0) {
                tooltipComponents.add(Component.translatable("item.sixty_seconds.sixty_seconds_note.written",
                                Component.literal(String.valueOf(chars)))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
