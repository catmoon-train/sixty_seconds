package net.exmo.sixty_seconds.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Inventory expansion module whose own CONTAINER component stores its items. */
public class SixtySecondsUnlockItem extends Item {
    private final int slots;

    public SixtySecondsUnlockItem(Properties properties, int slots) {
        super(properties);
        this.slots = slots;
    }

    /** Module tier: 1, 2 or 3. */
    public int getUnlockSlots() {
        return slots;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "item.sixty_seconds.sixty_seconds_unlock_slots.desc", slots * 9));
    }
}
