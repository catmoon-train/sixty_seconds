package net.exmo.sixty_seconds;

/**
 * 海洋世界创建参数（common 端，客户端 GUI 写入、服务端地形生成读取）。
 *
 * <p>与 LostCities 的 {@code Config.profileFromClient} 机制一致：该字段在客户端「创建世界」界面
 * 由 GUI 写入，在单人/局域网（内嵌服务端，同 JVM）下服务端直接读取并应用到新建世界。
 * 独立专用服务端需通过数据包/命令另行置位。
 */
public final class SixtySecondsOceanSetup {
    /** 客户端在创建世界界面是否勾选了「60秒·海洋」。 */
    public static boolean enabled = false;
    /** 海洋地形种子（与海图/地形共用）。 */
    public static long oceanSeed = 1337L;
    /** 每区域岛屿数量（0~3）。 */
    public static int oceanIslandCount = 2;
    /** 海平面 Y。 */
    public static int oceanSeaY = 80;

    private SixtySecondsOceanSetup() {
    }

    public static void reset() {
        enabled = false;
        oceanSeed = 1337L;
        oceanIslandCount = 2;
        oceanSeaY = 80;
    }

    public static void copyFrom(boolean enabled, long seed, int islands, int seaY) {
        SixtySecondsOceanSetup.enabled = enabled;
        SixtySecondsOceanSetup.oceanSeed = seed;
        SixtySecondsOceanSetup.oceanIslandCount = islands;
        SixtySecondsOceanSetup.oceanSeaY = seaY;
    }
}
