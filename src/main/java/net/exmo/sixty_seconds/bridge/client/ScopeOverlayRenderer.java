package net.exmo.sixty_seconds.bridge.client;

public final class ScopeOverlayRenderer {
    private static boolean inScope;
    private ScopeOverlayRenderer() {}
    public static boolean isInScopeView() { return inScope; }
    public static void setInScopeView(boolean value) { inScope = value; }
}
