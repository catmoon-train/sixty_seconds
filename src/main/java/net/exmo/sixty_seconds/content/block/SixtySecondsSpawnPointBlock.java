package net.exmo.sixty_seconds.content.block;

import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.MapCodec;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 出生点方块
 * 材质/碰撞继承「电脑方块」：透明、不可见、无碰撞体积。
 * 自动生成的庇护所 / 房子不方便用指令设置出生点，故放置此方块即可把所在位置登记为
 * 该建筑的出生点（自动判定其落在「住宅模板」还是「庇护所模板」范围内）。
 */
public class SixtySecondsSpawnPointBlock extends Block {
    public static final MapCodec<SixtySecondsSpawnPointBlock> CODEC = simpleCodec(SixtySecondsSpawnPointBlock::new);

    public SixtySecondsSpawnPointBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    // ── 继承电脑方块的材质：透明、不可见、无碰撞 ──

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    // ── 放置时登记出生点 ──

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SixtySecondsConfigStore.load(serverLevel).ifPresent(cfg -> {
            BoundingBox resBox = cfg.residentialTemplate != null ? cfg.residentialTemplate.toBox() : null;
            BoundingBox shelBox = cfg.shelterTemplate != null ? cfg.shelterTemplate.toBox() : null;
            boolean residential = resBox != null && resBox.isInside(pos);
            boolean shelter = shelBox != null && shelBox.isInside(pos);
            if (!residential && !shelter) {
                if (placer instanceof Player player) {
                    player.displayClientMessage(Component.translatable(
                            "message.sixty_seconds.sixty_seconds.spawn_point_outside_template", pos.toShortString())
                            .withStyle(ChatFormatting.RED), true);
                }
                return;
            }
            if (residential) {
                cfg.residentialSpawn = new SixtySecondsConfig.Vec(pos.getX(), pos.getY(), pos.getZ());
            }
            if (shelter) {
                cfg.shelterSpawn = new SixtySecondsConfig.Vec(pos.getX(), pos.getY(), pos.getZ());
            }
            SixtySecondsConfigStore.save(serverLevel, cfg);
            if (placer instanceof Player player) {
                String which = (residential ? "住宅" : "") + (shelter ? (residential ? "+" : "") + "庇护所" : "");
                player.displayClientMessage(Component.translatable(
                        "message.sixty_seconds.sixty_seconds.spawn_point_set",
                        which, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GREEN), true);
            }
        });
    }
}
