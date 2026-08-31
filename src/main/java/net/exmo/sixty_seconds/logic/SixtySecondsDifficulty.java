package net.exmo.sixty_seconds.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsDayCycle;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.entity.OceanCreatureEntity;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 末日60秒的<b>难度系统</b>（0~10，默认 0 = 当前各项指标）。难度随等级线性恶化：
 * <ul>
 *   <li>饥饿 / 口渴 / 理智下降倍率：难度 0 = ×1.0 → 难度 6 起封顶 ×2.0；</li>
 *   <li>污染值提升倍率：难度 0 = ×1.0 → 难度 8 起封顶 ×2.5；</li>
 *   <li>怪物攻击 / 血量倍率：难度 0 = ×1.0 → 难度 10 = ×3.2；</li>
 *   <li>生病概率加成：每级 +1%，难度 10 = +10%；</li>
 *   <li>晴朗概率：难度 0 = 50% → 难度 10 = 20%。</li>
 * </ul>
 * 难度按<b>服务器存档</b>持久化在 {@code sixty_seconds_difficulty.json}（与 NPC 商店表同类做法），
 * 换图沿用、重启不丢。
 */
public final class SixtySecondsDifficulty {

    public static final int MIN = 0;
    public static final int MAX_LEVEL = 10;

    // ── 倍率曲线锚点（改动这里即可整体调参）──────────────────────────────
    /** 状态下降倍率达到上限的难度等级。 */
    public static final int DRAIN_CAP_LEVEL = 6;
    /** 状态下降倍率上限。 */
    public static final double DRAIN_MAX_MULT = 2.0;
    /** 污染增速倍率达到上限的难度等级。 */
    public static final int POLLUTION_CAP_LEVEL = 8;
    /** 污染增速倍率上限。 */
    public static final double POLLUTION_MAX_MULT = 2.5;
    /** 怪物属性倍率上限（在 {@link #MAX_LEVEL} 达成）。 */
    public static final double MOB_STAT_MAX_MULT = 3.2;
    /** 每级额外生病概率。 */
    public static final double SICK_CHANCE_PER_LEVEL = 0.01;
    /** 难度 0 的晴朗概率。 */
    public static final double CLEAR_CHANCE_BASE = 0.5;
    /** 难度 10 的晴朗概率。 */
    public static final double CLEAR_CHANCE_MIN = 0.2;

    private static final String FILE_NAME = "sixty_seconds_difficulty.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, Integer> CACHE = new WeakHashMap<>();
    /** 实体持久化数据里记录「已施加的怪物属性倍率」，避免重复/反向缩放。 */
    private static final String KEY_APPLIED_MULT = "sixty_seconds_difficulty_mult";
    /** 实体持久化数据里记录的「未缩放」原始最大生命基准。 */
    private static final String KEY_BASE_HP = "sixty_seconds_difficulty_base_hp";
    /** 实体持久化数据里记录的「未缩放」原始攻击力基准。 */
    private static final String KEY_BASE_ATK = "sixty_seconds_difficulty_base_atk";

    private SixtySecondsDifficulty() {
    }

    /** 存档数据结构（Gson 直接序列化）。 */
    public static final class Data {
        public int difficulty = MIN;
    }

    // ── 读取 / 写入 ────────────────────────────────────────────────────

    public static int get(ServerLevel level) {
        if (level == null) {
            return MIN;
        }
        int diff = CACHE.computeIfAbsent(level.getServer(), SixtySecondsDifficulty::loadOrDefault);
        // 难度联动：白天时长随难度压缩（夜晚相应变长）。随 Data 同步到客户端，
        // 在取值处惰性同步，确保加载/改难度后日内相位正确。
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        data.daytimeTicks = (int) (SixtySecondsDayCycle.DAYTIME_TICKS - daytimeShortenTicks(diff));
        return diff;
    }

    public static int get(Level level) {
        return level instanceof ServerLevel server ? get(server) : MIN;
    }

    public static int get(MinecraftServer server) {
        if (server == null) {
            return MIN;
        }
        return CACHE.computeIfAbsent(server, SixtySecondsDifficulty::loadOrDefault);
    }

    /** 设置难度并落盘，同时让已加载的怪物立刻按新难度重算属性。 */
    public static void set(ServerLevel level, int value) {
        if (level == null) {
            return;
        }
        int v = Mth.clamp(value, MIN, MAX_LEVEL);
        CACHE.put(level.getServer(), v);
        writeFile(level, v);
        reapplyToAll(level);
    }

    private static Path path(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static int loadOrDefault(MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    return Mth.clamp(data.difficulty, MIN, MAX_LEVEL);
                }
            } catch (IOException | RuntimeException e) {
                SixtySeconds.LOGGER.warn("[60s] Failed to read {}: {}", FILE_NAME, e.toString());
            }
        }
        return MIN;
    }

    private static void writeFile(ServerLevel level, int value) {
        Path path = path(level);
        Data data = new Data();
        data.difficulty = value;
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            SixtySeconds.LOGGER.warn("[60s] Failed to write {}: {}", FILE_NAME, e.toString());
        }
    }

    // ── 各项倍率 ──────────────────────────────────────────────────────

    /** 饥饿 / 口渴 / 理智的下降倍率：难度 0 = ×1.0，难度 {@link #DRAIN_CAP_LEVEL} 起 = ×{@link #DRAIN_MAX_MULT}。 */
    public static double drainMultiplier(int level) {
        int l = Mth.clamp(level, MIN, MAX_LEVEL);
        double t = Math.min(l, DRAIN_CAP_LEVEL) / (double) DRAIN_CAP_LEVEL;
        return 1.0 + (DRAIN_MAX_MULT - 1.0) * t;
    }

    /** 污染增速倍率：难度 0 = ×1.0，难度 {@link #POLLUTION_CAP_LEVEL} 起 = ×{@link #POLLUTION_MAX_MULT}。 */
    public static double pollutionMultiplier(int level) {
        int l = Mth.clamp(level, MIN, MAX_LEVEL);
        double t = Math.min(l, POLLUTION_CAP_LEVEL) / (double) POLLUTION_CAP_LEVEL;
        return 1.0 + (POLLUTION_MAX_MULT - 1.0) * t;
    }

    /** 怪物攻击 / 血量倍率：难度 0 = ×1.0，难度 {@link #MAX_LEVEL} = ×{@link #MOB_STAT_MAX_MULT}。 */
    public static double mobStatMultiplier(int level) {
        int l = Mth.clamp(level, MIN, MAX_LEVEL);
        return 1.0 + (MOB_STAT_MAX_MULT - 1.0) * (l / (double) MAX_LEVEL);
    }

    /** 额外生病概率：每级 +{@link #SICK_CHANCE_PER_LEVEL}，难度 10 = +10%。 */
    public static double sickChanceBonus(int level) {
        return SICK_CHANCE_PER_LEVEL * Mth.clamp(level, MIN, MAX_LEVEL);
    }

    /** 晴朗天气概率：难度 0 = 50%，难度 {@link #MAX_LEVEL} = 20%。 */
    public static double clearWeatherChance(int level) {
        int l = Mth.clamp(level, MIN, MAX_LEVEL);
        return CLEAR_CHANCE_BASE
                + (CLEAR_CHANCE_MIN - CLEAR_CHANCE_BASE) * (l / (double) MAX_LEVEL);
    }

    /**
     * 污染增量按难度放大（只放大正向增量，治疗/净化类负值原样返回）。
     * 小数部分按概率进位，避免小基数在低难度下被四舍五入吞成 0。
     */
    public static int scalePollutionGain(Level level, int amount) {
        if (amount <= 0 || level == null) {
            return amount;
        }
        double exact = amount * pollutionMultiplier(get(level));
        int result = (int) exact;
        if (level.getRandom().nextDouble() < exact - result) {
            result++;
        }
        return Math.max(1, result);
    }

    /** 怪物造成的健康伤害按难度放大（{@code attacker} 为 null 时按所在世界难度计算）。 */
    public static int scaleInjury(Entity attacker, int base) {
        if (base <= 0) {
            return base;
        }
        Level level = attacker == null ? null : attacker.level();
        if (level == null) {
            return base;
        }
        double exact = base * mobStatMultiplier(get(level));
        int result = (int) exact;
        if (level.getRandom().nextDouble() < exact - result) {
            result++;
        }
        return Math.max(1, result);
    }

    // ── 怪物属性缩放 ───────────────────────────────────────────────────

    /** 是否参与难度缩放的敌对生物（自研陆地怪 / Boss / 全部海洋生物）。 */
    private static boolean isScaledMob(Entity entity) {
        return entity instanceof SixtySecondsMonsterEntity || entity instanceof OceanCreatureEntity;
    }

    /**
     * 把当前难度的属性倍率施加到怪物身上。
     * <b>以「原始基准值」为锚点</b>而非对当前 base 做增量缩放：Boss 的 {@code applyBossLevel}
     * 与读档都会把 base 重设为未缩放值，增量缩放会被这类重设抵消甚至累积放大。
     * 在实体加入世界时、Boss 定级后、以及难度变更时调用（幂等）。
     */
    public static void applyToMob(LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel level) || !isScaledMob(mob)) {
            return;
        }
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (hp == null) {
            return;
        }
        double target = mobStatMultiplier(get(level));
        CompoundTag data = mob.getPersistentData();

        // 首次记录未缩放的原始基准（若此前已被缩放过，用记录的倍率还原）
        final double baseHp;
        final double baseAtk;
        if (data.contains(KEY_BASE_HP)) {
            baseHp = data.getDouble(KEY_BASE_HP);
            baseAtk = data.getDouble(KEY_BASE_ATK);
        } else {
            double applied = data.contains(KEY_APPLIED_MULT) ? data.getDouble(KEY_APPLIED_MULT) : 1.0D;
            baseHp = hp.getBaseValue() / applied;
            baseAtk = atk == null ? 0.0D : atk.getBaseValue() / applied;
            data.putDouble(KEY_BASE_HP, baseHp);
            data.putDouble(KEY_BASE_ATK, baseAtk);
        }
        data.putDouble(KEY_APPLIED_MULT, target);

        double oldMax = mob.getMaxHealth();
        hp.setBaseValue(Math.max(1.0D, baseHp * target));
        if (atk != null) {
            atk.setBaseValue(Math.max(0.1D, baseAtk * target));
        }
        // 当前生命同比例缩放：新生成的怪 health==maxHealth ⇒ 等价于满血；已受伤的怪保留受伤比例
        if (oldMax > 0.0F) {
            float scaled = (float) (mob.getHealth() * (mob.getMaxHealth() / oldMax));
            mob.setHealth(Math.max(1.0F, Math.min((float) mob.getMaxHealth(), scaled)));
        }
    }

    /**
     * 丢弃已缓存的「原始基准」，以怪物<b>当前</b>的属性 base 为新基准重算难度缩放。
     * 用于 Boss 的 {@code applyBossLevel} 之类会重设 base 的场合，避免沿用旧基准。
     */
    public static void reapply(LivingEntity mob) {
        CompoundTag data = mob.getPersistentData();
        data.remove(KEY_BASE_HP);
        data.remove(KEY_BASE_ATK);
        data.remove(KEY_APPLIED_MULT);
        applyToMob(mob);
    }

    /** 难度变更后，把世界中已加载的怪物重算一遍（先收集再处理，避免并发修改实体存储）。 */
    private static void reapplyToAll(ServerLevel level) {
        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && isScaledMob(living)) {
                mobs.add(living);
            }
        }
        for (LivingEntity mob : mobs) {
            applyToMob(mob);
        }
    }

    // ───────────────────────────────────────────────────────────────────
    //  难度联动：商人 / NPC 价格
    // ───────────────────────────────────────────────────────────────────
    /** 售价倍率（玩家向商人购买时支付）：难度 0 = ×1，难度 10 = ×3。 */
    public static float npcSellPriceMultiplier(int level) {
        return 1.0F + 2.0F * clamp01(level) / 10.0F;
    }

    /** 收购价倍率（商人向玩家回收时支付）：难度 0 = ×1，难度 10 = ×(1/3)。与售价互逆。 */
    public static float npcBuyPriceMultiplier(int level) {
        return 1.0F / npcSellPriceMultiplier(level);
    }

    /** 商店每日库存回补倍率（按难度下浮）：难度 0 = ×1，难度 10 = ×0.5。 */
    public static float npcRestockMultiplier(int level) {
        return 1.0F - 0.5F * clamp01(level) / 10.0F;
    }

    // ───────────────────────────────────────────────────────────────────
    //  难度联动：白天时长
    // ───────────────────────────────────────────────────────────────────
    /** 白天被压缩的刻数（最多 1.5 分钟 = 1800 刻），难度 10 达最大。夜晚相应变长。 */
    public static long daytimeShortenTicks(int level) {
        return Math.round(1800.0 * clamp01(level) / 10.0);
    }

    // ───────────────────────────────────────────────────────────────────
    //  难度联动：被袭击 / 夜袭
    // ───────────────────────────────────────────────────────────────────
    /** 被袭击（夜袭触发）概率倍率：难度 10 = ×2.2。 */
    public static float raidChanceMultiplier(int level) {
        return 1.0F + 1.2F * clamp01(level) / 10.0F;
    }

    /** 夜袭刷怪数量 / 频率倍率：难度 10 = ×1.5。 */
    public static float nightSpawnMultiplier(int level) {
        return 1.0F + 0.5F * clamp01(level) / 10.0F;
    }

    // ───────────────────────────────────────────────────────────────────
    //  难度联动：前几天安全 / 降难缓冲
    // ───────────────────────────────────────────────────────────────────
    /**
     * 前几天刷怪难度爬升窗口被压缩的额外天数：
     * 难度 <5 不压缩（保留完整安全缓冲）；难度 5 起逐步压缩；难度 8 时压缩满额 → 无缓冲。
     */
    public static int graceCompressDays(int level) {
        if (level < 5) return 0;
        return (int) Math.round(7.0 * (level - 4) / 4.0);
    }

    /** 前几天安全缓冲 / 降难机制是否完全取消（难度 ≥8）。 */
    public static boolean graceDisabled(int level) {
        return level >= 8;
    }

    // ───────────────────────────────────────────────────────────────────
    //  难度联动：Boss 提前出现
    // ───────────────────────────────────────────────────────────────────
    /** Boss 出现提前天数（0..3）：难度 5 起，难度 8 达最大 3 天。 */
    public static int bossEarlyDayShift(int level) {
        if (level < 5) return 0;
        return (int) Math.round(3.0 * (level - 5) / 3.0);
    }

    private static float clamp01(float v) {
        return v < 0.0F ? 0.0F : Math.min(1.0F, v);
    }
}
