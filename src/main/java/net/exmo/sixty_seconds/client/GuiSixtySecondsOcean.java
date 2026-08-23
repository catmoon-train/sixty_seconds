package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.SixtySecondsOceanSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * 海洋世界配置 GUI（镜像 LostCities 的 GuiLCConfig，精简为 4 项）。
 * 配置写入 common 端 {@link SixtySecondsOceanSetup}，由新建世界的内嵌服务端读取。
 */
public final class GuiSixtySecondsOcean extends Screen {
    private final Screen parent;

    private Button enableButton;
    private EditBox seedBox;
    private Button islandButton;
    private Button seaYButton;

    private boolean enabled = SixtySecondsOceanSetup.enabled;
    private long seed = SixtySecondsOceanSetup.oceanSeed;
    private int islands = SixtySecondsOceanSetup.oceanIslandCount;
    private int seaY = SixtySecondsOceanSetup.oceanSeaY;

    public GuiSixtySecondsOcean(Screen parent) {
        super(Component.translatable("gui.sixty_seconds.ocean_world.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;

        enableButton = Button.builder(
                        Component.translatable(enabled ? "gui.sixty_seconds.ocean_world.enabled.yes"
                                : "gui.sixty_seconds.ocean_world.enabled.no"),
                        b -> {
                            enabled = !enabled;
                            b.setMessage(Component.translatable(enabled ? "gui.sixty_seconds.ocean_world.enabled.yes"
                                    : "gui.sixty_seconds.ocean_world.enabled.no"));
                        })
                .bounds(cx - 100, 50, 200, 20).build();
        this.addRenderableWidget(enableButton);

        seedBox = new EditBox(this.font, cx - 100, 80, 200, 20, Component.literal("seed"));
        seedBox.setResponder(s -> {
            try {
                seed = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
            }
        });
        seedBox.setValue(String.valueOf(seed));
        this.addRenderableWidget(seedBox);

        islandButton = Button.builder(Component.translatable("gui.sixty_seconds.ocean_world.islands", islands),
                        b -> {
                            islands = (islands + 1) % 4; // 0..3
                            b.setMessage(Component.translatable("gui.sixty_seconds.ocean_world.islands", islands));
                        })
                .bounds(cx - 100, 110, 200, 20).build();
        this.addRenderableWidget(islandButton);

        seaYButton = Button.builder(Component.translatable("gui.sixty_seconds.ocean_world.sea_y", seaY),
                        b -> {
                            seaY += 8;
                            if (seaY > 120) seaY = 40;
                            b.setMessage(Component.translatable("gui.sixty_seconds.ocean_world.sea_y", seaY));
                        })
                .bounds(cx - 100, 140, 200, 20).build();
        this.addRenderableWidget(seaYButton);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.sixty_seconds.ocean_world.done"),
                        b -> this.onClose()).bounds(cx - 100, 180, 200, 20).build());
    }

    @Override
    public void onClose() {
        SixtySecondsOceanSetup.copyFrom(enabled, seed, islands, seaY);
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        this.renderBackground(gui, mx, my, pt);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        gui.drawString(this.font, Component.translatable("gui.sixty_seconds.ocean_world.seed"),
                this.width / 2 - 100, 68, 0xAAAAAA);
        super.render(gui, mx, my, pt);
    }
}
