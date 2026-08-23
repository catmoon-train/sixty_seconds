package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.ILostCities;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 通过 LostCities 官方 IMC 通道获取 {@link ILostCities} 实例的桥接。
 *
 * <p>LostCities 在 NeoForge 的 {@code InterModProcessEvent} 阶段把 {@link ILostCities} 的实现（{@code LostCitiesImp}）
 * 通过 IMC 消息回调给第三方模组。本类在六十秒模组构造阶段向 LostCities 注册该回调，把拿到的实例缓存下来供
 * {@link SixtySecondsLostCitiesStarMap} 使用。</p>
 *
 * <p>对 LostCities 的访问仅使用其公开 API（{@code mcjty.lostcities.api.*}），不修改 LostCities 任何实现。</p>
 */
public final class SixtySecondsLostCitiesAccess {

    @Nullable
    private static volatile ILostCities api;

    private SixtySecondsLostCitiesAccess() {
    }

    /** 返回缓存的 {@link ILostCities} 实例；LostCities 未接入/未回调时返回 null。 */
    @Nullable
    public static ILostCities api() {
        return api;
    }

    /**
     * 供六十秒主类构造阶段调用，向 LostCities 注册「获取 ILostCities」IMC 回调。
     * LostCities 未安装/类路径缺失时静默跳过，api 保持 null。
     */
    public static void init() {
        try {
            // LostCities 在 processIMC 阶段期望 IMC 消息的 Supplier.get() 直接返回
            // 一个 Function<ILostCities, Void>（见 LostCities.processIMC 对 messageSupplier.get() 的 cast）。
            net.neoforged.fml.InterModComms.sendTo(
                    ILostCities.LOSTCITIES,
                    ILostCities.GET_LOST_CITIES,
                    (Supplier<Function<ILostCities, Void>>) () -> api -> {
                        SixtySecondsLostCitiesAccess.api = api;
                        return null;
                    });
        } catch (Throwable ignored) {
            // LostCities 不可用：不注册 IMC，后续全部静默降级为无建筑星级。
        }
    }
}
