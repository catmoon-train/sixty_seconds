package net.exmo.sixty_seconds.traits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 类僵尸毁灭工程（Project Zomboid）式的天赋特质注册表。
 * <p>
 * 点数规则：初始 {@link #INITIAL_POINTS}=6；选择正面特质消耗点数（delta 为负），
 * 选择负面特质获得点数（delta 为正）。某玩家的总余额 = 6 + 所有已选特质的 delta 之和，必须 ≥ 0。
 * 特质一旦点亮无法取消。
 */
public final class SixtySecondsTrait {

    private SixtySecondsTrait() {
    }

    public enum Category {
        POSITIVE,
        NEGATIVE
    }

    public static final class TraitDef {
        public final String id;
        public final String nameKey;
        public final String descKey;
        /** 点数增量：正面特质为负（消耗），负面特质为正（获得）。 */
        public final int delta;
        public final Category category;

        public TraitDef(String id, String nameKey, String descKey, int delta, Category category) {
            this.id = id;
            this.nameKey = nameKey;
            this.descKey = descKey;
            this.delta = delta;
            this.category = category;
        }

        public boolean isPositive() {
            return delta < 0;
        }

        /** 绝对值：正面=消耗点数，负面=获得点数。 */
        public int cost() {
            return Math.abs(delta);
        }
    }

    public static final int INITIAL_POINTS = 6;

    public static final List<TraitDef> TRAITS = new ArrayList<>();
    public static final Map<String, TraitDef> BY_ID = new HashMap<>();

    private static void add(String id, String nameKey, String descKey, int delta, Category cat) {
        TraitDef d = new TraitDef(id, nameKey, descKey, delta, cat);
        TRAITS.add(d);
        BY_ID.put(id, d);
    }

    static {
        // ───────────── 正面特质（消耗点数） ─────────────
        add("strong", "trait.strong.name", "trait.strong.desc", -10, Category.POSITIVE);
        add("athletic", "trait.athletic.name", "trait.athletic.desc", -10, Category.POSITIVE);
        add("keen_hearing", "trait.keen_hearing.name", "trait.keen_hearing.desc", -6, Category.POSITIVE);
        add("fast_learner", "trait.fast_learner.name", "trait.fast_learner.desc", -6, Category.POSITIVE);
        add("dexterous", "trait.dexterous.name", "trait.dexterous.desc", -4, Category.POSITIVE);
        add("organized", "trait.organized.name", "trait.organized.desc", -6, Category.POSITIVE);
        add("fit", "trait.fit.name", "trait.fit.desc", -6, Category.POSITIVE);
        add("thick_skinned", "trait.thick_skinned.name", "trait.thick_skinned.desc", -8, Category.POSITIVE);
        add("wakeful", "trait.wakeful.name", "trait.wakeful.desc", -5, Category.POSITIVE);
        add("outdoorsman", "trait.outdoorsman.name", "trait.outdoorsman.desc", -4, Category.POSITIVE);
        add("swimmer", "trait.swimmer.name", "trait.swimmer.desc", -10, Category.POSITIVE);
        add("infection_resistant", "trait.infection_resistant.name", "trait.infection_resistant.desc", -5, Category.POSITIVE);
        add("lucky", "trait.lucky.name", "trait.lucky.desc", -4, Category.POSITIVE);
        add("eagle_eyed", "trait.eagle_eyed.name", "trait.eagle_eyed.desc", -6, Category.POSITIVE);
        add("inconspicuous", "trait.inconspicuous.name", "trait.inconspicuous.desc", -4, Category.POSITIVE);
        add("graceful", "trait.graceful.name", "trait.graceful.desc", -4, Category.POSITIVE);
        add("speed_demon", "trait.speed_demon.name", "trait.speed_demon.desc", -2, Category.POSITIVE);

        // 额外正面（基于原版属性扩展）
        add("herculean", "trait.herculean.name", "trait.herculean.desc", -10, Category.POSITIVE);
        add("iron_will", "trait.iron_will.name", "trait.iron_will.desc", -6, Category.POSITIVE);
        add("bottomless", "trait.bottomless.name", "trait.bottomless.desc", -6, Category.POSITIVE);
        add("camel", "trait.camel.name", "trait.camel.desc", -6, Category.POSITIVE);
        add("night_owl", "trait.night_owl.name", "trait.night_owl.desc", -6, Category.POSITIVE);
        add("marksman", "trait.marksman.name", "trait.marksman.desc", -8, Category.POSITIVE);

        // ───────────── 负面特质（获得点数） ─────────────
        add("smoker", "trait.smoker.name", "trait.smoker.desc", +10, Category.NEGATIVE);
        add("allergic", "trait.allergic.name", "trait.allergic.desc", +6, Category.NEGATIVE);
        add("weak_stomach", "trait.weak_stomach.name", "trait.weak_stomach.desc", +3, Category.NEGATIVE);
        add("short_sighted", "trait.short_sighted.name", "trait.short_sighted.desc", +4, Category.NEGATIVE);
        add("conspicuous", "trait.conspicuous.name", "trait.conspicuous.desc", +4, Category.NEGATIVE);
        add("high_thirst", "trait.high_thirst.name", "trait.high_thirst.desc", +8, Category.NEGATIVE);
        add("clumsy", "trait.clumsy.name", "trait.clumsy.desc", +5, Category.NEGATIVE);
        add("pacifist", "trait.pacifist.name", "trait.pacifist.desc", +4, Category.NEGATIVE);
        add("slow_healer", "trait.slow_healer.name", "trait.slow_healer.desc", +5, Category.NEGATIVE);
        add("hearty_appetite", "trait.hearty_appetite.name", "trait.hearty_appetite.desc", +8, Category.NEGATIVE);
        add("underweight", "trait.underweight.name", "trait.underweight.desc", +6, Category.NEGATIVE);
        add("stingy", "trait.stingy.name", "trait.stingy.desc", +6, Category.NEGATIVE);
        add("drowning", "trait.drowning.name", "trait.drowning.desc", +8, Category.NEGATIVE);
        add("low_immunity", "trait.low_immunity.name", "trait.low_immunity.desc", +6, Category.NEGATIVE);
        add("lazy", "trait.lazy.name", "trait.lazy.desc", +5, Category.NEGATIVE);
        add("coward", "trait.coward.name", "trait.coward.desc", +4, Category.NEGATIVE);
        add("tall", "trait.tall.name", "trait.tall.desc", +4, Category.NEGATIVE);

        // 额外负面（基于原版属性扩展）
        add("glutton", "trait.glutton.name", "trait.glutton.desc", +6, Category.NEGATIVE);
        add("frail", "trait.frail.name", "trait.frail.desc", +6, Category.NEGATIVE);
        add("invalid", "trait.invalid.name", "trait.invalid.desc", +6, Category.NEGATIVE);
        add("heavy", "trait.heavy.name", "trait.heavy.desc", +6, Category.NEGATIVE);
        add("nyctophobic", "trait.nyctophobic.name", "trait.nyctophobic.desc", +4, Category.NEGATIVE);
    }

    public static TraitDef get(String id) {
        return BY_ID.get(id);
    }

    public static boolean exists(String id) {
        return BY_ID.containsKey(id);
    }

    /** 已选集合的点数余额（可正可负）。余额 < 0 视为非法。 */
    public static int pointsOf(Set<String> chosen) {
        int total = INITIAL_POINTS;
        for (String id : chosen) {
            TraitDef d = BY_ID.get(id);
            if (d != null) {
                total += d.delta;
            }
        }
        return total;
    }

    /** 是否允许再加入该特质：未选过，且（负面 或 加入后余额 ≥ 0）。 */
    public static boolean canAdd(Set<String> chosen, String id) {
        TraitDef d = BY_ID.get(id);
        if (d == null || chosen.contains(id)) {
            return false;
        }
        if (d.delta > 0) {
            return true; // 负面特质永远可加（增加点数）
        }
        return pointsOf(chosen) + d.delta >= 0; // 正面特质需余额足够
    }
}
