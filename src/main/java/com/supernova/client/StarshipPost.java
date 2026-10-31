package com.supernova.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.supernova.SupernovaMod;
import com.supernova.entity.ShipEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Efecto de espacio de la nave: tus shaders starship_flight / starship_impulse portados al sistema de
 * post-proceso de Minecraft (sin Veil). Dibuja planetas, estrellas, nubes volumetricas, fuego del motor, aros y el salto temporal.
 * Si el shader fallara, se desactiva solo y el cielo vuelve al sistema normal (SkyRenderer).
 */
public final class StarshipPost {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation POST = SupernovaMod.id("shaders/post/starship_flight.json");
    private static PostChain chain;
    private static PostPass flightPass, impulsePass;
    private static boolean failed;
    private static int lastW = -1, lastH = -1;

    private StarshipPost() {}

    /** true mientras el shader esta en uso (el SkyRenderer deja de dibujar cupula/planetas). */
    public static boolean available() {
        return !failed;
    }

    public static void reset() {
        if (chain != null) {
            try { chain.close(); } catch (Throwable ignored) { }
        }
        chain = null; flightPass = null; impulsePass = null;
        failed = false; lastW = lastH = -1;
    }

    // planetas: {land rgb, water rgb, atmosphere rgb, seed, sea level, clouds}
    private static final float[] EARTH = {0.20f, 0.46f, 0.18f, 0.03f, 0.20f, 0.52f, 0.35f, 0.62f, 1.00f, 3.7f, 0.50f, 0.55f};
    private static final float[] GLEBA = {0.78f, 0.14f, 0.22f, 0.36f, 0.05f, 0.30f, 1.00f, 0.42f, 0.52f, 11.3f, 0.34f, 0.30f};

    public static void render(WorldRenderContext ctx) {
        if (failed) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || !(mc.player.getVehicle() instanceof ShipEntity ship)) return;
            float pt = ClientClock.partial();
            SceneFx.update(pt);
            if (!SceneFx.shipFlight) return;
            boolean need = SceneFx.spaceVis > 0.002f || SceneFx.cloudVis > 0.002f || SceneFx.fireEng > 0.02f || SceneFx.fireRe > 0.02f
                    || SceneFx.impulse > 0.001f || SceneFx.arrival > 0.001f;
            if (!need) return;
            ensure(mc);
            apply(mc, ctx, ship, pt);
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(pt);
            mc.getMainRenderTarget().bindWrite(true);
        } catch (Throwable t) {
            failed = true;
            LOGGER.error("[Supernova] El shader de vuelo fallo y se desactiva (se usa el cielo normal)", t);
            try { Minecraft.getInstance().getMainRenderTarget().bindWrite(true); } catch (Throwable ignored) { }
        }
    }

    @SuppressWarnings("unchecked")
    private static void ensure(Minecraft mc) throws Exception {
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (chain == null) {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), POST);
            chain.resize(w, h);
            lastW = w; lastH = h;
            for (Field f : PostChain.class.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                List<?> l = (List<?>) f.get(chain);
                if (l != null && !l.isEmpty() && l.get(0) instanceof PostPass) {
                    flightPass = (PostPass) l.get(0);
                    impulsePass = (PostPass) l.get(1);
                }
            }
            if (flightPass == null || impulsePass == null) throw new IllegalStateException("no se encontraron los pasos del post-proceso");
        } else if (w != lastW || h != lastH) {
            chain.resize(w, h);
            lastW = w; lastH = h;
        }
    }

    private static void f(EffectInstance e, String n, float a) { e.safeGetUniform(n).set(a); }
    private static void f(EffectInstance e, String n, float a, float b) { e.safeGetUniform(n).set(a, b); }
    private static void f(EffectInstance e, String n, float a, float b, float c) { e.safeGetUniform(n).set(a, b, c); }
    private static void f(EffectInstance e, String n, float a, float b, float c, float d) { e.safeGetUniform(n).set(a, b, c, d); }

    private static void apply(Minecraft mc, WorldRenderContext ctx, ShipEntity ship, float pt) {
        EffectInstance fl = flightPass.getEffect();
        Vec3 cam = ctx.camera().getPosition();
        Quaternionf q = ctx.camera().rotation();
        fl.safeGetUniform("InvProjection").set(new Matrix4f(ctx.projectionMatrix()).invert());
        f(fl, "CameraRotation", q.x, q.y, q.z, q.w);
        f(fl, "FlightTime", (mc.level.getGameTime() + pt) / 20f);

        boolean gleba = SceneFx.belowGleba;
        float[] below = gleba ? GLEBA : EARTH, other = gleba ? EARTH : GLEBA;
        f(fl, "PlanetCenter", 0f, -(7000f + SceneFx.planetBelow + 4f), 0f);
        f(fl, "LandColor", below[0], below[1], below[2]);
        f(fl, "WaterColor", below[3], below[4], below[5]);
        f(fl, "AtmosphereColor", below[6], below[7], below[8]);
        f(fl, "PlanetSeed", below[9]);
        f(fl, "SeaLevel", below[10]);
        f(fl, "Clouds", below[11]);
        Vec3 dir = new Vec3(0.45, 0.22, -0.86).normalize();
        f(fl, "DestinationCenter", (float) (dir.x * SceneFx.destDist), (float) (dir.y * SceneFx.destDist), (float) (dir.z * SceneFx.destDist));
        f(fl, "DestinationLandColor", other[0], other[1], other[2]);
        f(fl, "DestinationWaterColor", other[3], other[4], other[5]);
        f(fl, "DestinationAtmosphereColor", other[6], other[7], other[8]);
        f(fl, "DestinationPlanetSeed", other[9]);
        f(fl, "DestinationSeaLevel", other[10]);
        f(fl, "DestinationClouds", other[11]);

        f(fl, "Visibility", SceneFx.spaceVis);
        f(fl, "OriginVisibility", SceneFx.originVis);
        f(fl, "DestinationVisibility", SceneFx.destVis);
        f(fl, "WarpAmount", SceneFx.warp);
        f(fl, "ExhaustAmount", SceneFx.exhaust);
        f(fl, "CloudVisibility", SceneFx.cloudVis);
        f(fl, "CloudExitProgress", SceneFx.cloudExit);
        f(fl, "CloudBounds", (float) SceneFx.cloudMinY, (float) SceneFx.cloudMaxY);
        f(fl, "CloudTransitionHeight", (float) ((SceneFx.cloudMinY + SceneFx.cloudMaxY) / 2.0));
        f(fl, "CloudCameraPosition", (float) cam.x, (float) cam.y, (float) cam.z);

        double ex = Mth.lerp(pt, ship.xo, ship.getX()) - cam.x, ey = Mth.lerp(pt, ship.yo, ship.getY()) + 0.35 - cam.y, ez = Mth.lerp(pt, ship.zo, ship.getZ()) - cam.z;
        f(fl, "EnginePosition", (float) ex, (float) ey, (float) ez);
        f(fl, "EngineDirection", 0f, -1f, 0f);
        boolean fire = SceneFx.fireEng > 0.02f || SceneFx.fireRe > 0.02f;
        fl.safeGetUniform("FireCount").set(fire ? 1 : 0);
        f(fl, "FireEngine0", (float) ex, (float) ey, (float) ez, SceneFx.fireEng);
        f(fl, "FireDirection0", 0f, -1f, 0f, SceneFx.fireRe);

        EffectInstance im = impulsePass.getEffect();
        f(im, "VanishingPoint", 0.5f, 0.5f);
        f(im, "Impulse", SceneFx.impulse);
        f(im, "Arrival", SceneFx.arrival);
    }
}
