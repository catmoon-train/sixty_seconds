package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.exmo.sixty_seconds.registry.ModSounds;

/**
 * 生病/感染系统：发烧持续掉健康直至死亡（须吃药治愈），伴咳嗽音效与字幕，
 * 每分钟 2% 概率感染身边人。救起后未用医疗包者（{@code recovering}）每 2 分钟 15% 概率生病。
 * <p>
 * 治愈入口 {@link #cure}：接到“吃药/医疗包”物品使用时调用（物品接线为 TODO）。
 * “使用库中中毒的逻辑”的更强复用可切换到 {@code InfectedPlayerComponent}（已含传播）——此处为自足实现。
 */
public final class SixtySecondsSicknessSystem {
    public static final int FEVER_INTERVAL = 125;        // 发烧掉血间隔（约 10 分钟致死，-60%累计）
    public static final int RECOVER_ROLL_INTERVAL = 20 * 120; // 每 2 分钟一次感染判定
    public static final double RECOVER_SICK_CHANCE = 0.15;   // 从 33% 降至 15%
    public static final int SPREAD_INTERVAL = 20 * 60;  // 每分钟一次传播判定
    public static final double SPREAD_CHANCE = 0.02;    // 从 3% 降至 2%
    private static final double SPREAD_RANGE_SQR = 6.0 * 6.0;
    /** 咳嗽判定间隔（每 5 秒掷一次）。 */
    public static final int COUGH_ROLL_INTERVAL = 20 * 5;
    /** 每次判定的咳嗽概率（≈平均每 20 秒咳一次）。 */
    public static final double COUGH_CHANCE = 0.25;

    private SixtySecondsSicknessSystem() {
    }

    public static void makeSick(ServerPlayer player) {
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        if (stats.sick) {
            return;
        }
        stats.sick = true;
        stats.recovering = false;
        stats.sync();
        player.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.sick")
                .withStyle(ChatFormatting.DARK_GREEN), false);
    }

    /** 吃药/医疗包治愈（物品接线 TODO）。 */
    public static void cure(ServerPlayer player) {
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        if (!stats.sick && !stats.recovering) {
            return;
        }
        if (net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.slowHealerFails(player)) {
            player.displayClientMessage(Component.translatable("message.sixty_seconds.trait.slow_healer_fail")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }
        stats.sick = false;
        stats.recovering = false;
        stats.sync();
        player.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.cured")
                .withStyle(ChatFormatting.GREEN), true);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (GameUtils.isPlayerEliminated(player)) {
                continue;
            }
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
            long phase = player.getUUID().getLeastSignificantBits() & 0x3F;

            // 救援后感染风险：每 2 分钟 33%
            if (stats.recovering && (now + phase) % RECOVER_ROLL_INTERVAL == 0) {
                if (level.getRandom().nextDouble() < RECOVER_SICK_CHANCE) {
                    makeSick(player);
                } else {
                    stats.recovering = false;
                    stats.sync();
                }
            }

            if (!stats.sick || stats.downed) {
                continue;
            }
            // 时不时的咳嗽（每 5 秒掷一次，命中则播放 cough.ogg）
            if ((now + phase) % COUGH_ROLL_INTERVAL == 0
                    && level.getRandom().nextDouble() < COUGH_CHANCE) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.COUGH, SoundSource.PLAYERS, 1.3F,
                        1F + (player.getRandom().nextInt(5) - 2) * 0.1F);
                player.displayClientMessage(Component.translatable("message.sixty_seconds.sixty_seconds.coughing")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            // 发烧掉健康直至死亡
            if ((now + phase) % FEVER_INTERVAL == 0 && stats.health > 0) {
                stats.health = Math.max(0, stats.health - 1);
                stats.sync();
                if (stats.health <= 0) {
                    SixtySecondsHealthSystem.onHealthZero(player, false, null);
                    continue;
                }
            }
            // 每分钟 3% 传染身边非病者
            if ((now + phase) % SPREAD_INTERVAL == 0) {
                for (ServerPlayer other : level.players()) {
                    if (other == player || GameUtils.isPlayerEliminated(other)) {
                        continue;
                    }
                    SixtySecondsStatsComponent otherStats = SixtySecondsStatsComponent.KEY.get(other);
                    if (otherStats.sick) {
                        continue;
                    }
                    if (other.distanceToSqr(player) <= SPREAD_RANGE_SQR
                            && level.getRandom().nextDouble() < SPREAD_CHANCE) {
                        makeSick(other);
                    }
                }
            }
        }
    }
}
