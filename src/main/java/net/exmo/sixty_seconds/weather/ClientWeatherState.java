package net.exmo.sixty_seconds.weather;

import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端持有「当前激活的主题化天气」类型，由 {@link net.exmo.sixty_seconds.network.WeatherS2CPacket}
 * 同步。指令预览（preview）与自然事件（event）为两个独立槽位，显示时预览优先、互不干扰：
 * 自然事件结束只会清 event 槽，绝不会清除正在进行的指令预览。
 * 位于公共包，不引用任何客户端类，故服务端亦可安全加载。
 */
public final class ClientWeatherState {
    private static final Logger LOG = LoggerFactory.getLogger(ClientWeatherState.class);
    private static volatile int previewWeatherId = -1;
    private static volatile int eventWeatherId = -1;

    private ClientWeatherState() {
    }

    /** 指令预览槽（-1 表示无）。 */
    public static void setPreview(int weatherId) {
        LOG.info("[60s-weather] setPreview({}) previewWeatherId 旧={} 新={}",
                weatherId, previewWeatherId, weatherId);
        previewWeatherId = weatherId;
    }

    /** 自然事件槽（-1 表示无）。 */
    public static void setEvent(int weatherId) {
        LOG.info("[60s-weather] setEvent({}) eventWeatherId 旧={} 新={}",
                weatherId, eventWeatherId, weatherId);
        eventWeatherId = weatherId;
    }

    /** 显示用的当前天气 id：预览优先，无预览时回退到自然事件。 */
    private static int activeId() {
        return previewWeatherId >= 0 ? previewWeatherId : eventWeatherId;
    }

    public static SixtySecondsEventSystem.EventType getEventType() {
        int id = activeId();
        if (id < 0) {
            return null;
        }
        SixtySecondsEventSystem.EventType[] values = SixtySecondsEventSystem.EventType.values();
        return id < values.length ? values[id] : null;
    }

    public static boolean isActive() {
        return activeId() >= 0;
    }

    /** 离开世界/断开连接时清空两个槽位，避免旧世界的预览带入新世界。 */
    public static void reset() {
        previewWeatherId = -1;
        eventWeatherId = -1;
    }
}
