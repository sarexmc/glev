package com.supernova.client;

import com.supernova.Timeline;
import com.supernova.entity.ShipEntity;
import com.supernova.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Calcula, para el fotograma actual, la intensidad de cada efecto visual. Lo leen el cielo, el overlay y los eventos. */
public final class SceneFx {
    public static float gray, white, flashOrange, red, tint, dim, dark, beam, bloom, letterbox, shake, fovMul, shockBand, wind, shockRing;
    public static float domeRed, domeBlack, domeGleba, nebula;
    public static boolean starOn, vivid, twoRings;
    public static float starR, starWhite, starBreath, ring1, ring2, ringT, ringR;
    public static float belowAlpha, belowGap, smallAlpha;
    public static boolean belowGleba;
    public static float tunnel, tunnelFadeOut, speed, entry;
    // --- parametros del shader de vuelo (StarshipPost)
    public static boolean shipFlight;
    public static float spaceVis, cloudVis, cloudExit, originVis, destVis, warp, exhaust, fireEng, fireRe, impulse, arrival;
    public static float planetBelow, destDist;
    public static double cloudMinY, cloudMaxY;

    private SceneFx() {}

    public static float ss(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    public static float ease(float x) {
        float t = 1f - Mth.clamp(x, 0f, 1f);
        return 1f - t * t * t;
    }

    public static boolean anySky() {
        return domeRed > 0 || domeBlack > 0 || domeGleba > 0 || belowAlpha > 0 || smallAlpha > 0 || starOn || ring1 > 0 || shockRing > 0;
    }

    private static void reset() {
        gray = white = flashOrange = red = tint = dim = dark = beam = bloom = letterbox = shake = shockBand = wind = shockRing = 0;
        fovMul = 1f;
        domeRed = domeBlack = domeGleba = nebula = 0;
        starOn = vivid = twoRings = false;
        starR = starWhite = starBreath = ring1 = ring2 = ringT = ringR = 0;
        belowAlpha = belowGap = smallAlpha = 0;
        belowGleba = false;
        tunnel = tunnelFadeOut = speed = entry = 0;
        shipFlight = false;
        spaceVis = cloudVis = cloudExit = originVis = destVis = warp = exhaust = fireEng = fireRe = impulse = arrival = 0;
        planetBelow = destDist = 0;
        cloudMinY = cloudMaxY = 0;
    }

    public static void update(float pt) {
        reset();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean gleba = mc.level.dimension().equals(ModDimensions.GLEBA);
        boolean overworld = mc.level.dimension() == Level.OVERWORLD;
        boolean sheltered = !mc.level.canSeeSky(mc.player.blockPosition().above());
        int sc = ClientSceneState.scene;
        float t = ClientSceneState.time(pt);

        if (overworld && sc == 1) part1(t, sheltered);
        else if (overworld && sc == 2) part2(t, sheltered);
        if (gleba) {
            float a2 = mc.level.getGameTime() + pt;
            domeGleba = 1f;
            nebula = 1f;
            starOn = true;
            vivid = true;
            starR = 26f;
            starBreath = 0.85f + 0.15f * Mth.sin(a2 * 0.05f);
            starWhite = 0f;
        }
        ship(mc, gleba);
        fovMul += 0.35f * speed;
    }

    private static void part1(float t, boolean sheltered) {
        float blast = Timeline.P1_BLAST, m60 = Timeline.P1_MSG60;
        if (t < blast) {
            starOn = true;
            float p = Mth.clamp((t - m60) / 1200f, 0f, 1f);
            starR = t < m60 ? Mth.lerp(t / m60, 6f, 8f) : 8f + 24f * p * p;
            starBreath = 0.92f + 0.08f * Mth.sin(t * 0.05f);
            if (t > m60) gray = 0.62f * (float) Math.pow(p, 1.3);
            bloom = ss(blast - 100, blast, t) * (sheltered ? 0f : 1f);
            starWhite = 0.7f * ss(blast - 100, blast, t);
        } else {
            float dt = t - blast;
            float end = Timeline.P1_END - blast;
            float fadeOut = 1f - ss(end - 300, end, dt);
            float w1 = Timeline.P1_WAVE - blast;
            // --- explosion cinematografica
            white = dt < 6 ? ss(0, 6, dt) : Math.max(0f, 1f - (dt - 6f) / 50f);
            flashOrange = 0.5f * ss(0, 20, dt) * (1f - ss(20, 140, dt));
            letterbox = ss(0, 25, dt) * (1f - ss(170, 230, dt));
            fovMul = 1f + 0.25f * (1f - ss(0, 160, dt));
            shockRing = ss(0, 140, dt) * (dt < 140 ? 1f : 0f);
            ring1 = ss(0, 60, dt) * fadeOut;
            ringT = dt;
            ringR = 6f + 52f * ease(dt / 160f);
            beam = ss(60, 140, dt) * fadeOut * (sheltered ? 0.15f : 1f);
            // --- el mundo se va oscureciendo despues de la explosion
            dim = 0.74f * ss(10, 1700, dt) * fadeOut;
            if (dt < 100) shake = 1.2f * (1f - dt / 100f);
            // --- onda expansiva: banda de luz que se acerca, golpe, viento
            shockBand = ss(120, w1, dt) * (dt < w1 ? 1f : 0f);
            if (dt >= w1 && dt < w1 + 40) flashOrange = Math.max(flashOrange, 0.75f * (1f - (dt - w1) / 40f));
            wind = ss(w1 - 10, w1 + 20, dt) * (1f - ss(w1 + 60, w1 + 170, dt));
            if (dt >= w1 && dt < w1 + 130) shake = Math.max(shake, 1.8f * (1f - (dt - w1) / 130f));
            if (sheltered) dark = 0.8f * ss(120, 220, dt) * fadeOut;
        }
    }

    private static void part2(float t, boolean sheltered) {
        float blast = Timeline.P2_BLAST;
        vivid = true;
        domeRed = ss(0, 100, t);
        if (t < blast) {
            red = 0.30f * ss(0, 100, t);
            starOn = true;
            starR = Mth.lerp(ss(0, blast, t), 16f, 28f) * (1f + 0.08f * Mth.sin(t * 0.075f));
            starBreath = 0.8f + 0.2f * Mth.sin(t * 0.075f + 0.8f);
            starWhite = ss(blast - 300, blast - 20, t);
            bloom = ss(blast - 100, blast, t) * (sheltered ? 0f : 1f);
        } else {
            float dt = t - blast;
            white = dt < 5 ? ss(0, 5, dt) * 0.8f : 0.8f * Math.max(0f, 1f - (dt - 5f) / 60f);
            flashOrange = 0.35f * ss(0, 20, dt) * (1f - ss(20, 120, dt));
            letterbox = ss(0, 25, dt) * (1f - ss(150, 210, dt));
            fovMul = 1f + 0.2f * (1f - ss(0, 140, dt));
            tint = 0.30f * ss(0, 40, dt);
            dim = 0.22f * ss(10, 80, dt);
            ring1 = ss(0, 50, dt);
            ring2 = ss(30, 90, dt);
            twoRings = true;
            ringT = dt;
            ringR = 8f + 44f * ease(dt / 140f);
            shockRing = ss(0, 120, dt) * (dt < 120 ? 1f : 0f);
            beam = ss(40, 120, dt);
            if (dt < 100) shake = 1.5f * (1f - dt / 100f);
        }
    }

    private static void ship(Minecraft mc, boolean gleba) {
        Entity v = mc.player.getVehicle();
        if (!(v instanceof ShipEntity ship)) return;
        ShipEntity.Stage st = ship.stage();
        int ticks = ship.stageTicksClient();
        int m = ship.meters();
        switch (st) {
            case CHARGING -> shake = Math.max(shake, 0.10f * ship.charge() / 100f);
            case COUNTDOWN -> shake = Math.max(shake, 0.25f + 0.4f * ticks / 60f);
            case LIFTOFF, ATMOSPHERE -> shake = Math.max(shake, 0.35f);
            case EXIT -> shake = Math.max(shake, 0.2f);
            case TIME_JUMP -> shake = Math.max(shake, 0.9f);
            default -> { }
        }
        belowGleba = gleba;
        boolean arriving = st.ordinal() >= ShipEntity.Stage.STABILIZE.ordinal();
        if (!arriving) {
            domeBlack = Math.max(domeBlack, Mth.clamp((m - 60) / 400f, 0f, 1f));
            belowAlpha = ss(580, 620, m);
            belowGap = 12f + Math.min(30f, Math.max(0, m - 600) * 0.08f);
            smallAlpha = ss(800, 860, m);
            speed = ss(900, 1000, m);
        } else {
            float alt = (float) (ship.getY() - ship.groundY());
            domeBlack = Mth.clamp((alt - 120f) / 300f, 0f, 1f);
            domeGleba = gleba ? 1f - domeBlack : 0f;
            belowAlpha = ss(200, 450, alt);
            belowGap = Mth.lerp(Mth.clamp((alt - 100f) / 900f, 0f, 1f), 8f, 40f);
            smallAlpha = ss(300, 450, alt);
            entry = 0.4f * Mth.sin((float) Math.PI * Mth.clamp((520f - alt) / 270f, 0f, 1f));
            if (st == ShipEntity.Stage.APPROACH) speed = 0.5f * ss(0, 40, ticks);
        }
        if (st == ShipEntity.Stage.TIME_JUMP) {
            tunnel = ss(0, 25, ticks);
            speed = 1f;
        }
        if (st == ShipEntity.Stage.STABILIZE) tunnelFadeOut = 1f - ss(0, 40, ticks);

        // ---- parametros para el shader de vuelo (planetas, estrellas, nubes, fuego, salto)
        shipFlight = true;
        float eng = switch (st) {
            case COUNTDOWN -> ticks > 20 ? (ticks - 20) / 40f * 0.6f : 0f;
            case LIFTOFF, ATMOSPHERE, EXIT, TIME_JUMP -> 1f;
            case STABILIZE -> 0.3f;
            case APPROACH, LANDING -> 0.7f;
            default -> 0f;
        };
        fireEng = eng;
        if (!arriving) {
            planetBelow = m;
            double base = ship.getY() - m;
            cloudMinY = base + 420;
            cloudMaxY = base + 520;
            spaceVis = ss(400, 540, m);
            cloudVis = ss(330, 420, m) * (1f - ss(520, 600, m));
            cloudExit = ss(450, 560, m);
            originVis = ss(540, 640, m);
            destVis = ss(800, 860, m);
            destDist = Mth.lerp(ss(800, 1000, m), 520000f, 120000f);
            warp = st == ShipEntity.Stage.TIME_JUMP ? 1f : ss(900, 1000, m);
            exhaust = (st.ordinal() >= ShipEntity.Stage.LIFTOFF.ordinal()) ? 0.8f : 0f;
            if (st == ShipEntity.Stage.TIME_JUMP) {
                spaceVis = 1f; originVis = 1f; destVis = 1f;
                impulse = ticks < 110 ? ticks / 110f : 0.86f;
            }
        } else {
            float alt = (float) (ship.getY() - ship.groundY());
            planetBelow = Math.max(0f, alt);
            double base = ship.groundY();
            cloudMinY = base + 300;
            cloudMaxY = base + 400;
            spaceVis = ss(120, 500, alt);
            cloudVis = ss(560, 470, alt) * (1f - ss(330, 260, alt));
            cloudExit = 0.5f;
            originVis = ss(150, 450, alt);
            destVis = ss(250, 450, alt);
            destDist = Mth.lerp(1f - ss(300, 1000, alt), 200000f, 600000f);
            warp = st == ShipEntity.Stage.APPROACH ? 0.4f * ss(0, 40, ticks) : 0f;
            exhaust = 0.5f;
            fireRe = (st == ShipEntity.Stage.APPROACH || st == ShipEntity.Stage.LANDING) ? ss(620, 440, alt) * ss(20, 120, alt) : 0f;
            if (st == ShipEntity.Stage.STABILIZE) arrival = 1f - Mth.clamp(ticks / 70f, 0f, 1f);
        }
    }
}
