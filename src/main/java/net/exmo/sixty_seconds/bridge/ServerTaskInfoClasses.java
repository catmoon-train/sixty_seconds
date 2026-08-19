package net.exmo.sixty_seconds.bridge;

import net.minecraft.server.MinecraftServer;

public final class ServerTaskInfoClasses {
    private ServerTaskInfoClasses() {
    }

    public abstract static class ServerTaskInfo {
        public boolean finished = false;
        public boolean cancelled = false;

        public boolean onTick(MinecraftServer server) {
            return true;
        }

        public void onFinished() {
        }
    }
}
