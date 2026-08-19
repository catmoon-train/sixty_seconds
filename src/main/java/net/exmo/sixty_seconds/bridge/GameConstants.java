package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.resources.ResourceLocation;

public final class GameConstants {
    public static int FADE_TIME = 40;
    public static int FADE_PAUSE = 20;

    private GameConstants() {
    }

    public static int getInTicks(int minutes, int seconds) {
        return (minutes * 60 + seconds) * 20;
    }

    public static final class DeathReasons {
        public static final ResourceLocation EXECUTE = SixtySeconds.id("execute");
        public static final ResourceLocation GENERIC = SixtySeconds.id("generic");
        public static final ResourceLocation GUN_SHOT = SixtySeconds.id("gun_shot");
        public static final ResourceLocation KNIFE = SixtySeconds.id("knife_stab");
        public static final ResourceLocation REVOLVER = SixtySeconds.id("revolver_shot");
        public static final ResourceLocation DERRINGER = SixtySeconds.id("derringer_shot");
        public static final ResourceLocation BAT = SixtySeconds.id("bat_hit");
        public static final ResourceLocation GRENADE = SixtySeconds.id("grenade");
        public static final ResourceLocation FELL_OUT_OF_TRAIN = SixtySeconds.id("fell_out_of_train");
        public static final ResourceLocation LAVA = SixtySeconds.id("swim_in_lava");
        public static final ResourceLocation SNIPER_RIFLE = SixtySeconds.id("sniper_rifle");
        public static final ResourceLocation ZERO_ONE_FIVE = SixtySeconds.id("zero_one_five_shot");
        public static final ResourceLocation DROWNED = SixtySeconds.id("drowned");
        public static final ResourceLocation FROZEN = SixtySeconds.id("frozen");
        public static final ResourceLocation THIRST = SixtySeconds.id("thirst");
        public static final ResourceLocation STARVED = SixtySeconds.id("starved");
        public static final ResourceLocation GENERAL_ATTACK = SixtySeconds.id("general_attack");
        public static final ResourceLocation FIRE_AXE = SixtySeconds.id("fire_axe");
        public static final ResourceLocation FALL_DAMAGE = SixtySeconds.id("fall_damage");

        private DeathReasons() {
        }
    }
}
