package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.entity.SixtySecondsNpcEntity;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 雇佣军人（服务端）：花代币让一名中立军人跟随作战一段时间。
 * 雇佣期间解除其驻守限制（见 {@code SixtySecondsNpcEntity.startHire}），到期自动回巡逻点。
 */
public final class SixtySecondsNpcHire {
    private SixtySecondsNpcHire() {
    }

    /** 雇佣结算：全量重校验（是军人/未被雇/白天/资金足），扣款成功才生效。 */
    public static void hire(ServerPlayer player, SixtySecondsNpcEntity npc) {
        ServerLevel level = player.serverLevel();
        if (!SixtySecondsMod.isActive(level)
                || npc.getVariant() != SixtySecondsNpcEntity.Variant.SOLDIER) {
            return;
        }
        if (SixtySecondsState.get(level).phase != SixtySecondsPhase.DAY) {
            return;
        }
        if (npc.isHired()) {
            player.displayClientMessage(Component.translatable(
                    "message.sixty_seconds.sixty_seconds.npc.already_hired")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        int cost = SixtySecondsBalance.NPC_HIRE_COST;
        if (SixtySecondsNpcShop.totalFunds(player) < cost) {
            player.displayClientMessage(Component.translatable(
                    "message.sixty_seconds.sixty_seconds.npc.no_tokens", cost)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!SixtySecondsNpcShop.spend(player, cost)) {
            return;
        }
        npc.startHire(player, SixtySecondsBalance.NPC_HIRE_TICKS);
        level.playSound(null, npc.getX(), npc.getY(), npc.getZ(),
                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8F, 0.9F);
        player.displayClientMessage(Component.translatable(
                "message.sixty_seconds.sixty_seconds.npc.hired",
                npc.getDisplayName(), SixtySecondsBalance.NPC_HIRE_TICKS / 20)
                .withStyle(ChatFormatting.GREEN), true);
    }
}
