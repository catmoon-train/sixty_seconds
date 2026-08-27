package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.bridge.client.CommonHudRenderCallback;
import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.exmo.sixty_seconds.bridge.fabric.HudRenderCallback;
import net.exmo.sixty_seconds.client.SixtySecondsHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

/**
 * 末日60秒客户端的 HUD 与覆盖层管理。
 * <ul>
 *   <li>所有 HUD 绘制统一收敛到 {@link RenderGuiEvent.Post}（绘制在原生 GUI 图层之上，等同于原 registerAboveAll 的置顶效果）：
 *       主状态栏 {@link SixtySecondsHud} 与各子 HUD（经 {@link CommonHudRenderCallback} 与 {@link HudRenderCallback} 注册）均在此一帧内完成绘制。</li>
 *   <li>进行中通过 {@link RenderGuiLayerEvent.Pre} 隐藏原版生命/饥饿/护甲/氧气/骑乘生命条，避免与原版 HUD 重叠。
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

    /** 统一 HUD 绘制：每帧在所有原生 GUI 图层之上绘制本模组全部 HUD（主状态栏 + 各子 HUD）。 */
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!isActive()) {
            return;
        }
        GuiGraphics gui = event.getGuiGraphics();
        var delta = event.getPartialTick();
        SixtySecondsHud.render(new FakeGuiGraphics(gui));
        for (HudRenderCallback callback : HudRenderCallback.EVENT.invokers()) {
            callback.onHudRender(gui, delta);
        }
        FakeGuiGraphics fake = new FakeGuiGraphics(gui, true);
        for (var consumer : CommonHudRenderCallback.EVENT.getConsumer()) {
            consumer.accept(fake, delta);
        }
    }

    /** 进行中时隐藏原版生命/饥饿/护甲/氧气/骑乘生命条 */
    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!isActive()) {
            return;
        }
        ResourceLocation id = event.getName();
        if (id.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "health"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "food"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "armor"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "air"))
                || id.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "mount_health"))) {
            event.setCanceled(true);
        }
    }
}
