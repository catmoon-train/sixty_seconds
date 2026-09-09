package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.client.ScreenLayoutSettings;
import com.sighs.petiteinventory.client.ClientInventoryContext;
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

/**
 * PNG-backed Tarkov-style inventory.
 *
 * <p>PetiteInventory owns the actual footprint, placement and click mapping.
 * This screen only supplies the artwork and the square 9-column background;
 * it never creates visual copies of a stack and never moves a slot by hand.</p>
 */
public class SpecialInventoryScreen extends AbstractContainerScreen<SpecialInventoryMenu> {
    private static final ResourceLocation TEXTURE =
            SixtySeconds.id("textures/gui/special_inventory_v2.png");
    private static final int TEXTURE_WIDTH = 1432;
    private static final int TEXTURE_HEIGHT = 1072;

    // PetiteInventory uses 18-pixel cells.  The PNG is rendered at 360x270 so
    // all nine columns and all five rows remain inside the metal right bay.
    private static final int WIDTH = 360;
    private static final int HEIGHT = 270;
    private static final int DIVIDER_X = 180;
    private static final int BAG_X = 190;
    private static final int BAG_Y = 39;
    private static final int BAG_CELL = 18;
    private static final int BAG_COLUMNS = 9;

    private static final int GRID_BACKGROUND = 0xD9162028;
    private static final int GRID_EDGE = 0x8053636D;
    private static final int TEXT = 0xFFE4E8E9;
    private static final int MUTED = 0xFF8E9BA4;
    private static final int ACCENT = 0xFFE0B15A;
    private static final int LOAD_GREEN = 0xFF6FBF83;
    private static final int LOAD_RED = 0xFFCC5D5D;

    private float uiScale = 1.0F;

    public SpecialInventoryScreen(SpecialInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        // PetiteInventory normally lets the player opt into the player-grid
        // layout per screen.  This screen is intentionally always opted in.
        ScreenLayoutSettings.setEnabled(this, true);
        ClientInventoryContext.invalidate();
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        super.init();
        this.uiScale = Math.min(1.0F, Math.min(
                (this.width - 8.0F) / WIDTH, (this.height - 8.0F) / HEIGHT));
        this.uiScale = Math.max(0.75F, this.uiScale);
        this.leftPos = Math.round((this.width - WIDTH * this.uiScale) / 2.0F);
        this.topPos = Math.round((this.height - HEIGHT * this.uiScale) / 2.0F);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);

        // This is only an empty-cell backdrop.  Item footprints and occupied
        // cells are rendered by PetiteInventory from the real Slot list.
        int visibleRows = 3 + (int) Math.ceil(this.menu.unlockedExtraSlots() / 9.0);
        int gridWidth = BAG_COLUMNS * BAG_CELL;
        int gridHeight = visibleRows * BAG_CELL;
        graphics.fill(x + BAG_X, y + BAG_Y,
                x + BAG_X + gridWidth, y + BAG_Y + gridHeight, GRID_BACKGROUND);
        for (int row = 0; row < visibleRows; row++) {
            for (int col = 0; col < BAG_COLUMNS; col++) {
                graphics.renderOutline(x + BAG_X + col * BAG_CELL,
                        y + BAG_Y + row * BAG_CELL, BAG_CELL, BAG_CELL, GRID_EDGE);
            }
        }

        drawTextLabels(graphics, x, y);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                x + 18, y + 40, x + 96, y + 177,
                58, 0.0F, mouseX, mouseY, this.menu.getPlayer());
    }

    private void drawTextLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.brand"), x + 14, y + 8, ACCENT, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.header"), x + 91, y + 8, TEXT, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.live_loadout"), x + WIDTH - 91, y + 8, MUTED, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.survivor"), x + 18, y + 31, TEXT, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.equipment"), x + 103, y + 31, MUTED, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.quick_access"), x + 18, y + 180, MUTED, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.backpack"), x + DIVIDER_X + 10, y + 31, TEXT, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.slots", 36 + this.menu.unlockedExtraSlots()),
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
        int barY = y + 237;
        int barW = 162;
        graphics.drawString(this.font, Component.translatable(
                "gui.sixty_seconds.inventory.load",
                String.format("%.1f", load), String.format("%.0f", max)),
                barX, y + 223, ratio >= 1.0f ? LOAD_RED : TEXT, false);
        graphics.fill(barX, barY, barX + barW, barY + 7, 0xFF0C1116);
        graphics.fill(barX + 1, barY + 1,
                barX + 1 + Math.round((barW - 2) * ratio), barY + 6,
                ratio >= 1.0f ? LOAD_RED : LOAD_GREEN);
        graphics.renderOutline(barX, barY, barW, 7, 0xFF465563);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Labels are drawn in renderBg so they align with the PNG frame.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        double baseMouseX = this.leftPos + (mouseX - this.leftPos) / this.uiScale;
        double baseMouseY = this.topPos + (mouseY - this.topPos) / this.uiScale;
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0);
        graphics.pose().scale(this.uiScale, this.uiScale, 1.0F);
        graphics.pose().translate(-this.leftPos, -this.topPos, 0);
        super.render(graphics, (int) baseMouseX, (int) baseMouseY, partialTick);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(
                this.leftPos + (mouseX - this.leftPos) / this.uiScale,
                this.topPos + (mouseY - this.topPos) / this.uiScale, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(
                this.leftPos + (mouseX - this.leftPos) / this.uiScale,
                this.topPos + (mouseY - this.topPos) / this.uiScale, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        return super.mouseDragged(
                this.leftPos + (mouseX - this.leftPos) / this.uiScale,
                this.topPos + (mouseY - this.topPos) / this.uiScale,
                button, dragX / this.uiScale, dragY / this.uiScale);
    }

}
