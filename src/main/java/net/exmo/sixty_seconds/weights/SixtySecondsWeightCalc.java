package net.exmo.sixty_seconds.weights;

import net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Map;

/**
 * 负重计算的纯逻辑工具。不依赖任何客户端类，可在服务端（逐 tick 减速）与客户端（HUD 显示）共用。
 */
public final class SixtySecondsWeightCalc {

    private SixtySecondsWeightCalc() {
    }

    /**
     * 返回用于查表的物品键。
     *
     * <p>TACZ 的枪/弹/配件都是统一物品，仅靠 {@code custom_data} 中的 {@code GunId}/{@code AmmoId}/
     * {@code AttachmentId} 区分具体型号，因此这里优先读取这些字段，使每把枪、每种弹、每个配件都能
     * 在配置里单独配置重量（参考科技线配方对 TACZ 物品的支持方式）。
     */
    public static String itemKey(ItemStack stack) {
        if (stack.isEmpty()) return "";
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag tag = cd.copyTag();
            String s = tag.getString("GunId");
            if (!s.isEmpty()) return s;
            s = tag.getString("AmmoId");
            if (!s.isEmpty()) return s;
            s = tag.getString("AttachmentId");
            if (!s.isEmpty()) return s;
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** 单件物品的重量（不含堆叠数量）。 */
    public static double unitWeight(ItemStack stack, SixtySecondsWeightConfig cfg) {
        if (stack.isEmpty()) return 0;
        // 屏障方块（60s 模式占位格）不计入负重
        if (stack.is(net.minecraft.world.item.Items.BARRIER)) return 0;
        String key = itemKey(stack);
        Double w = cfg.itemWeights.get(key);
        if (w != null) return w;
        for (Map.Entry<String, Double> e : cfg.tagWeights.entrySet()) {
            String tk = e.getKey();
            if (tk.startsWith("#")) {
                ResourceLocation loc = ResourceLocation.tryParse(tk.substring(1));
                if (loc != null && stack.is(ItemTags.create(loc))) return e.getValue();
            }
        }
        return fallbackWeight(key, cfg.defaultWeight);
    }

    /** 未显式配置时的兜底重量：TACZ 物品按类型启发式估算，其余用默认重量。 */
    private static double fallbackWeight(String key, double def) {
        if (key.startsWith("tacz:")) {
            if (key.contains("ammo_box")) return 3.0;
            if (key.contains("9mm") || key.contains("22wmr") || key.contains("50ae")) return 0.3;
            if (key.contains("12g")) return 0.4;
            if (key.contains("762x39") || key.contains("792x57")) return 0.5;
            if (key.contains("40mm") || key.contains("338")) return 0.6;
            if (key.contains("rpg_rocket")) return 1.0;
            if (key.contains("sight") || key.contains("scope") || key.contains("laser")) return 0.3;
            if (key.contains("muzzle") || key.contains("bayonet")) return 0.5;
            if (key.contains("stock")) return 0.6;
            if (key.contains("grip")) return 0.4;
            if (key.contains("mag") || key.contains("ammo_mod")) return 0.4;
            return 5.0; // 默认按一把枪估算
        }
        return def;
    }

    /**
     * 计算玩家总负重。
     *
     * <p>规则：
     * <ul>
     *   <li>手持/穿戴/物品栏中的物品按 {@code handMultiplier} 计重；</li>
     *   <li>背包物品本身按手持倍率计其自重；</li>
     *   <li>背包内储存的物品统一结算，每件按 {@code backpackMultiplier} 折算为更轻的重量
     *       （实际重量 = 基础重量 ÷ backpackMultiplier，默认 ÷1.5），累加进总量。也就是说，
     *       把物品装进背包会让它们更轻。</li>
     * </ul>
     */
    public static double computeLoad(Player player, SixtySecondsWeightConfig cfg) {
        double load = 0;
        Inventory inv = player.getInventory();
        for (ItemStack stack : inv.items) load += weighted(stack, cfg, false);
        for (ItemStack stack : inv.armor) load += weighted(stack, cfg, false);
        for (ItemStack stack : inv.offhand) load += weighted(stack, cfg, false);
        return load;
    }

    private static double weighted(ItemStack stack, SixtySecondsWeightConfig cfg, boolean inBackpack) {
        if (stack.isEmpty()) return 0;
        double mult = inBackpack ? cfg.backpackMultiplier : cfg.handMultiplier;
        if (stack.getItem() instanceof SixtySecondsBackpackItem) {
            double self = unitWeight(stack, cfg) * mult;
            double inner = 0;
            ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            for (ItemStack innerStack : contents.nonEmptyStream().toList()) {
                // 装在背包里的物品更轻：实际重量 = 基础重量 ÷ backpackMultiplier
                inner += unitWeight(innerStack, cfg) / cfg.backpackMultiplier * innerStack.getCount();
            }
            return self + inner;
        }
        return unitWeight(stack, cfg) * mult * stack.getCount();
    }
}
