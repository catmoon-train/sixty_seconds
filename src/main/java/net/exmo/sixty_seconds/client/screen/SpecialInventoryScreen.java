package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.client.WeightConfigClient;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** PNG-backed Tarkov-style inventory with square, runtime-sized backpack cells. */
public class SpecialInventoryScreen extends AbstractContainerScreen<SpecialInventoryMenu> {
    private static final ResourceLocation TEXTURE = SixtySeconds.id("textures/gui/special_inventory_v2.png");
    private static final int TEXTURE_WIDTH = 1432;
    private static final int TEXTURE_HEIGHT = 1072;

    private static final int WIDTH = 400;
    private static final int MIN_HEIGHT = 300;
    private static final int DIVIDER_X = 180;
    private static final int BAG_X = 190;
    private static final int BAG_Y = 39;
    /** The backpack grid is intentionally square; all item areas use this same cell size. */
    private static final int BAG_CELL = 23;
    private static final int BAG_COLUMNS = 9;
    // The hotbar is intentionally kept on the left, so the right-hand
    // backpack has 27 base slots plus two unlockable rows (5 rows total).
    private static final int BAG_ROWS = 5;

    private static final int GRID_BACKGROUND = 0xF0162028;
    private static final int GRID_SLOT = 0xE01C2A33;
    private static final int GRID_HOVER = 0xF03D5964;
    private static final int GRID_EDGE = 0xFF53636D;
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
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
        rebuildLayout();
    }

    @Override
    protected void init() {
        rebuildLayout();
        this.imageHeight = Math.max(MIN_HEIGHT,
                BAG_Y + Math.max(BAG_ROWS, backpackLayout.rows()) * BAG_CELL + 72);
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

        // The generated PNG supplies the metal frame, lighting, bevels and decoration.
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
        drawTextLabels(graphics, x, y);
        drawSquareBackpackGrid(graphics, mouseX, mouseY);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x + 18, y + 40, x + 96, y + 177,
                58, 0.0F, mouseX, mouseY, this.menu.getPlayer());
    }

    private void drawTextLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.literal("60 SECONDS"), x + 14, y + 8, ACCENT, false);
        graphics.drawString(this.font, Component.literal("FIELD INVENTORY"), x + 91, y + 8, TEXT, false);
        graphics.drawString(this.font, Component.literal("LIVE LOADOUT"), x + WIDTH - 91, y + 8, MUTED, false);
        graphics.drawString(this.font, Component.literal("SURVIVOR"), x + 18, y + 31, TEXT, false);
        graphics.drawString(this.font, Component.literal("EQUIPMENT"), x + 103, y + 31, MUTED, false);
        graphics.drawString(this.font, Component.literal("QUICK ACCESS"), x + 18, y + 180, MUTED, false);
        graphics.drawString(this.font, Component.literal("BACKPACK"), x + DIVIDER_X + 10, y + 31, TEXT, false);
        graphics.drawString(this.font,
                Component.literal("9 x 5  //  " + (SpecialInventoryMenu.PLAYER_MAIN_END
                        + this.menu.unlockedExtraSlots()) + "/54 SLOTS"),
                x + WIDTH - 114, y + 31, MUTED, false);
        drawWeight(graphics, x, y);
    }

    private void drawWeight(GuiGraphics graphics, int x, int y) {
        SixtySecondsWeightConfig config = WeightConfigClient.getOrBuiltin();
        if (config == null || !config.enabled) return;
        double load = SixtySecondsWeightCalc.computeLoad(this.menu.getPlayer(), config);
        double max = SixtySecondsTraitSystem.traitMaxLoad(this.menu.getPlayer(), config.maxLoad);
        float ratio = (float) Math.max(0.0, Math.min(1.0, load / Math.max(0.01, max)));
        int barX = x + 18;
        int barY = y + 267;
        int barW = 162;
        graphics.drawString(this.font,
                Component.literal(String.format("LOAD  %.1f / %.0f KG", load, max)),
                barX, y + 253, ratio >= 1.0f ? LOAD_RED : TEXT, false);
        graphics.fill(barX, barY, barX + barW, barY + 7, 0xFF0C1116);
        graphics.fill(barX + 1, barY + 1, barX + 1 + Math.round((barW - 2) * ratio), barY + 6,
                ratio >= 1.0f ? LOAD_RED : LOAD_GREEN);
        graphics.renderOutline(barX, barY, barW, 7, 0xFF465563);
    }

    /** Draws the actual square cells over the PNG's decorative right-hand bay. */
    private void drawSquareBackpackGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridWidth = BAG_COLUMNS * BAG_CELL;
        int gridHeight = Math.max(BAG_ROWS, backpackLayout.rows()) * BAG_CELL;
        graphics.fill(this.leftPos + BAG_X, this.topPos + BAG_Y,
                this.leftPos + BAG_X + gridWidth, this.topPos + BAG_Y + gridHeight,
                GRID_BACKGROUND);
        graphics.renderOutline(this.leftPos + BAG_X - 1, this.topPos + BAG_Y - 1,
                gridWidth + 2, gridHeight + 2, GRID_EDGE);

        for (int index = SpecialInventoryMenu.PLAYER_MAIN_START;
             index < SpecialInventoryMenu.EXTRA_END; index++) {
            PackedItemLayout.Position position = backpackLayout.position(index);
            if (position == null) continue;
            int slotX = BAG_X + position.column() * BAG_CELL;
            int slotY = BAG_Y + position.row() * BAG_CELL;
            int slotWidth = position.width() * BAG_CELL;
            int slotHeight = position.height() * BAG_CELL;
            boolean hovered = mouseX >= this.leftPos + slotX && mouseX < this.leftPos + slotX + slotWidth
                    && mouseY >= this.topPos + slotY && mouseY < this.topPos + slotY + slotHeight;
            graphics.fill(this.leftPos + slotX + 1, this.topPos + slotY + 1,
                    this.leftPos + slotX + slotWidth - 1,
                    this.topPos + slotY + slotHeight - 1,
                    hovered ? GRID_HOVER : GRID_SLOT);
            graphics.renderOutline(this.leftPos + slotX, this.topPos + slotY,
                    slotWidth, slotHeight, hovered ? ACCENT : GRID_EDGE);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // All labels are part of the custom PNG-backed layout.
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        PackedItemLayout.Position position = backpackLayout.position(slot.index);
        if (position != null) {
            int targetX = BAG_X + position.column() * BAG_CELL;
            int targetY = BAG_Y + position.row() * BAG_CELL;
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
            PackedItemLayout.Position position = backpackLayout.position(slot.index);
            int x = position == null ? slot.x : BAG_X + position.column() * BAG_CELL;
            int y = position == null ? slot.y : BAG_Y + position.row() * BAG_CELL;
            int cell = position == null ? 18 : BAG_CELL;
            graphics.renderOutline(this.leftPos + x, this.topPos + y,
                    area.width() * cell, area.height() * cell, ACCENT);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        for (int index = SpecialInventoryMenu.PLAYER_MAIN_START;
             index < SpecialInventoryMenu.EXTRA_END; index++) {
            PackedItemLayout.Position position = backpackLayout.position(index);
            if (position == null) continue;
            int left = BAG_X + position.column() * BAG_CELL;
            int top = BAG_Y + position.row() * BAG_CELL;
            if (x >= left && x < left + position.width() * BAG_CELL
                    && y >= top && y < top + position.height() * BAG_CELL) {
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
