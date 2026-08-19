package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface UseItemCallback {
    Event<UseItemCallback> EVENT = new Event<>();

    InteractionResultHolder<ItemStack> interact(Player player, Level world, InteractionHand hand);
}
