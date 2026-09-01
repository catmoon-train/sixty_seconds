package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.exmo.sixty_seconds.bridge.fabric.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * 服务端逐 tick 推进的负重系统。
 *
 * <p>仅在 {@link SixtySecondsMod#isActive()} 为真且配置 {@code enabled} 时生效；对创造/旁观模式玩家豁免。
 * 超重时默认施加 {@link MobEffects#MOVEMENT_SLOWDOWN}（移动减速）作为负面效果。
 */
public final class SixtySecondsWeightSystem {

    private SixtySecondsWeightSystem() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(SixtySecondsWeightSystem::tick);
    }

    private static void tick(ServerLevel level) {
        if (!SixtySecondsMod.isActive(level)) return;
        SixtySecondsWeightConfig cfg = SixtySecondsWeightConfigStore.get(level.getServer());
        if (cfg == null || !cfg.enabled) {
            clearEffects(level);
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            double load = SixtySecondsWeightCalc.computeLoad(player, cfg);
            applyPenalty(player, load, cfg);
        }
    }

    private static void clearEffects(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
    }

    private static void applyPenalty(ServerPlayer player, double load, SixtySecondsWeightConfig cfg) {
        if (!cfg.speedPenaltyEnabled || load <= cfg.maxLoad) {
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            return;
        }
        double excess = load - cfg.maxLoad;
        int level_ = (int) Math.floor(excess / Math.max(1e-4, cfg.speedPenaltyPerLoad));
        level_ = Math.max(0, Math.min(10, level_));
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, 60, level_, false, false, true));
    }
}
