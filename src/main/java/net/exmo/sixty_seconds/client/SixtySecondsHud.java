package net.exmo.sixty_seconds.client;

import net.exmo.sixty_seconds.bridge.client.SixtySecBridgeClient;
import net.exmo.sixty_seconds.bridge.client.FakeGuiGraphics;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.content.item.SixtySecondsClockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.client.CommonHudRenderCallback;
import net.exmo.sixty_seconds.bridge.SixtySecPlayerMinigameTaskComponent;
import net.exmo.sixty_seconds.weather.WeatherVisualConfig;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒模式 HUD：<b>血条（居中、紧贴物品栏）</b> + <b>右中下角状态竖排</b> + <b>左上角时间信息</b>。
 * <p>
 * 时间信息（第 X/N 天 · 家庭身份 · 时钟 · 警示）放在屏幕左上角，从 y=30 向下自动排列。
 * 血条居中、紧贴物品栏上方；饥饿/口渴/理智/污染 状态栏默认绘制在左中侧竖排（一行一个），
 * 可在客户端配置 hudSide 切换为右侧。
 * <ul>
 *   <li>健康值上限 = {@link SixtySecondsStatsComponent#HEALTH_MAX}（150），不再被 100 截断。</li>
 *   <li>理智上限缺口（杀人永久降上限）保留：sanityMax &lt; 100 时画暗红锁死区。</li>
 *   <li>低值（≤25%）脉冲红框警示；污染满值才警示。</li>
 *   <li>倒地 / 自动复活覆盖层画在屏幕中央（与面板位置无关）。</li>
 * </ul>
 */
public final class SixtySecondsHud {
    // ── 面板布局 ──
    private static final int PANEL_W = 200;
    private static final int PAD = 5;
    /** 原版 hotbar 顶端 y = guiHeight - 39。 */
    private static final int HOTBAR_TOP_OFFSET = 22;
    private static final int GAP_ABOVE_HOTBAR = 2;
    private static final int SEP_H = 2;
    private static final int BAR_H = 4;            // 纯色简约：原 5，-25% 厚度
    private static final int HEALTH_BAR_H = 5;     // 纯色简约：原 7，-25% 厚度
    private static final int VALUE_GAP = 2;
    private static final int VALUE_H = 9;
    private static final int STAT_GAP = 4;
    private static final int STAT_COUNT = 4; // 饥饿/口渴/理智/污染（健康单独一行）
    private static final int ROW_GAP = 4;                    // 健康行与 2×2 网格的间隔，与网格内上下排间隔(gridRowGap)一致
    /** 游戏币（代币）图标。 */
    private static final ResourceLocation GAME_COIN =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/game_coin.png");
    private static final double LOW_RATIO = 0.25;

    // ── 左上角信息布局 ──
    private static final int INFO_X = 16;
    private static final int INFO_Y_START = 30;
    private static final int INFO_LINE_H = 11;

    // ── 纯色简约配色（ARGB）──
    private static final int COL_TITLE = 0xFFE8D9A8;     // 标题/状态名（浅金）
    private static final int COL_FAMILY = 0xFF5EB7D8;    // 身份（功能蓝）
    private static final int COL_VALUE = 0xFFF0F0F0;     // 数值（亮白）
    private static final int COL_HEALTH_TRACK = 0xFF202020; // 血量背板（暗灰）

    /** 状态图标纹理（16x16，位于 textures/gui/hud/）。 */
    private static final ResourceLocation ICON_HEALTH =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/hud/hud_health.png");
    private static final ResourceLocation ICON_HUNGER =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/hud/hud_hunger.png");
    private static final ResourceLocation ICON_THIRST =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/hud/hud_thirst.png");
    private static final ResourceLocation ICON_SANITY =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/hud/hud_sanity.png");
    private static final ResourceLocation ICON_POLLUTION =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/gui/hud/hud_pollution.png");

    private static final int HEALTH_GAP = 0;                    // 血条直接贴物品栏上方（不再下移到物品栏内重叠）

    private SixtySecondsHud() {
    }

    public static void register() {
        // 自定义 HUD 现统一通过 RegisterGuiLayersEvent（SixtySecondsClientHud）每帧可靠绘制，
        // 不再依赖 CommonHudRenderCallback 的触发点，避免重复绘制。
    }

    /** 游戏未开始（开局准备阶段）时冻结的显示快照，避免数值在无游戏推进时跳动。 */
    private static SixtySecondsStatsComponent frozenSnapshot;

    private static void copyStats(SixtySecondsStatsComponent dst, SixtySecondsStatsComponent src) {
        dst.dayNumber = src.dayNumber;
        dst.totalDays = src.totalDays;
        dst.phaseEndTick = src.phaseEndTick;
        dst.health = src.health;
        dst.healthMax = src.healthMax;
        dst.hunger = src.hunger;
        dst.hungerMax = src.hungerMax;
        dst.thirst = src.thirst;
        dst.thirstMax = src.thirstMax;
        dst.sanity = src.sanity;
        dst.sanityMax = src.sanityMax;
        dst.pollution = src.pollution;
        dst.pollutionMax = src.pollutionMax;
        dst.teamId = src.teamId;
        dst.familyPosition = src.familyPosition;
        dst.sick = src.sick;
        dst.monster = src.monster;
        dst.downed = src.downed;
        dst.reviveEndTick = src.reviveEndTick;
        dst.exploreCooldownEndTick = src.exploreCooldownEndTick;
    }

    static void render(FakeGuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || !SixtySecBridgeClient.shouldShowHud()) {
            SixtySecondsStateAlerts.reset();
            SixtySecondsSanityShader.instance.resetVisualEffects();
            frozenSnapshot = null;
            return;
        }
        LocalPlayer player = client.player;
        SixtySecondsStatsComponent live = SixtySecondsStatsComponent.KEY.get(player);
        boolean running = SixtySecBridgeClient.gameComponent.isRunning();

        // 进行中：实时刷新显示；未开始（开局准备）：使用冻结快照，数值不变化
        SixtySecondsStatsComponent stats;
        if (running) {
            if (frozenSnapshot == null) {
                frozenSnapshot = new SixtySecondsStatsComponent(player);
            }
            copyStats(frozenSnapshot, live);
            stats = live;
        } else {
            if (frozenSnapshot == null) {
                frozenSnapshot = new SixtySecondsStatsComponent(player);
                copyStats(frozenSnapshot, live);
            }
            stats = frozenSnapshot;
            SixtySecondsStateAlerts.reset();
            SixtySecondsSanityShader.instance.resetVisualEffects();
        }

        if (running) {
            renderPrepBanner(graphics, client, stats);

            // 未分配家庭（旁观/未加入）不画状态栏
            if (stats.teamId < 0) {
                SixtySecondsStateAlerts.reset();
                SixtySecondsSanityShader.instance.resetVisualEffects();
                return;
            }
            SixtySecondsStateAlerts.tick(graphics, client, player, stats);
            // 低理智滤镜 / 血丝 / 幻听：后处理由 GameRendererMixin 每帧消费，这里只推进状态机与覆盖层
            SixtySecondsSanityShader.instance.tick(player, graphics.getDefaultGuiGraphics(),
                    client.getTimer().getGameTimeDeltaPartialTick(true));
        }

        if (stats.downed) {
            renderDownedOverlay(graphics, client, player, stats);
        }
        if (stats.reviveEndTick > 0L) {
            renderReviveOverlay(graphics, client, player, stats);
        }

        // ── 计算时间/状态信息 ──
        boolean hasFamily = stats.familyPosition != null;
        long gameTime = client.level.getGameTime();
        long remaining = stats.phaseEndTick - gameTime;
        boolean isDayPhase = stats.dayNumber >= 1 && remaining > 0
                && remaining <= net.exmo.sixty_seconds.SixtySecondsDayCycle.DAY_TOTAL_TICKS;
        boolean hasClock = isDayPhase && (hasClockInInventory(player) || isInShelterZone(player));
        boolean prepCountdown = stats.dayNumber == 0 && remaining > 0;
        long exploreCd = stats.exploreCooldownEndTick - gameTime;
        boolean hasExploreCd = exploreCd > 0;

        float pulse = 0.55f + 0.45f * Mth.sin(player.tickCount * 0.35f);

        // ── 左上角：时间信息（向下自动排列）──
        renderTopLeftInfo(graphics, client, stats, hasFamily, remaining, hasClock,
                prepCountdown, hasExploreCd, exploreCd, pulse);

        // ── 物品栏上方：状态面板 ──
        // vanilla=原版状态栏显示（默认，居中贴物品栏，2×2 网格 + 健康独占一行）
        // compact=简洁竖排（左/右侧）
        if (net.exmo.sixty_seconds.weather.WeatherVisualConfig.isVanillaHud()) {
            renderVanillaPanel(graphics, client, stats);
        } else {
            renderCompactPanel(graphics, client, stats);
        }
    }

    /**
     * 左上角时间信息：第 X/N 天 / 家庭身份 / 时钟 / 警示行，从 y=30 向下自动排列。
     */
    private static void renderTopLeftInfo(FakeGuiGraphics graphics, Minecraft client,
            SixtySecondsStatsComponent stats, boolean hasFamily, long remaining, boolean hasClock,
            boolean prepCountdown, boolean hasExploreCd, long exploreCd, float pulse) {
        int x = INFO_X;
        int y = INFO_Y_START;

        // 第 X/N 天
        graphics.drawString(client.font,
                Component.translatable("hud.sixty_seconds.sixty_seconds.day",
                        Math.max(0, stats.dayNumber), stats.totalDays),
                x, y, COL_TITLE);
        y += INFO_LINE_H;

        // 家庭身份
        if (hasFamily) {
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.family."
                            + stats.familyPosition.name().toLowerCase()),
                    x, y, COL_FAMILY);
            y += INFO_LINE_H;
        }

        // 日内时钟
        if (hasClock) {
            boolean sleep = net.exmo.sixty_seconds.SixtySecondsDayCycle.isSleepWindowByRemaining(remaining);
            Component subName = sleep
                    ? Component.translatable("hud.sixty_seconds.sixty_seconds.subphase.sleep")
                    : Component.translatable(net.exmo.sixty_seconds.SixtySecondsDayCycle
                            .subPhaseByRemaining(remaining).translationKey());
            long left = net.exmo.sixty_seconds.SixtySecondsDayCycle.subPhaseRemainingByRemaining(remaining);
            long seconds = left / 20;
            String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
            int color = sleep ? 0xFFB06AE6
                    : net.exmo.sixty_seconds.SixtySecondsDayCycle.subPhaseByRemaining(remaining)
                            == net.exmo.sixty_seconds.SixtySecondsDayCycle.SubPhase.NIGHT
                                    ? 0xFF6FA8FF : 0xFFFFD08A;
            graphics.drawString(client.font,
                    Component.empty().append(subName).append(Component.literal(" " + time)), x, y, color);
            y += INFO_LINE_H;
            // 游戏币（代币）余额：显示在时钟下方；图标取自 SixtySeconds
            LocalPlayer coinPlayer = client.player;
            if (coinPlayer != null) {
                int tokens = SixtySecPlayerMinigameTaskComponent.KEY.get(coinPlayer).getTokens();
                graphics.getDefaultGuiGraphics().blit(GAME_COIN, x, y, 0, 0, 16, 16, 16, 16);
                graphics.drawString(client.font, " " + tokens, x + 18, y + 4, 0xFFFFFF);
                y += INFO_LINE_H;
            }
        }

        // 准备阶段倒计时（最后 10 秒红色脉冲）
        if (prepCountdown) {
            int seconds = (int) Math.ceil(remaining / 20.0);
            int color = seconds <= 10 ? (((int) (0x80 + 0x7F * pulse)) << 24 | 0xFF5050) : 0xFFFFD08A;
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.prep_countdown", seconds), x, y, color);
            y += INFO_LINE_H;
        }

        // 生病警示
        if (stats.sick) {
            int color = ((int) (0x90 + 0x6F * pulse)) << 24 | 0xFF6060;
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.sick_warning"), x, y, color);
            y += INFO_LINE_H;
        }

        // 探索归来冷却
        if (hasExploreCd) {
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.explore_cooldown",
                            (int) Math.ceil(exploreCd / 20.0)), x, y, COL_FAMILY);
            y += INFO_LINE_H;
        }
    }

    /**
     * 简洁竖排面板（compact 模式，纯色简约风 bar + 深色背景面板）：
     * <ul>
     *   <li>健康条：居中、紧贴物品栏上方；健康数值在右侧。</li>
     *   <li>饥饿/口渴/理智/污染：移至右中下角、竖排（一行一个）；数值位置保持（bar 下方居中）。</li>
     *   <li>按下 Shift 时在每个状态条的数值上方显示其名称。</li>
     * </ul>
     */
    private static void renderCompactPanel(FakeGuiGraphics graphics, Minecraft client,
            SixtySecondsStatsComponent stats) {
        boolean shift = isShiftDown(client);
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int usableW = PANEL_W - PAD * 2;

        // ── 健康条：居中、紧贴物品栏上方（含背景）──
        String healthText = String.valueOf(Mth.clamp(stats.health, 0, stats.healthMax));
        int healthTextW = client.font.width(healthText);
        int healthBarW = usableW - healthTextW - 4;
        int healthH = Math.max(HEALTH_BAR_H, VALUE_H);
        int healthPanelH = PAD + healthH + PAD;
        int healthPanelY = screenH - HOTBAR_TOP_OFFSET - healthPanelH - HEALTH_GAP;

        int hx = (screenW - PANEL_W) / 2 + PAD;
        int hy = healthPanelY + PAD;
        int healthValX = hx + healthBarW + 4;
        drawHealthBar(graphics, client, hx, hy, healthBarW, stats.health, stats.healthMax, 0xFFE64848);
        graphics.drawString(client.font, healthText, healthValX, hy, COL_VALUE);
        if (shift) {
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.health"),
                    healthValX, hy - VALUE_H - 1, COL_TITLE);
        }

        // ── 其他状态条：右中下角竖排（背景面板 + 背板色条 + 文字左/数值右）──
        int statBarW = 120; // 1.5x
        int ROW_GAP_V = 1;  // 紧贴
        int statsRight = 10;
        int ROW_H = Math.max(BAR_H, VALUE_H); // 9

        int[] statValues = {stats.hunger, stats.thirst, stats.sanity, stats.pollution};
        int[] statMaxes = {stats.hungerMax, stats.thirstMax,
                Math.max(stats.sanityMax, SixtySecondsStatsComponent.MAX), stats.pollutionMax};
        int[] statColors = {0xFFE0A030, 0xFF37A7E6, 0xFFB06AE6, 0xFF74B04A};
        boolean[] highIsBad = {false, false, false, true};
        net.minecraft.network.chat.Component[] statNames = {
                Component.translatable("hud.sixty_seconds.sixty_seconds.hunger"),
                Component.translatable("hud.sixty_seconds.sixty_seconds.thirst"),
                Component.translatable("hud.sixty_seconds.sixty_seconds.sanity"),
                Component.translatable("hud.sixty_seconds.sixty_seconds.pollution")
        };

        // 计算最大文字宽
        int nameMaxW = 0, valMaxW = 0;
        for (net.minecraft.network.chat.Component n : statNames) nameMaxW = Math.max(nameMaxW, client.font.width(n));
        for (int v : statValues) valMaxW = Math.max(valMaxW, client.font.width(String.valueOf(v)));

        int rowContentW = nameMaxW + 4 + statBarW + 4 + valMaxW;
        int panelW = rowContentW + PAD * 2;
        int panelH = PAD + STAT_COUNT * ROW_H + (STAT_COUNT - 1) * ROW_GAP_V + PAD;

        // 状态栏位置：默认左侧（左中侧），可在客户端配置 hudSide 切换为右侧；整体贴着物品栏上方
        boolean left = WeatherVisualConfig.isHudLeft();
        int panelX = left ? statsRight : screenW - statsRight - panelW;
        int panelY = screenH - HOTBAR_TOP_OFFSET - panelH - 2;

        // 不绘制黑色透明背景（按需求）

        for (int i = 0; i < STAT_COUNT; i++) {
            int ry = panelY + PAD + i * (ROW_H + ROW_GAP_V);
            int cy = ry + (ROW_H - VALUE_H) / 2; // 文字垂直居中

            // 左侧：名称
            graphics.drawString(client.font, statNames[i], panelX + PAD, cy, COL_TITLE);

            // 中间：色条（背板 + 填充）
            int barX = panelX + PAD + nameMaxW + 4;
            int barY = ry + (ROW_H - BAR_H) / 2;
            graphics.fill(barX, barY, barX + statBarW, barY + BAR_H, COL_HEALTH_TRACK); // 背板

            int clamped = Mth.clamp(statValues[i], 0, statMaxes[i]);
            double ratio = statMaxes[i] > 0 ? clamped / (double) statMaxes[i] : 0;
            boolean low = highIsBad[i] ? clamped >= statMaxes[i] : ratio <= LOW_RATIO;
            int fill = low ? 0xFFFF6060 : statColors[i];
            int fillW = (int) Math.round(statBarW * ratio);
            if (fillW > 0) {
                graphics.fill(barX, barY, barX + fillW, barY + BAR_H, fill);
            }

            // 右侧：数值
            graphics.drawString(client.font, String.valueOf(clamped), barX + statBarW + 4, cy,
                    low ? 0xFFFF6060 : COL_VALUE);
        }
    }

    /**
     * 原版状态栏显示（vanilla 模式，默认）：居中紧贴物品栏上方。
     * <ul>
     *   <li>2×2 网格：理智值/污染值（上排），饥饿值/口渴值（下排），每格 = 图标 + 条。</li>
     *   <li>健康值独占一行（图标 + 横跨整行条），位于 2×2 网格之上、物品栏之下。</li>
     *   <li>按下 Shift 时在每个状态条上方显示其名称与数值。</li>
     * </ul>
     */
    private static void renderVanillaPanel(FakeGuiGraphics graphics, Minecraft client,
            SixtySecondsStatsComponent stats) {
        boolean shift = isShiftDown(client);
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int usableW = PANEL_W - PAD * 2;

        int iconSize = 12;
        int iconBarGap = 3;
        int gridRowGap = 4;
        int colGap = 8;

        // 每个状态条宽度（2 列）
        int colW = (usableW - colGap) / 2;
        int barW = colW - iconSize - iconBarGap;

        int gridRowH = Math.max(BAR_H, iconSize) + gridRowGap;
        int gridH = 2 * (Math.max(BAR_H, iconSize)) + gridRowGap; // 两行图标/条高度
        int healthRowH = Math.max(HEALTH_BAR_H, iconSize);
        int panelH = PAD + gridH + ROW_GAP + healthRowH + PAD;

        // 紧贴物品栏上方
        int bottomY = screenH - HOTBAR_TOP_OFFSET - GAP_ABOVE_HOTBAR;
        int panelY = bottomY - panelH;
        int panelX = (screenW - PANEL_W) / 2;

        // 不绘制黑色透明背景（按需求）

        // 2×2 网格状态定义
        Stat[] cells = {
                new Stat("sanity", stats.sanity, Math.max(stats.sanityMax, SixtySecondsStatsComponent.MAX),
                        0xFFB06AE6, false),
                new Stat("pollution", stats.pollution, stats.pollutionMax, 0xFF74B04A, true),
                new Stat("hunger", stats.hunger, stats.hungerMax, 0xFFE0A030, false),
                new Stat("thirst", stats.thirst, stats.thirstMax, 0xFF37A7E6, false),
        };

        int gridTop = panelY + PAD;
        int leftColX = panelX + PAD;
        int rightColX = panelX + PAD + colW + colGap;
        for (int r = 0; r < 2; r++) {
            int y = gridTop + r * (Math.max(BAR_H, iconSize) + gridRowGap);
            drawStatCell(graphics, client, leftColX, y, iconSize, barW, cells[r * 2], shift);
            drawStatCell(graphics, client, rightColX, y, iconSize, barW, cells[r * 2 + 1], shift);
        }

        // 健康独占一行（图标 + 整行条）
        int healthY = gridTop + gridH + ROW_GAP;
        int hIconX = panelX + PAD;
        int hBarX = hIconX + iconSize + iconBarGap;
        int hBarW = usableW - iconSize - iconBarGap;
        drawIcon(graphics, "health", hIconX, healthY, iconSize, 0xFFE64848);
        drawHealthBar(graphics, client, hBarX, healthY + (HEALTH_BAR_H - BAR_H) / 2, hBarW,
                stats.health, stats.healthMax, 0xFFE64848);
        if (shift) {
            graphics.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds.health"),
                    hBarX, healthY - BAR_H - 1, COL_TITLE);
            graphics.drawString(client.font, String.valueOf(Mth.clamp(stats.health, 0, stats.healthMax)),
                    hBarX + hBarW + 2, healthY + (HEALTH_BAR_H - VALUE_H) / 2, COL_VALUE);
        }
    }

    /** 单个状态单元格：图标 + 条（+Shift 时名称与数值）。 */
    private static void drawStatCell(FakeGuiGraphics g, Minecraft client, int x, int y,
            int iconSize, int barW, Stat s, boolean shift) {
        drawIcon(g, s.key, x, y, iconSize, s.color);
        int barX = x + iconSize + 3;
        int barY = y + (iconSize - BAR_H) / 2;
        g.fill(barX, barY, barX + barW, barY + BAR_H, COL_HEALTH_TRACK);

        int clamped = Mth.clamp(s.value, 0, s.max);
        double ratio = s.max > 0 ? clamped / (double) s.max : 0;
        boolean low = s.highIsBad ? clamped >= s.max : ratio <= LOW_RATIO;
        int fill = low ? 0xFFFF6060 : s.color;
        int fillW = (int) Math.round(barW * ratio);
        if (fillW > 0) {
            g.fill(barX, barY, barX + fillW, barY + BAR_H, fill);
        }
        if (shift) {
            g.drawString(client.font,
                    Component.translatable("hud.sixty_seconds.sixty_seconds." + s.key),
                    barX, barY - BAR_H - 1, COL_TITLE);
            g.drawString(client.font, String.valueOf(clamped), barX + barW + 2, barY, COL_VALUE);
        }
    }

    /** 状态单元数据载体。 */
    private static final class Stat {
        final String key;
        final int value;
        final int max;
        final int color;
        final boolean highIsBad;

        Stat(String key, int value, int max, int color, boolean highIsBad) {
            this.key = key;
            this.value = value;
            this.max = max;
            this.color = color;
            this.highIsBad = highIsBad;
        }
    }

    /**
     * 绘制状态图标（从 PNG 纹理加载）：将 16x16 纹理缩放到 s 大小，绘制在 (x, y)。
     */
    private static void drawIcon(FakeGuiGraphics g, String key, int x, int y, int s, int color) {
        ResourceLocation tex = switch (key) {
            case "health" -> ICON_HEALTH;
            case "hunger" -> ICON_HUNGER;
            case "thirst" -> ICON_THIRST;
            case "sanity" -> ICON_SANITY;
            case "pollution" -> ICON_POLLUTION;
            default -> null;
        };
        if (tex == null) {
            g.fill(x, y, s, s, color);
            return;
        }
        // 完整绘制 16x16 纹理并缩放到 s，避免只显示四分之一
        g.blit(tex, x, y, s, s, 0, 0, 16, 16, 16, 16);
    }

    /**
     * 倒地覆盖层（屏幕中央准星下方）。
     */
    private static void renderDownedOverlay(FakeGuiGraphics graphics, Minecraft client, LocalPlayer player,
            SixtySecondsStatsComponent stats) {
        float pulse = 0.55f + 0.45f * Mth.sin(player.tickCount * 0.35f);
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;

        Component title = Component.translatable("hud.sixty_seconds.sixty_seconds.downed_title");
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy + 28, 0);
        graphics.pose().scale(1.5f, 1.5f, 1f);
        int alpha = Mth.clamp((int) (0xB0 + 0x4F * pulse), 0, 0xFF);
        graphics.drawString(client.font, title, -client.font.width(title) / 2, 0, (alpha << 24) | 0xFF4040);
        graphics.pose().popPose();

        int y = cy + 46;
        int health = stats.health;
        Component healthText = Component.translatable("hud.sixty_seconds.sixty_seconds.downed_health", health);
        int healthColor = health > 15 ? 0xFFFFA0A0 : 0xFFFF4040;
        graphics.drawString(client.font, healthText, cx - client.font.width(healthText) / 2, y, healthColor);
        y += 11;

        Component hint = Component.translatable("hud.sixty_seconds.sixty_seconds.downed_hint",
                net.exmo.sixty_seconds.logic.SixtySecondsHealthSystem.REVIVE_TICKS / 20);
        graphics.drawString(client.font, hint, cx - client.font.width(hint) / 2, y, 0xFFB0B8C0);
    }

    /**
     * 自动复活倒计时（屏幕中央）。
     */
    private static void renderReviveOverlay(FakeGuiGraphics graphics, Minecraft client, LocalPlayer player,
            SixtySecondsStatsComponent stats) {
        long remainingTicks = stats.reviveEndTick - client.level.getGameTime();
        if (remainingTicks < 0) {
            remainingTicks = 0;
        }
        int totalSeconds = (int) Math.ceil(remainingTicks / 20.0);
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;

        Component title = Component.translatable("hud.sixty_seconds.sixty_seconds.revive_title");
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 60, 0);
        graphics.pose().scale(1.4f, 1.4f, 1f);
        graphics.drawString(client.font, title, -client.font.width(title) / 2, 0, 0xFFE8D9A8);
        graphics.pose().popPose();

        boolean soon = totalSeconds <= 10;
        float pulse = 0.6f + 0.4f * Mth.sin(player.tickCount * 0.4f);
        int color = soon ? ((Mth.clamp((int) (0xC0 + 0x3F * pulse), 0, 0xFF) << 24) | 0x60FF60) : 0xFFFFFFFF;
        Component time = Component.translatable("hud.sixty_seconds.sixty_seconds.revive_countdown",
                totalSeconds / 60, String.format("%02d", totalSeconds % 60));
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 40, 0);
        graphics.pose().scale(2.0f, 2.0f, 1f);
        graphics.drawString(client.font, time, -client.font.width(time) / 2, 0, color);
        graphics.pose().popPose();

        Component hint = Component.translatable("hud.sixty_seconds.sixty_seconds.revive_hint");
        graphics.drawString(client.font, hint, cx - client.font.width(hint) / 2, cy - 16, 0xFFAAAAAA);
    }

    /**
     * 健康条（纯色简约：暗色背板 + 实心填充）：低值时整条变红。
     * 数值由调用方绘制在右侧。
     */
    private static void drawHealthBar(FakeGuiGraphics g, Minecraft client, int x, int y, int w,
            int value, int max, int color) {
        // ── 背板（全宽暗色轨道）──
        g.fill(x, y, x + w, y + HEALTH_BAR_H, COL_HEALTH_TRACK);

        int clamped = Mth.clamp(value, 0, max);
        double ratio = max > 0 ? clamped / (double) max : 0;
        boolean low = ratio <= LOW_RATIO;
        int fill = low ? 0xFFFF4040 : color;

        int fillW = (int) Math.round(w * ratio);
        if (fillW > 0) {
            g.fill(x, y, x + fillW, y + HEALTH_BAR_H, fill);
        }
    }

    /** 是否按住 Shift（用于显式显示各状态条名称）。 */
    private static boolean isShiftDown(Minecraft client) {
        return client.options.keyShift.isDown();
    }

    /** 检查玩家背包中是否持有末日时钟（主物品栏 + 副手）。 */
    private static boolean hasClockInInventory(LocalPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SixtySecondsClockItem) {
                return true;
            }
        }
        return player.getOffhandItem().getItem() instanceof SixtySecondsClockItem;
    }

    /** 检查玩家是否在避难所/住宅安全区。 */
    private static boolean isInShelterZone(LocalPlayer player) {
        AABB zone = SixtySecondsClientMapZone.activeZone();
        if (zone == null || !SixtySecondsClientMapZone.isInSafeZone()) {
            return false;
        }
        return zone.contains(player.getX(), player.getY(), player.getZ());
    }

    /** 开局准备阶段（第 0 天）在屏幕上方居中显示醒目的倒计时横幅。 */
    private static void renderPrepBanner(FakeGuiGraphics graphics, Minecraft client, SixtySecondsStatsComponent stats) {
        long gameTime = client.level.getGameTime();
        long remaining = stats.phaseEndTick - gameTime;
        if (stats.dayNumber != 0 || remaining <= 0) {
            return;
        }
        int seconds = (int) Math.ceil(remaining / 20.0);
        int cx = graphics.guiWidth() / 2;
        int top = 60;

        Component title = Component.translatable("hud.sixty_seconds.sixty_seconds.prep_title");
        graphics.pose().pushPose();
        graphics.pose().translate(cx, top, 0);
        graphics.pose().scale(1.4f, 1.4f, 1f);
        int tw = client.font.width(title);
        graphics.drawString(client.font, title, -tw / 2, 0, 0xFFE8D9A8, false);
        graphics.pose().popPose();

        Component time = Component.literal(String.valueOf(seconds));
        int color = seconds <= 10 ? 0xFFFF5050 : 0xFFFFC857;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, top + 26, 0);
        graphics.pose().scale(2.6f, 2.6f, 1f);
        int w2 = client.font.width(time);
        graphics.drawString(client.font, time, -w2 / 2, 0, color, false);
        graphics.pose().popPose();
    }
}
