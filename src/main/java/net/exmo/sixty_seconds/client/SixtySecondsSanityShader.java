package net.exmo.sixty_seconds.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Random;

/**
 * 低理智视觉反馈（纯客户端）。
 */
public class SixtySecondsSanityShader {
    public static SixtySecondsSanityShader instance = new SixtySecondsSanityShader();

    /** 血丝覆盖贴图（本模组命名空间）。 */
    public static final ResourceLocation BLOOD_TENDRILS_OVERLAY =
            SixtySeconds.id("textures/overlay/blood_tendrils.png");

    /** 低理智幻听短句（理智 ≤70% 时抽取）。 */
    public static final MutableComponent[] HINTS0;
    /** 偏高理智时的零碎念头（理智 &gt;70% 时抽取）。 */
    public static final MutableComponent[] HINTS1;

    // ── 阈值（与 SRE 一致，分母是理智比例 0..1）──
    private static final float LOW_SAN_SHADER_START = 0.45f;
    private static final float LOW_SAN_SHADER_FULL = 0.0f;
    private static final float BLOOD_TENDRILS_START = 0.45f;
    private static final float BLOOD_TENDRILS_FULL_ALPHA = 0.3f;
    /** 幻觉短句：低于此值不再生成新低语（交给滤镜/血丝表现）。 */
    private static final float HINT_SPAWN_MIN = 0.4f;
    /** 幻觉短句：低于此值不再绘制。 */
    private static final float HINT_RENDER_MIN = 0.36f;
    /** 抽取 HINTS0（低理智组）的上界。 */
    private static final float HINT_LOW_GROUP_MAX = 0.7f;

    /** 血丝被动脉动前的静默时长（tick）。 */
    private static final float BT_DELAY = 5f * 20;

    // ── 电影效果参数 ──
    private static final float CINEMATIC_MOVEMENT_SPEED = 0.5f;
    private static final float CINEMATIC_MOVEMENT_RANGE = 10f;

    // ── 模糊效果参数（状态机保留；SRE 中 blur pass 未启用，此处同样未接 pass）──
    private static final float BLUR_MAX_INTENSITY = 0.8f;
    private static final float BLUR_MIN_INTENSITY = 0.2f;
    private static final float BLUR_TRIGGER_CHANCE = 0.003f;
    private static final float BLUR_DURATION_MIN = 20f;
    private static final float BLUR_DURATION_MAX = 60f;

    private final Minecraft m_mc;
    private final Random m_random = new Random();

    private PostProcessor m_post;
    private float m_dt;

    private float m_sanity;
    private float m_sanityGain;
    private float m_flashTimer;
    private float m_flashSanityGain;
    private float m_hintTimer;
    private float m_showingHintTimer;
    private float m_maxShowingHintTimer;

    private float m_btGainedAlpha;
    private float m_btDelay;
    private float m_btAlpha;
    private double m_btTimer;

    private float m_cinematicOffsetX = 0f;
    private float m_cinematicOffsetY = 0f;
    private float m_cinematicTimer = 0f;
    private float m_blurEffectTimer = 0f;
    private float m_blurEffectIntensity = 0f;
    private boolean m_isBlurActive = false;

    private MutableComponent m_hint;

    static {
        HINTS0 = new MutableComponent[12];
        for (int i = 0; i < HINTS0.length; i++) {
            HINTS0[i] = Component.translatable("gui." + SixtySeconds.MOD_ID + ".hint0" + i);
        }
        HINTS1 = new MutableComponent[9];
        for (int i = 0; i < HINTS1.length; i++) {
            HINTS1[i] = Component.translatable("gui." + SixtySeconds.MOD_ID + ".hint1" + i);
        }
    }

    public SixtySecondsSanityShader() {
        m_mc = Minecraft.getInstance();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 强度计算
    // ══════════════════════════════════════════════════════════════════════

    private static float getLowSanBaseIntensity(float sanity) {
        return Mth.clamp((LOW_SAN_SHADER_START - sanity)
                / (LOW_SAN_SHADER_START - LOW_SAN_SHADER_FULL), 0f, 1f);
    }

    private static float getBloodTendrilsAlpha(float sanity) {
        return Mth.clamp((BLOOD_TENDRILS_START - sanity)
                / (BLOOD_TENDRILS_START - BLOOD_TENDRILS_FULL_ALPHA), 0f, 1f);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 后处理
    // ══════════════════════════════════════════════════════════════════════

    private void initSanityPostProcess() {
        Minecraft mc = Minecraft.getInstance();
        m_post.addSinglePassEntry("sixty_seconds:insanity", pass -> processPlayer(mc.player, sanity -> {
            if (sanity > HINT_RENDER_MIN)
                return false;

            float intensity = getLowSanBaseIntensity((float) sanity);
            if (intensity <= 0.001f)
                return false;

            var effect = pass.getEffect();
            if (effect == null)
                return false;

            var desaturateUniform = effect.safeGetUniform("DesaturateFactor");
            if (desaturateUniform != null) {
                desaturateUniform.set(intensity * 0.69f);
            }

            var spreadUniform = effect.safeGetUniform("SpreadFactor");
            if (spreadUniform != null) {
                spreadUniform.set(intensity * 1.43f);
            }

            return true;
        }));

        m_post.addSinglePassEntry("sixty_seconds:chromatical", pass -> processPlayer(mc.player, sanity -> {
            if (sanity > HINT_RENDER_MIN)
                return false;

            float intensity = getLowSanBaseIntensity((float) sanity);
            if (intensity <= 0.001f)
                return false;

            var effect = pass.getEffect();
            if (effect == null)
                return false;

            var factorUniform = effect.safeGetUniform("Factor");
            if (factorUniform != null) {
                factorUniform.set(intensity * 0.1f);
            }

            var timeTotalUniform = effect.safeGetUniform("TimeTotal");
            if (timeTotalUniform != null) {
                timeTotalUniform.set(m_post.getTime() / 20.0f);
            }

            return true;
        }));
    }

    private boolean processPlayer(LocalPlayer player, java.util.function.DoublePredicate action) {
        return player != null
                && !player.isCreative() && !player.isSpectator()
                && action.test(currentSanity());
    }

    /** 当前理智比例（0..1）；不在 60s 局内返回 1（滤镜不生效）。 */
    private static float currentSanity() {
        return SixtySecondsStateAlerts.sanityScale();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 每 tick 驱动
    // ══════════════════════════════════════════════════════════════════════

    public void tick(LocalPlayer player, GuiGraphics context, float dt) {
        if (m_mc.player == null || m_mc.isPaused()
                || !SixtySecBridgeClient.isPlayerAliveAndInSurvivalIgnoreShitSplit()
                || SixtySecBridgeClient.gameComponent == null
                || !SixtySecBridgeClient.gameComponent.isRunning())
            return;

        m_sanity = currentSanity();
        m_dt = dt;

        if (m_flashTimer > 0)
            m_flashTimer -= dt;

        m_sanityGain = m_sanity;
        if (Math.abs(m_sanityGain) >= 0.01f)
            m_flashTimer = 20;
        m_flashSanityGain = m_flashTimer <= 0 ? 0 : m_flashSanityGain + m_sanityGain;

        tickHint(dt);
        tickBt(dt);
        updateCinematicEffect(dt);
        updateBlurEffect(dt);

        if (m_sanity <= BLOOD_TENDRILS_START) {
            renderBloodTendrilsOverlay(context.pose(), m_mc.getWindow().getGuiScaledWidth(),
                    m_mc.getWindow().getGuiScaledHeight());
        }
        renderHint(context, dt);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 电影化漂移 / 模糊状态机
    // ══════════════════════════════════════════════════════════════════════

    private void updateCinematicEffect(float dt) {
        m_cinematicTimer += dt * CINEMATIC_MOVEMENT_SPEED;

        m_cinematicOffsetX = (float) (Math.sin(m_cinematicTimer) * CINEMATIC_MOVEMENT_RANGE);
        m_cinematicOffsetY = (float) (Math.cos(m_cinematicTimer * 0.7f) * CINEMATIC_MOVEMENT_RANGE * 0.7f);

        // 理智越低，漂移幅度越大
        if (m_sanity < 0.3f) {
            m_cinematicOffsetX *= 1.5f;
            m_cinematicOffsetY *= 1.5f;
        }
    }

    private void updateBlurEffect(float dt) {
        if (m_sanity < 0.5f) {
            float triggerChance = BLUR_TRIGGER_CHANCE * (1.0f - m_sanity * 1.5f);
            if (m_random.nextFloat() < triggerChance && !m_isBlurActive) {
                m_isBlurActive = true;
                m_blurEffectTimer = BLUR_DURATION_MIN + m_random.nextFloat() * (BLUR_DURATION_MAX - BLUR_DURATION_MIN);
                m_blurEffectIntensity = BLUR_MIN_INTENSITY
                        + m_random.nextFloat() * (BLUR_MAX_INTENSITY - BLUR_MIN_INTENSITY);
            }
        }

        if (m_isBlurActive) {
            m_blurEffectTimer -= dt;
            if (m_blurEffectTimer > m_blurEffectIntensity * 10f) {
                m_blurEffectIntensity = Math.min(m_blurEffectIntensity + dt * 0.05f, BLUR_MAX_INTENSITY);
            } else if (m_blurEffectTimer < 20f) {
                m_blurEffectIntensity = Math.max(m_blurEffectIntensity - dt * 0.05f, 0f);
            }
            if (m_blurEffectTimer <= 0f) {
                m_isBlurActive = false;
                m_blurEffectIntensity = 0f;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 幻听低语
    // ══════════════════════════════════════════════════════════════════════

    private void tickHint(float dt) {
        if (m_sanity <= HINT_SPAWN_MIN)
            return;

        if (m_hintTimer <= 0f && m_showingHintTimer <= 0f) {
            if (m_sanity <= HINT_LOW_GROUP_MAX) {
                m_hint = HINTS0[m_random.nextInt(HINTS0.length)];
                m_hintTimer = 2000;
            } else {
                m_hint = HINTS1[m_random.nextInt(HINTS1.length)];
                m_hintTimer = 600;
            }
            m_showingHintTimer = (m_maxShowingHintTimer = 199f);
            m_cinematicTimer = 0f;
        }
        if (m_showingHintTimer > 0f)
            m_showingHintTimer -= dt;
        else
            m_hintTimer = Mth.clamp(m_hintTimer - dt, 0f, Float.MAX_VALUE);
    }

    private void renderHint(GuiGraphics context, float partialTicks) {
        if (m_mc.player == null || m_mc.player.isCreative() || m_mc.player.isSpectator()
                || m_hint == null || m_sanity > HINT_RENDER_MIN)
            return;

        Font font = m_mc.font;
        int scw = context.guiWidth();
        int sch = context.guiHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        PoseStack poseStack = context.pose();
        poseStack.pushPose();

        poseStack.translate(scw / 2f + m_cinematicOffsetX, sch / 2f + m_cinematicOffsetY, 0f);

        // 理智很低时整句轻微摇摆
        if (m_sanity < 0.2f) {
            float rotation = (float) Math.sin(m_cinematicTimer * 0.1f) * 0.5f;
            poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(rotation)));
        }

        poseStack.scale(3f, 3f, 1f);

        float o = ((int) m_showingHintTimer % 10) / 10f;
        o = ((int) m_showingHintTimer / 10) % 2 == 0 ? o : 1 - o;
        int opacity = Mth.clamp((int) (Mth.lerp(o,
                (m_showingHintTimer >= m_maxShowingHintTimer - 9f) || m_showingHintTimer < 10f ? 0f : .5f, 1f) * 0xFF),
                0x10, 0xEF) << 24;

        int pX = -font.width(m_hint) / 2;
        int pY = -font.lineHeight / 2;

        // 理智极低时压一层红色阴影
        if (m_sanity < 0.25f) {
            int shadowColor = 0xAA0000 | (opacity >> 24 << 24);
            context.drawString(font, m_hint, pX + 1, pY + 1, shadowColor, false);
        }
        context.drawString(font, m_hint, pX, pY, 0xFFFFFF | opacity, false);

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 血丝覆盖
    // ══════════════════════════════════════════════════════════════════════

    private void tickBt(float dt) {
        if (m_sanityGain >= .002f)
            m_btGainedAlpha = Mth.lerp(clampNorm(Mth.inverseLerp(m_sanityGain, .002f, .02f)), .4f, .75f);

        if (m_btGainedAlpha > 0f) {
            if (m_btAlpha < m_btGainedAlpha)
                m_btAlpha = Mth.clamp(m_btAlpha + .5f, 0f, m_btGainedAlpha);
            else
                m_btGainedAlpha = 0f;
        } else if (m_btDelay >= BT_DELAY) {
            if (m_btAlpha < .15f) {
                m_btTimer = 0;
                m_btAlpha = Mth.clamp(m_btAlpha + .1f, m_btAlpha, .15f);
            } else if (m_btAlpha > .3f) {
                m_btTimer = Mth.PI / .2f;
                m_btAlpha = Mth.clamp(m_btAlpha - .1f, .3f, m_btAlpha);
            } else {
                m_btAlpha = Mth.lerp((-Mth.cos((float) m_btTimer * .2f) + 1f) * .5f, .15f, .3f);
                m_btTimer += m_dt;
            }
        } else {
            m_btAlpha = Mth.clamp(m_btAlpha - .1f, 0f, m_btAlpha);
        }
        m_btDelay += dt;
    }

    private void renderBloodTendrilsOverlay(PoseStack poseStack, int scw, int sch) {
        if (m_mc.player == null || m_mc.player.isCreative() || m_mc.player.isSpectator())
            return;

        float moodAlpha = getBloodTendrilsAlpha(m_sanity);
        if (moodAlpha <= 0.001f) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        poseStack.pushPose();
        RenderSystem.setShaderTexture(0, BLOOD_TENDRILS_OVERLAY);

        float finalAlpha = Mth.clamp(moodAlpha * Math.max(m_btAlpha, 1f), 0f, 1f);
        renderFullscreen(poseStack, scw, sch, 100, 58, 0, 0, 100, 58, finalAlpha);

        poseStack.popPose();
        RenderSystem.disableBlend();
    }

    private static float clampNorm(float value) {
        return Mth.clamp(value, 0f, 1f);
    }

    private static void renderFullscreen(PoseStack poseStack, int scw, int sch, int texw, int texh, int uoffset,
            int voffset, int spritew, int spriteh, float alpha) {
        Matrix4f mat = poseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        final var begin = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        begin.addVertex(mat, 0f, 0f, 0f).setColor(1f, 1f, 1f, alpha).setUv((float) uoffset / texw,
                (float) voffset / texh);
        begin.addVertex(mat, 0f, (float) sch, 0f).setColor(1f, 1f, 1f, alpha).setUv((float) uoffset / texw,
                (float) (voffset + spriteh) / texh);
        begin.addVertex(mat, (float) scw, (float) sch, 0f).setColor(1f, 1f, 1f, alpha)
                .setUv((float) (uoffset + spritew) / texw, (float) (voffset + spriteh) / texh);
        begin.addVertex(mat, (float) scw, 0f, 0f).setColor(1f, 1f, 1f, alpha).setUv((float) (uoffset + spritew) / texw,
                (float) voffset / texh);
        BufferUploader.drawWithShader(begin.buildOrThrow());
        RenderSystem.disableBlend();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 生命周期
    // ══════════════════════════════════════════════════════════════════════

    public void initPostProcessor() {
        if (m_post != null)
            return;

        m_post = new PostProcessor();
        initSanityPostProcess();
    }

    public PostProcessor getPostProcessor() {
        return m_post;
    }

    public void renderPostProcess(float partialTicks) {
        if (m_post == null)
            return;

        // 不在 60s 局内时驱动值恒为 1（= 滤镜关闭），pass 自己会跳过，无需额外门禁
        m_post.render(partialTicks);
    }

    public void resize(int w, int h) {
        if (m_post == null)
            return;

        m_post.resize(w, h);
    }

    /** 重置所有视觉效果（换维度 / 退出对局 / 重连时调用）。 */
    public void resetVisualEffects() {
        m_cinematicOffsetX = 0f;
        m_cinematicOffsetY = 0f;
        m_cinematicTimer = 0f;
        m_blurEffectTimer = 0f;
        m_blurEffectIntensity = 0f;
        m_isBlurActive = false;
        m_btGainedAlpha = 0f;
        m_btDelay = 0f;
        m_btAlpha = 0f;
        m_btTimer = 0d;
        m_hintTimer = 0f;
        m_showingHintTimer = 0f;
        m_hint = null;
        m_sanity = 1f;
    }
}
