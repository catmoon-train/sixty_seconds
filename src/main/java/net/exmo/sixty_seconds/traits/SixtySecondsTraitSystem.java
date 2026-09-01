package net.exmo.sixty_seconds.traits;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.ServerTickEvents;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.content.item.SixtySecondsCigaretteItem;
import net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

/**
 * 天赋特质系统：负责读取玩家已选特质并对外提供所有效果查询；
 * 并在每秒钟为每位玩家应用持续性效果（隐藏药水、怪物发光、溺水、学习神速、夜猫子、不起眼、高大、老司机、吸烟者检测）。
 *
 * <p>分配规则：仅当 60s 游戏进行中（{@link SixtySecondsMod#isActive}）才允许加点；
 * 游戏结束时由 {@link #resetAll(ServerLevel)} 清除所有玩家加点与其药水效果/属性。
 */
public final class SixtySecondsTraitSystem {

    private SixtySecondsTraitSystem() {
    }

    static {
        ServerTickEvents.END_WORLD_TICK.register(SixtySecondsTraitSystem::tick);
    }

    // ───────────────────────── 主循环 ─────────────────────────

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return; // 每秒执行一次
        }
        for (ServerPlayer player : level.players()) {
            if (GameUtils.isPlayerEliminated(player)) {
                continue;
            }
            SixtySecondsTraitComponent comp = SixtySecondsTraitComponent.KEY.get(player);
            if (comp.chosen.isEmpty()) {
                continue;
            }
            applyContinuous(player, comp, level.getGameTime());
        }
    }

    private static void applyContinuous(ServerPlayer player, SixtySecondsTraitComponent comp, long gameTime) {
        // 1) 常驻隐藏药水效果
        applyHiddenEffects(player, comp);

        // 2) 听力敏锐：5 格内模组怪物发光
        if (comp.has("keen_hearing")) {
            AABB box = player.getBoundingBox().inflate(5);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e != player && isModMonster(e)) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, false));
                }
            }
        }

        // 3) 溺水：在水中每 5 秒 -1 健康
        if (comp.has("drowning") && player.isInWater()) {
            if ((gameTime + Math.abs(player.getUUID().hashCode()) % 97) % 100 == 0) {
                SixtySecondsHealthSystem.applyInjury(player, null, 1);
            }
        }

        // 4) 学习神速：理智 < 20 时立即恢复 50（一局仅一次）
        if (comp.has("fast_learner") && !comp.fastLearnerUsed) {
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
            if (stats.sanity < 20) {
                stats.sanity = Math.min(SixtySecondsStatsComponent.MAX, stats.sanity + 50);
                comp.fastLearnerUsed = true;
                stats.sync();
            }
        }

        // 5) 夜猫子：免疫黑暗与失明
        if (comp.has("wakeful")) {
            if (player.hasEffect(MobEffects.BLINDNESS)) {
                player.removeEffect(MobEffects.BLINDNESS);
            }
            if (player.hasEffect(MobEffects.DARKNESS)) {
                player.removeEffect(MobEffects.DARKNESS);
            }
        }

        // 6) 不起眼：清除以该玩家为目标的怪物锁定
        if (comp.has("inconspicuous")) {
            AABB tbox = player.getBoundingBox().inflate(24);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, tbox)) {
                if (e instanceof Mob mob && mob.getTarget() == player) {
                    mob.setTarget(null);
                }
            }
        }

        // 7) 高大：体型 +20%
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) {
            double target = comp.has("tall") ? 1.2 : 1.0;
            if (Math.abs(scale.getBaseValue() - target) > 0.001) {
                scale.setBaseValue(target);
            }
        }

        // 8) 老司机：驾驶载具时载具速度 +20%
        if (comp.has("speed_demon") && player.isPassenger()) {
            Entity veh = player.getVehicle();
            if (veh != null) {
                addHiddenEntity(veh, MobEffects.MOVEMENT_SPEED, 1, 60);
            }
        }

        // 9) 嗜烟如命：检测到正在使用香烟/雪茄则刷新“已抽烟”时间戳
        if (comp.has("smoker") && player.isUsingItem()) {
            ItemStack use = player.getUseItem();
            if (use.getItem() instanceof SixtySecondsCigaretteItem) {
                comp.lastSmokeTick = gameTime;
            }
        }
    }

    private static void applyHiddenEffects(Player player, SixtySecondsTraitComponent comp) {
        if (comp.has("strong")) {
            addHidden(player, MobEffects.DAMAGE_BOOST, 0);
        }
        if (comp.has("athletic")) {
            addHidden(player, MobEffects.MOVEMENT_SPEED, 0);
            addHidden(player, MobEffects.JUMP, 0);
        }
        if (comp.has("swimmer")) {
            addHidden(player, MobEffects.DOLPHINS_GRACE, 0);
        }
        if (comp.has("clumsy")) {
            addHidden(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
        }
        if (comp.has("pacifist")) {
            addHidden(player, MobEffects.WEAKNESS, 0);
        }
        if (comp.has("herculean")) {
            addHidden(player, MobEffects.DAMAGE_BOOST, 1);
        }
        if (comp.has("night_owl")) {
            addHidden(player, MobEffects.NIGHT_VISION, 0);
        }
        if (comp.has("marksman")) {
            addHidden(player, MobEffects.LUCK, 0);
        }
    }

    private static void addHidden(Player player, MobEffect effect, int amp) {
        MobEffectInstance cur = player.getEffect(effect);
        if (cur == null || cur.getAmplifier() != amp) {
            player.addEffect(new MobEffectInstance(effect, Integer.MAX_VALUE, amp, false, false, false));
        }
    }

    private static void addHidden(Player player, MobEffect effect, int amp, int ticks) {
        player.addEffect(new MobEffectInstance(effect, ticks, amp, false, false, false));
    }

    private static void addHiddenEntity(Entity e, MobEffect effect, int amp, int ticks) {
        MobEffectInstance cur = e.getEffect(effect);
        if (cur == null || cur.getAmplifier() != amp) {
            e.addEffect(new MobEffectInstance(effect, ticks, amp, false, false, false));
        }
    }

    private static boolean isModMonster(LivingEntity e) {
        if (e instanceof Player) {
            return false;
        }
        return e.getClass().getName().contains("sixty_seconds");
    }

    private static boolean recentlySmoked(ServerPlayer p) {
        SixtySecondsTraitComponent c = SixtySecondsTraitComponent.KEY.get(p);
        return c.lastSmokeTick >= 0 && p.level().getGameTime() - c.lastSmokeTick < 1800L; // 1.5 分钟
    }

    // ───────────────────────── 查询接口（供其它系统调用） ─────────────────────────

    public static boolean has(Player p, String id) {
        return SixtySecondsTraitComponent.KEY.get(p).has(id);
    }

    /** 受击时触发的一次性效果（钢筋铁骨 / 鹰眼 / 胆小鬼）。 */
    public static void onAttacked(ServerPlayer p) {
        SixtySecondsTraitComponent c = SixtySecondsTraitComponent.KEY.get(p);
        long gt = p.level().getGameTime();
        if (gt - c.lastAttackEffectTick < 100) {
            return; // 冷却 5 秒，避免环境伤害频繁触发
        }
        c.lastAttackEffectTick = gt;
        if (has(p, "thick_skinned")) {
            addHidden(p, MobEffects.DAMAGE_BOOST, 1, 100); // 力量 II，5 秒
        }
        if (has(p, "eagle_eyed")) {
            addHidden(p, MobEffects.NIGHT_VISION, 0, 200); // 夜视，10 秒
        }
        if (has(p, "coward")) {
            addHidden(p, MobEffects.BLINDNESS, 0, 100);
            addHidden(p, MobEffects.MOVEMENT_SLOWDOWN, 0, 100);
        }
    }

    public static double damageMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "fit")) {
            m *= 0.85; // 15% 免伤
        }
        if (has(p, "underweight")) {
            m *= 1.15; // 受伤 +15%
        }
        if (has(p, "frail")) {
            m *= 1.15;
        }
        return m;
    }

    public static double hungerMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "hearty_appetite")) {
            m *= 2.0;
        }
        if (has(p, "glutton")) {
            m *= 1.3;
        }
        if (has(p, "bottomless")) {
            m *= 0.7;
        }
        return m;
    }

    public static double thirstMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "high_thirst")) {
            m *= 2.0;
        }
        if (has(p, "camel")) {
            m *= 0.7;
        }
        return m;
    }

    public static double pollutionMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "infection_resistant")) {
            m *= 0.6; // 抗感染：污染上升更慢
        }
        return m;
    }

    public static double sanityMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "iron_will")) {
            m *= 0.7;
        }
        if (has(p, "nyctophobic") && p.level().getRawBrightness(p.blockPosition(), 0) < 7) {
            m *= 1.3;
        }
        if (has(p, "smoker") && !recentlySmoked((ServerPlayer) p)) {
            m *= 1.5;
        }
        return m;
    }

    public static double traitMaxLoad(Player p, double base) {
        double v = base;
        if (has(p, "strong")) {
            v += 25; // 力量 +25 负重
        }
        if (has(p, "lazy")) {
            v *= 0.8; // 懒惰 -20%
        }
        if (has(p, "heavy")) {
            v *= 0.9;
        }
        return v;
    }

    public static double lootSearchMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "dexterous")) {
            m *= 0.7; // 搜刮快 30%
        }
        if (has(p, "short_sighted")) {
            m *= 1.25; // 慢 20%
        }
        return m;
    }

    public static double sicknessMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "outdoorsman")) {
            m *= 0.5;
        }
        if (has(p, "low_immunity")) {
            m *= 1.5;
        }
        if (has(p, "frail")) {
            m *= 1.3;
        }
        if (has(p, "invalid")) {
            m *= 1.3;
        }
        return m;
    }

    public static double spawnMultiplier(ServerLevel level) {
        return 1.0; // 海怪/鲨鱼/Boss 刷新不受特质影响（优雅、万众瞩目只影响惊动概率）
    }

    /**
     * 惊动怪物概率倍率（仅作用于探索区游荡怪的「惊动」刷新判定）：
     * 优雅降低惊动概率，万众瞩目提高惊动概率。
     */
    public static double startleMultiplier(Player p) {
        double m = 1.0;
        if (has(p, "graceful")) {
            m *= 0.8; // 优雅：惊动概率 -20%
        }
        if (has(p, "conspicuous")) {
            m *= 1.25; // 万众瞩目：惊动概率 +25%
        }
        return m;
    }

    public static double lootExtraChance(Player p) {
        return has(p, "lucky") ? 0.10 : 0.0; // 额外物资 +10%
    }

    public static double weakStomachMultiplier(Player p) {
        return has(p, "weak_stomach") ? 2.0 : 1.0; // 吃坏肚子惩罚翻倍
    }

    public static boolean slowHealerFails(ServerPlayer p) {
        return has(p, "slow_healer") && p.level().getRandom().nextDouble() < 0.5;
    }

    public static int backpackSlotBonus(Player p) {
        int b = 0;
        if (has(p, "organized")) {
            b += 2;
        }
        if (has(p, "stingy")) {
            b -= 2;
        }
        return Math.max(0, b);
    }

    // ───────────────────────── 重置 ─────────────────────────

    /** 游戏结束时清除所有玩家加点及其药水效果 / 属性。 */
    public static void resetAll(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            clearTraitEffects(p);
            SixtySecondsTraitComponent.KEY.get(p).reset();
        }
    }

    public static void clearTraitEffects(ServerPlayer player) {
        for (MobEffect e : new MobEffect[]{
                MobEffects.DAMAGE_BOOST, MobEffects.MOVEMENT_SPEED, MobEffects.JUMP,
                MobEffects.DOLPHINS_GRACE, MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
                MobEffects.NIGHT_VISION, MobEffects.LUCK, MobEffects.BLINDNESS, MobEffects.GLOWING
        }) {
            if (player.hasEffect(e)) {
                player.removeEffect(e);
            }
        }
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && Math.abs(scale.getBaseValue() - 1.0) > 0.001) {
            scale.setBaseValue(1.0);
        }
    }
}
