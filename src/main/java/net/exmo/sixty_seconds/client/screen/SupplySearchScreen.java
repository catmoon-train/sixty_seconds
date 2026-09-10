package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.client.ClientInventoryContext;
import com.sighs.petiteinventory.client.ScreenLayoutSettings;
import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.content.item.SixtySecondsLootMagnifierItem;
import net.exmo.sixty_seconds.menu.SupplySearchMenu;
import net.exmo.sixty_seconds.network.SupplySearchRevealC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Supply-box search screen; PetiteInventory owns the real item footprints. */
public class SupplySearchScreen extends AbstractContainerScreen<SupplySearchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final Map<Integer, Long> searchStart = new HashMap<>();
    private final Map<Integer, Integer> searchDuration = new HashMap<>();

    public SupplySearchScreen(SupplySearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 168;
        this.inventoryLabelY = 74;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        this.imageHeight = 168;
        this.inventoryLabelY = 74;
        // House searching uses the legacy layout.  PetiteInventory resumes
        // after the player has returned to the shelter.
        // This screen has its own 27-cell layout and its own footprint
        // mapping.  PetiteInventory must not remap these container slots to
        // the player inventory, otherwise magnifiers render at the mapped
        // slot's coordinates (often in the lower-right of the screen).
        ScreenLayoutSettings.setEnabled(this, false);
        ClientInventoryContext.invalidate();
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // generic_54.png is a 256x256 atlas.  The original chest screen uses
        // these two source rectangles; blitting the whole atlas would stretch
        // the texture and shift every slot.
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, 71);
        graphics.blit(TEXTURE, x, y + 71, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.minecraft.level != null) {
            long now = this.minecraft.level.getGameTime();
            for (Integer slot : new ArrayList<>(searchStart.keySet())) {
                long elapsed = now - searchStart.get(slot);
                float progress = (float) elapsed / searchDuration.get(slot);
                if (progress >= 1f) {
                    SixtySecondsLootMagnifierItem.setSearching(this.menu.getSlot(slot).getItem(), false);
                    sendReveal(slot);
                    searchStart.remove(slot);
                    searchDuration.remove(slot);
                }
            }
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (slot.index >= SupplySearchMenu.CONTAINER_SIZE || slot.getItem().isEmpty()) {
            return;
        }
        ItemArea area = PetiteInventoryApi.getItemArea(slot.getItem());
        int width = Math.max(1, area.width());
        int height = Math.max(1, area.height());
        // AbstractContainerScreen already translates the pose to leftPos/topPos
        // before calling renderSlot.  Adding them here again moves the bar and
        // footprint toward the screen's lower-right corner.
        int x = slot.x;
        int y = slot.y;
        int pixelWidth = width * 18;
        int pixelHeight = height * 18;
        // One real stack is stored only at the anchor slot.  This outline
        // shows its complete footprint without creating visual ItemStack
        // copies in the covered cells.
        if (width > 1 || height > 1) {
            graphics.renderOutline(x, y, pixelWidth, pixelHeight, 0xC0B88D4A);
        }
        if (SixtySecondsLootMagnifierItem.isMagnifier(slot.getItem())
                && searchStart.containsKey(slot.index)) {
            long now = this.minecraft.level == null ? 0 : this.minecraft.level.getGameTime();
            float progress = (float) (now - searchStart.get(slot.index)) / searchDuration.get(slot.index);
            progress = Math.max(0f, Math.min(1f, progress));
            // The search indicator follows the complete footprint, not only
            // the 16x16 anchor cell. Keep it on the footprint's bottom edge
            // and make its length equal to that footprint's width.
            int barLeft = x + 1;
            int barRight = x + pixelWidth - 1;
            int barTop = y + pixelHeight - 4;
            int barBottom = y + pixelHeight - 1;
            int barWidth = Math.max(0, barRight - barLeft);
            int filledWidth = Math.round(barWidth * progress);
            graphics.fill(barLeft, barTop, barRight, barBottom, 0xFF222222);
            graphics.fill(barLeft, barTop, barLeft + filledWidth, barBottom, 0xFF3FC46B);
        }
    }

    private Slot getSlotAt(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        for (int i = this.menu.slots.size() - 1; i >= 0; i--) {
            Slot slot = this.menu.getSlot(i);
            if (x >= slot.x && x < slot.x + 16 && y >= slot.y && y < slot.y + 16) {
                if (slot.hasItem()) {
                    return slot;
                }
                // Keep looking: this can be an empty physical cell covered
                // by a multi-cell stack anchored at an earlier slot.
            }
        }
        // A multi-cell item has one authoritative slot at its top-left
        // anchor.  Resolve clicks on its covered cells back to that anchor.
        int col = (int) Math.floor((x - 8) / 18.0);
        int row = (int) Math.floor((y - 18) / 18.0);
        if (col >= 0 && col < SupplySearchMenu.CONTAINER_COLS
                && row >= 0 && row < SupplySearchMenu.CONTAINER_ROWS) {
            int clickedCell = row * SupplySearchMenu.CONTAINER_COLS + col;
            for (int i = 0; i < SupplySearchMenu.CONTAINER_SIZE; i++) {
                ItemStack stack = this.menu.getSlot(i).getItem();
                if (stack.isEmpty()) continue;
                ItemArea area = PetiteInventoryApi.getItemArea(stack);
                int w = Math.max(1, area.width());
                int h = Math.max(1, area.height());
                int anchorRow = i / SupplySearchMenu.CONTAINER_COLS;
                int anchorCol = i % SupplySearchMenu.CONTAINER_COLS;
                if (anchorCol <= col && col < anchorCol + w
                        && anchorRow <= row && row < anchorRow + h) {
                    return this.menu.getSlot(i);
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Resolve the clicked cell through PetiteInventory first.  A footprint
        // may cover several empty physical cells, but all of them map back to
        // one real source Slot; no ItemStack is copied into the other cells.
        Slot hovered = this.getSlotAt(mouseX, mouseY);
        if (hovered != null) {
            Slot mapped = ClientInventoryContext.getMappedSlot(hovered);
            if (mapped != null) hovered = mapped;
            if (hovered.index < SupplySearchMenu.CONTAINER_SIZE
                    && button == 0
                    && SixtySecondsLootMagnifierItem.isMagnifier(hovered.getItem())
                    && !searchStart.containsKey(hovered.index)) {
                startSearch(hovered.index);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startSearch(int slotIndex) {
        ItemStack magnifier = this.menu.getSlot(slotIndex).getItem();
        int ticks = SixtySecondsLootMagnifierItem.getSearchTicks(magnifier);
        if (ticks <= 0) ticks = SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS;
        searchStart.put(slotIndex, this.minecraft.level.getGameTime());
        searchDuration.put(slotIndex, ticks);
        SixtySecondsLootMagnifierItem.setSearching(magnifier, true);
        if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.CHEST_OPEN, 0.9f, 1.0f);
        }
    }

    private void sendReveal(int slotIndex) {
        ClientPlayNetworking.send(new SupplySearchRevealC2SPacket(slotIndex));
    }
}
