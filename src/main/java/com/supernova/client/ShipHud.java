package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supernova.entity.ShipEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Interfaz de la nave: distancia, velocidad, combustible, fases de vuelo, cuenta atras y carga de motores. */
public class ShipHud {
    static final int CYAN = 0xFF7FE8FF, CYAN_DIM = 0xA07FE8FF, PINK = 0xFFFF4F7A, PINK_DIM = 0x50FF4F7A, WHITE = 0xFFFFFFFF;
    static final String[] NAMES = {"ENCENDIDO DE MOTORES", "SECUENCIA DE DESPEGUE", "DESPEGUE", "TRÁNSITO ATMOSFÉRICO",
            "SALIDA DE ATMÓSFERA", "SALTO TEMPORAL", "ESTABILIZACIÓN", "APROXIMACIÓN", "ATERRIZAJE"};

    private static final String[] KEYS = {null, null, "starship.flight.takeoff", "starship.flight.atmospheric_transit", "starship.flight.orbit",
            "starship.flight.jump", "starship.flight.stabilizing", "starship.flight.approach", "starship.flight.landing"};

    static String n(int i) {
        if (KEYS[i] == null) return NAMES[i];
        return I18n.get(KEYS[i]).toUpperCase(java.util.Locale.ROOT);
    }

    static void text(GuiGraphics g, String s, float x, float y, int color, float scale, int align) {
        Font f = Minecraft.getInstance().font;
        Component c = SpaceFont.c(s);
        float wd = f.width(c) * scale;
        float ox = align == 1 ? -wd / 2f : align == 2 ? -wd : 0f;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(x + ox, y, 0);
        ps.scale(scale, scale, 1f);
        g.drawString(f, c, 0, 0, color, false);
        ps.popPose();
    }

    public static void render(GuiGraphics g, int w, int h, float pt, ShipEntity ship) {
        ShipEntity.Stage st = ship.stage();
        int si = st.ordinal();
        int ticks = ship.stageTicksClient();
        int cx = w / 2;

        // ---- distancia + regla (0..1200 m)
        text(g, ship.meters() + " m", cx, 12, PINK, 1.6f, 1);
        int rx0 = cx - 170, ry = 36;
        g.fill(rx0, ry, rx0 + 340, ry + 1, CYAN_DIM);
        for (int i = 0; i <= 40; i++) {
            int x = rx0 + Math.round(340f * i / 40f);
            boolean major = i % 10 == 0;
            g.fill(x, ry, x + 1, ry + (major ? 7 : 4), CYAN_DIM);
            if (major) text(g, String.valueOf(i * 30), x, ry + 10, CYAN_DIM, 0.7f, 1);
        }
        int mx = rx0 + Math.round(340f * Mth.clamp(ship.meters() / 1200f, 0f, 1f));
        g.fill(mx - 3, ry + 24, mx + 4, ry + 25, PINK);
        g.fill(mx - 2, ry + 23, mx + 3, ry + 24, PINK);
        g.fill(mx - 1, ry + 22, mx + 2, ry + 23, PINK);
        g.fill(mx, ry + 21, mx + 1, ry + 22, PINK);

        // ---- velocidad
        text(g, "VELOCIDAD", w - 90, 14, CYAN_DIM, 0.75f, 1);
        text(g, String.format("%.1f", ship.speedMs()), w - 90, 26, WHITE, 1.4f, 1);
        text(g, "m/s", w - 90, 44, CYAN_DIM, 0.7f, 1);
        g.fill(w - 140, 56, w - 40, 57, CYAN_DIM);

        // ---- combustible (10 segmentos): se gasta de verdad durante el ascenso
        int fx = 26, fy = h / 2 - 60;
        text(g, "COMBUSTIBLE", fx, fy - 14, CYAN_DIM, 0.7f, 0);
        g.fill(fx, fy, fx + 1, fy + 110, PINK_DIM);
        for (int i = 0; i < 10; i++) {
            boolean on = ship.fuel() >= (10 - i) * 10 - 4;
            g.fill(fx + 8, fy + i * 11, fx + 32, fy + i * 11 + 8, on ? PINK : PINK_DIM);
        }
        text(g, "100", fx + 38, fy - 1, CYAN_DIM, 0.6f, 0);
        text(g, "50", fx + 38, fy + 50, CYAN_DIM, 0.6f, 0);
        text(g, "0", fx + 38, fy + 100, CYAN_DIM, 0.6f, 0);
        text(g, ship.fuel() + "%", fx + 58, fy + 48, PINK, 1.1f, 0);
        text(g, "RESERVA", fx, fy + 116, CYAN_DIM, 0.7f, 0);

        // ---- lista de fases (derecha)
        int lx = w - 190, ly = h / 2 - 56;
        text(g, I18n.get("starship.flight.route").toUpperCase(java.util.Locale.ROOT), lx, ly - 16, CYAN_DIM, 0.7f, 0);
        text(g, "FASE " + Math.max(1, Math.min(7, si - 1)) + "/7", lx, ly - 5, CYAN_DIM, 0.6f, 0);
        for (int i = 2; i <= 8; i++) {
            String mark = si > i ? "● " : si == i ? "► " : "○ ";
            int col = si == i ? PINK : si > i ? CYAN : CYAN_DIM;
            text(g, mark + n(i), lx, ly + 10 + (i - 2) * 14, col, 0.75f, 0);
        }

        // ---- fase actual (arriba del mensaje de accion)
        if (si >= 2) {
            text(g, n(si) + " · EN CURSO", cx, h - 128, CYAN, 0.9f, 1);
            g.fill(cx - 110, h - 134, cx + 110, h - 133, CYAN_DIM);
            g.fill(cx - 110, h - 115, cx + 110, h - 114, CYAN_DIM);
        }
        // ---- encendido de motores: mantener CTRL
        if (st == ShipEntity.Stage.CHARGING) {
            int bx0 = cx - 140, bx1 = cx + 140, by = h - 150;
            int c = ship.charge();
            for (int[] k : new int[][]{{bx0 - 14, by - 62, 1, 1}, {bx1 + 14, by - 62, -1, 1}, {bx0 - 14, by + 42, 1, -1}, {bx1 + 14, by + 42, -1, -1}}) {
                g.fill(k[0], k[1], k[0] + k[2] * 12, k[1] + (k[3] > 0 ? 1 : -1), CYAN_DIM);
                g.fill(k[0], k[1], k[0] + (k[2] > 0 ? 1 : -1), k[1] + k[3] * 12, CYAN_DIM);
            }
            text(g, "ENCENDIENDO MOTORES", cx, by - 52, CYAN_DIM, 0.6f, 1);
            text(g, "CARGANDO", cx, by - 38, WHITE, 1.1f, 1);
            g.fill(bx0, by - 12, bx1, by - 4, 0x60FFFFFF);
            g.fill(bx0 + 1, by - 11, bx0 + 1 + (int) ((bx1 - bx0 - 2) * c / 100f), by - 5, CYAN);
            text(g, "CARGA " + c + "%", cx, by + 2, CYAN_DIM, 0.6f, 1);
            text(g, "[CTRL]  SIGUE MANTENIENDO", cx, by + 18, WHITE, 0.75f, 1);
            text(g, "SUELTA PARA CANCELAR", cx, by + 34, CYAN_DIM, 0.6f, 1);
        }
        // ---- cuenta atras 3 2 1
        if (st == ShipEntity.Stage.COUNTDOWN) {
            int num = Mth.clamp(3 - ticks / 20, 1, 3);
            float ry2 = h * 0.58f;
            text(g, "SECUENCIA DE DESPEGUE", cx, ry2 - 52, CYAN, 0.8f, 1);
            Gfx2D.ring(g, cx, ry2, 26, 30, 48, new float[]{0.5f, 0.9f, 1f}, new float[]{0.7f, 0.5f, 1f}, 0.9f);
            text(g, String.valueOf(num), cx, ry2 - 12, WHITE, 3f, 1);
        }
        // ---- nombre del planeta de destino
        if (st == ShipEntity.Stage.STABILIZE || (st == ShipEntity.Stage.APPROACH && ticks < 100)) {
            text(g, ship.isReturning() ? "TIERRA" : "GLEBA", w - 120, h - 150, WHITE, 3f, 1);
        }
    }
}
