package net.exmo.sixty_seconds.bridge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FakeGuiGraphics {

    public static int trackCount = 0;
    

    private final GuiGraphics real;
    private boolean noOptimizing = false;

    public FakeGuiGraphics(GuiGraphics real) {
        this.real = real;
    }

    public FakeGuiGraphics(GuiGraphics real, boolean noOptimizing) {
        this.real = real;
        this.noOptimizing = true;
    }

    /** Access the wrapped real GuiGraphics when you need to escape the fake. */
    public GuiGraphics getDefaultGuiGraphics() {
        return real;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TEXT RENDERING — intercepted, batched, throttle-aware
    // ═════════════════════════════════════════════════════════════════════════

    public int drawString(Font font, @Nullable String string, int x, int y, int color) {
        return drawString(font, string, x, y, color, true);
    }

    public int drawString(Font font, @Nullable String string, int x, int y, int color, boolean shadow) {
        if (noOptimizing) {
            return real.drawString(font, string, x, y, color, shadow);
        }
        if (string == null)
            return 0;
        /* optimized path stripped */;
        return x + font.width(string);
    }

    public int drawString(Font font, FormattedCharSequence seq, int x, int y, int color) {
        return drawString(font, seq, x, y, color, true);
    }

    public int drawString(Font font, FormattedCharSequence seq, int x, int y, int color, boolean shadow) {
        if (noOptimizing) {
            return real.drawString(font, seq, x, y, color, shadow);
        }
        /* optimized path stripped */;
        return x + font.width(seq);
    }

    public int drawString(Font font, Component component, int x, int y, int color) {
        return drawString(font, component, x, y, color, true);
    }

    public int drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
        if (noOptimizing) {
            return real.drawString(font, component, x, y, color, shadow);
        }
        /* optimized path stripped */;
        return x + font.width(component);
    }

    public void drawCenteredString(Font font, String string, int cx, int y, int color) {
        if (noOptimizing) {
            real.drawCenteredString(font, string, cx, y, color);
            return;
        }
        drawString(font, string, cx - font.width(string) / 2, y, color);
    }

    public void drawCenteredString(Font font, Component component, int cx, int y, int color) {
        if (noOptimizing) {
            real.drawCenteredString(font, component, cx, y, color);
            return;
        }
        FormattedCharSequence seq = component.getVisualOrderText();
        drawString(font, seq, cx - font.width(seq) / 2, y, color);
    }

    public void drawCenteredString(Font font, FormattedCharSequence seq, int cx, int y, int color) {
        if (noOptimizing) {
            real.drawCenteredString(font, seq, cx, y, color);
            return;
        }
        drawString(font, seq, cx - font.width(seq) / 2, y, color);
    }

    public void drawWordWrap(Font font, FormattedText text, int x, int y, int maxWidth, int color) {
        for (FormattedCharSequence seq : font.split(text, maxWidth)) {
            drawString(font, seq, x, y, color, false);
            y += 9;
        }
    }

    public int drawStringWithBackdrop(Font font, Component component, int x, int y, int width, int color) {
        if (noOptimizing) {
            return real.drawStringWithBackdrop(font, component, x, y, width, color);
        }
        int bgColor = Minecraft.getInstance().options.getBackgroundColor(0.0F);
        if (bgColor != 0) {
            fill(x - 2, y - 2, x + width + 2, y + 9 + 2,
                    net.minecraft.util.FastColor.ARGB32.multiply(bgColor, color));
        }
        return drawString(font, component, x, y, color, true);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ALL OTHER GuiGraphics METHODS — cached via OptimizedTextRenderer
    // ═════════════════════════════════════════════════════════════════════════

    // ── Geometry ──────────────────────────────────────────────────────────────

    public PoseStack pose() {
        if (trackCount > 0) {
            SixtySeconds.LOGGER.info("[Track] Pose ", new Throwable());
        }
        return real.pose();
    }

    public MultiBufferSource.BufferSource bufferSource() {
        return real.bufferSource();
    }

    public int guiWidth() {
        return real.guiWidth();
    }

    public int guiHeight() {
        return real.guiHeight();
    }

    public void hLine(int x1, int x2, int y, int color) {
        if (noOptimizing) {
            real.hLine(x1, x2, y, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void hLine(RenderType rt, int x1, int x2, int y, int color) {
        if (noOptimizing) {
            real.hLine(rt, x1, x2, y, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void vLine(int x, int y1, int y2, int color) {
        if (noOptimizing) {
            real.vLine(x, y1, y2, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void vLine(RenderType rt, int x, int y1, int y2, int color) {
        if (noOptimizing) {
            real.vLine(rt, x, y1, y2, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        if (noOptimizing) {
            real.fill(x1, y1, x2, y2, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void fill(int x1, int y1, int x2, int y2, int z, int color) {
        if (noOptimizing) {
            real.fill(x1, y1, x2, y2, z, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void fill(RenderType rt, int x1, int y1, int x2, int y2, int color) {
        if (noOptimizing) {
            real.fill(rt, x1, y1, x2, y2, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void fill(RenderType rt, int x1, int y1, int x2, int y2, int z, int color) {
        if (noOptimizing) {
            real.fill(rt, x1, y1, x2, y2, z, color);
            return;
        }
        /* optimized path stripped */;
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int c1, int c2) {
        if (noOptimizing) {
            real.fillGradient(x1, y1, x2, y2, c1, c2);
            return;
        }
        /* optimized path stripped */;
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int z, int c1, int c2) {
        if (noOptimizing) {
            real.fillGradient(x1, y1, x2, y2, z, c1, c2);
            return;
        }
        /* optimized path stripped */;
    }

    public void fillGradient(RenderType rt, int x1, int y1, int x2, int y2, int c1, int c2, int z) {
        if (noOptimizing) {
            real.fillGradient(rt, x1, y1, x2, y2, c1, c2, z);
            return;
        }
        /* optimized path stripped */;
    }

    public void fillRenderType(RenderType rt, int x1, int y1, int x2, int y2, int z) {
        if (noOptimizing) {
            real.fillRenderType(rt, x1, y1, x2, y2, z);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderOutline(int x, int y, int w, int h, int color) {
        if (noOptimizing) {
            real.renderOutline(x, y, w, h, color);
            return;
        }
        /* optimized path stripped */;
    }

    // ── Scissor ───────────────────────────────────────────────────────────────

    public void enableScissor(int x1, int y1, int x2, int y2) {
        if (noOptimizing) {
            real.enableScissor(x1, y1, x2, y2);
            return;
        }
        /* optimized path stripped */;
    }

    public void disableScissor() {
        if (noOptimizing) {
            real.disableScissor();
            return;
        }
        /* optimized path stripped */;
    }

    public boolean containsPointInScissor(int x, int y) {
        return real.containsPointInScissor(x, y);
    }

    // ── Color & state ─────────────────────────────────────────────────────────

    public void setColor(float r, float g, float b, float a) {
        if (noOptimizing) {
            real.setColor(r, g, b, a);
            return;
        }
        /* optimized path stripped */;
    }

    // ── Blit / Sprites ────────────────────────────────────────────────────────

    public void blit(int x, int y, int z, int w, int h, TextureAtlasSprite sprite) {
        if (noOptimizing) {
            real.blit(x, y, z, w, h, sprite);
            return;
        }
        /* optimized path stripped */;
    }

    public void blit(int x, int y, int z, int w, int h, TextureAtlasSprite sprite, float r, float g, float b, float a) {
        if (noOptimizing) {
            real.blit(x, y, z, w, h, sprite, r, g, b, a);
            return;
        }
        /* optimized path stripped */;
    }

    public void blit(ResourceLocation loc, int x, int y, int z, float u, float v, int w, int h, int tw, int th) {
        if (noOptimizing) {
            real.blit(loc, x, y, z, u, v, w, h, tw, th);
            return;
        }
        /* optimized path stripped */;
    }

    public void blit(ResourceLocation loc, int x, int y, float u, float v, int w, int h, int tw, int th) {
        if (noOptimizing) {
            real.blit(loc, x, y, u, v, w, h, tw, th);
            return;
        }
        /* optimized path stripped */;
    }

    public void blit(ResourceLocation loc, int x, int y, int w, int h, float u, float v, int uw, int vh, int tw,
            int th) {
        if (noOptimizing) {
            real.blit(loc, x, y, w, h, u, v, uw, vh, tw, th);
            return;
        }
        /* optimized path stripped */;
    }

    public void blitSprite(ResourceLocation loc, int x, int y, int w, int h) {
        if (noOptimizing) {
            real.blitSprite(loc, x, y, w, h);
            return;
        }
        /* optimized path stripped */;
    }

    public void blitSprite(ResourceLocation loc, int x, int y, int z, int w, int h) {
        if (noOptimizing) {
            real.blitSprite(loc, x, y, z, w, h);
            return;
        }
        /* optimized path stripped */;
    }

    public void blitSprite(ResourceLocation loc, int tw, int th, int u, int v, int x, int y, int w, int h) {
        if (noOptimizing) {
            real.blitSprite(loc, tw, th, u, v, x, y, w, h);
            return;
        }
        /* optimized path stripped */;
    }

    public void blitSprite(ResourceLocation loc, int tw, int th, int u, int v, int x, int y, int z, int w, int h) {
        if (noOptimizing) {
            real.blitSprite(loc, tw, th, u, v, x, y, z, w, h);
            return;
        }
        /* optimized path stripped */;
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    public void renderItem(ItemStack stack, int x, int y) {
        if (noOptimizing) {
            real.renderItem(stack, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderItem(ItemStack stack, int x, int y, int seed) {
        if (noOptimizing) {
            real.renderItem(stack, x, y, seed);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderItem(ItemStack stack, int x, int y, int seed, int z) {
        if (noOptimizing) {
            real.renderItem(stack, x, y, seed, z);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderFakeItem(ItemStack stack, int x, int y) {
        if (noOptimizing) {
            real.renderFakeItem(stack, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderFakeItem(ItemStack stack, int x, int y, int seed) {
        if (noOptimizing) {
            real.renderFakeItem(stack, x, y, seed);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderItem(LivingEntity entity, ItemStack stack, int x, int y, int seed) {
        if (noOptimizing) {
            real.renderItem(entity, stack, x, y, seed);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (noOptimizing) {
            real.renderItemDecorations(font, stack, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String label) {
        if (noOptimizing) {
            real.renderItemDecorations(font, stack, x, y, label);
            return;
        }
        /* optimized path stripped */;
    }

    // ── Tooltips ──────────────────────────────────────────────────────────────

    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        if (noOptimizing) {
            real.renderTooltip(font, stack, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderTooltip(Font font, List<Component> lines, Optional<TooltipComponent> image, int x, int y) {
        if (noOptimizing) {
            real.renderTooltip(font, lines, image, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderTooltip(Font font, Component component, int x, int y) {
        if (noOptimizing) {
            real.renderTooltip(font, component, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderComponentTooltip(Font font, List<Component> lines, int x, int y) {
        if (noOptimizing) {
            real.renderComponentTooltip(font, lines, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderTooltip(Font font, List<? extends FormattedCharSequence> lines, int x, int y) {
        if (noOptimizing) {
            real.renderTooltip(font, lines, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    public void renderComponentHoverEffect(Font font, @Nullable Style style, int x, int y) {
        if (noOptimizing) {
            real.renderComponentHoverEffect(font, style, x, y);
            return;
        }
        /* optimized path stripped */;
    }

    // ── Managed block ──────────────────────────────────────────────────────────

    @Deprecated
    public void drawManaged(Runnable runnable) {
        if (noOptimizing) {
            real.drawManaged(runnable);
            return;
        }
        /* optimized path stripped */;
    }

    // ── innerBlit ─────────────────────────────────────────────────────────────

    public void innerBlit(ResourceLocation texture, int x1, int x2, int y1, int y2, int z,
            float u0, float u1, float v0, float v1, float r, float g, float b, float a) {
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, texture);
        com.mojang.blaze3d.systems.RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader);
        org.joml.Matrix4f matrix4f = pose().last().pose();
        com.mojang.blaze3d.vertex.BufferBuilder buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance()
                .begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                        com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix4f, (float) x1, (float) y1, (float) z).setUv(u0, v0).setColor(r, g, b, a);
        buffer.addVertex(matrix4f, (float) x1, (float) y2, (float) z).setUv(u0, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix4f, (float) x2, (float) y2, (float) z).setUv(u1, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix4f, (float) x2, (float) y1, (float) z).setUv(u1, v0).setColor(r, g, b, a);
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PLAYER FACE RENDERING
    // ═════════════════════════════════════════════════════════════════════════

    private static final int SKIN_HEAD_U = 8;
    private static final int SKIN_HEAD_V = 8;
    private static final int SKIN_HEAD_WIDTH = 8;
    private static final int SKIN_HEAD_HEIGHT = 8;
    private static final int SKIN_HAT_U = 40;
    private static final int SKIN_HAT_V = 8;
    private static final int SKIN_TEX_WIDTH = 64;
    private static final int SKIN_TEX_HEIGHT = 64;

    public void drawPlayerFace(ResourceLocation skinTexture, int x, int y, int size) {
        drawPlayerFace(skinTexture, x, y, size, true, false);
    }

    public void drawPlayerFace(ResourceLocation skinTexture, int x, int y, int size,
            boolean drawHat, boolean upsideDown) {
        int vOffset = SKIN_HEAD_V + (upsideDown ? SKIN_HEAD_HEIGHT : 0);
        int vHeight = SKIN_HEAD_HEIGHT * (upsideDown ? -1 : 1);

        // Head base layer – call our blit which respects noOptimizing
        blit(skinTexture, x, y, size, size,
                (float) SKIN_HEAD_U, (float) vOffset,
                SKIN_HEAD_WIDTH, vHeight,
                SKIN_TEX_WIDTH, SKIN_TEX_HEIGHT);

        if (drawHat) {
            int hatVOffset = SKIN_HAT_V + (upsideDown ? SKIN_HEAD_HEIGHT : 0);
            int hatVHeight = SKIN_HEAD_HEIGHT * (upsideDown ? -1 : 1);
            blit(skinTexture, x, y, size, size,
                    (float) SKIN_HAT_U, (float) hatVOffset,
                    SKIN_HEAD_WIDTH, hatVHeight,
                    SKIN_TEX_WIDTH, SKIN_TEX_HEIGHT);
        }
    }

    public void drawPlayerFace(net.minecraft.client.resources.PlayerSkin skin, int x, int y, int size) {
        drawPlayerFace(skin.texture(), x, y, size);
    }
}