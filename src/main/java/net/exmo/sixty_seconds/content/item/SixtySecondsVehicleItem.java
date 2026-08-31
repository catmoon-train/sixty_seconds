package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.content.entity.SixtySecondsVehicleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 载具物品（摩托车/小汽车）：对地面右键放置对应载具实体（参照 WheelchairItem 的放置流程）。
 * 载具需要加注燃料（燃料罐/柴油罐）才能行驶，见 {@link SixtySecondsVehicleEntity}。
 */
public class SixtySecondsVehicleItem extends Item {

    private final Supplier<EntityType<SixtySecondsVehicleEntity>> typeSupplier;
    /** 加油提示翻译键（摩托车=燃油罐，小汽车=柴油罐）。 */
    private final String refuelTooltipKey;

    public SixtySecondsVehicleItem(Properties properties,
            Supplier<EntityType<SixtySecondsVehicleEntity>> typeSupplier,
            String refuelTooltipKey) {
        super(properties);
        this.typeSupplier = typeSupplier;
        this.refuelTooltipKey = refuelTooltipKey;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = ctx.getItemInHand();
        BlockPos clicked = ctx.getClickedPos();
        Direction face = ctx.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        BlockPos spawnPos = state.getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(face);
        SixtySecondsVehicleEntity entity = typeSupplier.get().create(serverLevel);
        if (entity == null) {
            return InteractionResult.PASS;
        }
        entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        if (ctx.getPlayer() != null) {
            entity.setYRot(ctx.getPlayer().getYRot());
        }
        serverLevel.addFreshEntity(entity);
        stack.consume(1, ctx.getPlayer());
        level.gameEvent(ctx.getPlayer(), GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(refuelTooltipKey).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
