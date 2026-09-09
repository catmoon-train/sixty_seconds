package net.exmo.sixty_seconds.integration;

import com.sighs.petiteinventory.config.ItemSizeRule;
import com.sighs.petiteinventory.config.ItemSizeRuleCache;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * Bridges the 60 Seconds weight table to PetiteInventory's footprint rules.
 *
 * <p>The rule is an area, not a stack-size rule: one stack still occupies the
 * footprint of one item.  This is the same convention used by PetiteInventory
 * and keeps the result consistent in chests, supply boxes and the player UI.</p>
 */
public final class PetiteInventoryIntegration {
    private static boolean installed;

    private PetiteInventoryIntegration() {
    }

    public static void installWeightRules() {
        if (installed || !ModList.get().isLoaded("petiteinventory")) {
            return;
        }
        installed = true;
        try {
            ItemSizeRuleCache.loadAllRule();
            SixtySecondsWeightConfig config = SixtySecondsWeightConfigStore.defaultConfig();
            for (var entry : BuiltInRegistries.ITEM.entrySet()) {
                var id = BuiltInRegistries.ITEM.getKey(entry.getValue());
                if (id == null || !SixtySeconds.MOD_ID.equals(id.getNamespace())) {
                    continue;
                }
                ItemStack sample = new ItemStack(entry.getValue());
                double weight = SixtySecondsWeightCalc.unitWeight(sample, config);
                ItemSizeRule rule = new ItemSizeRule();
                rule.match = List.of(id.toString());
                rule.result = footprint(weight);
                ItemSizeRuleCache.putEntry(rule);
            }
            // Keep the generated rules visible to PetiteInventory's config and
            // make the mapping survive a reload without overwriting other mods'
            // rules.
            ItemSizeRuleCache.saveConfig();
            SixtySeconds.LOGGER.info("Installed weight-based PetiteInventory footprints for 60 Seconds items");
        } catch (LinkageError | RuntimeException error) {
            // PetiteInventory is an optional runtime dependency.  A mismatched
            // version should not prevent the 60 Seconds game from starting.
            installed = false;
            SixtySeconds.LOGGER.warn("Could not install PetiteInventory weight footprints", error);
        }
    }

    /**
     * Converts weight to a rectangle with exactly {@code ceil(weight)} cells.
     * PetiteInventory stores rectangular footprints, so prime/non-factorable
     * sizes use a 1-by-N strip instead of silently rounding the area again.
     */
    public static String footprint(double weight) {
        int cells = Math.max(1, (int) Math.ceil(Math.max(0.0, weight)));
        int width = 1;
        for (int candidate = Math.min(cells, 9); candidate >= 2; candidate--) {
            if (cells % candidate == 0) {
                width = candidate;
                break;
            }
        }
        int height = cells / width;
        return width + "*" + height;
    }
}
