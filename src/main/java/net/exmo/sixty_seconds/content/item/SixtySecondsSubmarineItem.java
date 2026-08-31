package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.entity.SixtySecondsSubmarineEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

/**
 * 潜水艇放置物品：右键水面生成潜水艇。
 * tooltip 描述操作方式（W/S 前后、A/D 转向、空格上浮、左 Ctrl 下潜）。
 */
public class SixtySecondsSubmarineItem extends Item {

    private static final double REACH = 5.0D;

    private final Supplier<EntityType<SixtySecondsSubmarineEntity>> typeSupplier;

    public SixtySecondsSubmarineItem(Properties properties,
            Supplier<EntityType<SixtySecondsSubmarineEntity>> typeSupplier) {
        super(properties);
        this.typeSupplier = typeSupplier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = BlockPos.containing(hit.getLocation());
        if (!level.getFluidState(pos).is(FluidTags.WATER)
                && !level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            player.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.boat_need_water")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        SixtySecondsSubmarineEntity sub = typeSupplier.get().create(serverLevel);
        if (sub == null) {
            return InteractionResultHolder.fail(stack);
        }
        sub.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        sub.setYRot(player.getYRot());
        if (!serverLevel.noCollision(sub, sub.getBoundingBox())) {
            return InteractionResultHolder.fail(stack);
        }
        serverLevel.addFreshEntity(sub);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sixty_seconds.submarine.controls").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sixty_seconds.submarine.refuel").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
