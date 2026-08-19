package net.exmo.sixty_seconds.bridge.stubs;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TrainVoicePlugin {
    private static final Set<UUID> GROUP = new HashSet<>();
    private TrainVoicePlugin() {}
    public static void addPlayer(UUID id) { GROUP.add(id); }
    public static void resetPlayer(UUID id) { GROUP.remove(id); }
    public static boolean isPlayerInGroup(UUID id) { return GROUP.contains(id); }
}
