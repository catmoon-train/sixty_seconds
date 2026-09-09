package net.exmo.sixty_seconds.integration;

import com.sighs.petiteinventory.config.ItemSizeRule;
import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import com.sighs.petiteinventory.platform.inventory.ItemInventoryService;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Installs 60 Seconds' weight table directly into PetiteInventory's cache. */
public final class PetiteInventoryIntegration {
    private PetiteInventoryIntegration() {
    }

    /**
     * PetiteInventory is a required dependency, so this deliberately uses its
     * public rule API directly.  The rules stay in memory and are rebuilt from
     * default_weights.json/config on every launch; we do not overwrite
     * PetiteInventory's own config file.
     */
    public static void installWeightRules() {
        // PetiteInventory may finish loading its persisted rules after the
        // common setup queue.  Rebuild the cache here instead of relying on a
        // one-shot flag, then install our exact entries last so an old
        // 1*1 rule can never shadow the weight table.
        ItemSizeRuleCache.loadAllRule();
        installCurrentRules();
    }

    /** Applies the 60 Seconds rules to PetiteInventory's already-loaded cache. */
    public static void installCurrentRules() {
        SixtySecondsWeightConfig config = SixtySecondsWeightConfigStore.defaultConfig();
        Map<String, List<String>> exactRules = new LinkedHashMap<>();

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = BuiltInRegistries.ITEM.getKey(entry.getValue());
            if (id == null || !SixtySeconds.MOD_ID.equals(id.getNamespace())) continue;

            String footprint = footprint(SixtySecondsWeightCalc.unitWeight(
                    new ItemStack(entry.getValue()), config));
            exactRules.computeIfAbsent(footprint, ignored -> new ArrayList<>()).add(id.toString());
        }

        for (Map.Entry<String, List<String>> entry : exactRules.entrySet()) {
            putRule(entry.getValue(), entry.getKey());
        }

        // TACZ stores the concrete gun/ammo/attachment id in custom_data.
        // Use PetiteInventory's NBT-rule table for all three item families;
        // the tiny compatibility mixin only teaches PetiteInventory the two
        // TACZ fields its current matcher does not yet recognize.
        Map<String, List<String>> taczRules = new LinkedHashMap<>();
        for (String key : config.itemWeights.keySet()) {
            if (!key.startsWith("tacz:")) continue;
            String footprint = footprint(config.itemWeights.getOrDefault(key, config.defaultWeight));
            taczRules.computeIfAbsent(footprint, ignored -> new ArrayList<>()).add(
                    "tacz:modern_kinetic_gun{GunId:\"" + key + "\"}");
            taczRules.get(footprint).add("tacz:ammo{AmmoId:\"" + key + "\"}");
            taczRules.get(footprint).add("tacz:attachment{AttachmentId:\"" + key + "\"}");
        }
        for (Map.Entry<String, List<String>> entry : taczRules.entrySet()) {
            putRule(entry.getValue(), entry.getKey());
        }

        int registered = exactRules.values().stream().mapToInt(List::size).sum();
        String waterId = "sixty_seconds:sixty_seconds_water_medium";
        String crowbarId = "sixty_seconds:sixty_seconds_crowbar";
        var water = BuiltInRegistries.ITEM.get(ResourceLocation.parse(waterId));
        var waterArea = water == null ? null : ItemInventoryService.getArea(new ItemStack(water));
        SixtySeconds.LOGGER.info(
                "Installed weight footprints in PetiteInventory for {} 60 Seconds items ({}={}, area={}x{}, {}={})",
                registered, waterId, ItemSizeRuleCache.matchItem(waterId),
                waterArea == null ? 0 : waterArea.width(),
                waterArea == null ? 0 : waterArea.height(),
                crowbarId, ItemSizeRuleCache.matchItem(crowbarId));
    }

    private static void putRule(List<String> matches, String result) {
        ItemSizeRule rule = new ItemSizeRule();
        rule.match = matches;
        rule.result = result;
        ItemSizeRuleCache.putEntry(rule);
    }

    /** Converts weight to exactly ceil(weight) PetiteInventory cells. */
    public static String footprint(double weight) {
        int cells = Math.max(1, (int) Math.ceil(Math.max(0.0, weight)));
        int width = 1;
        for (int candidate = Math.min(cells, 9); candidate >= 2; candidate--) {
            if (cells % candidate == 0) {
                width = candidate;
                break;
            }
        }
        return width + "*" + (cells / width);
    }
}
