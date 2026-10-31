package com.supernova.client;

import com.mojang.logging.LogUtils;
import com.supernova.entity.ShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

/** Capa de pantalla: gris, resplandor, destello, filtro rojo, oscuridad, letterbox cinematografico, tunel y HUD de la nave. */
public class FxOverlay {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int failures = 0;

    public static void render(GuiGraphics g, float pt) {
        if (failures > 5) return;
        try {
            inner(g, pt);
        } catch (Throwable t) {
            failures++;
            LOGGER.error("[Supernova] Error en el overlay (se desactiva tras 5 fallos)", t);
        }
    }

    private static void inner(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        SceneFx.update(pt);
        float anim = mc.level.getGameTime() + pt;

        if (SceneFx.red > 0) {
            Gfx2D.fill(g, 0, 0, w, h, 170, 0, 0, SceneFx.red);
            g.fillGradient(0, h / 2, w, h, Gfx2D.argb(0f, 60, 0, 0), Gfx2D.argb(SceneFx.red * 1.2f, 50, 0, 0));
        }
        if (SceneFx.gray > 0) Gfx2D.fill(g, 0, 0, w, h, 115, 115, 120, SceneFx.gray);
        if (SceneFx.bloom > 0) bloom(g, mc, w, h, SceneFx.bloom);
        if (SceneFx.beam > 0) sweep(g, w, h, ClientSceneState.time(pt), SceneFx.beam, SceneFx.vivid);
        if (SceneFx.dim > 0) {
            Gfx2D.fill(g, 0, 0, w, h, 0, 0, 4, SceneFx.dim);
            g.fillGradient(0, 0, w, h, Gfx2D.argb(SceneFx.dim * 0.3f, 0, 0, 0), Gfx2D.argb(0f, 0, 0, 0));
        }
        if (SceneFx.tint > 0) {
            Gfx2D.fill(g, 0, 0, w, h, 130, 0, 10, SceneFx.tint);
            g.fillGradient(0, 0, w, h / 2, Gfx2D.argb(SceneFx.tint * 0.8f, 40, 0, 0), Gfx2D.argb(0f, 40, 0, 0));
        }
        if (SceneFx.dark > 0) Gfx2D.fill(g, 0, 0, w, h, 0, 0, 6, SceneFx.dark);
        if (SceneFx.entry > 0) Gfx2D.fill(g, 0, 0, w, h, 255, 90, 90, SceneFx.entry);
        if (SceneFx.shockBand > 0) shockBand(g, w, h, SceneFx.shockBand);
        if (SceneFx.wind > 0) wind(g, w, h, anim, SceneFx.wind);

        ShipEntity ship = mc.player.getVehicle() instanceof ShipEntity s ? s : null;
        if (SceneFx.tunnel > 0) tunnel(g, w, h, anim, SceneFx.tunnel, ship);
        else if (SceneFx.speed > 0 && !(StarshipPost.available() && SceneFx.shipFlight)) Gfx2D.streaks(g, w / 2f, h / 2f, anim, Math.max(w, h) * 0.7f, 70, SceneFx.speed * 0.7f);
        if (ship != null) ShipHud.render(g, w, h, pt, ship);
        if (SceneFx.tunnelFadeOut > 0) Gfx2D.fill(g, 0, 0, w, h, 0, 0, 0, SceneFx.tunnelFadeOut);

        if (SceneFx.flashOrange > 0) Gfx2D.fill(g, 0, 0, w, h, 255, 150, 60, SceneFx.flashOrange);
        if (SceneFx.white > 0) Gfx2D.fill(g, 0, 0, w, h, 255, 255, 255, SceneFx.white);
        if (SceneFx.letterbox > 0) {
            int bar = (int) (h * 0.13f * SceneFx.letterbox);
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
    }

    private static float[] starScreen(Minecraft mc, int w, int h) {
        Camera cam = mc.gameRenderer.getMainCamera();
        if (!cam.isInitialized()) return null;
        Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
        Vec3 d = SkyRenderer.STAR_DIR;
        float z = (float) (d.x * look.x() + d.y * look.y() + d.z * look.z());
        if (z <= 0.05f) return null;
        float xl = (float) (d.x * left.x() + d.y * left.y() + d.z * left.z());
        float yu = (float) (d.x * up.x() + d.y * up.y() + d.z * up.z());
        float th = (float) Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        float aspect = (float) w / h;
        return new float[]{(-xl / (z * th * aspect) * 0.5f + 0.5f) * w, (0.5f - yu / (z * th) * 0.5f) * h};
    }

    private static void bloom(GuiGraphics g, Minecraft mc, int w, int h, float k) {
        Gfx2D.fill(g, 0, 0, w, h, 235, 225, 255, 0.10f * k);
        float[] p = starScreen(mc, w, h);
        if (p == null) return;
        Gfx2D.radial(g, p[0], p[1], h * 0.75f * (0.5f + 0.5f * k), 1f, 0.95f, 1f, 0.55f * k);
        Gfx2D.radial(g, p[0], p[1], h * 0.22f, 1f, 1f, 1f, 0.85f * k);
        int ly = (int) p[1];
        Gfx2D.fill(g, 0, ly, w, ly + 1, 235, 235, 255, 0.55f * k);
        Gfx2D.fill(g, 0, ly - 2, w, ly + 3, 200, 200, 255, 0.10f * k);
    }

    private static void sweep(GuiGraphics g, int w, int h, float t, float alpha, boolean vivid) {
        float speed = vivid ? 0.012f : 0.007f;
        float frac = (t * speed) % 1.4f - 0.2f;
        int x = (int) (frac * w);
        int ly = (int) (h * 0.20f);
        Gfx2D.fill(g, 0, ly, w, ly + 1, 235, 235, 255, alpha * 0.55f);
        Gfx2D.fill(g, 0, ly - 1, w, ly + 2, 200, 200, 255, alpha * 0.12f);
        for (int k = 0; k < 8; k++) {
            int bw = 2 + k * 6;
            Gfx2D.fill(g, x - bw, 0, x + bw, h, 220, 225, 255, alpha * 0.14f * (1 - k / 8f) * (vivid ? 1.3f : 1f));
        }
        for (int k = 0; k < 6; k++)
            Gfx2D.fill(g, x - 20 - k * 12, ly - 1 - k, x + 20 + k * 12, ly + 2 + k, 255, 255, 255, alpha * 0.14f);
    }

    /** La onda expansiva acercandose: una banda de luz naranja que sube desde el horizonte. */
    private static void shockBand(GuiGraphics g, int w, int h, float k) {
        int top = (int) (h * (1f - 0.45f * k));
        g.fillGradient(0, top, w, h, Gfx2D.argb(0f, 255, 170, 80), Gfx2D.argb(0.6f * k, 255, 150, 60));
        Gfx2D.fill(g, 0, top, w, top + 2, 255, 230, 190, 0.5f * k);
    }

    private static void wind(GuiGraphics g, int w, int h, float anim, float k) {
        for (int i = 0; i < 22; i++) {
            float y = (i * 97 % 100) / 100f * h;
            float x = ((anim * 55f + i * 173f) % (w + 500)) - 250;
            float len = 120 + (i * 61 % 260);
            Gfx2D.fill(g, (int) x, (int) y, (int) (x + len), (int) y + 1, 255, 235, 210, 0.45f * k);
        }
        Gfx2D.fill(g, 0, 0, w, h, 255, 190, 120, 0.12f * k);
    }

    /** Salto temporal: aros neon, rayas de estrellas y barra de carga de terreno del destino. */
    private static void tunnel(GuiGraphics g, int w, int h, float anim, float a, ShipEntity ship) {
        float cx = w / 2f, cy = h / 2f, maxR = Math.min(w, h) * 0.55f;
        if (!(StarshipPost.available() && SceneFx.shipFlight)) {   // sin el shader: tunel de respaldo en 2D
            Gfx2D.fill(g, 0, 0, w, h, 0, 0, 0, a);
            Gfx2D.streaks(g, cx, cy, anim, Math.max(w, h) * 0.7f, 90, a);
            float[] cyan = {0.25f, 0.9f, 1f}, purple = {0.7f, 0.4f, 1f};
            for (int k = 0; k < 7; k++) {
                float phase = (anim * 0.016f + k / 7f) % 1f;
                float r = 18 + (float) Math.pow(phase, 1.6) * maxR;
                float thick = 3 + phase * 9;
                Gfx2D.ring(g, cx, cy, r, r + thick, 64, cyan, purple, a * Mth.sin((float) Math.PI * phase));
            }
        }
        ShipHud.text(g, "SALTO TEMPORAL · EN CURSO", cx, h * 0.80f, 0xFFFF4F7A, 0.9f, 1);
        if (ship != null) {
            int pct = ship.loadProgress();
            String dest = ship.isReturning() ? "TIERRA" : "GLEBA";
            ShipHud.text(g, "CARGANDO TERRENO · " + dest + "  " + pct + "%", cx, h * 0.86f, 0xFF7FE8FF, 0.8f, 1);
            int bx0 = (int) cx - 120, bx1 = (int) cx + 120, by = (int) (h * 0.90f);
            g.fill(bx0, by, bx1, by + 5, 0x50FFFFFF);
            g.fill(bx0 + 1, by + 1, bx0 + 1 + (int) (238 * pct / 100f), by + 4, 0xFF7FE8FF);
        }
    }
}
