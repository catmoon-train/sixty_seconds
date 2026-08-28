package net.exmo.sixty_seconds.bridge.minigame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小游戏注册表（纯服务端安全，不引用任何客户端类）
 * 所有电脑可用的小游戏都在这里注册
 */
public class QuestMinigames {

    private static final Map<String, QuestMinigame> REGISTRY = new LinkedHashMap<>();

    /** 撬锁小游戏 */
    public static final QuestMinigame LOCKPICK = register(
            QuestMinigame.of("lockpick", "minigame.sixty_seconds.lockpick"));

    /** 判定 */
    public static final QuestMinigame KEY_MAKING = register(
            QuestMinigame.of("key_making", "minigame.sixty_seconds.key_making"));

    /** 数学题小游戏 */
    public static final QuestMinigame MATH = register(
            QuestMinigame.of("math", "minigame.sixty_seconds.math"));

    /** 烹饪小游戏 */
    public static final QuestMinigame COOKING = register(
            QuestMinigame.of("cooking", "minigame.sixty_seconds.cooking"));

    public static final QuestMinigame REACTOR_TEMPERATURE = register(
            QuestMinigame.of("reactor_temperature", "minigame.sixty_seconds.reactor_temperature"));
    public static final QuestMinigame BOX_SORT = register(
            QuestMinigame.of("box_sort", "minigame.sixty_seconds.box_sort"));
    public static final QuestMinigame WIRE_CONNECT = register(
            QuestMinigame.of("wire_connect", "minigame.sixty_seconds.wire_connect"));
    public static final QuestMinigame HOLD_BUTTON = register(
            QuestMinigame.of("hold_button", "minigame.sixty_seconds.hold_button"));
    public static final QuestMinigame CLEAN_DEBRIS = register(
            QuestMinigame.of("clean_debris", "minigame.sixty_seconds.clean_debris"));
    public static final QuestMinigame SHOOTING = register(
            QuestMinigame.of("shooting", "minigame.sixty_seconds.shooting"));
    public static final QuestMinigame WIRE_TUNING = register(
            QuestMinigame.of("wire_tuning", "minigame.sixty_seconds.wire_tuning"));
    public static final QuestMinigame SIGNAL_CALIBRATION = register(
            QuestMinigame.of("signal_calibration", "minigame.sixty_seconds.signal_calibration"));
    public static final QuestMinigame SHAPE_MATCH = register(
            QuestMinigame.of("shape_match", "minigame.sixty_seconds.shape_match"));
    public static final QuestMinigame SEQUENCE_BUTTONS = register(
            QuestMinigame.of("sequence_buttons", "minigame.sixty_seconds.sequence_buttons"));
    public static final QuestMinigame CLEAN_STAINS = register(
            QuestMinigame.of("clean_stains", "minigame.sixty_seconds.clean_stains"));
    public static final QuestMinigame PULL_LEVER = register(
            QuestMinigame.of("pull_lever", "minigame.sixty_seconds.pull_lever"));
    public static final QuestMinigame TRASH_RECYCLE = register(
            QuestMinigame.of("trash_recycle", "minigame.sixty_seconds.trash_recycle"));
    public static final QuestMinigame ITEM_CHECKLIST = register(
            QuestMinigame.of("item_checklist", "minigame.sixty_seconds.item_checklist"));
    public static final QuestMinigame SWIPE_CARD = register(
            QuestMinigame.of("swipe_card", "minigame.sixty_seconds.swipe_card"));
    public static final QuestMinigame PLAY_MUSIC = register(
            QuestMinigame.of("play_music", "minigame.sixty_seconds.play_music"));
    public static final QuestMinigame RHYTHM = register(
            QuestMinigame.of("rhythm", "minigame.sixty_seconds.rhythm"));
    public static final QuestMinigame LIGHT_CANDLES = register(
            QuestMinigame.of("light_candles", "minigame.sixty_seconds.light_candles"));
    public static final QuestMinigame WHACK_MOLE = register(
            QuestMinigame.of("whack_mole", "minigame.sixty_seconds.whack_mole"));
    public static final QuestMinigame PUZZLE = register(
            QuestMinigame.of("puzzle", "minigame.sixty_seconds.puzzle"));
    public static final QuestMinigame STORAGE = register(
            QuestMinigame.of("storage", "minigame.sixty_seconds.storage"));
    public static final QuestMinigame SLICE_FOOD = register(
            QuestMinigame.of("slice_food", "minigame.sixty_seconds.slice_food"));
    public static final QuestMinigame THREE_CARDS = register(
            QuestMinigame.of("three_cards", "minigame.sixty_seconds.three_cards"));
    public static final QuestMinigame BREAK_JAR = register(
            QuestMinigame.of("break_jar", "minigame.sixty_seconds.break_jar"));
    public static final QuestMinigame ZONE_CALIBRATION = register(
            QuestMinigame.of("zone_calibration", "minigame.sixty_seconds.zone_calibration"));

    /** 水阀小游戏 */
    public static final QuestMinigame WATER_VALVE = register(
            QuestMinigame.of("water_valve", "minigame.sixty_seconds.water_valve"));

    /** 打字小游戏 */
    public static final QuestMinigame TYPING = register(
            QuestMinigame.of("typing", "minigame.sixty_seconds.typing"));

    /** 管道小鸟（Flappy Bird） */
    public static final QuestMinigame PIPE_BIRD = register(
            QuestMinigame.of("pipe_bird", "minigame.sixty_seconds.pipe_bird"));

    /** 水果忍者 */
    public static final QuestMinigame FRUIT_NINJA = register(
            QuestMinigame.of("fruit_ninja", "minigame.sixty_seconds.fruit_ninja"));

    /** 打老鼠 */
    public static final QuestMinigame MOUSE_WHACK = register(
            QuestMinigame.of("mouse_whack", "minigame.sixty_seconds.mouse_whack"));

    /** 打砖块 */
    public static final QuestMinigame BRICK_BREAKER = register(
            QuestMinigame.of("brick_breaker", "minigame.sixty_seconds.brick_breaker"));

    /** 找零钱 */
    public static final QuestMinigame MAKE_CHANGE = register(
            QuestMinigame.of("make_change", "minigame.sixty_seconds.make_change"));

    /** 贪吃蛇 */
    public static final QuestMinigame SNAKE = register(
            QuestMinigame.of("snake", "minigame.sixty_seconds.snake"));
    /** 扫雷 */
    public static final QuestMinigame MINESWEEPER = register(
            QuestMinigame.of("minesweeper", "minigame.sixty_seconds.minesweeper"));
    /** 记忆游戏 */
    public static final QuestMinigame SIMON_SAYS = register(
            QuestMinigame.of("simon_says", "minigame.sixty_seconds.simon_says"));
    /** 2048 */
    public static final QuestMinigame GAME_2048 = register(
            QuestMinigame.of("game_2048", "minigame.sixty_seconds.game_2048"));
    /** 接蛋 */
    public static final QuestMinigame CATCH_EGGS = register(
            QuestMinigame.of("catch_eggs", "minigame.sixty_seconds.catch_eggs"));
    /** 颜色分类 */
    public static final QuestMinigame COLOR_SORT = register(
            QuestMinigame.of("color_sort", "minigame.sixty_seconds.color_sort"));
    /** 猜数字 */
    public static final QuestMinigame GUESS_NUMBER = register(
            QuestMinigame.of("guess_number", "minigame.sixty_seconds.guess_number"));
    /** 反应测试 */
    public static final QuestMinigame REACTION_TEST = register(
            QuestMinigame.of("reaction_test", "minigame.sixty_seconds.reaction_test"));
    /** 连连看 */
    public static final QuestMinigame LINK_MATCH = register(
            QuestMinigame.of("link_match", "minigame.sixty_seconds.link_match"));
    /** 俄罗斯方块 */
    public static final QuestMinigame TETRIS = register(
            QuestMinigame.of("tetris", "minigame.sixty_seconds.tetris"));
    /** 翻牌配对 */
    public static final QuestMinigame MEMORY_MATCH = register(
            QuestMinigame.of("memory_match", "minigame.sixty_seconds.memory_match"));
    /** 接管道 */
    public static final QuestMinigame PIPE_CONNECT = register(
            QuestMinigame.of("pipe_connect", "minigame.sixty_seconds.pipe_connect"));
    /** 点灯 */
    public static final QuestMinigame LIGHTS_OUT = register(
            QuestMinigame.of("lights_out", "minigame.sixty_seconds.lights_out"));
    /** 24点 */
    public static final QuestMinigame GAME_24 = register(
            QuestMinigame.of("game_24", "minigame.sixty_seconds.game_24"));
    /** 迷宫 */
    public static final QuestMinigame MAZE = register(
            QuestMinigame.of("maze", "minigame.sixty_seconds.maze"));
    /** 平衡天平 */
    public static final QuestMinigame BALANCE_SCALE = register(
            QuestMinigame.of("balance_scale", "minigame.sixty_seconds.balance_scale"));
    /** 华容道 */
    public static final QuestMinigame KLOTSKI = register(
            QuestMinigame.of("klotski", "minigame.sixty_seconds.klotski"));
    public static final QuestMinigame GOLD_MINER = register(
            QuestMinigame.of("gold_miner", "minigame.sixty_seconds.gold_miner"));
    public static final QuestMinigame ONE_STROKE = register(
            QuestMinigame.of("one_stroke", "minigame.sixty_seconds.one_stroke"));
    public static final QuestMinigame CLAW_MACHINE = register(
            QuestMinigame.of("claw_machine", "minigame.sixty_seconds.claw_machine"));
    public static final QuestMinigame BALLOON_SNIPER = register(
            QuestMinigame.of("balloon_sniper", "minigame.sixty_seconds.balloon_sniper"));
    public static final QuestMinigame EXTINGUISH_FIRE = register(
            QuestMinigame.of("extinguish_fire", "minigame.sixty_seconds.extinguish_fire"));
    public static final QuestMinigame PACHINKO = register(
            QuestMinigame.of("pachinko", "minigame.sixty_seconds.pachinko"));
    public static final QuestMinigame MIX_DRINK = register(
            QuestMinigame.of("mix_drink", "minigame.sixty_seconds.mix_drink"));
    public static final QuestMinigame BALLOON_PUMP = register(
            QuestMinigame.of("balloon_pump", "minigame.sixty_seconds.balloon_pump"));
    public static final QuestMinigame THROW_BALL = register(
            QuestMinigame.of("throw_ball", "minigame.sixty_seconds.throw_ball"));

    // ══════════════════════════════════════════════
    // 注册方法
    // ══════════════════════════════════════════════

    private static QuestMinigame register(QuestMinigame minigame) {
        REGISTRY.put(minigame.id(), minigame);
        return minigame;
    }

    /** 根据ID获取小游戏信息 */
    public static QuestMinigame get(String id) {
        return REGISTRY.get(id);
    }

    /** 获取所有已注册的小游戏列表 */
    public static List<QuestMinigame> getAll() {
        return new ArrayList<>(REGISTRY.values());
    }

    /** 获取默认小游戏ID */
    public static String getDefaultId() {
        return REGISTRY.isEmpty() ? "" : REGISTRY.keySet().iterator().next();
    }
}
