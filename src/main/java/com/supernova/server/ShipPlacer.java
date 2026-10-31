package com.supernova.server;

import com.supernova.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public class ShipPlacer {
    /** Coloca una base de 3x3 y la nave encima. @return posicion del bloque nave. */
    public static BlockPos place(ServerLevel level, BlockPos near) {
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near);
        int y = surface.getY() - 1;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(new BlockPos(near.getX() + dx, y, near.getZ() + dz), ModBlocks.LAUNCH_PAD.defaultBlockState(), 3);
                for (int dy = 1; dy <= 8; dy++)
                    level.setBlock(new BlockPos(near.getX() + dx, y + dy, near.getZ() + dz), Blocks.AIR.defaultBlockState(), 3);
            }
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++)
                if (Math.abs(dx) > 1 || Math.abs(dz) > 1)
                    for (int dy = 1; dy <= 8; dy++) level.setBlock(new BlockPos(near.getX() + dx, y + dy, near.getZ() + dz), Blocks.AIR.defaultBlockState(), 3);
        BlockPos shipPos = new BlockPos(near.getX(), y + 1, near.getZ());
        level.setBlock(shipPos, ModBlocks.SHIP.defaultBlockState(), 3);
        return shipPos;
    }
}
