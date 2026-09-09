package net.exmo.sixty_seconds.client.screen;

import com.sighs.petiteinventory.client.ClientInventoryContext;

/**
 * 客户端搜索区状态——由服务端网络包驱动。
 * <p>
 * SeaChartFullScreen 用此判断玩家是否处于出门探索状态（才能返回住所）。
 * </p>
 */
public final class SixtySecondsSearchZonesClient {

    private static boolean inSearchZone = false;

    private SixtySecondsSearchZonesClient() {
    }

    public static boolean isInSearchZone() {
        return inSearchZone;
    }

    public static void setInSearchZone(boolean value) {
        inSearchZone = value;
        // Rebuild the PetiteInventory mapping immediately when a teleport
        // changes the phase, so an already-open container cannot keep stale
        // multi-cell coordinates.
        ClientInventoryContext.invalidate();
    }
}
