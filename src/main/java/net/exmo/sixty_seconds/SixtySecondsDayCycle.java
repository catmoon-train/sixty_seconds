package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.state.SixtySecondsState;

/**
 * 日内子相位时间轴：清晨 1 分钟 → 白天 6 分钟 → 晚上 2.5 分钟（其中最后 45 秒为睡觉时间），
 * 共 9.5 分钟一天。全部由 {@code phaseEndTick - gameTime}（剩余 tick）推算，不额外计时。
 * <p>
 * 白天时长会随难度压缩（见 {@link SixtySecondsDifficulty#daytimeShortenTicks}）：{@code Data#daytimeTicks}
 * 由难度同步，夜晚相应变长。整天总时长不变。
 */
public final class SixtySecondsDayCycle {
    public static final int MORNING_TICKS = 20 * 60;        // 清晨 1 分钟（禁 PvP）
    public static final int DAYTIME_TICKS = 20 * 60 * 6;    // 白天 6 分钟（难度基准值）
    public static final int NIGHT_TICKS = 20 * 150;         // 晚上 2.5 分钟（1 分钟让给白天）
    public static final int SLEEP_WINDOW_TICKS = 20 * 45;   // 晚上最后 45 秒 = 睡觉时间
    public static final int DAY_TOTAL_TICKS = MORNING_TICKS + DAYTIME_TICKS + NIGHT_TICKS; // 9.5 分钟

    public enum SubPhase {
        MORNING, DAYTIME, NIGHT;

        public String translationKey() {
            return "hud.sixty_seconds.sixty_seconds.subphase." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private SixtySecondsDayCycle() {
    }

    /** 当前夜晚时长（白天被压缩后夜晚变长，整天总时长不变）。 */
    private static long nightTicks(SixtySecondsState.Data data) {
        return DAY_TOTAL_TICKS - MORNING_TICKS - data.daytimeTicks;
    }

    /** 本日已过 tick（0..DAY_TOTAL_TICKS）；非 DAY 相位返回 0。 */
    public static long elapsed(SixtySecondsState.Data data, long gameTime) {
        if (data.phase != SixtySecondsPhase.DAY) {
            return 0L;
        }
        long remaining = data.phaseEndTick - gameTime;
        return Math.max(0L, Math.min(DAY_TOTAL_TICKS, DAY_TOTAL_TICKS - remaining));
    }

    public static SubPhase subPhase(SixtySecondsState.Data data, long gameTime) {
        long elapsed = elapsed(data, gameTime);
        if (elapsed < MORNING_TICKS) {
            return SubPhase.MORNING;
        }
        if (elapsed < MORNING_TICKS + data.daytimeTicks) {
            return SubPhase.DAYTIME;
        }
        return SubPhase.NIGHT;
    }

    public static boolean isNight(SixtySecondsState.Data data, long gameTime) {
        return data.phase == SixtySecondsPhase.DAY && subPhase(data, gameTime) == SubPhase.NIGHT;
    }

    /** 是否处于睡觉时间（晚上最后 45 秒）。 */
    public static boolean isSleepWindow(SixtySecondsState.Data data, long gameTime) {
        if (data.phase != SixtySecondsPhase.DAY) {
            return false;
        }
        long remaining = data.phaseEndTick - gameTime;
        return remaining > 0 && remaining <= SLEEP_WINDOW_TICKS;
    }

    /** 当前子相位剩余 tick（用于时钟显示）。 */
    public static long subPhaseRemaining(SixtySecondsState.Data data, long gameTime) {
        long elapsed = elapsed(data, gameTime);
        if (elapsed < MORNING_TICKS) {
            return MORNING_TICKS - elapsed;
        }
        if (elapsed < MORNING_TICKS + data.daytimeTicks) {
            return MORNING_TICKS + data.daytimeTicks - elapsed;
        }
        return DAY_TOTAL_TICKS - elapsed;
    }

    // ── 基于「剩余 tick」的推算（客户端 HUD 用：仅需 phaseEndTick 同步）──────
    // 说明：客户端 HUD 仅用于显示，使用基准白天时长；实际玩法（刷怪/夜袭/Boss/世界时间）
    // 由上面的 data 版本方法驱动，已随难度压缩白天。

    /** 按剩余 tick 推算子相位（客户端显示用，基准白天时长）。 */
    public static SubPhase subPhaseByRemaining(long remaining) {
        long elapsed = Math.max(0L, Math.min(DAY_TOTAL_TICKS, DAY_TOTAL_TICKS - remaining));
        if (elapsed < MORNING_TICKS) {
            return SubPhase.MORNING;
        }
        if (elapsed < MORNING_TICKS + DAYTIME_TICKS) {
            return SubPhase.DAYTIME;
        }
        return SubPhase.NIGHT;
    }

    /** 按剩余 tick 推算当前子相位剩余 tick（客户端显示用）。 */
    public static long subPhaseRemainingByRemaining(long remaining) {
        long elapsed = Math.max(0L, Math.min(DAY_TOTAL_TICKS, DAY_TOTAL_TICKS - remaining));
        if (elapsed < MORNING_TICKS) {
            return MORNING_TICKS - elapsed;
        }
        if (elapsed < MORNING_TICKS + DAYTIME_TICKS) {
            return MORNING_TICKS + DAYTIME_TICKS - elapsed;
        }
        return DAY_TOTAL_TICKS - elapsed;
    }

    public static boolean isSleepWindowByRemaining(long remaining) {
        return remaining > 0 && remaining <= SLEEP_WINDOW_TICKS;
    }

    /**
     * 把世界 dayTime 映射到当前日内子相位（每 tick 调用，平滑推进）：
     * 清晨=日出 23000→1000，白天=1000→13000，晚上=13000→23000——天空亮暗与玩法时段一致。
     * 非 DAY 相位（准备/结算）固定为清晨 1000。
     */
    public static void applyWorldTime(net.minecraft.server.level.ServerLevel level, SixtySecondsState.Data data) {
        long t;
        if (data.phase != SixtySecondsPhase.DAY) {
            t = 1000L;
        } else {
            long elapsed = elapsed(data, level.getGameTime());
            if (elapsed < MORNING_TICKS) {
                t = 23000L + elapsed * 2000L / MORNING_TICKS;
            } else if (elapsed < MORNING_TICKS + data.daytimeTicks) {
                t = 1000L + (elapsed - MORNING_TICKS) * 12000L / data.daytimeTicks;
            } else {
                t = 13000L + (elapsed - MORNING_TICKS - data.daytimeTicks) * 10000L / nightTicks(data);
            }
        }
        level.setDayTime(t % 24000L);
    }

    /** 指定子相位的「本日已过 tick」起点（供时间调整指令用）。 */
    public static int startOf(SubPhase subPhase, SixtySecondsState.Data data) {
        return switch (subPhase) {
            case MORNING -> 0;
            case DAYTIME -> MORNING_TICKS;
            case NIGHT -> MORNING_TICKS + data.daytimeTicks;
        };
    }
}
