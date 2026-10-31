package com.supernova.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Utilidades de dibujo 2D (anillos, rayas, resplandores) para el overlay. */
public final class Gfx2D {
    private Gfx2D() {}

    public static int argb(float a, int r, int g, int b) {
        return ((int) (Mth.clamp(a, 0f, 1f) * 255f) << 24) | (r << 16) | (g << 8) | b;
    }

    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int gr, int b, float a) {
        if (a <= 0.003f) return;
        g.fill(x1, y1, x2, y2, argb(a, r, gr, b));
    }

    private static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static BufferBuilder quads() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
    }

    public static void ring(GuiGraphics g, float cx, float cy, float r0, float r1, int segs, float[] ca, float[] cb, float alpha) {
        if (alpha <= 0.003f) return;
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = quads();
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2 / segs), a1 = (float) ((i + 1) * Math.PI * 2 / segs);
            float f0 = 0.5f + 0.5f * Mth.cos(a0), f1 = 0.5f + 0.5f * Mth.cos(a1);
            v(b, m, cx, cy, r1, a0, ca, cb, f0, alpha);
            v(b, m, cx, cy, r1, a1, ca, cb, f1, alpha);
            v(b, m, cx, cy, r0, a1, ca, cb, f1, alpha * 0.15f);
            v(b, m, cx, cy, r0, a0, ca, cb, f0, alpha * 0.15f);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    private static void v(BufferBuilder b, Matrix4f m, float cx, float cy, float r, float a, float[] ca, float[] cb, float f, float alpha) {
        b.addVertex(m, cx + Mth.cos(a) * r, cy + Mth.sin(a) * r, 0f)
                .setColor(Mth.lerp(f, ca[0], cb[0]), Mth.lerp(f, ca[1], cb[1]), Mth.lerp(f, ca[2], cb[2]), alpha);
    }

    /** Muchas rayas radiales (efecto tunel / aceleracion). */
    public static void streaks(GuiGraphics g, float cx, float cy, float time, float maxR, int count, float alpha) {
        if (alpha <= 0.003f) return;
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder b = quads();
        for (int i = 0; i < count; i++) {
            float ang = i * 2.39996f;
            float phase = (time * (0.012f + (i % 7) * 0.003f) + (i * 0.173f)) % 1f;
            float r0 = phase * maxR, r1 = r0 + 6f + phase * phase * 80f;
            float w = 0.8f + phase * 1.2f;
            float dx = Mth.cos(ang), dy = Mth.sin(ang), px = -dy * w, py = dx * w;
            float al = alpha * Mth.sin((float) Math.PI * phase);
            b.addVertex(m, cx + dx * r0 - px, cy + dy * r0 - py, 0).setColor(0.85f, 0.9f, 1f, 0f);
            b.addVertex(m, cx + dx * r0 + px, cy + dy * r0 + py, 0).setColor(0.85f, 0.9f, 1f, 0f);
            b.addVertex(m, cx + dx * r1 + px, cy + dy * r1 + py, 0).setColor(0.85f, 0.9f, 1f, al);
            b.addVertex(m, cx + dx * r1 - px, cy + dy * r1 - py, 0).setColor(0.85f, 0.9f, 1f, al);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
    }

    /** Resplandor circular (alpha maximo en el centro, 0 en el borde). */
    public static void radial(GuiGraphics g, float cx, float cy, float radius, float r, float gr, float b, float alpha) {
        if (alpha <= 0.003f) return;
        begin();
        Matrix4f m = g.pose().last().pose();
        BufferBuilder bb = quads();
        int rings = 8, segs = 40;
        for (int i = 0; i < rings; i++) {
            float r0 = radius * i / rings, r1 = radius * (i + 1) / rings;
            float a0 = alpha * (float) Math.pow(1f - i / (float) rings, 2), a1 = alpha * (float) Math.pow(1f - (i + 1) / (float) rings, 2);
            for (int j = 0; j < segs; j++) {
                float t0 = (float) (j * Math.PI * 2 / segs), t1 = (float) ((j + 1) * Math.PI * 2 / segs);
                bb.addVertex(m, cx + Mth.cos(t0) * r0, cy + Mth.sin(t0) * r0, 0).setColor(r, gr, b, a0);
                bb.addVertex(m, cx + Mth.cos(t0) * r1, cy + Mth.sin(t0) * r1, 0).setColor(r, gr, b, a1);
                bb.addVertex(m, cx + Mth.cos(t1) * r1, cy + Mth.sin(t1) * r1, 0).setColor(r, gr, b, a1);
                bb.addVertex(m, cx + Mth.cos(t1) * r0, cy + Mth.sin(t1) * r0, 0).setColor(r, gr, b, a0);
            }
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
    }
}
