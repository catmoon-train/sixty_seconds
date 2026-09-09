package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

/** Tarkov-inspired split inventory: player/equipment on the left, bag on the right. */
public class SpecialInventoryScreen extends AbstractContainerScreen<SpecialInventoryMenu> {
    private static final int PANEL = 0xE814171D;
    private static final int PANEL_EDGE = 0xFF59616B;
    private static final int SLOT = 0xB820252C;
    private static final int SLOT_LOCKED = 0x7022262C;
    private static final int ACCENT = 0xFFE0B25C;

    private PackedItemLayout.Layout backpackLayout;

    public SpecialInventoryScreen(SpecialInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 340;
        this.imageHeight = 190;
        this.titleLabelX = 14;
        this.titleLabelY = 7;
        this.inventoryLabelX = 150;
        this.inventoryLabelY = 7;
        rebuildLayout();
    }

    @Override
    protected void init() {
        rebuildLayout();
        this.imageHeight = Math.max(190, 36 + backpackLayout.rows() * 18);
        super.init();
    }

    private void rebuildLayout() {
        this.backpackLayout = PackedItemLayout.build(this.menu,
                SpecialInventoryMenu.PLAYER_MAIN_START,
                SpecialInventoryMenu.EXTRA_END,
                9,
                SpecialInventoryMenu.PLAYER_MAIN_END + this.menu.unlockedExtraSlots());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        rebuildLayout();
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth,
                this.topPos + this.imageHeight, PANEL);
        graphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, PANEL_EDGE);
        graphics.fill(this.leftPos + 130, this.topPos + 4, this.leftPos + 132,
                this.topPos + this.imageHeight - 4, 0xFF343B43);

        drawSlotBoxes(graphics);
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                this.leftPos + 12, this.topPos + 16,
                this.leftPos + 116, this.topPos + 105,
                45, 0.0F, mouseX, mouseY, this.menu.getPlayer());
    }

    private void drawSlotBoxes(GuiGraphics graphics) {
        for (Slot slot : this.menu.slots) {
            int x = slot.x;
            int y = slot.y;
            PackedItemLayout.Position packed = backpackLayout.position(slot.index);
            if (packed != null) {
                x = 150 + packed.column() * 18;
                y = 18 + packed.row() * 18;
            }
            if (x < 0 || y < 0 || x > this.imageWidth || y > this.imageHeight) continue;
            boolean locked = slot.index >= SpecialInventoryMenu.EXTRA_START
                    && slot.index < SpecialInventoryMenu.EXTRA_END
                    && slot.index - SpecialInventoryMenu.EXTRA_START >= this.menu.unlockedExtraSlots();
            graphics.fill(this.leftPos + x - 1, this.topPos + y - 1,
                    this.leftPos + x + 17, this.topPos + y + 17,
                    locked ? SLOT_LOCKED : SLOT);
            graphics.renderOutline(this.leftPos + x - 1, this.topPos + y - 1,
                    18, 18, locked ? 0xFF3C424B : 0xFF6E7883);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        PackedItemLayout.Position packed = backpackLayout.position(slot.index);
        if (packed != null) {
            int targetX = 150 + packed.column() * 18;
            int targetY = 18 + packed.row() * 18;
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
            if (area.width() > 1 || area.height() > 1) {
                PackedItemLayout.Position packed = backpackLayout.position(slot.index);
                int x = packed == null ? slot.x : 150 + packed.column() * 18;
                int y = packed == null ? slot.y : 18 + packed.row() * 18;
                graphics.renderOutline(this.leftPos + x - 1, this.topPos + y - 1,
                        area.width() * 18, area.height() * 18, ACCENT);
            }
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
            int left = 150 + packed.column() * 18;
            int top = 18 + packed.row() * 18;
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
