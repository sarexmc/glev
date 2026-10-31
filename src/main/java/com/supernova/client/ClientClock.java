package com.supernova.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Fraccion de tick transcurrida (para animar suave sin depender de la API de DeltaTracker). */
public final class ClientClock {
    private static long lastTickNs = System.nanoTime();

    private ClientClock() {}

    public static void onTick() {
        lastTickNs = System.nanoTime();
    }

    public static float partial() {
        if (Minecraft.getInstance().isPaused()) return 0f;
        return Mth.clamp((System.nanoTime() - lastTickNs) / 50_000_000f, 0f, 1f);
    }
}
