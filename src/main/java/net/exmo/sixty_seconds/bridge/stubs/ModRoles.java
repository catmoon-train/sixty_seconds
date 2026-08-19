package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.SixtySeconds;
import net.minecraft.resources.ResourceLocation;

public final class ModRoles {
    private ModRoles() {
    }

    public static final ResourceLocation CHEF_ID = id("chef");
    public static final ResourceLocation NOISEMAKER_ID = id("noisemaker");
    public static final ResourceLocation PSYCHOLOGIST_ID = id("psychologist");
    public static final ResourceLocation ALCHEMIST_ID = id("alchemist");
    public static final ResourceLocation AGENT_ID = id("agent");
    public static final ResourceLocation FIREFIGHTER_ID = id("firefighter");
    public static final ResourceLocation BUILDER_ID = id("builder");
    public static final ResourceLocation GLITCH_ROBOT_ID = id("glitch_robot");
    public static final ResourceLocation MONITOR_ID = id("monitor");
    public static final ResourceLocation OLDMAN_ID = id("oldman");
    public static final ResourceLocation CORONER_ID = id("coroner");
    public static final ResourceLocation CAKE_MAKER_ID = id("cake_maker");
    public static final ResourceLocation JADE_GENERAL_ID = id("jade_general");
    public static final ResourceLocation SUPERSTAR_ID = id("star");
    public static final ResourceLocation SINGER_ID = id("singer");
    public static final ResourceLocation ATTENDANT_ID = id("attendant");
    public static final ResourceLocation FIGHTER_ID = id("fighter");
    public static final ResourceLocation GREAT_DETECTIVE_ID = id("great_detective");
    public static final ResourceLocation DOCTOR_ID = id("doctor");
    public static final ResourceLocation BROADCASTER_ID = id("broadcaster");
    public static final ResourceLocation AWESOME_BINGLUS_ID = id("awesome_binglus");
    public static final ResourceLocation GHOST_ID = id("ghost");
    public static final ResourceLocation CANDLE_BEARER_ID = id("candlebearer");
    public static final ResourceLocation THIEF_ID = id("thief");

    private static ResourceLocation id(String path) {
        return SixtySeconds.id(path);
    }
}
