package com.supernova.server;

import com.supernova.Timeline;
import com.supernova.network.ScenePayload;
import com.supernova.registry.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/** Logica de servidor de las dos partes de la supernova. */
public class SupernovaManager {
    /** Si es true, los jugadores que no esten refugiados mueren cuando llega la onda expansiva (parte 1). */
    public static final boolean KILL_UNPROTECTED = true;

    private static int scene = 0;
    private static int tick = 0;

    public static void start(MinecraftServer server, int newScene) {
        scene = newScene;
        tick = 0;
        sync(server);
    }

    public static void stop(MinecraftServer server) {
        scene = 0;
        tick = 0;
        sync(server);
    }

    private static void sync(MinecraftServer server) {
        ScenePayload pl = new ScenePayload(scene, tick);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) ServerPlayNetworking.send(p, pl);
    }

    public static void onJoin(ServerPlayer p) {
        if (scene != 0) ServerPlayNetworking.send(p, new ScenePayload(scene, tick));
    }

    public static void onTick(MinecraftServer server) {
        if (scene == 0) return;
        tick++;
        if (tick % 100 == 0) sync(server);
        if (scene == 1) part1(server); else part2(server);
    }

    // ------------------------------------------------------------------ parte 1
    private static void part1(MinecraftServer s) {
        countdown(s, Timeline.P1_BLAST);
        if (tick == Timeline.P1_BLAST - 80) sound(s, ModSounds.RUMBLE, 2f, 1f);
        if (tick == Timeline.P1_BLAST) {
            sound(s, ModSounds.EXPLOSION, 5f, 1f);
            broadcast(s, Component.literal("¡Supernova! La onda expansiva se acerca.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }
        if (tick == Timeline.P1_BLAST + 30) sound(s, ModSounds.SPACE_HUM, 2.5f, 0.6f);
        if (tick == Timeline.P1_WAVE - 64) sound(s, ModSounds.SHOCKWAVE, 6f, 1f);
        if (tick == Timeline.P1_WAVE) {
            sound(s, ModSounds.WIND_GUST, 5f, 0.8f);
            shockwave(s);
        }
        if (tick >= Timeline.P1_END) stop(s);
    }

    private static void shockwave(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            if (p.level().dimension() != Level.OVERWORLD || p.isCreative() || p.isSpectator()) continue;
            if (isSheltered(p)) {
                p.displayClientMessage(Component.literal("El bunker resiste la onda expansiva.").withStyle(ChatFormatting.GRAY), true);
            } else if (KILL_UNPROTECTED) {
                p.hurt(p.level().damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
    }

    // ------------------------------------------------------------------ parte 2
    private static void part2(MinecraftServer s) {
        countdown(s, Timeline.P2_BLAST);
        if (tick == Timeline.P2_BLAST - 80) sound(s, ModSounds.RUMBLE, 2f, 1.1f);
        if (tick == Timeline.P2_BLAST) {
            sound(s, ModSounds.EXPLOSION, 6f, 1f);
            sound(s, ModSounds.RINGING, 0.7f, 1f);
            broadcast(s, Component.literal("¡Supernova! La onda expansiva se acerca.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        }
        if (tick == Timeline.P2_BLAST + 50) sound(s, ModSounds.EXPLOSION, 6f, 0.85f);
        if (tick == Timeline.P2_BLAST + 180) sound(s, ModSounds.SHOCKWAVE, 5f, 0.9f);
        if (tick == Timeline.P2_SHIP_MSG)
            broadcast(s, Component.literal("Han explotado dos supernovas juntas. ¡Debemos irnos en una nave!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    // ------------------------------------------------------------------ utilidades
    private static void countdown(MinecraftServer s, int blast) {
        int left = blast - tick;
        for (int secs : new int[]{60, 30, 10, 5, 4, 3, 2, 1}) {
            if (left == secs * 20) {
                Component c = Component.literal("La estrella colapsará en " + secs + (secs == 1 ? " segundo" : " segundos")
                        + ". ¡Aléjate todo lo que puedas!").withStyle(ChatFormatting.RED);
                broadcast(s, c, secs == 60);
            }
        }
    }

    private static void broadcast(MinecraftServer s, Component c, boolean chat) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.displayClientMessage(c, true);
            if (chat) p.sendSystemMessage(c);
        }
    }

    private static void sound(MinecraftServer s, SoundEvent ev, float vol, float pitch) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) p.playNotifySound(ev, SoundSource.MASTER, vol, pitch);
    }

    /** Refugiado = dentro de un bunker trazado, o bajo tierra sin acceso al cielo. */
    public static boolean isSheltered(ServerPlayer p) {
        if (SupernovaData.get(p.server).contains(p.position())) return true;
        BlockPos pos = p.blockPosition().above();
        return !p.serverLevel().canSeeSky(pos) && p.serverLevel().getBrightness(LightLayer.SKY, pos) <= 2;
    }
}
