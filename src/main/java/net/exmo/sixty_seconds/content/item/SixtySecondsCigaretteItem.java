package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 香烟 / 雪茄：右键使用。
 * <ul>
 *   <li>香烟（3 耐久）：反胃 + 失明 10 秒，恢复 12 理智，耐久 -1，60 秒冷却</li>
 *   <li>雪茄（5 耐久）：反胃 + 失明 5 秒，恢复 18 理智，耐久 -1，60 秒冷却</li>
 * </ul>
 */
public class SixtySecondsCigaretteItem extends Item {

    private final int nauseaSeconds;
    private final int sanityRestore;

    public SixtySecondsCigaretteItem(Properties properties, int nauseaSeconds, int sanityRestore) {
        super(properties);
        this.nauseaSeconds = nauseaSeconds;
        this.sanityRestore = sanityRestore;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        if (!SixtySecondsMod.isActive(level)) {
            return InteractionResultHolder.pass(stack);
        }
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(serverPlayer);
        if (stats.monster || stats.downed) {
            return InteractionResultHolder.pass(stack);
        }

        // 获得反胃 + 失明
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nauseaSeconds * 20, 0, false, false, true));
        serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, nauseaSeconds * 20, 0, false, false, true));

        // 恢复理智
        int before = stats.sanity;
        stats.sanity = Mth.clamp(stats.sanity + sanityRestore, 0, stats.sanityMax);
        if (stats.sanity != before) {
            stats.sync();
        }

        // 烟雾粒子 + 音效
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                serverPlayer.getX(), serverPlayer.getEyeY() - 0.3, serverPlayer.getZ(),
                10, 0.15, 0.05, 0.15, 0.01);
        serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.FIRE_AMBIENT,
                SoundSource.PLAYERS, 0.6F, 0.9F);

        serverPlayer.displayClientMessage(Component.translatable(
                "message.sixty_seconds.sixty_seconds.smoke_used", sanityRestore)
                .withStyle(ChatFormatting.GREEN), true);

        // 冷却 60 秒 + 掉耐久（创造模式不消耗）
        serverPlayer.getCooldowns().addCooldown(this, 20 * 60);
        if (!serverPlayer.isCreative()) {
            stack.hurtAndBreak(1, serverPlayer, serverPlayer.getEquipmentSlotForItem(stack));
        }
        return InteractionResultHolder.success(stack);
    }
}
