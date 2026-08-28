package net.exmo.sixty_seconds.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

/**
 * 受限背包界面。
 * 仅隐藏被屏障锁定的槽位。
 */
public class SixtySecondsInventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.parse("minecraft:textures/gui/container/inventory.png");

    public SixtySecondsInventoryScreen(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        // 与 SixtySecondsInventoryLimit 的屏障占位保持一致：被屏障锁定的槽位不渲染。
        if (slot.getItem().is(Items.BARRIER)) {
            return;
        }
        super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
    }
}
