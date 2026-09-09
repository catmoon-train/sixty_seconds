package net.exmo.sixty_seconds.client.screen;

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
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Supply-box search screen with a collision-free dynamic item layout. */
public class SupplySearchScreen extends AbstractContainerScreen<SupplySearchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final Map<Integer, Long> searchStart = new HashMap<>();
    private final Map<Integer, Integer> searchDuration = new HashMap<>();
    private PackedItemLayout.Layout containerLayout;
    private int playerOffset;

    public SupplySearchScreen(SupplySearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        rebuildLayout();
        this.imageHeight = 114 + Math.max(3, containerLayout.rows()) * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        rebuildLayout();
        this.imageHeight = 114 + Math.max(3, containerLayout.rows()) * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        super.init();
    }

    private void rebuildLayout() {
        this.containerLayout = PackedItemLayout.build(this.menu, 0,
                SupplySearchMenu.CONTAINER_SIZE, 9, SupplySearchMenu.CONTAINER_SIZE);
        this.playerOffset = Math.max(0, containerLayout.rows() - 3) * 18;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        rebuildLayout();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        g.blit(TEXTURE, i, j, 0, 0, this.imageWidth, containerLayout.rows() * 18 + 17);
        g.blit(TEXTURE, i, j + containerLayout.rows() * 18 + 17,
                0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = this.minecraft.level.getGameTime();
        for (Integer slot : new ArrayList<>(searchStart.keySet())) {
            long elapsed = now - searchStart.get(slot);
            float prog = (float) elapsed / searchDuration.get(slot);
            if (prog >= 1f) {
                SixtySecondsLootMagnifierItem.setSearching(this.menu.getSlot(slot).getItem(), false);
                sendReveal(slot);
                searchStart.remove(slot);
                searchDuration.remove(slot);
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        int targetX = visualX(slot);
        int targetY = visualY(slot);
        g.pose().pushPose();
        g.pose().translate(targetX - slot.x, targetY - slot.y, 0);
        super.renderSlot(g, slot);
        g.pose().popPose();

        if (SixtySecondsLootMagnifierItem.isMagnifier(slot.getItem())
                && searchStart.containsKey(slot.index)) {
            long now = this.minecraft.level.getGameTime();
            float prog = (float) (now - searchStart.get(slot.index)) / searchDuration.get(slot.index);
            prog = Math.max(0f, Math.min(1f, prog));
            g.fill(this.leftPos + targetX, this.topPos + targetY,
                    this.leftPos + targetX + 16, this.topPos + targetY + 16, 0x80000000);
            int barW = Math.round(14 * prog);
            g.fill(this.leftPos + targetX + 1, this.topPos + targetY + 13,
                    this.leftPos + targetX + 15, this.topPos + targetY + 15, 0xFF222222);
            g.fill(this.leftPos + targetX + 1, this.topPos + targetY + 13,
                    this.leftPos + targetX + 1 + barW, this.topPos + targetY + 15, 0xFF3FC46B);
        }
    }

    private int visualX(Slot slot) {
        PackedItemLayout.Position position = containerLayout.position(slot.index);
        return position == null ? slot.x : 8 + position.column() * 18;
    }

    private int visualY(Slot slot) {
        PackedItemLayout.Position position = containerLayout.position(slot.index);
        if (position != null) return 18 + position.row() * 18;
        return slot.y + playerOffset;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        rebuildLayout();
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        for (int index = 0; index < SupplySearchMenu.CONTAINER_SIZE; index++) {
            PackedItemLayout.Position position = containerLayout.position(index);
            if (position == null) continue;
            int left = 8 + position.column() * 18;
            int top = 18 + position.row() * 18;
            if (x < left || x >= left + position.width() * 18
                    || y < top || y >= top + position.height() * 18) continue;
            Slot slot = this.menu.getSlot(index);
            if (slot.hasItem() && SixtySecondsLootMagnifierItem.isMagnifier(slot.getItem())) {
                if (button == 0 && !searchStart.containsKey(index)) startSearch(index);
            } else {
                onMouseClick(slot, index, button, ClickType.PICKUP);
            }
            return true;
        }

        for (int index = SupplySearchMenu.CONTAINER_SIZE; index < this.menu.slots.size(); index++) {
            Slot slot = this.menu.getSlot(index);
            int left = visualX(slot);
            int top = visualY(slot);
            if (x >= left && x < left + 18 && y >= top && y < top + 18) {
                onMouseClick(slot, index, button, ClickType.PICKUP);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onMouseClick(Slot slot, int slotId, int button, ClickType type) {
        if (this.minecraft.gameMode != null && this.minecraft.player != null) {
            this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId,
                    slotId, button, type, this.minecraft.player);
        }
    }

    private void startSearch(int slotIndex) {
        ItemStack mag = this.menu.getSlot(slotIndex).getItem();
        int ticks = SixtySecondsLootMagnifierItem.getSearchTicks(mag);
        if (ticks <= 0) ticks = SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS;
        searchStart.put(slotIndex, this.minecraft.level.getGameTime());
        searchDuration.put(slotIndex, ticks);
        SixtySecondsLootMagnifierItem.setSearching(mag, true);
        if (this.minecraft.player != null) this.minecraft.player.playSound(SoundEvents.CHEST_OPEN, 0.9f, 1.0f);
    }

    private void sendReveal(int slotIndex) {
        ClientPlayNetworking.send(new SupplySearchRevealC2SPacket(slotIndex));
    }
}
