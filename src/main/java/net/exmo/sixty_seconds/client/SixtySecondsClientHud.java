package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.Sixty_seconds;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.IGuiOverlay;
import net.neoforged.neoforge.client.gui.overlay.OverlayIdentifier;

/**
 * 末日60秒客户端的 HUD 图层与覆盖层管理。
 * <ul>
 *   <li>通过 NeoForge 的 {@link RegisterGuiLayersEvent} 注册一个位于游戏内 HUD 之上的自定义图层，
 *       每帧由 NeoForge 可靠调用，绘制 {@link SixtySecondsHud} 状态栏（比 CommonHudRenderCallback 更稳，不依赖其触发点）。</li>
 *   <li>通过 {@link RenderGuiOverlayEvent.Pre} 在进行中隐藏原版生命/饥饿/护甲/氧气/骑乘生命条，避免与原版 HUD 重叠。</li>
 * </ul>
 */
public final class SixtySecondsClientHud {
    private SixtySecondsClientHud() {
    }

    /** 当前是否处于本模组游戏进行中（几何与 GuiMixin 同一判断，依赖 SixtySecGameWorldComponent 同步）。 */
    public static boolean isActive() {
        SixtySecGameWorldComponent g = SixtySecBridgeClient.gameComponent;
        return g != null && g.isRunning() && g.getGameMode() == SixtySecondsMod.MODE;
    }

    /** 在 Mod 事件总线注册自定义 HUD 图层（游戏内 HUD 之上）。 */
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        OverlayIdentifier id = new OverlayIdentifier(
                ResourceLocation.fromNamespaceAndPath(Sixty_seconds.MODID, "hud"));
        IGuiOverlay layer = (GuiGraphics guiGraphics, DeltaTracker deltaTracker) -> {
            if (isActive()) {
                SixtySecondsHud.render(new FakeGuiGraphics(guiGraphics));
            }
        };
        event.register(id, layer);
    }

    /** 进行中时隐藏原版生命/饥饿/护甲/氧气/骑乘生命条。按覆盖层 id 判定，避免依赖 VanillaGuiOverlay 常量名。 */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!isActive()) {
            return;
        }
        ResourceLocation overlayId = event.getOverlay().id();
        if (overlayId.equals(ResourceLocation.fromNamespaceAndPath("neoforge", "health"))
                || overlayId.equals(ResourceLocation.fromNamespaceAndPath("neoforge", "food"))
                || overlayId.equals(ResourceLocation.fromNamespaceAndPath("neoforge", "armor"))
                || overlayId.equals(ResourceLocation.fromNamespaceAndPath("neoforge", "air"))
                || overlayId.equals(ResourceLocation.fromNamespaceAndPath("neoforge", "mount_health"))) {
            event.setCanceled(true);
        }
    }
}
