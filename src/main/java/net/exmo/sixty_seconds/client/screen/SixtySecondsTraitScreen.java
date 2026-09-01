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

/**
 * 天赋特质界面（默认按键 C 打开）。
 * <p>
 * - 60s 未启动：仅预览，无法加点（按钮禁用）。
 * - 60s 已启动：可点击分配；一旦点亮无法取消。
 * - 游戏结束时服务端会重置所有加点，界面重新打开即空白。
 */
public class SixtySecondsTraitScreen extends Screen {

    private final List<SixtySecondsTrait.TraitDef> list = new ArrayList<>();
    private int scroll = 0;
    private static final int ROW_H = 22;
    private static final int TOP = 46;

    public SixtySecondsTraitScreen() {
        super(Component.translatable("trait.screen.title"));
        for (SixtySecondsTrait.TraitDef d : SixtySecondsTrait.TRAITS) {
            if (d.category == SixtySecondsTrait.Category.POSITIVE) {
                list.add(d);
            }
        }
        for (SixtySecondsTrait.TraitDef d : SixtySecondsTrait.TRAITS) {
            if (d.category == SixtySecondsTrait.Category.NEGATIVE) {
                list.add(d);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        buildButtons();
    }

    private void buildButtons() {
        this.clearWidgets();
        Player player = Minecraft.getInstance().player;
        SixtySecondsTraitComponent comp = player == null ? null : SixtySecondsTraitComponent.KEY.get(player);
        boolean active = player != null && SixtySecondsMod.isActive(player.level());
        int visible = Math.max(1, (this.height - TOP - 24) / ROW_H);
        int maxScroll = Math.max(0, list.size() - visible);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        if (scroll < 0) {
            scroll = 0;
        }
        int y = TOP;
        for (int i = scroll; i < Math.min(scroll + visible, list.size()); i++) {
            SixtySecondsTrait.TraitDef d = list.get(i);
            boolean owned = comp != null && comp.has(d.id);
            boolean canPick = active && comp != null && !owned && comp.canAdd(d.id);
            String prefix = owned ? "[✓] " : (d.isPositive() ? "[-" + d.cost() + "] " : "[+" + d.cost() + "] ");
            String label = prefix + Component.translatable(d.nameKey).getString();
            Button btn = Button.builder(Component.literal(label), b -> {
                if (canPick) {
                    ClientPlayNetworking.send(new TraitAllocateC2SPacket(d.id));
                }
            }).bounds(20, y, this.width - 40, ROW_H - 2).build();
            btn.active = canPick || owned;
            this.addRenderableWidget(btn);
            y += ROW_H;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        int visible = Math.max(1, (this.height - TOP - 24) / ROW_H);
        int maxScroll = Math.max(0, list.size() - visible);
        scroll = (int) Math.max(0, Math.min(maxScroll, scroll - scrollY * 2));
        buildButtons();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        Player player = Minecraft.getInstance().player;
        int points = player != null ? SixtySecondsTraitComponent.KEY.get(player).points() : 0;
        boolean active = player != null && SixtySecondsMod.isActive(player.level());
        g.drawString(this.font, Component.translatable("trait.screen.points", points).getString(), 20, 12, 0xFFFFFF);
        String mode = active
                ? Component.translatable("trait.screen.mode_active").getString()
                : Component.translatable("trait.screen.mode_preview").getString();
        g.drawString(this.font, mode, 20, 28, active ? 0x55FF55 : 0xFFAA55);
        super.render(g, mx, my, pt);
    }
}
