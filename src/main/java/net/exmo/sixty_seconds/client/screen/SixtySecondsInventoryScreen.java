package net.exmo.sixty_seconds.client.screen;

import com.mojang.datafixers.util.Pair;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.SixtySecPlayerMinigameTaskComponent;
import net.exmo.sixty_seconds.client.WeightConfigClient;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightCalc;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 60s 受限背包界面：<b>只显示快捷栏一条</b>，不画原版 176×166 背包网格。
 * 服务端侧的容器点击在 60s 模式已由 {@code SixtySecondsInventoryLimit} 放行，故这里只补客户端表现。
 */
public class SixtySecondsInventoryScreen extends LimitedHandledScreen<InventoryMenu> {

    public static final ResourceLocation BACKGROUND_TEXTURE =
            SixtySeconds.id("textures/gui/container/limited_inventory.png");

    /** 游戏币（代币）图标。 */
    private static final ResourceLocation GAME_COIN =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/game_coin.png");

    /** 装备槽在 InventoryMenu 中的菜单槽序号：头(5)/胸(6)/腿(7)/脚(8) + 副手(45)。 */
    private static final int[] EQUIP_SLOTS = { 5, 6, 7, 8, 45 };
    /** 背包第二排在 InventoryMenu 中的菜单槽序号（=容器槽 9..17）。 */
    private static final int[] BACKPACK_SLOTS = { 9, 10, 11, 12, 13, 14, 15, 16, 17 };

    private static final int SLOT_STEP = 18;
    /** 两排附加槽的行高（16 槽 + 双描边 3+3 + 2 间距）。 */
    private static final int ROW_STEP = 24;
    /**
     * 背景贴图里槽位框的起点：快捷栏槽内容绘制在背景 (8,8) 起、步进 18，
     * 故含 1px 描边的槽位框在贴图 (7,7) 处、每格 18×18。
     * 附加行直接按格数横向切割这条槽位框带，与快捷栏观感完全一致。
     */
    private static final int SLOT_TEX_U = 7;
    private static final int SLOT_TEX_V = 7;

    private static final int COIN_BTN_W = 90;
    private static final int COIN_BTN_H = 14;

    /** 游戏币余额距屏幕边缘的留白（屏幕右上角，与物品栏面板位置无关）。 */
    private static final int COIN_MARGIN = 8;
    /** 游戏币图标边长。 */
    private static final int COIN_ICON = 16;
    /** 图标与数字之间的间距。 */
    private static final int COIN_GAP = 2;

    private static final int COIN_TEXT = 0xFFFFF4DC;
    private static final int OUTLINE_DARK = 0xFF1A1813;
    private static final int OUTLINE_GOLD = 0xFFABA376;
    private static final int PLACEHOLDER_BG = 0xFF9E8B6E;

    public final LocalPlayer player;

    public SixtySecondsInventoryScreen(@NotNull LocalPlayer player) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("container.inventory"));
        this.player = player;
    }

    // ─────────────────────────────────────────────────────────────────
    // 附加行布局（相对屏幕左上角；与快捷栏窄条同 x，位于其下方）
    // ─────────────────────────────────────────────────────────────────

    /** 一排附加槽的左起 X（在窄条下方居中）。 */
    private int rowStartX(int slotCount) {
        return this.x + (this.backgroundWidth - slotCount * SLOT_STEP) / 2 + 1;
    }

    /** 背包第二排的顶部 Y（窄条正下方留出描边 3px + 间距 3px）。 */
    private int backpackRowY() {
        return this.y + this.backgroundHeight + 6;
    }

    /** 装备槽这一排的顶部 Y（背包第二排之下）。 */
    private int equipRowY() {
        return backpackRowY() + ROW_STEP;
    }

    private int coinButtonX() {
        return this.x + (this.backgroundWidth - COIN_BTN_W) / 2;
    }

    private int coinButtonY() {
        return equipRowY() + ROW_STEP;
    }

    private boolean inCoinButton(double mouseX, double mouseY) {
        int bx = coinButtonX();
        int by = coinButtonY();
        return mouseX >= bx && mouseX < bx + COIN_BTN_W && mouseY >= by && mouseY < by + COIN_BTN_H;
    }

    // ─────────────────────────────────────────────────────────────────
    // 绘制
    // ─────────────────────────────────────────────────────────────────

    @Override
    protected void drawBackground(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(BACKGROUND_TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        renderTokenBalance(context);
        renderWeight(context);
        ItemStack hoveredBackpack = renderRow(context, BACKPACK_SLOTS,
                rowStartX(BACKPACK_SLOTS.length), backpackRowY(), mouseX, mouseY);
        ItemStack hoveredEquip = renderRow(context, EQUIP_SLOTS,
                rowStartX(EQUIP_SLOTS.length), equipRowY(), mouseX, mouseY);
        renderCoinButton(context, mouseX, mouseY);

        if (this.handler.getCarried().isEmpty()) {
            if (!hoveredBackpack.isEmpty()) {
                context.renderTooltip(this.font, hoveredBackpack, mouseX, mouseY);
            } else if (!hoveredEquip.isEmpty()) {
                context.renderTooltip(this.font, hoveredEquip, mouseX, mouseY);
            }
        }
    }

    /** 画一排附加槽，返回当前 hover 的物品（无则 EMPTY）。 */
    private ItemStack renderRow(GuiGraphics context, int[] menuSlots, int startX, int rowY,
            int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ItemStack hovered = ItemStack.EMPTY;
        int w = menuSlots.length * SLOT_STEP;
        // 外深内金双描边：复刻背景面板自身的边框配色，否则附加行贴在深色界面上看不出边界
        context.renderOutline(startX - 3, rowY - 3, w + 4, 22, OUTLINE_DARK);
        context.renderOutline(startX - 2, rowY - 2, w + 2, 20, OUTLINE_GOLD);
        // 槽位底：从背景贴图的快捷栏槽位框整条切割（按槽数取宽）
        context.blit(BACKGROUND_TEXTURE, startX - 1, rowY - 1,
                SLOT_TEX_U, SLOT_TEX_V, w, 18);
        for (int i = 0; i < menuSlots.length; i++) {
            Slot slot = this.handler.slots.get(menuSlots[i]);
            int sx = startX + i * SLOT_STEP;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                Pair<ResourceLocation, ResourceLocation> icon = slot.getNoItemIcon();
                if (icon != null) {
                    // 占位剪影是纯 #555555，直接画在近黑槽底上不可见；先垫一层浅褐底
                    context.fill(sx, rowY, sx + 16, rowY + 16, PLACEHOLDER_BG);
                    TextureAtlasSprite sprite = mc.getTextureAtlas(icon.getFirst()).apply(icon.getSecond());
                    context.blit(sx, rowY, 0, 16, 16, sprite);
                }
            } else {
                context.renderItem(stack, sx, rowY);
                context.renderItemDecorations(font, stack, sx, rowY);
            }
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= rowY && mouseY < rowY + 16) {
                drawSlotHighlight(context, sx, rowY, 0);
                if (!stack.isEmpty()) {
                    hovered = stack;
                }
            }
        }
        return hovered;
    }

    /**
     * 屏幕右上角显示本人游戏币余额（图标取自 game_coin.png，与 HUD 一致）。
     *
     * <p>坐标基于<b>整个屏幕</b>（{@code this.width}）而非物品栏面板（{@code this.x + backgroundWidth}），
     * 因此面板位置/大小怎么变，余额都固定在屏幕右上角。
     * 绘制在 {@link #render} 而非 {@link #drawBackground} 中——此时位姿是屏幕空间，
     * 且画在槽位之上，不会被面板遮住。</p>
     */
    private void renderTokenBalance(GuiGraphics context) {
        int tokens = SixtySecPlayerMinigameTaskComponent.KEY.get(this.player).getTokens();
        Component text = Component.literal(String.valueOf(tokens));
        int textW = this.font.width(text);
        int iconX = this.width - COIN_MARGIN - textW - COIN_GAP - COIN_ICON;
        int y = COIN_MARGIN;
        context.blit(GAME_COIN, iconX, y, 0, 0, COIN_ICON, COIN_ICON, COIN_ICON, COIN_ICON);
        // 现在浮在半透明的世界画面上（不再垫着物品栏底图），加描边阴影保证可读
        context.drawString(this.font, text, iconX + COIN_ICON + COIN_GAP, y + 4, COIN_TEXT, true);
    }

    /**
     * 在右上角游戏币余额下方显示当前负重（仅在本模式且负重系统启用时）。
     * 配置取自客户端缓存；未打开过配置面板时回退到模组内置默认配置，因此不需要服务端额外发包。
     */
    private void renderWeight(GuiGraphics context) {
        SixtySecondsWeightConfig cfg = WeightConfigClient.getOrBuiltin();
        if (cfg == null || !cfg.enabled) {
            return;
        }
        if (!SixtySecondsMod.isActive(this.player.level())) {
            return;
        }
        double load = SixtySecondsWeightCalc.computeLoad(this.player, cfg);
        int ratio = (int) Math.min(100, load / Math.max(1e-4, net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.traitMaxLoad(this.player, cfg.maxLoad)) * 100);
        Component text = Component.translatable("hud.sixty_seconds.sixty_seconds.weight", String.format("%.1f", load), String.format("%.0f", net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.traitMaxLoad(this.player, cfg.maxLoad)));
        int textW = this.font.width(text);
        int x = this.width - COIN_MARGIN - textW;
        int y = COIN_MARGIN + COIN_ICON + 4; // 货币图标正下方
        int col = ratio >= 100 ? 0xFFCC3333 : 0xFFE6E6E6;
        context.drawString(this.font, text, x, y, col, true);
    }

    private void renderCoinButton(GuiGraphics context, int mouseX, int mouseY) {
        int bx = coinButtonX();
        int by = coinButtonY();
        boolean hovered = inCoinButton(mouseX, mouseY);
        context.fillGradient(bx, by, bx + COIN_BTN_W, by + COIN_BTN_H, 0xD81A1008, 0xD820140A);
        context.renderOutline(bx, by, COIN_BTN_W, COIN_BTN_H, hovered ? 0xFFD4AF37 : 0xFF8B6914);
        context.drawCenteredString(this.font,
                Component.translatable("message.sixty_seconds.sixty_seconds.coin_exchange_button"),
                bx + COIN_BTN_W / 2, by + (COIN_BTN_H - 8) / 2 + 1, hovered ? 0xFFFFF4DC : 0xFFC8B898);
    }

    // ─────────────────────────────────────────────────────────────────
    // 交互
    // ─────────────────────────────────────────────────────────────────

    /** 一排附加槽的命中测试：鼠标落在某槽内则返回对应 Slot。 */
    @Nullable
    private Slot rowSlotAt(int[] menuSlots, int startX, int rowY, double mouseX, double mouseY) {
        for (int i = 0; i < menuSlots.length; i++) {
            int sx = startX + i * SLOT_STEP;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= rowY && mouseY < rowY + 16) {
                return this.handler.slots.get(menuSlots[i]);
            }
        }
        return null;
    }

    /** 让附加行也参与命中测试——父类的拾取/放置/拖拽状态机由此自动覆盖它们。 */
    @Override
    @Nullable
    protected Slot getSlotAt(double x, double y) {
        Slot extra = rowSlotAt(BACKPACK_SLOTS, rowStartX(BACKPACK_SLOTS.length), backpackRowY(), x, y);
        if (extra == null) {
            extra = rowSlotAt(EQUIP_SLOTS, rowStartX(EQUIP_SLOTS.length), equipRowY(), x, y);
        }
        return extra != null ? extra : super.getSlotAt(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inCoinButton(mouseX, mouseY)) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            mc.setScreen(new TokenExchangeScreen());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        // Q → 屏障区批量丢弃；Shift+1~9 → 屏障区转移到对应快捷栏
        if (mc.options.keyDrop.matches(keyCode, scanCode) && !hasControlDown()) {
            quickDropFromBarrierArea();
            return true;
        }
        if (hasShiftDown()) {
            for (int i = 0; i < 9; i++) {
                if (mc.options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
                    transferFromBarrierToHotbar(i);
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ─────────────────────────────────────────────────────────────────
    // 屏障区快捷操作（被 SixtySecondsInventoryLimit 锁定的槽位塞有屏障占位）
    // ─────────────────────────────────────────────────────────────────

    /** 找到背包中第一个屏障占位槽的索引（0-35），没有则返回 36。 */
    private int findFirstBarrierSlot() {
        for (int i = 0; i <= 35; i++) {
            if (player.getInventory().getItem(i).is(Items.BARRIER)) {
                return i;
            }
        }
        return 36;
    }

    /** Q：把屏障区中非屏障物品全部丢到地面。 */
    private void quickDropFromBarrierArea() {
        int firstBarrier = findFirstBarrierSlot();
        if (firstBarrier > 35) {
            return;
        }
        for (int i = firstBarrier; i <= 35; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !stack.is(Items.BARRIER)) {
                this.minecraft.gameMode.handleInventoryMouseClick(
                        this.handler.containerId, i, 0, net.minecraft.world.inventory.ClickType.THROW,
                        this.minecraft.player);
            }
        }
    }

    /** Shift+数字键：把屏障区第一个非屏障物品换入指定快捷栏（索引 0-8）。 */
    private void transferFromBarrierToHotbar(int hotbarIndex) {
        int firstBarrier = findFirstBarrierSlot();
        if (firstBarrier > 35) {
            return;
        }
        for (int i = firstBarrier; i <= 35; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && !stack.is(Items.BARRIER)) {
                this.minecraft.gameMode.handleInventoryMouseClick(
                        this.handler.containerId, i, hotbarIndex,
                        net.minecraft.world.inventory.ClickType.SWAP, this.minecraft.player);
                return;
            }
        }
    }
}
