package com.supernova.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.supernova.SupernovaMod;
import com.supernova.registry.ModDimensions;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.util.Random;

/** Dibuja en el cielo: estrella, supernova (anillo + rayo + onda), cupulas, nebulosa, estrellas y planetas texturizados. */
public class SkyRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int failures = 0;

    static final Vec3 STAR_DIR = new Vec3(0.55, 0.5, -0.66).normalize();
    static final float DIST = 90f;
    static final Vec3 G_DIR = new Vec3(0.35, 0.55, 0.75).normalize();
    static final Vec3 E_DIR = new Vec3(-0.75, 0.15, -0.62).normalize();
    static final ResourceLocation EARTH = SupernovaMod.id("textures/environment/earth.png");
    static final ResourceLocation GLEBA = SupernovaMod.id("textures/environment/gleba.png");
    private static final float TWO_PI = (float) (Math.PI * 2);
    private static final float[][] STARS = new float[800][5];

    static {
        Random r = new Random(42);
        for (float[] s : STARS) {
            double u = r.nextDouble() * 2 - 1, a = r.nextDouble() * Math.PI * 2, q = Math.sqrt(1 - u * u);
            s[0] = (float) (q * Math.cos(a));
            s[1] = (float) u;
            s[2] = (float) (q * Math.sin(a));
            s[3] = 0.15f + r.nextFloat() * 0.35f;
            s[4] = 0.5f + r.nextFloat() * 0.5f;
        }
    }

    interface ColorFn { void get(float r, float a, float[] out); }

    public static void render(WorldRenderContext ctx) {
        if (failures > 5) return;
        try {
            inner(ctx);
        } catch (Throwable t) {
            failures++;
            LOGGER.error("[Supernova] Error dibujando el cielo (se desactiva tras 5 fallos)", t);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static BufferBuilder begin(boolean tex) {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, tex ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
    }

    private static void inner(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean gleba = mc.level.dimension().equals(ModDimensions.GLEBA);
        if (mc.level.dimension() != Level.OVERWORLD && !gleba) return;
        float pt = ClientClock.partial();
        SceneFx.update(pt);
        if (!SceneFx.anySky()) return;

        // rotacion de la camara (igual que hace Minecraft para el mundo): asi los astros quedan fijos en el cielo
        Matrix4f m = new Matrix4f().rotation(ctx.camera().rotation().conjugate(new Quaternionf()));
        RenderSystem.setProjectionMatrix(ctx.projectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        float anim = mc.level.getGameTime() + pt;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.defaultBlendFunc();

        if (SceneFx.domeGleba > 0) {
            dome(m, new float[]{0.10f, 0.02f, 0.22f}, new float[]{0.66f, 0.18f, 0.58f}, 0.95f * SceneFx.domeGleba);
            stars(m, 0.9f * SceneFx.domeGleba, anim);
        }
        if (SceneFx.domeRed > 0) dome(m, new float[]{0.28f, 0f, 0.02f}, new float[]{0.9f, 0.1f, 0.06f}, 0.78f * SceneFx.domeRed);
        if (SceneFx.nebula > 0) nebula(m, anim, SceneFx.nebula * (1f - 0.6f * SceneFx.domeBlack));
        boolean shader = StarshipPost.available() && SceneFx.shipFlight;
        if (SceneFx.domeBlack > 0 && !shader) {
            dome(m, new float[]{0, 0, 0}, new float[]{0, 0, 0}, SceneFx.domeBlack);
            stars(m, SceneFx.domeBlack, anim);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            Vec3 sunDir = new Vec3(-0.45, 0.55, 0.7).normalize();
            float sa = SceneFx.domeBlack;
            disc(m, sunDir, DIST, 7f, 3, 24, (r, a, o) -> { o[0] = 1f; o[1] = 0.9f; o[2] = 0.45f; o[3] = sa * 0.5f * (1 - r) * (1 - r); });
            disc(m, sunDir, DIST, 1.6f, 1, 4, (r, a, o) -> { o[0] = 1f; o[1] = 0.95f; o[2] = 0.3f; o[3] = sa; });
            RenderSystem.defaultBlendFunc();
        }
        Vec3 light = new Vec3(0.5, 0.7, -0.3).normalize();
        ResourceLocation below = SceneFx.belowGleba ? GLEBA : EARTH, small = SceneFx.belowGleba ? EARTH : GLEBA;
        float[] atBelow = SceneFx.belowGleba ? new float[]{1f, 0.45f, 0.55f} : new float[]{0.35f, 0.6f, 1f};
        float[] atSmall = SceneFx.belowGleba ? new float[]{0.35f, 0.6f, 1f} : new float[]{1f, 0.4f, 0.5f};
        if (SceneFx.belowAlpha > 0 && !shader)
            texSphere(m, below, 320f, 0, -(320f + SceneFx.belowGap), 0, SceneFx.belowAlpha, anim * 0.0004f, light, atBelow);
        if (SceneFx.smallAlpha > 0 && !shader) {
            Vec3 d = (SceneFx.belowGleba ? E_DIR : G_DIR).scale(DIST);
            texSphere(m, small, 9f, (float) d.x, (float) d.y, (float) d.z, SceneFx.smallAlpha, anim * 0.0009f, light, atSmall);
        }
        if (SceneFx.starOn) star(m, anim);
        if (SceneFx.ring1 > 0) {
            if (SceneFx.twoRings) {
                supernova(m, STAR_DIR.yRot(-0.30f), SceneFx.ringR, anim, SceneFx.ring1, true, 0f);
                supernova(m, STAR_DIR.yRot(0.30f), SceneFx.ringR * 0.92f, anim, SceneFx.ring2, true, 1.9f);
            } else {
                supernova(m, STAR_DIR, SceneFx.ringR, anim, SceneFx.ring1, false, 0f);
            }
        }
        if (SceneFx.shockRing > 0) shockRing(m, SceneFx.shockRing);
    }

    // ------------------------------------------------------------------ primitivas
    private static void disc(Matrix4f m, Vec3 d, float dist, float radius, int rings, int segs, ColorFn fn) {
        Vec3 ref = Math.abs(d.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 rt = d.cross(ref).normalize();
        Vec3 up = rt.cross(d).normalize();
        float cx = (float) (d.x * dist), cy = (float) (d.y * dist), cz = (float) (d.z * dist);
        BufferBuilder b = begin(false);
        float[] c = new float[4];
        for (int i = 0; i < rings; i++) {
            float r0 = i / (float) rings, r1 = (i + 1) / (float) rings;
            for (int j = 0; j < segs; j++) {
                float a0 = j * TWO_PI / segs, a1 = (j + 1) * TWO_PI / segs;
                vtx(b, m, rt, up, cx, cy, cz, radius, r0, a0, fn, c);
                vtx(b, m, rt, up, cx, cy, cz, radius, r1, a0, fn, c);
                vtx(b, m, rt, up, cx, cy, cz, radius, r1, a1, fn, c);
                vtx(b, m, rt, up, cx, cy, cz, radius, r0, a1, fn, c);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void vtx(BufferBuilder b, Matrix4f m, Vec3 rt, Vec3 up, float cx, float cy, float cz,
                            float radius, float rn, float a, ColorFn fn, float[] c) {
        fn.get(rn, a, c);
        float ca = Mth.cos(a) * radius * rn, sa = Mth.sin(a) * radius * rn;
        b.addVertex(m, (float) (cx + rt.x * ca + up.x * sa), (float) (cy + rt.y * ca + up.y * sa), (float) (cz + rt.z * ca + up.z * sa))
                .setColor(c[0], c[1], c[2], Mth.clamp(c[3], 0f, 1f));
    }

    private static void streak(Matrix4f m, Vec3 d, float dist, float len, float width, float angle, float[] col, float alpha) {
        Vec3 ref = Math.abs(d.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 rt = d.cross(ref).normalize();
        Vec3 up = rt.cross(d).normalize();
        Vec3 ax = rt.scale(Mth.cos(angle)).add(up.scale(Mth.sin(angle)));
        Vec3 pp = rt.scale(-Mth.sin(angle)).add(up.scale(Mth.cos(angle)));
        Vec3 c = d.scale(dist);
        BufferBuilder b = begin(false);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 end = c.add(ax.scale(len * side));
            Vec3 t1 = c.add(pp.scale(width)), t2 = c.subtract(pp.scale(width));
            b.addVertex(m, (float) end.x, (float) end.y, (float) end.z).setColor(col[0], col[1], col[2], 0f);
            b.addVertex(m, (float) t1.x, (float) t1.y, (float) t1.z).setColor(col[0], col[1], col[2], alpha * 0.1f);
            b.addVertex(m, (float) c.x, (float) c.y, (float) c.z).setColor(col[0], col[1], col[2], alpha);
            b.addVertex(m, (float) t2.x, (float) t2.y, (float) t2.z).setColor(col[0], col[1], col[2], alpha * 0.1f);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void dome(Matrix4f m, float[] top, float[] hor, float alpha) {
        int stacks = 14, slices = 28;
        float R = 95f;
        BufferBuilder b = begin(false);
        for (int i = 0; i < stacks; i++) {
            float p0 = (float) (-Math.PI / 2 + Math.PI * i / stacks), p1 = (float) (-Math.PI / 2 + Math.PI * (i + 1) / stacks);
            for (int j = 0; j < slices; j++) {
                float l0 = j * TWO_PI / slices, l1 = (j + 1) * TWO_PI / slices;
                dv(b, m, R, p0, l0, top, hor, alpha);
                dv(b, m, R, p1, l0, top, hor, alpha);
                dv(b, m, R, p1, l1, top, hor, alpha);
                dv(b, m, R, p0, l1, top, hor, alpha);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private static void dv(BufferBuilder b, Matrix4f m, float R, float phi, float lam, float[] top, float[] hor, float a) {
        float y = Mth.sin(phi), q = Mth.cos(phi);
        float f = y > 0 ? (float) Math.pow(y, 0.6) : 0f;
        b.addVertex(m, q * Mth.cos(lam) * R, y * R, q * Mth.sin(lam) * R)
                .setColor(Mth.lerp(f, hor[0], top[0]), Mth.lerp(f, hor[1], top[1]), Mth.lerp(f, hor[2], top[2]), a);
    }

    private static void stars(Matrix4f m, float alpha, float anim) {
        BufferBuilder b = begin(false);
        for (int i = 0; i < STARS.length; i++) {
            float[] s = STARS[i];
            Vec3 d = new Vec3(s[0], s[1], s[2]);
            Vec3 ref = Math.abs(d.y) > 0.98 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 rt = d.cross(ref).normalize().scale(s[3]);
            Vec3 up = rt.cross(d).normalize().scale(s[3]);
            Vec3 c = d.scale(88);
            float a = alpha * s[4] * (0.8f + 0.2f * Mth.sin(anim * 0.07f + i));
            float[][] k = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (float[] q : k) {
                Vec3 p = c.add(rt.scale(q[0])).add(up.scale(q[1]));
                b.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(1f, 1f, 1f, a);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** Nebulosa: manchas de color en el cielo de Gleba (magenta, violeta, turquesa, naranja). */
    private static void nebula(Matrix4f m, float anim, float alpha) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        Vec3[] dirs = {new Vec3(-0.4, 0.5, 0.7), new Vec3(0.7, 0.3, 0.5), new Vec3(-0.7, 0.4, -0.5), new Vec3(0.1, 0.9, -0.2), new Vec3(0.5, 0.25, -0.8)};
        float[][] cols = {{0.9f, 0.2f, 0.7f}, {0.5f, 0.25f, 1f}, {0.2f, 0.8f, 0.9f}, {0.8f, 0.3f, 0.9f}, {1f, 0.5f, 0.3f}};
        float[] rad = {62, 54, 58, 46, 50};
        for (int i = 0; i < dirs.length; i++) {
            final float seed = i * 1.7f;
            final float[] cc = cols[i];
            disc(m, dirs[i].normalize(), DIST, rad[i], 6, 48, (r, a, o) -> {
                float n = 0.5f + 0.5f * Mth.sin(a * 3f + r * 4f + seed + anim * 0.002f) * Mth.sin(a * 5f - seed);
                o[0] = cc[0]; o[1] = cc[1]; o[2] = cc[2];
                o[3] = 0.28f * alpha * (0.35f + 0.65f * n) * (float) Math.pow(1f - r, 1.4);
            });
        }
        RenderSystem.defaultBlendFunc();
    }

    private static void star(Matrix4f m, float anim) {
        float R = SceneFx.starR * SceneFx.starBreath;
        float white = SceneFx.starWhite;
        float breath = SceneFx.starBreath;
        boolean viv = SceneFx.vivid;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float big = viv ? 0.6f : 0.42f;
        disc(m, STAR_DIR, DIST, R * 3.6f, 6, 40, (r, a, o) -> {
            o[0] = 1f; o[1] = Mth.lerp(white, 0.5f, 0.95f); o[2] = Mth.lerp(white, 0.2f, 0.9f);
            float f = 1 - r; o[3] = big * breath * f * f;
        });
        disc(m, STAR_DIR, DIST, R * 1.9f, 5, 40, (r, a, o) -> {
            o[0] = 1f; o[1] = Mth.lerp(white, 0.78f, 1f); o[2] = Mth.lerp(white, 0.45f, 1f);
            float f = 1 - r; o[3] = 0.45f * breath * (float) Math.pow(f, 1.5);
        });
        if (SceneFx.nebula > 0)  // resplandor grande y pulsante cuando la supernova esta arriba (cielo de Gleba)
            disc(m, STAR_DIR, DIST, R * 8f, 6, 40, (r, a, o) -> { o[0] = 1f; o[1] = 0.5f; o[2] = 0.35f; o[3] = 0.20f * breath * (float) Math.pow(1f - r, 2.0); });
        RenderSystem.defaultBlendFunc();
        disc(m, STAR_DIR, DIST, R, 6, 48, (r, a, o) -> {
            float rr = (float) Math.pow(r, 1.5f);
            float gran = 0.12f * Mth.sin(a * 9 + r * 14 + anim * 0.02f) * r + 0.1f * Mth.sin(a * 17 - r * 9 - anim * 0.015f) * r;
            float cr = Mth.lerp(rr, 1f, 1f), cg = Mth.lerp(rr, 0.97f, 0.5f) + gran, cb = Mth.lerp(rr, 0.78f, 0.12f) + gran * 0.5f;
            o[0] = Mth.lerp(white, cr, 1f); o[1] = Mth.lerp(white, cg, 1f); o[2] = Mth.lerp(white, cb, 1f);
            o[3] = 1f;
        });
        if (viv) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            disc(m, STAR_DIR, DIST, R * 1.15f, 3, 40, (r, a, o) -> { o[0] = 1f; o[1] = 0.9f; o[2] = 0.7f; o[3] = 0.35f * breath * r; });
            RenderSystem.defaultBlendFunc();
        }
    }

    private static void supernova(Matrix4f m, Vec3 dir, float R, float anim, float alpha, boolean vivid, float seed) {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float boost = vivid ? 1.2f : 0.9f;
        disc(m, dir, DIST, R, 16, 110, (r, a, o) -> {
            float n = 0.5f + 0.5f * (Mth.sin(a * 11 + anim * 0.01f + seed) * 0.5f + Mth.sin(a * 29 - anim * 0.015f + r * 6) * 0.35f + Mth.sin(a * 53 + 1.7f) * 0.15f);
            float t1 = 0.5f + 0.5f * Mth.sin(a * 3 + r * 5 + anim * 0.004f + seed);
            float cr = Mth.lerp(t1, 1f, 0.2f), cg = Mth.lerp(t1, 0.25f, 0.9f), cb = Mth.lerp(t1, 0.35f, 0.8f);
            float pu = n * (1 - t1) * 0.5f;
            cr = Mth.lerp(pu, cr, 0.6f); cg = Mth.lerp(pu, cg, 0.3f); cb = Mth.lerp(pu, cb, 1f);
            float prof = SceneFx.ss(0.2f, 0.75f, r) * 0.6f + SceneFx.ss(0.78f, 0.92f, r) * 0.4f;
            prof *= 1 - SceneFx.ss(0.93f, 1f, r);
            o[0] = cr; o[1] = cg; o[2] = cb;
            o[3] = prof * (0.55f + 0.45f * n) * alpha * boost;
        });
        float ang = 0.6f + Mth.sin(anim * 0.002f + seed) * 0.3f;
        float[] w1 = {0.85f, 0.88f, 1f};
        streak(m, dir, DIST, R * 1.7f, R * 0.05f, ang, w1, alpha * 0.95f);
        streak(m, dir, DIST, R * 1.2f, R * 0.035f, ang + 1.9f, w1, alpha * 0.55f);
        disc(m, dir, DIST, R * 0.09f, 2, 20, (r, a, o) -> { o[0] = 1f; o[1] = 1f; o[2] = 1f; o[3] = alpha * (1 - r); });
        RenderSystem.defaultBlendFunc();
    }

    /** Anillo de choque brillante que se expande por el cielo desde la estrella en el momento de la explosion. */
    private static void shockRing(Matrix4f m, float k) {
        float R = 40f + 160f * SceneFx.ease(k);
        float al = (1f - k) * (1f - k);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        disc(m, STAR_DIR, DIST, R, 4, 120, (r, a, o) -> {
            float band = (float) Math.exp(-Math.pow((r - 0.92f) / 0.06f, 2));
            o[0] = 1f; o[1] = 0.82f; o[2] = 0.6f; o[3] = band * al * 0.9f + (float) Math.exp(-Math.pow((r - 0.75f) / 0.2f, 2)) * al * 0.18f;
        });
        RenderSystem.defaultBlendFunc();
    }

    // ------------------------------------------------------------------ planetas texturizados
    private static void texSphere(Matrix4f m, ResourceLocation tex, float R, float cx, float cy, float cz, float alpha,
                                  float rot, Vec3 L, float[] atmo) {
        int lat = 30, lon = 60;
        RenderSystem.enableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder b = begin(true);
        for (int i = 0; i < lat; i++) {
            float p0 = (float) (-Math.PI / 2 + Math.PI * i / lat), p1 = (float) (-Math.PI / 2 + Math.PI * (i + 1) / lat);
            float v0 = 0.5f - p0 / (float) Math.PI, v1 = 0.5f - p1 / (float) Math.PI;
            for (int j = 0; j < lon; j++) {
                float u0 = j / (float) lon, u1 = (j + 1) / (float) lon;
                float l0 = u0 * TWO_PI + rot, l1 = u1 * TWO_PI + rot;
                tv(b, m, R, cx, cy, cz, p0, l0, u0, v0, L, alpha);
                tv(b, m, R, cx, cy, cz, p1, l0, u0, v1, L, alpha);
                tv(b, m, R, cx, cy, cz, p1, l1, u1, v1, L, alpha);
                tv(b, m, R, cx, cy, cz, p0, l1, u1, v0, L, alpha);
            }
        }
        BufferUploader.drawWithShader(b.buildOrThrow());

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        BufferBuilder a = begin(false);
        float RA = R * 1.04f;
        for (int i = 0; i < lat; i++) {
            float p0 = (float) (-Math.PI / 2 + Math.PI * i / lat), p1 = (float) (-Math.PI / 2 + Math.PI * (i + 1) / lat);
            for (int j = 0; j < lon; j++) {
                float l0 = j * TWO_PI / lon, l1 = (j + 1) * TWO_PI / lon;
                av(a, m, RA, cx, cy, cz, p0, l0, alpha, atmo);
                av(a, m, RA, cx, cy, cz, p1, l0, alpha, atmo);
                av(a, m, RA, cx, cy, cz, p1, l1, alpha, atmo);
                av(a, m, RA, cx, cy, cz, p0, l1, alpha, atmo);
            }
        }
        BufferUploader.drawWithShader(a.buildOrThrow());
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void tv(BufferBuilder b, Matrix4f m, float R, float cx, float cy, float cz, float phi, float lam,
                           float u, float v, Vec3 L, float alpha) {
        float nx = Mth.cos(phi) * Mth.cos(lam), ny = Mth.sin(phi), nz = Mth.cos(phi) * Mth.sin(lam);
        float light = Math.max(0.3f, (float) (nx * L.x + ny * L.y + nz * L.z) * 0.7f + 0.45f);
        b.addVertex(m, cx + nx * R, cy + ny * R, cz + nz * R).setUv(u, v).setColor(light, light, light, alpha);
    }

    private static void av(BufferBuilder b, Matrix4f m, float R, float cx, float cy, float cz, float phi, float lam,
                           float alpha, float[] c) {
        float nx = Mth.cos(phi) * Mth.cos(lam), ny = Mth.sin(phi), nz = Mth.cos(phi) * Mth.sin(lam);
        float x = cx + nx * R, y = cy + ny * R, z = cz + nz * R;
        float len = Mth.sqrt(x * x + y * y + z * z);
        float fres = 1f - Math.max(0f, (-(nx * x + ny * y + nz * z)) / len);
        float rim = (float) Math.pow(fres, 3.0);
        b.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], Mth.clamp(rim * 0.85f * alpha, 0f, 1f));
    }
}
