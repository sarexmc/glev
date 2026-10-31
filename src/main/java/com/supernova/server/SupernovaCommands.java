package com.supernova.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.supernova.registry.ModItems;
import com.supernova.world.GlebaChunkGenerator;
import com.supernova.world.GlebaDimension;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class SupernovaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("supernova").requires(s -> s.hasPermission(2))
                .then(Commands.literal("part1").executes(c -> {
                    SupernovaManager.start(c.getSource().getServer(), 1);
                    c.getSource().sendSuccess(() -> Component.literal("Supernova - Parte 1 iniciada."), true);
                    return 1;
                }))
                .then(Commands.literal("part2").executes(c -> {
                    SupernovaManager.start(c.getSource().getServer(), 2);
                    c.getSource().sendSuccess(() -> Component.literal("Supernova - Parte 2 iniciada."), true);
                    return 1;
                }))
                .then(Commands.literal("stop").executes(c -> {
                    SupernovaManager.stop(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Supernova detenida."), true);
                    return 1;
                }))
                .then(Commands.literal("bunker")
                        .executes(c -> bunker(c.getSource(), false))
                        .then(Commands.literal("tp").executes(c -> bunker(c.getSource(), true))))
                .then(Commands.literal("ship").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel level = p.serverLevel();
                    BlockPos near = p.blockPosition().relative(p.getDirection(), 5);
                    BlockPos shipPos = ShipPlacer.place(level, near);
                    p.getInventory().add(new ItemStack(ModItems.DILITHIUM_TANK, 2));
                    c.getSource().sendSuccess(() -> Component.literal("Nave colocada en " + shipPos.toShortString()
                            + ". Tienes 2 tanques de dilitio: abre la nave con click derecho."), true);
                    return 1;
                }))
                .then(Commands.literal("gleba").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel gleba = GlebaDimension.get(c.getSource().getServer());
                    if (gleba == null) {
                        c.getSource().sendFailure(Component.literal("No se pudo crear la dimension Gleba: " + GlebaDimension.lastError));
                        return 0;
                    }
                    p.teleportTo(gleba, 0.5, GlebaChunkGenerator.heightAt(0, 0) + 2, 0.5, 0f, 0f);
                    return 1;
                })));
    }

    private static int bunker(CommandSourceStack src, boolean tp) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos origin = p.blockPosition();
        if (origin.getY() - 9 < level.getMinBuildHeight() + 1) {
            src.sendFailure(Component.literal("Muy cerca del fondo del mundo para trazar el bunker aqui."));
            return 0;
        }
        BlockPos inside = BunkerBuilder.build(level, origin);
        src.sendSuccess(() -> Component.literal("Bunker trazado bajo tu posicion. Entrada: trampilla en "
                + origin.offset(4, 0, 0).toShortString() + " (escalera hacia abajo). Interior: " + inside.toShortString()), true);
        if (tp) p.teleportTo(level, inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, p.getYRot(), 0f);
        return 1;
    }
}
