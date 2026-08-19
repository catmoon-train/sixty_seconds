package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public interface AttackEntityCallback {
    Event<AttackEntityCallback> EVENT = new Event<>();

    InteractionResult interact(Player player, Level world, InteractionHand hand, Entity entity,
            @Nullable EntityHitResult hitResult);
}
