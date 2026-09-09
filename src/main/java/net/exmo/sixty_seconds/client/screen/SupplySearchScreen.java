package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.client.ClientInventoryContext;
import net.exmo.sixty_seconds.SixtySecondsBalance;
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
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
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
        if (slot.index >= SupplySearchMenu.CONTAINER_SIZE
                || !SixtySecondsLootMagnifierItem.isMagnifier(slot.getItem())
                || !searchStart.containsKey(slot.index)) {
            return;
        }
        long now = this.minecraft.level == null ? 0 : this.minecraft.level.getGameTime();
        float progress = (float) (now - searchStart.get(slot.index)) / searchDuration.get(slot.index);
        progress = Math.max(0f, Math.min(1f, progress));
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        graphics.fill(x, y, x + 16, y + 16, 0x80000000);
        int barW = Math.round(14 * progress);
        graphics.fill(x + 1, y + 13, x + 15, y + 15, 0xFF222222);
        graphics.fill(x + 1, y + 13, x + 1 + barW, y + 15, 0xFF3FC46B);
    }

    private Slot getSlotAt(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        for (int i = this.menu.slots.size() - 1; i >= 0; i--) {
            Slot slot = this.menu.getSlot(i);
            if (x >= slot.x && x < slot.x + 16 && y >= slot.y && y < slot.y + 16) {
                return slot;
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
