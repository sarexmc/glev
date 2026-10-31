package com.supernova.client;

import com.supernova.entity.ShipEntity;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** VFX de la nave (humo, fuego, plasma, aros, polvo, condensacion) con sprites propios. */
public final class ShipVfx {
    private ShipVfx() {}

    public static void tick(ShipEntity e) {
        RandomSource r = e.level().getRandom();
        ShipEntity.Stage st = e.stage();
        int t = e.stageTicksClient(), m = e.meters();
        double x = e.getX(), y = e.getY(), z = e.getZ();
        double ground = e.groundY() + 1;
        boolean arriving = st.ordinal() >= ShipEntity.Stage.STABILIZE.ordinal();

        float power = switch (st) {
            case COUNTDOWN -> t > 20 ? (t - 20) / 40f * 0.6f : 0f;
            case LIFTOFF, ATMOSPHERE, EXIT -> 1f;
            case TIME_JUMP -> 1.2f;
            case STABILIZE -> 0.35f;
            case APPROACH, LANDING -> 0.8f;
            default -> 0f;
        };
        if (power > 0) plume(r, x, y + 0.35, z, power);

        // aros de energia que salen de la nave
        if ((st == ShipEntity.Stage.LIFTOFF || st == ShipEntity.Stage.ATMOSPHERE || st == ShipEntity.Stage.EXIT || st == ShipEntity.Stage.TIME_JUMP) && t % 10 == 0) {
            boolean alt = (t / 10) % 2 == 0;
            VfxClient.ring(x, y + 0.2, z, -0.4, 2.2f, 14f, 22, alt ? 0.35f : 0.7f, alt ? 0.9f : 0.45f, 1f, 0.85f, false);
        }

        // polvo y onda de choque al despegar / aterrizar
        double h = y - ground;
        double near = !arriving ? h : (st == ShipEntity.Stage.LANDING ? h : 999);
        if (near < 24 && (st == ShipEntity.Stage.COUNTDOWN || st == ShipEntity.Stage.LIFTOFF || st == ShipEntity.Stage.LANDING)) {
            float k = (float) (1 - near / 24.0);
            if (t % 2 == 0) {
                for (int i = 0; i < 7; i++) {
                    double a = r.nextDouble() * Math.PI * 2, sp = 0.3 + r.nextDouble() * 0.5;
                    VfxClient.smoke(x + Math.cos(a) * 1.6, ground + 0.3, z + Math.sin(a) * 1.6, Math.cos(a) * sp, 0.05 + r.nextDouble() * 0.08, Math.sin(a) * sp,
                            2f, 7f, 40 + r.nextInt(25), 0.72f, 0.64f, 0.54f, 0.5f * k + 0.1f);
                }
            }
            if (t % 7 == 0) VfxClient.ring(x, ground + 0.15, z, 0, 3f, 22f * (0.5f + k), 26, 1f, 0.85f, 0.65f, 0.7f * k + 0.1f, true);
        }

        // condensacion en la punta + nubes (450 a 620 m)
        if (!arriving && m >= 450 && m < 620 && (st == ShipEntity.Stage.ATMOSPHERE || st == ShipEntity.Stage.EXIT)) {
            if (t % 3 == 0) VfxClient.ring(x, y + 6.6, z, -0.05, 2f, 9f, 16, 1f, 0.9f, 0.95f, 0.8f, true);
            for (int i = 0; i < 3; i++)
                VfxClient.smoke(x + (r.nextDouble() - .5) * 18, y + (r.nextDouble() - .5) * 14, z + (r.nextDouble() - .5) * 18, 0, -0.7, 0,
                        4f, 9f, 28, 1f, 1f, 1f, 0.45f);
        }
        // escombros al abrirse paso entre el terreno
        if (!arriving && (st == ShipEntity.Stage.LIFTOFF || st == ShipEntity.Stage.ATMOSPHERE) && m < 330 && t % 2 == 0) {
            for (int i = 0; i < 3; i++)
                VfxClient.smoke(x + (r.nextDouble() - .5) * 5, y + 5 + r.nextDouble() * 2, z + (r.nextDouble() - .5) * 5, (r.nextDouble() - .5) * 0.2, 0.05, (r.nextDouble() - .5) * 0.2,
                        1.4f, 4.5f, 30, 0.5f, 0.42f, 0.34f, 0.5f);
            VfxClient.spark(x + (r.nextDouble() - .5) * 4, y + 5, z + (r.nextDouble() - .5) * 4, (r.nextDouble() - .5) * 0.4, 0.2, (r.nextDouble() - .5) * 0.4, 0.5f, 0.1f, 22, 1f, 0.8f, 0.4f, 1f);
        }
        // plasma de reentrada alrededor del casco
        if (arriving && (st == ShipEntity.Stage.APPROACH || st == ShipEntity.Stage.LANDING) && h < 620) {
            float k = (float) Mth.clamp((620 - h) / 220.0, 0, 1) * (float) Mth.clamp(h / 90.0, 0.2, 1);
            for (int i = 0; i < (int) (9 * k) + 1; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                VfxClient.flame(x + Math.cos(a) * 2.6, y + r.nextDouble() * 6.4, z + Math.sin(a) * 2.6, 0, 0.55, 0, 1.8f, 0.4f, 10, 1f, 0.45f, 0.14f, 0.9f * k);
            }
            VfxClient.glow(x, y + 6.4, z, 0, 0, 0, 9f * k + 1f, 9f * k + 1f, 2, 1f, 0.5f, 0.2f, 0.6f * k);
        }
    }

    private static void plume(RandomSource r, double x, double ny, double z, float power) {
        int nf = (int) (power * 5) + (r.nextFloat() < (power * 5) % 1 ? 1 : 0);
        for (int i = 0; i < nf; i++) {
            double ox = (r.nextDouble() - .5) * 0.7, oz = (r.nextDouble() - .5) * 0.7;
            double sp = (0.6 + r.nextDouble() * 0.5) * (0.6 + 0.4 * power);
            VfxClient.flame(x + ox, ny, z + oz, ox * 0.06, -sp, oz * 0.06, 1.3f, 0.3f, 8 + r.nextInt(7), 1f, 0.62f, 0.2f, 0.9f);
        }
        VfxClient.glow(x, ny - 0.1, z, 0, -0.4, 0, 3.4f * power, 1.2f, 4, 1f, 0.85f, 0.55f, 0.85f);
        for (int i = 0; i < 2; i++) {
            double ox = (r.nextDouble() - .5) * 0.4, oz = (r.nextDouble() - .5) * 0.4;
            VfxClient.glow(x + ox, ny - 0.5, z + oz, 0, -0.9, 0, 1.0f, 0.2f, 10, 0.4f, 0.7f, 1f, 0.75f);
        }
        if (r.nextFloat() < 0.7f * power)
            VfxClient.smoke(x + (r.nextDouble() - .5) * 0.8, ny - 1.0, z + (r.nextDouble() - .5) * 0.8, (r.nextDouble() - .5) * 0.1, -0.22, (r.nextDouble() - .5) * 0.1,
                    1.4f, 4.2f, 45 + r.nextInt(30), 0.86f, 0.86f, 0.9f, 0.45f);
        for (int i = 0; i < (int) (power * 2); i++)
            VfxClient.spark(x, ny, z, (r.nextDouble() - .5) * 0.5, -0.5 - r.nextDouble() * 0.6, (r.nextDouble() - .5) * 0.5, 0.5f, 0.1f, 18 + r.nextInt(16), 1f, 0.7f, 0.3f, 1f);
    }
}
