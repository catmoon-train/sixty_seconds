package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.bridge.NeoForgeEvents;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecItems;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecSounds;
import net.exmo.sixty_seconds.network.ModNetwork;
import net.exmo.sixty_seconds.init.ModOceanEntities;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModEffects;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.exmo.sixty_seconds.registry.ModItems;
import net.exmo.sixty_seconds.registry.ModSounds;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.exmo.sixty_seconds.registry.ModParticles;
import net.exmo.sixty_seconds.weather.WeatherVisualConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Sixty_seconds.MODID)
public class Sixty_seconds {
    public static final String MODID = SixtySeconds.MOD_ID;

    /**
     * 海洋鲨鱼「数据驱动自然刷新」的上限与节奏控制（在 {@link #registerSpawnPlacements} 的
     * 刷怪位置判定里强制执行）。biome_modifier 的 add_spawns 只决定「权重 / 是否刷」，
     * 真正的数量上限与刷新速度由这里把关，避免进入海域瞬间刷出一大堆、且数量无上限。
     */
    public static final int SHARK_GLOBAL_CAP = 24;            // 海洋维度内鲨鱼总数上限
    public static final int SHARK_AREA_CAP = 3;              // 单点附近（SHARK_AREA_RADIUS 内）的局部密度上限
    public static final double SHARK_AREA_RADIUS = 48.0;     // 局部密度检测半径
    public static final long SHARK_SPAWN_INTERVAL_TICKS = 20L * 90; // 两次成功刷新最小间隔（≈90 秒）

    public Sixty_seconds(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        SixtySecondsCreativeTab.register(modEventBus);
        net.exmo.sixty_seconds.island.SixtySecondsOceanFeature.register(modEventBus); // 海洋世界地形生成 Feature
        net.exmo.sixty_seconds.world.OceanChunkGenerator.CHUNK_GENERATORS.register(modEventBus); // 海洋维度生成器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(ModNetwork::register);
        ModParticles.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.CLIENT, WeatherVisualConfig.SPEC);
        NeoForge.EVENT_BUS.register(NeoForgeEvents.class);
        net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesAccess.init(); // 通过 IMC 获取 LostCities API（建筑星级映射）
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            SixtySecItems.bind();
            SixtySecSounds.bind();
            net.exmo.sixty_seconds.init.ModOceanEntities.bind();
            SixtySecondsMod.init();
        });
    }

    /**
     * 海洋鲨鱼数据刷怪（biome_modifier add_spawns）必需的刷怪位置规则登记。
     * NeoForge 1.21.1 通过 {@link RegisterSpawnPlacementsEvent}（mod 总线）注册，
     * 没有这一步，数据包里的 add_spawns 不会真正刷出实体。
     * <p>这里同时把关「是否在水里」与「刷新间隔 / 总数上限 / 局部密度上限」：
     * 判定返回 false 时自然刷怪器会干净地跳过该候选点（不会真的创建实体），
     */
    private void registerSpawnPlacements(final RegisterSpawnPlacementsEvent event) {
        event.register(ModOceanEntities.OCEAN_SHARK,
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> {
                    // 只在海洋维度、且为服务端才合法
                    if (!(level instanceof ServerLevel sl)) {
                        return false;
                    }
                    if (!sl.dimension().equals(SixtySeconds.OCEAN_DIMENSION)) {
                        return false;
                    }
                    // 必须是水下才合法
                    if (!level.getFluidState(pos).is(FluidTags.WATER)
                            || !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                        return false;
                    }
                    // 指令/召唤等明确手动来源（COMMAND）直接放行，数量由调用方控制
                    if (spawnType == MobSpawnType.COMMAND) {
                        return true;
                    }

                    SixtySecondsState.Data data = SixtySecondsState.get(sl);

                    // 1) 全局数量上限（覆盖整个海洋维度）——对自然刷新与区块生成都生效
                    AABB whole = new AABB(-30_000_000, level.getMinBuildHeight(), -30_000_000,
                            30_000_000, level.getMaxBuildHeight(), 30_000_000);
                    int globalCount = sl.getEntitiesOfClass(OceanSharkEntity.class, whole).size();
                    if (globalCount >= SHARK_GLOBAL_CAP) {
                        return false;
                    }
                    // 2) 局部密度上限（候选点附近已有太多则不放，避免扎堆）
                    int near = sl.getEntitiesOfClass(OceanSharkEntity.class,
                            new AABB(pos).inflate(SHARK_AREA_RADIUS)).size();
                    if (near >= SHARK_AREA_CAP) {
                        return false;
                    }

                    // 3) 周期性自然刷新限速（区块生成只受上面数量上限约束，可形成有界初始种群）
                    if (spawnType == MobSpawnType.NATURAL) {
                        long now = sl.getGameTime();
                        if (data.lastSharkSpawnTick != Long.MIN_VALUE
                                && now - data.lastSharkSpawnTick < SHARK_SPAWN_INTERVAL_TICKS) {
                            return false;
                        }
                        data.lastSharkSpawnTick = now;
                    }
                    return true;
                },
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
