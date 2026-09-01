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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 天赋特质界面（默认按键 C 打开）。
 * <p>
 * - 60s 未启动：仅预览，无法加点（按钮禁用）。
 * - 60s 已启动：可点击分配；一旦点亮无法取消。
 * <p>
 * 布局（三栏式居中面板，不铺满全屏）：
 * - 顶部：标题 / 点数 / 模式 / 两个标签页按钮（正面特质 / 负面特质）。
 * - 左侧：当前分类的特质列表（可滚轮滚动），鼠标悬停按钮时显示该特质效果描述。
 * - 右侧：已拥有的正面特质与负面特质分别列出（名称 + 效果），带可拖拽的滚动条。
 */
public class SixtySecondsTraitScreen extends Screen {

    private SixtySecondsTrait.Category currentTab = SixtySecondsTrait.Category.POSITIVE;
    private int scroll = 0;            // 左侧列表滚动
    private int rightScroll = 0;       // 右侧已拥有列表滚动
    private boolean draggingRight = false;
    /** 上一次组件状态签名，用于检测服务端同步后自动刷新页面。 */
    private String lastSig = "";

    // 面板布局（在 init 中计算）
    private int panelX, panelY, panelW, panelH;

    private static final int ROW_H = 26;       // 每行特质高度
    private static final int HEADER_H = 96;    // 标题 + 点数 + 模式 + 标签 占用高度
    private static final int TAB_H = 22;
    private static final int TAB_GAP = 8;
    private static final int RIGHT_W = 168;    // 右侧面板宽度
    private static final int SCROLLBAR_W = 6;

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
        panelW = Math.min(620, this.width - 24);
        panelH = Math.min(440, this.height - 24);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;
    }

    /** 左侧列表区域范围。 */
    private int leftX() { return panelX + 12; }
    private int leftW() { return panelW - 24 - RIGHT_W - 12; }
    private int listTop() { return panelY + HEADER_H; }
    private int listBottom() { return panelY + panelH - 12; }

    /** 右侧面板区域范围。 */
    private int rightX() { return panelX + panelW - 12 - RIGHT_W; }
    private int rightY() { return panelY + HEADER_H; }
    private int rightH() { return panelH - HEADER_H - 12; }

    private List<SixtySecondsTrait.TraitDef> currentList() {
        return SixtySecondsTrait.TRAITS.stream()
                .filter(d -> d.category == currentTab)
                .collect(Collectors.toList());
    }

    private List<SixtySecondsTrait.TraitDef> ownedList(SixtySecondsTrait.Category cat) {
        Player player = Minecraft.getInstance().player;
        SixtySecondsTraitComponent comp = player == null ? null : SixtySecondsTraitComponent.KEY.get(player);
        if (comp == null) return new ArrayList<>();
        return SixtySecondsTrait.TRAITS.stream()
                .filter(d -> d.category == cat && comp.has(d.id))
                .collect(Collectors.toList());
    }

    private void buildButtons() {
        this.clearWidgets();

        Player player = Minecraft.getInstance().player;
        SixtySecondsTraitComponent comp = player == null ? null : SixtySecondsTraitComponent.KEY.get(player);
        boolean active = player != null && SixtySecondsMod.isActive(player.level());

        // 顶部两个标签页按钮（位于左侧区域上方）
        int tabW = (leftW() - TAB_GAP) / 2;
        int tabY = panelY + HEADER_H - TAB_H - 8;
        Button posTab = Button.builder(Component.translatable("trait.tab.positive"),
                b -> { currentTab = SixtySecondsTrait.Category.POSITIVE; scroll = 0; buildButtons(); })
                .bounds(leftX(), tabY, tabW, TAB_H).build();
        Button negTab = Button.builder(Component.translatable("trait.tab.negative"),
                b -> { currentTab = SixtySecondsTrait.Category.NEGATIVE; scroll = 0; buildButtons(); })
                .bounds(leftX() + tabW + TAB_GAP, tabY, tabW, TAB_H).build();
        this.addRenderableWidget(posTab);
        this.addRenderableWidget(negTab);

        // 当前分类的特质列表（左侧，可滚动）
        List<SixtySecondsTrait.TraitDef> cat = currentList();
        int top = listTop();
        int bottom = listBottom();
        int visible = Math.max(1, (bottom - top) / ROW_H);
        int maxScroll = Math.max(0, cat.size() - visible);
        if (scroll > maxScroll) scroll = maxScroll;
        if (scroll < 0) scroll = 0;

        int y = top;
        int bw = leftW();
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
            }).bounds(leftX(), y, bw, ROW_H - 4).build();
            btn.active = canPick || owned;
            // 鼠标悬停时显示该特质效果描述（左侧 tooltip）
            final SixtySecondsTrait.TraitDef def = d;
            btn.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.translatable(def.descKey)));
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
        int tabW = (leftW() - TAB_GAP) / 2;
        int tabY = panelY + HEADER_H - TAB_H - 8;
        int activeX = (currentTab == SixtySecondsTrait.Category.POSITIVE)
                ? leftX()
                : leftX() + tabW + TAB_GAP;
        g.fill(activeX - 2, tabY - 2, activeX + tabW + 2, tabY + TAB_H + 2, 0x33FFFFFF);

        // 左右区域分隔线
        int sepX = panelX + panelW - 12 - RIGHT_W - 6;
        g.vLine(sepX, panelY + HEADER_H - 4, panelY + panelH - 8, 0x554A6FA5);

        // 右侧面板标题
        g.drawString(this.font, Component.translatable("trait.screen.owned").getString(),
                rightX(), rightY() - 2, 0xFFD700);

        // 右侧已拥有列表（正面 / 负面 分开）
        renderOwned(g);

        // 右侧滚动条
        renderRightScrollbar(g);
    }

    private void renderOwned(GuiGraphics g) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        int rx = rightX() + 4;
        int rw = RIGHT_W - 8 - SCROLLBAR_W - 2;
        int top = rightY() + 12;
        int bottom = rightY() + rightH() - 4;

        // 裁剪到右侧区域
        g.enableScissor(rx - 2, top, rx + rw + SCROLLBAR_W + 4, bottom);

        int y = top - rightScroll;
        // 正面特质组
        List<SixtySecondsTrait.TraitDef> pos = ownedList(SixtySecondsTrait.Category.POSITIVE);
        List<SixtySecondsTrait.TraitDef> neg = ownedList(SixtySecondsTrait.Category.NEGATIVE);
        if (!pos.isEmpty()) {
            g.drawString(this.font, Component.translatable("trait.tab.positive").getString(), rx, y, 0x77FF88);
            y += 12;
            for (SixtySecondsTrait.TraitDef d : pos) {
                drawOwnedEntry(g, d, rx, y, rw);
                y += 30;
            }
            y += 4;
        }
        if (!neg.isEmpty()) {
            g.drawString(this.font, Component.translatable("trait.tab.negative").getString(), rx, y, 0xFF8888);
            y += 12;
            for (SixtySecondsTrait.TraitDef d : neg) {
                drawOwnedEntry(g, d, rx, y, rw);
                y += 30;
            }
        }

        g.disableScissor();

        // 计算右侧内容总高，用于滚动条
        int contentH = 0;
        if (!pos.isEmpty()) contentH += 12 + 4 + pos.size() * 30;
        if (!neg.isEmpty()) contentH += 12 + neg.size() * 30;
        contentH = Math.max(0, contentH - (bottom - top));
        if (rightScroll > contentH) rightScroll = contentH;
        if (rightScroll < 0) rightScroll = 0;
        this.rightContentH = contentH;
    }

    private int rightContentH = 0;

    private void drawOwnedEntry(GuiGraphics g, SixtySecondsTrait.TraitDef d, int x, int y, int w) {
        g.drawString(this.font, Component.translatable(d.nameKey).getString(), x, y, 0xFFFFFF);
        // 效果描述：自动换行（简单按宽度截断）
        List<String> lines = wrap(Component.translatable(d.descKey).getString(), w);
        int ly = y + 12;
        for (String ln : lines) {
            g.drawString(this.font, ln, x + 4, ly, 0xAAAAAA);
            ly += 9;
        }
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        StringBuilder line = new StringBuilder();
        for (char c : text.toCharArray()) {
            line.append(c);
            if (this.font.width(line.toString()) > maxW) {
                out.add(line.substring(0, Math.max(0, line.length() - 1)));
                line = new StringBuilder(String.valueOf(c));
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

    private void renderRightScrollbar(GuiGraphics g) {
        if (rightContentH <= 0) return;
        int top = rightY() + 12;
        int bottom = rightY() + rightH() - 4;
        int trackH = bottom - top;
        int rx = rightX() + RIGHT_W - 8 - SCROLLBAR_W;
        // 轨道
        g.fill(rx, top, rx + SCROLLBAR_W, bottom, 0x33000000);
        // 滑块
        int thumbH = Math.max(16, (int) ((double) trackH * trackH / (trackH + rightContentH)));
        int thumbY = top + (int) ((double) rightScroll / rightContentH * (trackH - thumbH));
        g.fill(rx, thumbY, rx + SCROLLBAR_W, thumbY + thumbH, 0xFF6A8FC5);
        this.rightThumbTop = thumbY;
        this.rightThumbH = thumbH;
        this.rightTrackTop = top;
        this.rightTrackH = trackH;
    }

    private int rightThumbTop = 0, rightThumbH = 0, rightTrackTop = 0, rightTrackH = 0;

    private boolean inRightScrollbar(int mx, int my) {
        if (rightContentH <= 0) return false;
        int rx = rightX() + RIGHT_W - 8 - SCROLLBAR_W;
        return mx >= rx && mx <= rx + SCROLLBAR_W
                && my >= rightTrackTop && my <= rightTrackTop + rightTrackH;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (mx >= rightX() && mx <= rightX() + RIGHT_W) {
            rightScroll = (int) Math.max(0, Math.min(rightContentH, rightScroll - scrollY * 12));
            return true;
        }
        List<SixtySecondsTrait.TraitDef> cat = currentList();
        int visible = Math.max(1, (listBottom() - listTop()) / ROW_H);
        int maxScroll = Math.max(0, cat.size() - visible);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - scrollY * 2));
        buildButtons();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inRightScrollbar((int) mx, (int) my)) {
            draggingRight = true;
            updateRightScrollFromMouse((int) my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingRight && button == 0) {
            draggingRight = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingRight) {
            updateRightScrollFromMouse((int) my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    private void updateRightScrollFromMouse(int my) {
        if (rightContentH <= 0) return;
        int trackH = rightTrackH;
        int thumbH = rightThumbH;
        int usable = trackH - thumbH;
        int rel = my - rightTrackTop;
        int thumbY = Math.max(rightTrackTop, Math.min(rightTrackTop + usable, rel - thumbH / 2));
        rightScroll = (int) ((double) (thumbY - rightTrackTop) / usable * rightContentH);
        rightScroll = Math.max(0, Math.min(rightContentH, rightScroll));
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
