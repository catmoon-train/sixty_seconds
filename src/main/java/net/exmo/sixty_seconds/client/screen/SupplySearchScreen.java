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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 物资箱容器搜刮界面：复用原版箱子背景，放大镜槽位上叠加「搜刮读条」进度条。
 * <p>
 * 左键放大镜开始搜刮（播放开箱音效并按物品重量/搜刮速度计算的时长读条），读条完成后
 * 向服务端发送 {@link SupplySearchRevealC2SPacket} 请求揭示真实战利品。放大镜禁止任何形式的取出/丢弃/移动。
 */
public class SupplySearchScreen extends AbstractContainerScreen<SupplySearchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private final int rows = SupplySearchMenu.CONTAINER_ROWS;
    /** slotIndex -> 搜刮开始时的游戏刻。 */
    private final Map<Integer, Long> searchStart = new HashMap<>();
    /** slotIndex -> 搜刮所需刻数。 */
    private final Map<Integer, Integer> searchDuration = new HashMap<>();

    public SupplySearchScreen(SupplySearchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageHeight = 114 + this.rows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        g.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.rows * 18 + 17);
        g.blit(TEXTURE, i, j + this.rows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = this.minecraft.level.getGameTime();
        for (Integer slot : new ArrayList<>(searchStart.keySet())) {
            long elapsed = now - searchStart.get(slot);
            float prog = (float) elapsed / searchDuration.get(slot);
            if (prog >= 1f) {
                sendReveal(slot);
                searchStart.remove(slot);
                searchDuration.remove(slot);
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderSlot(GuiGraphics g, Slot slot) {
        super.renderSlot(g, slot);
        if (searchStart.containsKey(slot.index)) {
            long now = this.minecraft.level.getGameTime();
            long elapsed = now - searchStart.get(slot.index);
            float prog = (float) elapsed / searchDuration.get(slot.index);
            prog = Math.max(0f, Math.min(1f, prog));
            int x = slot.x;
            int y = slot.y;
            // 半透明暗化遮罩，表示正在搜刮
            g.fill(x, y, x + 16, y + 16, 0x80000000);
            // 底部进度条
            int barW = Math.round(14 * prog);
            g.fill(x + 1, y + 13, x + 15, y + 15, 0xFF222222);
            g.fill(x + 1, y + 13, x + 1 + barW, y + 15, 0xFF3FC46B);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && slot.hasItem() && SixtySecondsLootMagnifierItem.isMagnifier(slot.getItem())) {
            if (button == 0 && !searchStart.containsKey(slot.index)) {
                startSearch(slot.index);
            }
            // 放大镜只响应左键搜刮，拦截其它一切交互
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startSearch(int slotIndex) {
        ItemStack mag = this.menu.getSlot(slotIndex).getItem();
        int ticks = SixtySecondsLootMagnifierItem.getSearchTicks(mag);
        if (ticks <= 0) {
            ticks = SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS;
        }
        searchStart.put(slotIndex, this.minecraft.level.getGameTime());
        searchDuration.put(slotIndex, ticks);
        if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.CHEST_OPEN, 0.9f, 1.0f);
        }
    }

    private void sendReveal(int slotIndex) {
        ClientPlayNetworking.send(new SupplySearchRevealC2SPacket(slotIndex));
    }
}
