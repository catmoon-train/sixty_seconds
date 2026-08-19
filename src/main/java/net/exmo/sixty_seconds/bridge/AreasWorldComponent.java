package net.exmo.sixty_seconds.bridge;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.cca.AutoSyncedComponent;
import net.exmo.sixty_seconds.bridge.cca.ComponentKey;
import net.exmo.sixty_seconds.bridge.cca.ComponentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AreasWorldComponent implements AutoSyncedComponent {
    public static final ComponentKey<AreasWorldComponent> KEY = ComponentRegistry.getOrCreate(
            SixtySeconds.id("areas"), AreasWorldComponent.class);

    public static class PosWithOrientation {
        public final Vec3 pos;
        public final float yaw;
        public final float pitch;

        public PosWithOrientation(Vec3 pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public PosWithOrientation(double x, double y, double z, float yaw, float pitch) {
            this(new Vec3(x, y, z), yaw, pitch);
        }
    }

    public enum MinecraftWeather {
        clear, rain, thunder
    }

    public static class AreasSettings {
        public boolean noReset = true;
        public boolean mustCopy = false;
        public boolean canJump = true;
        public long time = 1000;
        public MinecraftWeather weather = MinecraftWeather.clear;
        public boolean weatherCycle = false;
        public double gravityModifier = 0;
    }

    private final Level world;
    public String mapName = "default";
    public AreasSettings areasSettings = new AreasSettings();
    public AABB playArea;
    public java.util.List<String> initialItems = new java.util.ArrayList<>();
    private PosWithOrientation spectatorSpawn;

    public AreasWorldComponent(Level world) {
        this.world = world;
        if (world instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            this.spectatorSpawn = new PosWithOrientation(Vec3.atCenterOf(spawn), 0, 0);
            this.playArea = new AABB(spawn).inflate(256);
        }
    }

    public PosWithOrientation getSpectatorSpawnPos() {
        if (spectatorSpawn != null) {
            return spectatorSpawn;
        }
        if (world instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            return new PosWithOrientation(Vec3.atCenterOf(spawn), 0, 0);
        }
        return new PosWithOrientation(Vec3.ZERO, 0, 0);
    }

    public PosWithOrientation getSpawnPos() {
        return getSpectatorSpawnPos();
    }

    public void setSpectatorSpawnPos(PosWithOrientation pos) {
        this.spectatorSpawn = pos;
    }
}
