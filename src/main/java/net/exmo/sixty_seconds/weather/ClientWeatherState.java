package net.exmo.sixty_seconds.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;

/**
 * 客户端持有「当前激活的主题化天气」类型，由 {@link net.exmo.sixty_seconds.network.WeatherS2CPacket}
 * 同步。位于公共包，不引用任何客户端类，故服务端亦可安全加载。
 */
public final class ClientWeatherState {
    private static volatile int activeWeatherId = -1;

    private ClientWeatherState() {
    }

    public static void set(int weatherId) {
        activeWeatherId = weatherId;
    }

    public static SixtySecondsEventSystem.EventType getEventType() {
        if (activeWeatherId < 0) {
            return null;
        }
        SixtySecondsEventSystem.EventType[] values = SixtySecondsEventSystem.EventType.values();
        return activeWeatherId < values.length ? values[activeWeatherId] : null;
    }

    public static boolean isActive() {
        return activeWeatherId >= 0;
    }
}
