package com.supernova.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Traza un bunker subterraneo 9x4x9 sin luces (queda "super oscuro"), con escalera y trampilla. */
public class BunkerBuilder {
    public static BlockPos build(ServerLevel level, BlockPos origin) {
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int floorY = oy - 9, ceilY = oy - 4;
        RandomSource rnd = level.getRandom();

        for (int x = ox - 5; x <= ox + 5; x++)
            for (int z = oz - 5; z <= oz + 5; z++)
                for (int y = floorY; y <= ceilY; y++) {
                    boolean edge = x == ox - 5 || x == ox + 5 || z == oz - 5 || z == oz + 5 || y == floorY || y == ceilY;
                    BlockState st = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        if (y == floorY) st = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
                        else if (y == ceilY) st = Blocks.DEEPSLATE_TILES.defaultBlockState();
                        else st = rnd.nextInt(7) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    }
                    level.setBlock(new BlockPos(x, y, z), st, 2);
                }
        for (int sx : new int[]{-2, 2})
            for (int sz : new int[]{-2, 2})
                for (int y = floorY + 1; y < ceilY; y++)
                    level.setBlock(new BlockPos(ox + sx, y, oz + sz), Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 2);

        for (int y = ceilY; y <= oy + 2; y++) {
            level.setBlock(new BlockPos(ox + 4, y, oz), Blocks.AIR.defaultBlockState(), 2);
            if (y > ceilY) {
                level.setBlock(new BlockPos(ox + 3, y, oz), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(ox + 5, y, oz), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(ox + 4, y, oz - 1), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
                level.setBlock(new BlockPos(ox + 4, y, oz + 1), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
            }
        }
        for (int y = oy + 1; y <= oy + 2; y++) {
            level.setBlock(new BlockPos(ox + 3, y, oz), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(ox + 5, y, oz), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(ox + 4, y, oz - 1), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(new BlockPos(ox + 4, y, oz + 1), Blocks.AIR.defaultBlockState(), 2);
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
        for (int y = floorY + 1; y <= oy - 1; y++) level.setBlock(new BlockPos(ox + 4, y, oz), ladder, 2);
        level.setBlock(new BlockPos(ox + 4, oy, oz), Blocks.SPRUCE_TRAPDOOR.defaultBlockState(), 2);

        SupernovaData.get(level.getServer()).add(ox - 5, floorY, oz - 5, ox + 6, oy + 1, oz + 6);
        return new BlockPos(ox, floorY + 1, oz);
    }
}
