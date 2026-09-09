package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.HashMap;
import java.util.Map;

/** A small deterministic rectangle packer for screens backed by PetiteInventory. */
final class PackedItemLayout {
    private PackedItemLayout() {
    }

    static Layout build(AbstractContainerMenu menu, int first, int lastExclusive,
                        int columns, int unlockedExclusive) {
        Map<Integer, Position> positions = new HashMap<>();
        int rows = 1;
        boolean[][] occupied = new boolean[64][Math.max(columns, 1)];

        for (int index = first; index < lastExclusive; index++) {
            if (index >= unlockedExclusive) continue;
            Slot slot = menu.slots.get(index);
            int width = 1;
            int height = 1;
            if (!slot.getItem().isEmpty()) {
                try {
                    ItemArea area = PetiteInventoryApi.getItemArea(slot.getItem());
                    width = Math.max(1, area.width());
                    height = Math.max(1, area.height());
                } catch (LinkageError ignored) {
                    // The integration is optional; the normal 1x1 layout remains usable.
                }
            }
            // PetiteInventory footprints are rectangles. Keep them intact even when a
            // configured item is wider than the normal nine-column panel.
            int requiredColumns = Math.max(columns, width);
            if (requiredColumns != occupied[0].length) {
                boolean[][] expanded = new boolean[occupied.length][requiredColumns];
                for (int row = 0; row < occupied.length; row++) {
                    System.arraycopy(occupied[row], 0, expanded[row], 0,
                            Math.min(occupied[row].length, expanded[row].length));
                }
                occupied = expanded;
                columns = requiredColumns;
            }

            int x = 0;
            int y = 0;
            while (!fits(occupied, x, y, width, height)) {
                x++;
                if (x + width > columns) {
                    x = 0;
                    y++;
                }
            }
            fill(occupied, x, y, width, height, true);
            rows = Math.max(rows, y + height);
            positions.put(index, new Position(x, y, width, height));
        }
        return new Layout(positions, columns, rows);
    }

    private static boolean fits(boolean[][] occupied, int x, int y, int width, int height) {
        if (y + height > occupied.length || x + width > occupied[0].length) return false;
        for (int row = y; row < y + height; row++) {
            for (int col = x; col < x + width; col++) {
                if (occupied[row][col]) return false;
            }
        }
        return true;
    }

    private static void fill(boolean[][] occupied, int x, int y, int width, int height, boolean value) {
        for (int row = y; row < y + height; row++) {
            for (int col = x; col < x + width; col++) occupied[row][col] = value;
        }
    }

    record Position(int column, int row, int width, int height) {
    }

    record Layout(Map<Integer, Position> positions, int columns, int rows) {
        Position position(int index) {
            return positions.get(index);
        }
    }
}
