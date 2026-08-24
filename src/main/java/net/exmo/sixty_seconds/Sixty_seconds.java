package net.exmo.sixty_seconds;

import net.exmo.sixty_seconds.bridge.NeoForgeEvents;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecItems;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecSounds;
import net.exmo.sixty_seconds.network.ModNetwork;
import net.exmo.sixty_seconds.init.ModOceanEntities;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
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
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Sixty_seconds.MODID)
public class Sixty_seconds {
    public static final String MODID = SixtySeconds.MOD_ID;

    public Sixty_seconds(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        SixtySecondsCreativeTab.register(modEventBus);
        net.exmo.sixty_seconds.island.SixtySecondsOceanFeature.register(modEventBus); // 海洋世界地形生成 Feature
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerSpawnPlacements);
        modEventBus.addListener(ModNetwork::register);
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
     */
    private void registerSpawnPlacements(final RegisterSpawnPlacementsEvent event) {
        event.register(ModOceanEntities.OCEAN_SHARK,
                SpawnPlacementTypes.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) ->
                        level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.above()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
