package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.SixtySecondsOceanSetup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * 创建世界界面注入「60秒·海洋」按钮（镜像 LostCities 的 ClientEventHandlers）。
 * 玩家在「更多选项」页可点此按钮打开配置 GUI，勾选后新建世界即被整体改写为海洋地图。
 */
@EventBusSubscriber(modid = "sixty_seconds", value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class SixtySecondsOceanClientHandler {
    private static Button oceanButton = null;

    private SixtySecondsOceanClientHandler() {
    }

    @SubscribeEvent
    public static void onGuiDraw(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen screen && oceanButton != null) {
            oceanButton.visible = screen.tabManager.getCurrentTab() instanceof CreateWorldScreen.MoreTab;
        }
    }

    @SubscribeEvent
    public static void onGuiPost(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen screen) {
            oceanButton = Button.builder(
                            net.minecraft.network.chat.Component.translatable(
                                    SixtySecondsOceanSetup.enabled
                                            ? "gui.sixty_seconds.ocean_world.button.on"
                                            : "gui.sixty_seconds.ocean_world.button.off"),
                            p -> Minecraft.getInstance().setScreen(new GuiSixtySecondsOcean(screen)))
                    .bounds(screen.width - 110, screen.height - 55, 100, 20)
                    .build();
            oceanButton.visible = false;
            event.addListener(oceanButton);
        }
    }
}
