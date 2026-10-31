package com.supernova.server;

import com.supernova.block.ShipBlockEntity;
import com.supernova.entity.ShipEntity;
import com.supernova.registry.ModBlocks;
import com.supernova.registry.ModDimensions;
import com.supernova.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class ShipLauncher {
    /** Pulsar el boton: sube a la tripulacion y empieza el encendido de motores (hay que mantener CTRL). */
    public static void tryLaunch(ServerLevel level, BlockPos pos, ServerPlayer starter) {
        if (!level.getBlockState(pos).is(ModBlocks.SHIP)) return;
        if (!(level.getBlockEntity(pos) instanceof ShipBlockEntity ship)) return;
        if (!level.getBlockState(pos.below()).is(ModBlocks.LAUNCH_PAD)) {
            starter.displayClientMessage(Component.literal("Falta la base de lanzamiento debajo de la nave."), true);
            return;
        }
        if (ship.fuel < ShipBlockEntity.TAKEOFF_FUEL) {
            starter.displayClientMessage(Component.literal("Combustible insuficiente: se necesita al menos "
                    + ShipBlockEntity.TAKEOFF_FUEL + "%. Inserta tanques de dilitio."), true);
            return;
        }
        List<ServerPlayer> crew = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(6), p -> !p.isSpectator()));
        if (!crew.contains(starter)) crew.add(starter);
        if (crew.size() > 4) crew = new ArrayList<>(crew.subList(0, 4));

        boolean fromGleba = level.dimension().equals(ModDimensions.GLEBA);
        int destX = 0, destZ = 0;
        double destGround = 70;
        if (fromGleba) {                       // viaje de regreso a la Tierra
            ServerLevel overworld = level.getServer().overworld();
            BlockPos dest = ship.returnPos != null ? ship.returnPos : overworld.getSharedSpawnPos();
            if (ship.returnPos != null) {
                destGround = dest.getY() - 1;
            } else {
                BlockPos top = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dest);
                dest = top.above();
                destGround = top.getY() - 1;
            }
            destX = dest.getX();
            destZ = dest.getZ();
        }

        ShipEntity entity = new ShipEntity(ModEntities.SHIP, level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setup(pos, pos.getY(), destGround, ship.fuel, ShipEntity.Stage.CHARGING, crew);
        entity.setRoute(fromGleba, destX, destZ);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.addFreshEntity(entity);
        for (ServerPlayer p : crew) p.startRiding(entity, true);
    }
}
