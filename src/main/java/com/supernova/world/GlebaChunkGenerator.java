package com.supernova.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.supernova.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Terreno de Gleba. Depende de la SEMILLA del mundo (cada mundo es distinto): dominio deformado, montanas con crestas,
 * cuencas con lagos y cuatro zonas: pantano de hongos (arboles de tallo + plantas chorus), tierras altas de cristal,
 * desierto de arena pálida y zona de CARNE (bloques de carne y tumores). Minerales de Gleba dentro de la roca.
 * La zona (0,0) es plana para aterrizar.
 */
public class GlebaChunkGenerator extends ChunkGenerator {
    public static final MapCodec<GlebaChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.getBiomeSource())
    ).apply(i, i.stable(GlebaChunkGenerator::new)));

    public static final int SEA = 62;
    private static final int MARSH = 0, HIGHLAND = 1, DESERT = 2, FLESH = 3;

    private static final class Noises {
        final SimplexNoise n1, n2, n3, n4, n5, n6, n7;
        Noises(long seed) {
            RandomSource r = RandomSource.create(seed ^ 0x6C656261L);
            n1 = new SimplexNoise(r); n2 = new SimplexNoise(r); n3 = new SimplexNoise(r); n4 = new SimplexNoise(r);
            n5 = new SimplexNoise(r); n6 = new SimplexNoise(r); n7 = new SimplexNoise(r);
        }
    }

    private static volatile Noises N = new Noises(1337L);
    private static volatile long seed = 1337L;

    public static void setSeed(long s) {
        if (s != seed) {
            seed = s;
            N = new Noises(s);
        }
    }

    public GlebaChunkGenerator(BiomeSource source) {
        super(source);
    }

    public static int zoneAt(int x, int z) {
        Noises n = N;
        if (n.n7.getValue(x / 330.0, z / 330.0) > 0.46) return FLESH;
        double v = n.n6.getValue(x / 260.0, z / 260.0);
        if (v < -0.28) return DESERT;
        if (v > 0.30) return HIGHLAND;
        return MARSH;
    }

    /** Y del bloque superior solido de la columna. */
    public static int heightAt(int x, int z) {
        Noises n = N;
        double wx = x + n.n3.getValue(x / 90.0, z / 90.0) * 28;
        double wz = z + n.n3.getValue(x / 90.0 + 50, z / 90.0) * 28;
        double base = n.n1.getValue(wx / 140.0, wz / 140.0) * 18;
        double hills = n.n2.getValue(wx / 55.0, wz / 55.0) * 8;
        double detail = n.n3.getValue(wx / 14.0, wz / 14.0) * 2.5;
        double ridge = 1 - Math.abs(n.n4.getValue(wx / 110.0 + 10, wz / 110.0));
        ridge = ridge * ridge * ridge;
        double mount = ridge * Math.max(0, n.n1.getValue(wx / 300.0 + 9, wz / 300.0 + 3) + 0.15) * 70;
        double h = 72 + base + hills + detail + mount;
        double basin = n.n5.getValue(wx / 160.0, wz / 160.0);
        if (basin > 0.55) h -= (basin - 0.55) * 70;
        double d = Math.sqrt((double) x * x + (double) z * z);
        double f = Mth.clamp((d - 24) / 40.0, 0, 1);
        f = f * f * (3 - 2 * f);
        h = Mth.lerp(f, 70.0, h);
        return Mth.clamp((int) Math.round(h), 40, 230);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long s, RandomState rs, BiomeManager bm, StructureManager sm, ChunkAccess chunk, GenerationStep.Carving step) { }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk) { }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) { }

    @Override
    public int getGenDepth() {
        return 384;
    }

    private static void put(ChunkAccess chunk, Heightmap ocean, Heightmap surf, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState st) {
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) return;
        chunk.setBlockState(pos.set(x, y, z), st, false);
        ocean.update(x, y, z, st);
        surf.update(x, y, z, st);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState rs, StructureManager sm, ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surf = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState stone = ModBlocks.GLEBA_STONE.defaultBlockState();
        BlockState soil = ModBlocks.GLEBA_SOIL.defaultBlockState();
        BlockState moss = ModBlocks.GLEBA_MOSS.defaultBlockState();
        BlockState sand = ModBlocks.GLEBA_SAND.defaultBlockState();
        BlockState flesh = ModBlocks.GLEBA_FLESH.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        int minY = chunk.getMinBuildHeight();
        int[][] hts = new int[16][16];
        int[][] zones = new int[16][16];

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = cp.getMinBlockX() + x, wz = cp.getMinBlockZ() + z;
                int h = heightAt(wx, wz);
                int zone = zoneAt(wx, wz);
                hts[x][z] = h;
                zones[x][z] = zone;
                int top = Math.max(h, SEA);
                BlockState topBlock = h <= SEA + 1 ? sand : switch (zone) {
                    case DESERT -> sand;
                    case HIGHLAND -> stone;
                    case FLESH -> flesh;
                    default -> moss;
                };
                BlockState under = zone == DESERT ? sand : zone == FLESH ? flesh : soil;
                for (int y = minY; y <= top; y++) {
                    BlockState st;
                    if (y == minY) st = bedrock;
                    else if (y <= h - 5) st = stone;
                    else if (y < h) st = (zone == HIGHLAND && y >= h - 2 && h > SEA + 1) ? stone : under;
                    else if (y == h) st = topBlock;
                    else st = water;
                    put(chunk, ocean, surf, pos, x, y, z, st);
                }
            }
        }

        RandomSource r = RandomSource.create(cp.toLong() ^ seed ^ 0x5EEDL);
        // minerales de Gleba (dentro de la roca)
        int veins = 3 + r.nextInt(4);
        for (int v = 0; v < veins; v++) {
            int ox = 1 + r.nextInt(14), oz = 1 + r.nextInt(14);
            int oy = minY + 10 + r.nextInt(Math.max(1, hts[ox][oz] - minY - 20));
            int size = 3 + r.nextInt(4);
            for (int k = 0; k < size; k++) {
                int px = ox + r.nextInt(3) - 1, py = oy + r.nextInt(3) - 1, pz = oz + r.nextInt(3) - 1;
                if (px < 0 || px > 15 || pz < 0 || pz > 15 || py < minY + 1 || py >= hts[px][pz] - 4) continue;
                if (chunk.getBlockState(pos.set(px, py, pz)).is(stone.getBlock()))
                    put(chunk, ocean, surf, pos, px, py, pz, ModBlocks.GLEBA_ORE.defaultBlockState());
            }
        }

        int cwx = cp.getMinBlockX() + 8, cwz = cp.getMinBlockZ() + 8;
        if (Math.abs(cwx) < 40 && Math.abs(cwz) < 40) return CompletableFuture.completedFuture(chunk); // zona de aterrizaje limpia

        BlockState stalk = ModBlocks.GLEBA_STALK.defaultBlockState();
        BlockState cap = ModBlocks.GLEBA_CAP.defaultBlockState();
        BlockState crystal = ModBlocks.GLEBA_CRYSTAL.defaultBlockState();

        // arboles de hongo gigantes (pantano)
        int trees = r.nextInt(3);
        for (int i = 0; i < trees; i++) {
            int x = 5 + r.nextInt(6), z = 5 + r.nextInt(6);
            int h = hts[x][z];
            if (zones[x][z] != MARSH || h <= SEA + 1) continue;
            int ht = 7 + r.nextInt(8), rad = 3 + r.nextInt(3);
            for (int k = 1; k <= ht; k++) put(chunk, ocean, surf, pos, x, h + k, z, stalk);
            int top = h + ht;
            for (int dx = -rad; dx <= rad; dx++)
                for (int dz = -rad; dz <= rad; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 <= rad * rad) put(chunk, ocean, surf, pos, x + dx, top + 1, z + dz, cap);
                    if (d2 <= (rad - 2) * (rad - 2) + 1) put(chunk, ocean, surf, pos, x + dx, top + 2, z + dz, cap);
                    if (d2 <= rad * rad && d2 > (rad - 1) * (rad - 1)) put(chunk, ocean, surf, pos, x + dx, top, z + dz, cap);
                }
        }
        // plantas "chorus": tallo con ramas y frutos luminosos
        for (int i = 0; i < 6; i++) {
            int x = 3 + r.nextInt(10), z = 3 + r.nextInt(10);
            int h = hts[x][z];
            if (zones[x][z] == FLESH || h <= SEA + 1) continue;
            int ht = 3 + r.nextInt(4);
            for (int k = 1; k <= ht; k++) put(chunk, ocean, surf, pos, x, h + k, z, stalk);
            put(chunk, ocean, surf, pos, x, h + ht + 1, z, cap);
            for (int b = 0; b < 2; b++) {
                int dx = r.nextBoolean() ? 1 : -1, dz = r.nextInt(3) - 1;
                int by = h + 2 + r.nextInt(Math.max(1, ht - 1));
                put(chunk, ocean, surf, pos, x + dx, by, z + dz, stalk);
                put(chunk, ocean, surf, pos, x + dx, by + 1, z + dz, stalk);
                put(chunk, ocean, surf, pos, x + dx, by + 2, z + dz, cap);
            }
        }
        // cristales (tierras altas y desierto)
        int clusters = r.nextInt(3);
        for (int i = 0; i < clusters; i++) {
            int x = 3 + r.nextInt(10), z = 3 + r.nextInt(10);
            int h = hts[x][z];
            if (zones[x][z] == MARSH || zones[x][z] == FLESH || h <= SEA + 2) continue;
            int ht = 4 + r.nextInt(7);
            for (int k = 1; k <= ht; k++) put(chunk, ocean, surf, pos, x, h + k, z, crystal);
            for (int s = 0; s < 4; s++) {
                int ox = Mth.clamp(x + r.nextInt(5) - 2, 0, 15), oz = Mth.clamp(z + r.nextInt(5) - 2, 0, 15);
                int hh = hts[ox][oz], sh = 2 + r.nextInt(4);
                for (int k = 1; k <= sh; k++) put(chunk, ocean, surf, pos, ox, hh + k, oz, crystal);
            }
        }
        // tumores de carne
        if (r.nextInt(3) == 0) {
            int x = 4 + r.nextInt(8), z = 4 + r.nextInt(8);
            int h = hts[x][z];
            if (zones[x][z] == FLESH && h > SEA + 1) {
                int rad = 2 + r.nextInt(3);
                for (int dx = -rad; dx <= rad; dx++)
                    for (int dy = -1; dy <= rad; dy++)
                        for (int dz = -rad; dz <= rad; dz++)
                            if (dx * dx + dy * dy * 1.4 + dz * dz <= rad * rad + 1)
                                put(chunk, ocean, surf, pos, x + dx, h + dy, z + dz, flesh);
            }
        }
        // rocas grandes
        if (r.nextInt(3) == 0) {
            int x = 4 + r.nextInt(8), z = 4 + r.nextInt(8);
            int h = hts[x][z];
            if (h > SEA + 1 && zones[x][z] != FLESH) {
                int rad = 2 + r.nextInt(2);
                for (int dx = -rad; dx <= rad; dx++)
                    for (int dy = 0; dy <= rad; dy++)
                        for (int dz = -rad; dz <= rad; dz++)
                            if (dx * dx + dy * dy * 1.6 + dz * dz <= rad * rad + 1)
                                put(chunk, ocean, surf, pos, x + dx, h + dy, z + dz, stone);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return SEA;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState rs) {
        return Math.max(heightAt(x, z), SEA) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState rs) {
        int h = heightAt(x, z);
        BlockState[] col = new BlockState[level.getHeight()];
        for (int i = 0; i < col.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y <= h) col[i] = ModBlocks.GLEBA_STONE.defaultBlockState();
            else if (y <= SEA) col[i] = Blocks.WATER.defaultBlockState();
            else col[i] = Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) { }
}
