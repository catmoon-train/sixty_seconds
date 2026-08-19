package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public interface UseBlockCallback {
    Event<UseBlockCallback> EVENT = new Event<>();

    InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult);
}
