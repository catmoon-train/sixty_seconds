package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.bridge.NeoForgeEvents;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecItems;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecSounds;
import net.exmo.sixty_seconds.network.ModNetwork;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModEffects;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.exmo.sixty_seconds.registry.ModItems;
import net.exmo.sixty_seconds.registry.ModSounds;
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
     * 海洋鲨鱼数量上限与局部密度上限，供 {@code OceanCreatureSpawner.tick} 在海洋维度以
     * 「受控方式」刷新鲨鱼时读取，避免一进海域就刷一大堆、数量无上限。
     * 鲨鱼不再走 biome_modifier 的 add_spawns（自定义海洋生成器在 CHUNK_GENERATION 阶段
     * 会把鲨鱼刷在虚空 Y 并瞬间死亡），统一由 OceanCreatureSpawner 内把关。
     */
    public static final int SHARK_GLOBAL_CAP = 24;            // 海洋维度内鲨鱼总数上限
    public static final int SHARK_AREA_CAP = 3;              // 单点附近（SHARK_AREA_RADIUS 内）的局部密度上限
    public static final double SHARK_AREA_RADIUS = 48.0;     // 局部密度检测半径

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
        modEventBus.addListener(ModNetwork::register);
        // 客户端 HUD：全部绘制统一收敛到 RenderGuiEvent.Post（见 SixtySecondsClientHud）；
        // 原版生命/饥饿等条隐藏在 RenderGuiLayerEvent.Pre。
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(net.exmo.sixty_seconds.client.SixtySecondsClientHud.class);
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

}
