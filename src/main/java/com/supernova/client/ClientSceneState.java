package com.supernova.client;

import net.minecraft.client.Minecraft;

/** Estado de la escena (parte 1 / parte 2) en el cliente. Se resincroniza con el servidor cada 5 segundos. */
public class ClientSceneState {
    public static int scene = 0;
    private static float elapsed = 0;

    public static void set(int s, int e) {
        scene = s;
        elapsed = e;
    }

    public static void tick() {
        if (scene != 0 && !Minecraft.getInstance().isPaused()) elapsed++;
    }

    public static float time(float partial) {
        return elapsed + (Minecraft.getInstance().isPaused() ? 0f : partial);
    }
}
