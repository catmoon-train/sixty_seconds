package net.exmo.sixty_seconds.bridge.stubs;

import net.exmo.sixty_seconds.bridge.SRERole;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TMMRoles {
    public static final Map<ResourceLocation, SRERole> ROLES = new HashMap<>();

    static {
        for (ResourceLocation id : List.of(
                ModRoles.CHEF_ID, ModRoles.NOISEMAKER_ID, ModRoles.PSYCHOLOGIST_ID, ModRoles.ALCHEMIST_ID,
                ModRoles.AGENT_ID, ModRoles.FIREFIGHTER_ID, ModRoles.BUILDER_ID, ModRoles.GLITCH_ROBOT_ID,
                ModRoles.MONITOR_ID, ModRoles.OLDMAN_ID, ModRoles.CORONER_ID, ModRoles.CAKE_MAKER_ID,
                ModRoles.JADE_GENERAL_ID, ModRoles.SUPERSTAR_ID, ModRoles.SINGER_ID, ModRoles.ATTENDANT_ID,
                ModRoles.FIGHTER_ID, ModRoles.GREAT_DETECTIVE_ID, ModRoles.DOCTOR_ID, ModRoles.BROADCASTER_ID,
                ModRoles.AWESOME_BINGLUS_ID, ModRoles.GHOST_ID, ModRoles.CANDLE_BEARER_ID, ModRoles.THIEF_ID)) {
            ROLES.put(id, new SRERole(id, Component.translatable("role." + id.getNamespace() + "." + id.getPath())));
        }
    }

    private TMMRoles() {
    }
}
