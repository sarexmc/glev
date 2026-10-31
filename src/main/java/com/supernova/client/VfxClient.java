package com.supernova.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.supernova.SupernovaMod;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Sistema de VFX PROPIO (no usa particulas de Minecraft): sprites con texturas propias, mezcla aditiva,
 * humo, llamas, destellos, anillos de choque. Se dibuja despues de la geometria translucida.
 */
public final class VfxClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation GLOW = SupernovaMod.id("textures/vfx/glow.png");
    public static final ResourceLocation SMOKE = SupernovaMod.id("textures/vfx/smoke.png");
    public static final ResourceLocation FLAME = SupernovaMod.id("textures/vfx/flame.png");
    public static final ResourceLocation RING = SupernovaMod.id("textures/vfx/ring.png");
    public static final ResourceLocation SHOCK = SupernovaMod.id("textures/vfx/shock.png");
    public static final ResourceLocation SPARK = SupernovaMod.id("textures/vfx/spark.png");
    private static final ResourceLocation[] ORDER = {SMOKE, FLAME, GLOW, SPARK, RING, SHOCK};
    private static final int MAX = 1800;
    private static int failures = 0;

    static final class P {
        double x, y, z, vx, vy, vz;
        float s0, s1, rot, rotV, r, g, b, a, drag = 1f, gravity;
        int age, life;
        ResourceLocation tex;
        boolean add, flat;
    }

    private static final List<P> LIST = new ArrayList<>();

    private VfxClient() {}

    private static P add(ResourceLocation tex, boolean additive, double x, double y, double z, double vx, double vy, double vz,
                         float s0, float s1, int life, float r, float g, float b, float a) {
        if (LIST.size() >= MAX) return null;
        P p = new P();
        p.tex = tex; p.add = additive;
        p.x = x; p.y = y; p.z = z; p.vx = vx; p.vy = vy; p.vz = vz;
        p.s0 = s0; p.s1 = s1; p.life = Math.max(2, life);
        p.r = r; p.g = g; p.b = b; p.a = a;
        p.rot = (float) (Math.random() * Math.PI * 2);
        LIST.add(p);
        return p;
    }

    public static void smoke(double x, double y, double z, double vx, double vy, double vz, float s0, float s1, int life, float r, float g, float b, float a) {
        P p = add(SMOKE, false, x, y, z, vx, vy, vz, s0, s1, life, r, g, b, a);
        if (p != null) { p.rotV = (float) (Math.random() - 0.5) * 0.08f; p.drag = 0.96f; }
    }

    public static void flame(double x, double y, double z, double vx, double vy, double vz, float s0, float s1, int life, float r, float g, float b, float a) {
        P p = add(FLAME, true, x, y, z, vx, vy, vz, s0, s1, life, r, g, b, a);
        if (p != null) { p.rotV = (float) (Math.random() - 0.5) * 0.15f; p.drag = 0.97f; }
    }

    public static void glow(double x, double y, double z, double vx, double vy, double vz, float s0, float s1, int life, float r, float g, float b, float a) {
        add(GLOW, true, x, y, z, vx, vy, vz, s0, s1, life, r, g, b, a);
    }

    public static void spark(double x, double y, double z, double vx, double vy, double vz, float s0, float s1, int life, float r, float g, float b, float a) {
        P p = add(SPARK, true, x, y, z, vx, vy, vz, s0, s1, life, r, g, b, a);
        if (p != null) { p.gravity = 0.02f; p.drag = 0.98f; }
    }

    public static void ring(double x, double y, double z, double vy, float s0, float s1, int life, float r, float g, float b, float a, boolean shock) {
        P p = add(shock ? SHOCK : RING, true, x, y, z, 0, vy, 0, s0, s1, life, r, g, b, a);
        if (p != null) { p.flat = true; p.rotV = (float) (Math.random() - 0.5) * 0.04f; }
    }

    public static void tick() {
        Iterator<P> it = LIST.iterator();
        while (it.hasNext()) {
            P p = it.next();
            if (++p.age >= p.life) { it.remove(); continue; }
            p.x += p.vx; p.y += p.vy; p.z += p.vz;
            p.vx *= p.drag; p.vy *= p.drag; p.vz *= p.drag;
            p.vy -= p.gravity;
            p.rot += p.rotV;
        }
    }

    public static void clear() {
        LIST.clear();
    }

    public static void render(WorldRenderContext ctx) {
        if (LIST.isEmpty() || failures > 5) return;
        try {
            draw(ctx);
        } catch (Throwable t) {
            failures++;
            LOGGER.error("[Supernova] Error dibujando VFX (se desactiva tras 5 fallos)", t);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private static void draw(WorldRenderContext ctx) {
        Camera cam = ctx.camera();
        Vec3 cp = cam.getPosition();
        Vector3f left = cam.getLeftVector(), up = cam.getUpVector();
        Matrix4f m = new Matrix4f().rotation(cam.rotation().conjugate(new Quaternionf()));
        RenderSystem.setProjectionMatrix(ctx.projectionMatrix(), VertexSorting.DISTANCE_TO_ORIGIN);
        float pt = ClientClock.partial();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        for (ResourceLocation tex : ORDER) {
            BufferBuilder b = null;
            boolean additive = false;
            for (P p : LIST) {
                if (p.tex != tex) continue;
                if (b == null) {
                    additive = p.add;
                    RenderSystem.setShaderTexture(0, tex);
                    if (additive) RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    else RenderSystem.defaultBlendFunc();
                    b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                }
                float f = (p.age + pt) / p.life;
                float fade = Math.min(1f, f / 0.12f) * (1f - f);
                float size = Mth.lerp(f, p.s0, p.s1) * 0.5f;
                float al = Mth.clamp(p.a * fade, 0f, 1f);
                float x = (float) (p.x + p.vx * pt - cp.x), y = (float) (p.y + p.vy * pt - cp.y), z = (float) (p.z + p.vz * pt - cp.z);
                float cr = Mth.cos(p.rot + p.rotV * pt), sr = Mth.sin(p.rot + p.rotV * pt);
                float ax, ay, az, bx, by, bz;
                if (p.flat) {
                    ax = cr; ay = 0; az = sr; bx = -sr; by = 0; bz = cr;
                } else {
                    float rx = -left.x(), ry = -left.y(), rz = -left.z();
                    ax = rx * cr + up.x() * sr; ay = ry * cr + up.y() * sr; az = rz * cr + up.z() * sr;
                    bx = -rx * sr + up.x() * cr; by = -ry * sr + up.y() * cr; bz = -rz * sr + up.z() * cr;
                }
                ax *= size; ay *= size; az *= size; bx *= size; by *= size; bz *= size;
                b.addVertex(m, x - ax - bx, y - ay - by, z - az - bz).setUv(0, 1).setColor(p.r, p.g, p.b, al);
                b.addVertex(m, x + ax - bx, y + ay - by, z + az - bz).setUv(1, 1).setColor(p.r, p.g, p.b, al);
                b.addVertex(m, x + ax + bx, y + ay + by, z + az + bz).setUv(1, 0).setColor(p.r, p.g, p.b, al);
                b.addVertex(m, x - ax + bx, y - ay + by, z - az + bz).setUv(0, 0).setColor(p.r, p.g, p.b, al);
            }
            if (b != null) BufferUploader.drawWithShader(b.buildOrThrow());
        }
    }
}
