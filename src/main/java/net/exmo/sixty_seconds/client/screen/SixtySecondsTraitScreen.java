package net.exmo.sixty_seconds.client.screen;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.ClientPlayNetworking;
import net.exmo.sixty_seconds.network.TraitAllocateC2SPacket;
import net.exmo.sixty_seconds.traits.SixtySecondsTrait;
import net.exmo.sixty_seconds.traits.SixtySecondsTraitComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 天赋特质界面（默认按键 C 打开）。
 * <p>
 * - 60s 未启动：仅预览，无法加点（按钮禁用）。
 * - 60s 已启动：可点击分配；一旦点亮无法取消。
 * - 游戏结束时服务端会重置所有加点，界面重新打开即空白。
 * <p>
 * 布局：居中紧凑面板（不铺满全屏），顶部两个标签页按钮在「正面特质 / 负面特质」间切换。
 */
public class SixtySecondsTraitScreen extends Screen {

    private SixtySecondsTrait.Category currentTab = SixtySecondsTrait.Category.POSITIVE;
    private int scroll = 0;
    /** 上一次组件状态签名，用于检测服务端同步后自动刷新页面。 */
    private String lastSig = "";

    // 面板布局（在 init 中计算）
    private int panelX, panelY, panelW, panelH;

    private static final int ROW_H = 26;       // 每行特质高度
    private static final int HEADER_H = 96;    // 标题 + 点数 + 模式 + 标签 占用高度
    private static final int TAB_H = 22;
    private static final int TAB_GAP = 8;

    public SixtySecondsTraitScreen() {
        super(Component.translatable("trait.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        computeLayout();
        buildButtons();
    }

    private void computeLayout() {
        panelW = Math.min(380, this.width - 24);
        panelH = Math.min(440, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    private List<SixtySecondsTrait.TraitDef> currentList() {
        return SixtySecondsTrait.TRAITS.stream()
                .filter(d -> d.category == currentTab)
                .collect(Collectors.toList());
    }

    private void buildButtons() {
        this.clearWidgets();

        Player player = Minecraft.getInstance().player;
        SixtySecondsTraitComponent comp = player == null ? null : SixtySecondsTraitComponent.KEY.get(player);
        boolean active = player != null && SixtySecondsMod.isActive(player.level());

        // 顶部两个标签页按钮
        int tabW = (panelW - 24 - TAB_GAP) / 2;
        int tabY = panelY + HEADER_H - TAB_H - 8;
        Button posTab = Button.builder(Component.translatable("trait.tab.positive"),
                b -> { currentTab = SixtySecondsTrait.Category.POSITIVE; scroll = 0; buildButtons(); })
                .bounds(panelX + 12, tabY, tabW, TAB_H).build();
        Button negTab = Button.builder(Component.translatable("trait.tab.negative"),
                b -> { currentTab = SixtySecondsTrait.Category.NEGATIVE; scroll = 0; buildButtons(); })
                .bounds(panelX + 12 + tabW + TAB_GAP, tabY, tabW, TAB_H).build();
        this.addRenderableWidget(posTab);
        this.addRenderableWidget(negTab);

        // 当前分类的特质列表（可滚动）
        List<SixtySecondsTrait.TraitDef> cat = currentList();
        int listTop = panelY + HEADER_H;
        int listBottom = panelY + panelH - 12;
        int visible = Math.max(1, (listBottom - listTop) / ROW_H);
        int maxScroll = Math.max(0, cat.size() - visible);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        int y = listTop;
        for (int i = scroll; i < Math.min(scroll + visible, cat.size()); i++) {
            SixtySecondsTrait.TraitDef d = cat.get(i);
            boolean owned = comp != null && comp.has(d.id);
            boolean canPick = active && comp != null && !owned && comp.canAdd(d.id);
            String prefix = owned ? "✓ " : (d.isPositive() ? "-" + d.cost() + " " : "+" + d.cost() + " ");
            Component label = Component.literal(prefix).append(Component.translatable(d.nameKey));
            Button btn = Button.builder(label, b -> {
                if (canPick) {
                    ClientPlayNetworking.send(new TraitAllocateC2SPacket(d.id));
                }
            }).bounds(panelX + 12, y, panelW - 24, ROW_H - 4).build();
            btn.active = canPick || owned;
            this.addRenderableWidget(btn);
            y += ROW_H;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        // 不调用 super.renderBackground：避免铺满全屏的默认泥土背景，仅绘制居中的紧凑面板
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101320);
        g.renderOutline(panelX, panelY, panelW, panelH, 0xFF4A6FA5);

        // 当前选中标签页的高亮（绘制在标签按钮下方，super.render 会随后把按钮画在其上）
        int tabW = (panelW - 24 - TAB_GAP) / 2;
        int tabY = panelY + HEADER_H - TAB_H - 8;
        int activeX = (currentTab == SixtySecondsTrait.Category.POSITIVE)
                ? panelX + 12
                : panelX + 12 + tabW + TAB_GAP;
        g.fill(activeX - 2, tabY - 2, activeX + tabW + 2, tabY + TAB_H + 2, 0x33FFFFFF);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        List<SixtySecondsTrait.TraitDef> cat = currentList();
        int listTop = panelY + HEADER_H;
        int listBottom = panelY + panelH - 12;
        int visible = Math.max(1, (listBottom - listTop) / ROW_H);
        int maxScroll = Math.max(0, cat.size() - visible);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - scrollY * 2));
        buildButtons();
        return true;
    }

    /** 当服务端同步回的点数 / 已点亮特质变化时，重建按钮刷新页面状态。 */
    private void checkRefresh() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        SixtySecondsTraitComponent comp = SixtySecondsTraitComponent.KEY.get(player);
        boolean active = SixtySecondsMod.isActive(player.level());
        StringBuilder sb = new StringBuilder();
        sb.append(active ? 'A' : 'P').append('|').append(comp.points());
        for (SixtySecondsTrait.TraitDef d : SixtySecondsTrait.TRAITS) {
            if (comp.has(d.id)) sb.append('|').append(d.id);
        }
        String sig = sb.toString();
        if (!sig.equals(lastSig)) {
            lastSig = sig;
            buildButtons();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        checkRefresh();
        // super.render 在最上方：先绘制面板背景（renderBackground）再绘制所有控件
        super.render(g, mx, my, pt);

        Player player = Minecraft.getInstance().player;
        int points = player != null ? SixtySecondsTraitComponent.KEY.get(player).points() : 0;
        boolean active = player != null && SixtySecondsMod.isActive(player.level());

        int tx = panelX + 12;
        g.drawString(this.font, Component.translatable("trait.screen.title").getString(), tx, panelY + 10, 0xFFFFFF);
        g.drawString(this.font, Component.translatable("trait.screen.points", points).getString(), tx, panelY + 30, 0xFFD700);
        String mode = active
                ? Component.translatable("trait.screen.mode_active").getString()
                : Component.translatable("trait.screen.mode_preview").getString();
        g.drawString(this.font, mode, tx, panelY + 48, active ? 0x55FF55 : 0xFFAA55);
    }
}
