package net.exmo.sixty_seconds.content.block;

import com.mojang.serialization.MapCodec;
import net.exmo.sixty_seconds.SixtySecondsDayCycle;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.content.block_entity.SixtySecondsRatHoleBlockEntity;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModItems;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 老鼠洞方块
 * 游戏进行中（每天）右键可掏出一些基础资源，也有可能掏不到（空洞）。
 * 通过方块实体记录上一次掏洞的游戏时刻，实现「每个游戏日只能掏一次」的冷却。
 *
 * <p>可能的结果（加权随机）：
 * <ul>
 *   <li>空   30%：洞里空空如也，只有灰尘</li>
 *   <li>废铁丝+破布 30%：基础制作材料</li>
 *   <li>小瓶清水 20%</li>
 *   <li>肉罐头 12%：稀有惊喜</li>
 *   <li>脏水   8%：老鼠打翻的浑水（玩笑）</li>
 * </ul>
 */
public class SixtySecondsRatHoleBlock extends BaseEntityBlock {
    private static final MapCodec<SixtySecondsRatHoleBlock> CODEC = simpleCodec(SixtySecondsRatHoleBlock::new);

    public SixtySecondsRatHoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SixtySecondsRatHoleBlockEntity(pos, state);
    }

    // ── 右键交互 ──

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        loot(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        loot(level, pos, player);
        return ItemInteractionResult.SUCCESS;
    }

    private void loot(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverLevel.getBlockEntity(pos) instanceof SixtySecondsRatHoleBlockEntity hole)) {
            return;
        }

        SixtySecondsState.Data data = SixtySecondsState.get(serverLevel);
        if (data.phase == SixtySecondsPhase.INACTIVE || data.phase == SixtySecondsPhase.FINISHED) {
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.rat_hole_inactive"), true);
            return;
        }

        long now = serverLevel.getGameTime();
        // lastLootTick 默认为 0（从未掏过）。只有真正掏过（>0）且仍处于同一游戏日内才进入冷却，
        // 避免第一天尚未掏过却被误判为「已掏过」。
        if (!serverPlayer.isCreative()
                && hole.getLastLootTick() > 0
                && now - hole.getLastLootTick() < SixtySecondsDayCycle.DAY_TOTAL_TICKS) {
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.rat_hole_cooldown"), true);
            return;
        }

        int roll = serverLevel.random.nextInt(100); // 0..99
        int scrap = 0, cloth = 0, water = 0, canned = 0, dirty = 0;
        String msgKey;
        if (roll < 30) {
            msgKey = "rat_hole_empty";
        } else if (roll < 60) {
            scrap = 2 + serverLevel.random.nextInt(3); // 2..4
            cloth = 1;
            msgKey = "rat_hole_scrap";
        } else if (roll < 80) {
            water = 1;
            msgKey = "rat_hole_water";
        } else if (roll < 92) {
            canned = 1;
            msgKey = "rat_hole_canned";
        } else {
            dirty = 1;
            msgKey = "rat_hole_dirty";
        }

        give(player, new ItemStack(ModItems.SIXTY_SECONDS_SCRAP, scrap));
        give(player, new ItemStack(ModItems.SIXTY_SECONDS_CLOTH_ROLL, cloth));
        give(player, new ItemStack(ModItems.SIXTY_SECONDS_WATER_SMALL, water));
        give(player, new ItemStack(ModItems.SIXTY_SECONDS_CANNED_FOOD, canned));
        give(player, new ItemStack(ModItems.SIXTY_SECONDS_DIRTY_WATER, dirty));

        player.displayClientMessage(
                Component.translatable("message.sixty_seconds.sixty_seconds." + msgKey), true);

        // 无论掏到与否，都消耗掉今天的次数（「每天早上」一次机会）
        hole.setLastLootTick(now);
    }

    private static void give(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
