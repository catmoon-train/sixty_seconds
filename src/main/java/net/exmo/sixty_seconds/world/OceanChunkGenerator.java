package net.exmo.sixty_seconds.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.island.SixtySecondsOceanFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 海洋维度的区块生成器。不使用 flat（flat 不会执行 biome feature，导致岛屿 feature 无法触发），
 * 而是用一个最简自定义生成器：直接把每个区块铺成“基岩 + 沙 + 水”的海床（与原本 flat layers 视觉一致），
 * 然后由 {@link #applyBiomeDecoration}（继承自 ChunkGenerator）正常触发 biome_modifier 注入的岛屿 feature。
 */
public final class OceanChunkGenerator extends ChunkGenerator {

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CHUNK_GENERATOR, SixtySeconds.MOD_ID);

    public static final MapCodec<OceanChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            inst -> inst.group(
                            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
                            net.minecraft.util.ExtraCodecs.intRange(0, 4096).fieldOf("sea_level").forGetter(g -> g.seaLevel),
                            net.minecraft.util.ExtraCodecs.intRange(-2048, 2048).fieldOf("min_y").forGetter(g -> g.minY),
                            net.minecraft.util.ExtraCodecs.intRange(0, 4096).fieldOf("height").forGetter(g -> g.height)
                    )
                    .apply(inst, OceanChunkGenerator::new)
    );

    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends ChunkGenerator>, MapCodec<OceanChunkGenerator>>
            HOLDER = CHUNK_GENERATORS.register("ocean", () -> CODEC);

    private final int seaLevel;
    private final int minY;
    private final int height;

    private static final int BEDROCK_Y = 10;
    private static final int SAND_TOP = 12;

    public OceanChunkGenerator(BiomeSource biomeSource, int seaLevel, int minY, int height) {
        super(biomeSource);
        this.seaLevel = seaLevel;
        this.minY = minY;
        this.height = height;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion p_223043_, long p_223044_, RandomState p_223045_, BiomeManager p_223046_,
                             StructureManager p_223047_, ChunkAccess p_223048_, GenerationStep.Carving p_223049_) {
        // 无雕刻
    }

    @Override
    public void buildSurface(WorldGenRegion p_223050_, StructureManager p_223051_, RandomState p_223052_, ChunkAccess p_223053_) {
        // 海床已在 fillFromNoise 中铺好
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion p_62167_) {
        // 由生物群系自然刷新负责
    }

    @Override
    public int getGenDepth() {
        return height;
    }

    @Override
    public int getSeaLevel() {
        return seaLevel;
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getBaseHeight(int p_223032_, int p_223033_, Heightmap.Types p_223034_, LevelHeightAccessor p_223035_, RandomState p_223036_) {
        return seaLevel;
    }

    @Override
    public net.minecraft.world.level.NoiseColumn getBaseColumn(int p_223028_, int p_223029_, LevelHeightAccessor p_223030_, RandomState p_223031_) {
        BlockState[] states = new BlockState[p_223030_.getHeight()];
        for (int y = p_223030_.getMinBuildHeight(); y < p_223030_.getHeight(); y++) {
            states[y - p_223030_.getMinBuildHeight()] = columnState(y);
        }
        return new net.minecraft.world.level.NoiseColumn(p_223030_.getMinBuildHeight(), states);
    }

    private BlockState columnState(int y) {
        if (y < BEDROCK_Y) return Blocks.AIR.defaultBlockState();
        if (y == BEDROCK_Y) return Blocks.BEDROCK.defaultBlockState();
        if (y <= SAND_TOP) return Blocks.SAND.defaultBlockState();
        if (y <= seaLevel) return Blocks.WATER.defaultBlockState();
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        ChunkPos cpos = chunk.getPos();
        int cx = cpos.getMinBlockX();
        int cz = cpos.getMinBlockZ();
        // 直接手动触发海洋岛屿 feature，确保岛屿在海洋维度一定生成（不依赖 biome_modifier 注入链）。
        SixtySecondsOceanFeature.generateChunk(level, cx, cz);
        super.applyBiomeDecoration(level, chunk, structureManager);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender p_223210_, RandomState p_223211_,
                                                        StructureManager p_223212_, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        int minBuild = chunk.getMinBuildHeight();
        int maxBuild = chunk.getMaxBuildHeight();
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = x0 + lx;
                int wz = z0 + lz;
                for (int y = minBuild; y < maxBuild; y++) {
                    chunk.setBlockState(mpos.set(wx, y, wz), columnState(y), false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void addDebugScreenInfo(List<String> p_223175_, RandomState p_223176_, BlockPos p_223177_) {
        p_223175_.add("sea_level=" + seaLevel);
    }

    @Override
    public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> p_223134_, StructureManager p_223135_,
                                                                      MobCategory p_223136_, BlockPos p_223137_) {
        return p_223134_.value().getMobSettings().getMobs(p_223136_);
    }

    @Override
    public void createReferences(WorldGenLevel p_223077_, StructureManager p_223078_, ChunkAccess p_223079_) {
        // 无结构引用
    }
}
