package net.exmo.sixty_seconds.client.screen;

import net.exmo.sixty_seconds.bridge.SixtySecPlayerMinigameTaskComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

/**
 * 受限背包界面。
 * <ul>
 *   <li>仅隐藏被屏障锁定的槽位（与 {@code SixtySecondsInventoryLimit} 的屏障占位保持一致）。</li>
 *   <li>装备栏（4 格盔甲 + 副手）以专用金色边框高亮，并标注当前护甲值。</li>
 *   <li>底部提供「兑换实体币」按钮，打开 {@link TokenExchangeScreen} 把游戏币余额兑成实体币。</li>
 * </ul>
 */
public class SixtySecondsInventoryScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final ResourceLocation INVENTORY_TEXTURE =
            ResourceLocation.parse("minecraft:textures/gui/container/inventory.png");

    /** 游戏币（代币）图标。 */
    private static final ResourceLocation GAME_COIN =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/game_coin.png");

    /** 装备栏在 InventoryMenu 中的槽位序号：5–8 为盔甲，45 为副手。 */
    private static final int ARMOR_SLOT_START = 5;
    private static final int ARMOR_SLOT_END = 8;
    private static final int OFFHAND_SLOT = 45;

    private static final int EQUIP_BORDER = 0xFF8B6914;      // 装备栏金色边框
    private static final int EQUIP_BORDER_DIM = 0x66FFE8C0;  // 内侧高光
    private static final int EQUIP_TEXT = 0xFFE8D9A8;        // 装备栏文字（浅金）
    private static final int COIN_TEXT = 0xFFFFF4DC;         // 余额文字

    private static final int EXCHANGE_BTN_W = 96;
    private static final int EXCHANGE_BTN_H = 18;

    public SixtySecondsInventoryScreen(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int btnX = this.leftPos + (this.imageWidth - EXCHANGE_BTN_W) / 2;
        // 贴在背包下沿；窗口过矮时向内收，避免溢出屏幕
        int btnY = Math.min(this.topPos + this.imageHeight + 4, this.height - EXCHANGE_BTN_H - 2);
        this.addRenderableWidget(Button.builder(
                Component.translatable("message.sixty_seconds.sixty_seconds.coin_exchange_button"),
                b -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new TokenExchangeScreen());
                    }
                }).bounds(btnX, btnY, EXCHANGE_BTN_W, EXCHANGE_BTN_H).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.blit(INVENTORY_TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight);
        renderEquipmentFrames(guiGraphics, left, top);
        renderTokenBalance(guiGraphics, left, top);
    }

    /** 给装备栏（盔甲 4 格 + 副手）逐格描金边，与受限主背包区分开。 */
    private void renderEquipmentFrames(GuiGraphics guiGraphics, int left, int top) {
        for (int i = 0; i < this.menu.slots.size(); i++) {
            if (!isEquipmentSlot(i)) {
                continue;
            }
            Slot slot = this.menu.slots.get(i);
            int x = left + slot.x - 2;
            int y = top + slot.y - 2;
            guiGraphics.renderOutline(x, y, 20, 20, EQUIP_BORDER);
            guiGraphics.fill(x + 1, y + 1, x + 19, y + 2, EQUIP_BORDER_DIM);
        }

        int armorValue = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getArmorValue() : 0;
        Component label = Component.translatable("message.sixty_seconds.sixty_seconds.inventory_equipment");
        Component armorText = Component.translatable(
                "message.sixty_seconds.sixty_seconds.inventory_armor_value", armorValue);
        guiGraphics.drawString(this.font, label, left + 30, top + 8, EQUIP_TEXT, false);
        guiGraphics.drawString(this.font, armorText, left + 30, top + 20, EQUIP_TEXT, false);
    }

    private static boolean isEquipmentSlot(int index) {
        return (index >= ARMOR_SLOT_START && index <= ARMOR_SLOT_END) || index == OFFHAND_SLOT;
    }

    /** 右上角显示本人游戏币余额（图标取自 game_coin.png，与 HUD 一致）。 */
    private void renderTokenBalance(GuiGraphics guiGraphics, int left, int top) {
        int tokens = this.minecraft != null && this.minecraft.player != null
                ? SixtySecPlayerMinigameTaskComponent.KEY.get(this.minecraft.player).getTokens() : 0;
        Component text = Component.literal(String.valueOf(tokens));
        int textW = this.font.width(text);
        int iconX = left + this.imageWidth - 8 - textW - 18;
        int y = top + 6;
        guiGraphics.blit(GAME_COIN, iconX, y, 0, 0, 16, 16, 16, 16);
        guiGraphics.drawString(this.font, text, iconX + 18, y + 4, COIN_TEXT, false);
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
