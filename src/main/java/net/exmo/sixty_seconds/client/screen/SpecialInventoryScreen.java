package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.exmo.sixty_seconds.client.WeightConfigClient;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Tarkov-inspired field inventory: survivor and equipment on the left, bag on the right. */
public class SpecialInventoryScreen extends AbstractContainerScreen<SpecialInventoryMenu> {
    private static final int WIDTH = 400;
    private static final int MIN_HEIGHT = 300;
    private static final int DIVIDER_X = 202;
    private static final int BAG_X = 216;
    private static final int BAG_Y = 48;
    private static final int BAG_COLUMNS = 9;

    private static final int OUTER = 0xF20A0E13;
    private static final int TOPBAR = 0xFF101820;
    private static final int PANEL = 0xE91A232D;
    private static final int PANEL_ALT = 0xE9141B23;
    private static final int EDGE = 0xFF465563;
    private static final int EDGE_SOFT = 0xFF2C3945;
    private static final int SLOT = 0xB51B2731;
    private static final int SLOT_HOVER = 0xD02D3A45;
    private static final int TEXT = 0xFFE4E8E9;
    private static final int MUTED = 0xFF8E9BA4;
    private static final int ACCENT = 0xFFE0B15A;
    private static final int ACCENT_DIM = 0xFF806537;
    private static final int LOAD_GREEN = 0xFF6FBF83;
    private static final int LOAD_RED = 0xFFCC5D5D;

    private PackedItemLayout.Layout backpackLayout;

    public SpecialInventoryScreen(SpecialInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = MIN_HEIGHT;
        // All text is drawn by the custom field-inventory header below.
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
        rebuildLayout();
    }

    @Override
    protected void init() {
        rebuildLayout();
        this.imageHeight = Math.max(MIN_HEIGHT, BAG_Y + backpackLayout.rows() * 18 + 72);
        super.init();
    }

    private void rebuildLayout() {
        this.backpackLayout = PackedItemLayout.build(this.menu,
                SpecialInventoryMenu.PLAYER_MAIN_START,
                SpecialInventoryMenu.EXTRA_END,
                BAG_COLUMNS,
                SpecialInventoryMenu.PLAYER_MAIN_END + this.menu.unlockedExtraSlots());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        rebuildLayout();
        int x = this.leftPos;
        int y = this.topPos;

        graphics.fillGradient(x, y, x + this.imageWidth, y + this.imageHeight, OUTER, 0xFF101820);
        graphics.fill(x, y, x + this.imageWidth, y + 28, TOPBAR);
        graphics.fill(x + 1, y + 27, x + this.imageWidth - 1, y + 29, ACCENT_DIM);
        graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, EDGE);

        // Two clear work areas: survivor/equipment and backpack.
        graphics.fill(x + 10, y + 36, x + DIVIDER_X - 8, y + this.imageHeight - 12, PANEL);
        graphics.fill(x + DIVIDER_X + 6, y + 36, x + this.imageWidth - 10,
                y + this.imageHeight - 12, PANEL_ALT);
        graphics.renderOutline(x + 10, y + 36, DIVIDER_X - 18, this.imageHeight - 48, EDGE_SOFT);
        graphics.renderOutline(x + DIVIDER_X + 6, y + 36,
                this.imageWidth - DIVIDER_X - 16, this.imageHeight - 48, EDGE_SOFT);
        graphics.fill(x + DIVIDER_X - 1, y + 36, x + DIVIDER_X + 1,
                y + this.imageHeight - 12, 0xFF26333D);

        drawHeader(graphics, x, y);
        drawLeftLabels(graphics, x, y);
        drawRightLabels(graphics, x, y);
        drawSlotBoxes(graphics, mouseX, mouseY);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x + 23, y + 38, x + 189, y + 132,
                55, 0.0F, mouseX, mouseY, this.menu.getPlayer());
    }

    private void drawHeader(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.literal("60 SECONDS"), x + 14, y + 8, ACCENT, false);
        graphics.drawString(this.font, Component.literal("FIELD INVENTORY"), x + 91, y + 8, TEXT, false);
        graphics.drawString(this.font, Component.literal("LIVE LOADOUT"), x + WIDTH - 91, y + 8, MUTED, false);
        graphics.fill(x + 14, y + 21, x + 22, y + 23, ACCENT);
        graphics.fill(x + 25, y + 21, x + 29, y + 23, ACCENT_DIM);
    }

    private void drawLeftLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.literal("SURVIVOR"), x + 18, y + 39, TEXT, false);
        graphics.drawString(this.font, Component.literal("EQUIPMENT"), x + 18, y + 137, MUTED, false);
        graphics.drawString(this.font, Component.literal("QUICK ACCESS"), x + 18, y + 210, MUTED, false);
        graphics.fill(x + 18, y + 228, x + 180, y + 229, EDGE_SOFT);
        drawWeight(graphics, x, y);
    }

    private void drawRightLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.literal("BACKPACK"), x + DIVIDER_X + 16, y + 39, TEXT, false);
        graphics.drawString(this.font,
                Component.literal("9 x 6  //  " + (SpecialInventoryMenu.PLAYER_MAIN_END
                        + this.menu.unlockedExtraSlots()) + "/54 SLOTS"),
                x + WIDTH - 114, y + 39, MUTED, false);
        graphics.fill(x + DIVIDER_X + 16, y + 43, x + WIDTH - 18, y + 44, ACCENT_DIM);
    }

    private void drawWeight(GuiGraphics graphics, int x, int y) {
        SixtySecondsWeightConfig config = WeightConfigClient.getOrBuiltin();
        if (config == null || !config.enabled) return;
        double load = SixtySecondsWeightCalc.computeLoad(this.menu.getPlayer(), config);
        double max = SixtySecondsTraitSystem.traitMaxLoad(this.menu.getPlayer(), config.maxLoad);
        float ratio = (float) Math.max(0.0, Math.min(1.0, load / Math.max(0.01, max)));
        int barX = x + 18;
        int barY = y + 273;
        int barW = 162;
        graphics.drawString(this.font,
                Component.literal(String.format("LOAD  %.1f / %.0f KG", load, max)),
                barX, y + 258, ratio >= 1.0f ? LOAD_RED : TEXT, false);
        graphics.fill(barX, barY, barX + barW, barY + 7, 0xFF0C1116);
        graphics.fill(barX + 1, barY + 1, barX + 1 + Math.round((barW - 2) * ratio), barY + 6,
                ratio >= 1.0f ? LOAD_RED : LOAD_GREEN);
        graphics.renderOutline(barX, barY, barW, 7, EDGE);
    }

    private void drawSlotBoxes(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            int slotX = slot.x;
            int slotY = slot.y;
            PackedItemLayout.Position packed = backpackLayout.position(slot.index);
            if (packed != null) {
                slotX = BAG_X + packed.column() * 18;
                slotY = BAG_Y + packed.row() * 18;
            }
            if (slotX < 0 || slotY < 0 || slotX + 18 > this.imageWidth
                    || slotY + 18 > this.imageHeight) continue;
            boolean hovered = mouseX >= this.leftPos + slotX - 1 && mouseX < this.leftPos + slotX + 17
                    && mouseY >= this.topPos + slotY - 1 && mouseY < this.topPos + slotY + 17;
            graphics.fill(this.leftPos + slotX - 1, this.topPos + slotY - 1,
                    this.leftPos + slotX + 17, this.topPos + slotY + 17,
                    hovered ? SLOT_HOVER : SLOT);
            graphics.renderOutline(this.leftPos + slotX - 1, this.topPos + slotY - 1,
                    18, 18, hovered ? ACCENT_DIM : EDGE_SOFT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // The vanilla title and "Inventory" labels are intentionally replaced
        // by the field-inventory header drawn in renderBg.
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        PackedItemLayout.Position packed = backpackLayout.position(slot.index);
        if (packed != null) {
            int targetX = BAG_X + packed.column() * 18;
            int targetY = BAG_Y + packed.row() * 18;
            graphics.pose().pushPose();
            graphics.pose().translate(targetX - slot.x, targetY - slot.y, 0);
            super.renderSlot(graphics, slot);
            graphics.pose().popPose();
            return;
        }
        super.renderSlot(graphics, slot);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFootprintFrames(graphics);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderFootprintFrames(GuiGraphics graphics) {
        for (Slot slot : this.menu.slots) {
            if (!slot.hasItem()) continue;
            ItemArea area;
            try {
                area = PetiteInventoryApi.getItemArea(slot.getItem());
            } catch (LinkageError error) {
                return;
            }
            if (area.width() <= 1 && area.height() <= 1) continue;
            PackedItemLayout.Position packed = backpackLayout.position(slot.index);
            int x = packed == null ? slot.x : BAG_X + packed.column() * 18;
            int y = packed == null ? slot.y : BAG_Y + packed.row() * 18;
            graphics.renderOutline(this.leftPos + x - 1, this.topPos + y - 1,
                    area.width() * 18, area.height() * 18, ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        for (int index = SpecialInventoryMenu.PLAYER_MAIN_START;
             index < SpecialInventoryMenu.EXTRA_END; index++) {
            PackedItemLayout.Position packed = backpackLayout.position(index);
            if (packed == null) continue;
            int left = BAG_X + packed.column() * 18;
            int top = BAG_Y + packed.row() * 18;
            if (x >= left && x < left + packed.width() * 18
                    && y >= top && y < top + packed.height() * 18) {
                onMouseClick(this.menu.getSlot(index), index, button, ClickType.PICKUP);
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
}
