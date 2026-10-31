package com.supernova.entity;

import com.supernova.ClientHooks;
import com.supernova.block.ShipBlockEntity;
import com.supernova.registry.*;
import com.supernova.world.GlebaDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * La nave. Ida (Tierra -> Gleba) y vuelta (Gleba -> Tierra) con el mismo trayecto:
 * CHARGING (mantener CTRL hasta 100%) -> COUNTDOWN (3-2-1) -> LIFTOFF -> ATMOSPHERE -> EXIT
 * -> TIME_JUMP (carga el terreno del destino y cambia de dimension) -> STABILIZE -> APPROACH -> LANDING.
 * Metros: salto temporal a los 1000 m, llegada a los 1200 m.
 */
public class ShipEntity extends Entity {
    public enum Stage { CHARGING, COUNTDOWN, LIFTOFF, ATMOSPHERE, EXIT, TIME_JUMP, STABILIZE, APPROACH, LANDING }

    public static final float SCALE = 1.0f;
    private static final EntityDataAccessor<Integer> D_STAGE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_METERS = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_FUEL = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_SPEED = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_CHARGE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_LOAD = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_GROUND = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> D_FLAGS = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);

    public static final double ARRIVAL_Y = 1000.0;
    public static final int JUMP_METERS = 1000;
    public static final int END_METERS = 1200;

    private final Set<UUID> crew = new HashSet<>();
    private final Map<UUID, Integer> lastHeld = new HashMap<>();
    private BlockPos homePos = BlockPos.ZERO;
    private double launchY;
    private double groundY = 70;
    private double vy;
    private float charge;
    private int stageTicks;
    private int startFuel;
    private int destX, destZ;
    private boolean returning;
    private boolean chargeSound;
    private boolean fallbackHome;

    // solo cliente
    private int clientStageTicks;
    private int lastStage = -1;
    private float door, doorPrev;
    private int lerpSteps;
    private double lx, ly, lz;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    // ------------------------------------------------------------------ API
    public void setup(BlockPos home, double launchY, double groundY, int fuel, Stage stage, Collection<ServerPlayer> players) {
        this.homePos = home;
        this.launchY = launchY;
        this.groundY = groundY;
        this.startFuel = fuel;
        this.entityData.set(D_FUEL, fuel);
        this.entityData.set(D_GROUND, (int) groundY);
        this.entityData.set(D_STAGE, stage.ordinal());
        this.stageTicks = 0;
        crew.clear();
        for (ServerPlayer p : players) crew.add(p.getUUID());
    }

    /** returning = true cuando el viaje va de Gleba a la Tierra. */
    public void setRoute(boolean returning, int destX, int destZ) {
        this.returning = returning;
        this.destX = destX;
        this.destZ = destZ;
        this.entityData.set(D_FLAGS, returning ? 1 : 0);
    }

    public Stage stage() { return Stage.values()[Mth.clamp(entityData.get(D_STAGE), 0, Stage.values().length - 1)]; }
    public int meters() { return entityData.get(D_METERS); }
    public int fuel() { return entityData.get(D_FUEL); }
    public int charge() { return entityData.get(D_CHARGE); }
    public int loadProgress() { return entityData.get(D_LOAD); }
    public float speedMs() { return entityData.get(D_SPEED) / 10f; }
    public int stageTicksClient() { return clientStageTicks; }
    public double groundY() { return entityData.get(D_GROUND); }
    public float doorProgress(float pt) { return Mth.lerp(pt, doorPrev, door); }
    public boolean isReturning() { return (entityData.get(D_FLAGS) & 1) != 0; }

    public void markHeld(ServerPlayer p) {
        lastHeld.put(p.getUUID(), tickCount);
    }

    private void setStage(Stage s) {
        entityData.set(D_STAGE, s.ordinal());
        stageTicks = 0;
        announce(s);
    }

    // ------------------------------------------------------------------ boilerplate
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(D_STAGE, 0);
        builder.define(D_METERS, 0);
        builder.define(D_FUEL, 0);
        builder.define(D_SPEED, 0);
        builder.define(D_CHARGE, 0);
        builder.define(D_LOAD, 0);
        builder.define(D_GROUND, 70);
        builder.define(D_FLAGS, 0);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override public boolean isPickable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean hurt(DamageSource src, float amount) { return false; }
    @Override public boolean isNoGravity() { return true; }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        if (!hasPassenger(passenger)) return;
        int i = getPassengers().indexOf(passenger);
        double dx = 0, dz = 0;
        if (i > 0) {
            double a = (i - 1) * (Math.PI * 2 / 3.0);
            dx = Math.cos(a) * 0.45;
            dz = Math.sin(a) * 0.45;
        }
        callback.accept(passenger, getX() + dx, getY() + 3.0, getZ() + dz);
    }

    /** Interpolacion suave en el cliente (dos firmas: segun la version una de ellas sobreescribe a Entity). */
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lx = x;
        this.ly = y;
        this.lz = z;
        this.lerpSteps = Math.max(steps, 2);
    }

    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpTo(x, y, z, yRot, xRot, steps);
    }

    // ------------------------------------------------------------------ utilidades de servidor
    private List<ServerPlayer> ridersNow() {
        List<ServerPlayer> list = new ArrayList<>();
        for (Entity e : getPassengers()) if (e instanceof ServerPlayer sp) list.add(sp);
        return list;
    }

    private void hideRiders() {
        for (ServerPlayer p : ridersNow())
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, false));
    }

    private void snd(SoundEvent ev, float vol) {
        ((ServerLevel) level()).playSound(null, getX(), getY(), getZ(), ev, SoundSource.NEUTRAL, vol, 1f);
    }

    private void announce(Stage s) {
        String dest = returning ? "la Tierra" : "Gleba";
        String msg = switch (s) {
            case COUNTDOWN -> "Motores al 100 %. Secuencia de despegue iniciada";
            case LIFTOFF -> "Despegue";
            case ATMOSPHERE -> "Transito atmosferico: presion de cabina estable";
            case EXIT -> "Saliendo de la atmosfera. Alerta de cabina";
            case TIME_JUMP -> "Preparando salto temporal · cargando el terreno de " + dest;
            case STABILIZE -> "Salto completado · estabilizando sistemas";
            case APPROACH -> "Aproximacion a " + dest + " · prepara la reentrada";
            case LANDING -> "Iniciando aterrizaje";
            default -> null;
        };
        if (msg == null || level().isClientSide) return;
        for (ServerPlayer p : ridersNow()) p.displayClientMessage(Component.literal(msg), true);
    }

    private List<ServerPlayer> crewPlayers() {
        MinecraftServer server = ((ServerLevel) level()).getServer();
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : crew) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) list.add(p);
        }
        return list;
    }

    private void keepCrewAboard() {
        for (ServerPlayer p : crewPlayers())
            if (p.level() == level() && p.getVehicle() != this) p.startRiding(this, true);
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) clientTick(); else serverTick();
    }

    private void serverTick() {
        stageTicks++;
        ServerLevel sl = (ServerLevel) level();
        Stage stage = stage();
        hideRiders();

        if (stage == Stage.CHARGING) {
            chargingTick(sl);
            return;
        }
        keepCrewAboard();

        double target = 0, ease = 0.05;
        int m = meters();
        switch (stage) {
            case COUNTDOWN -> {
                if (stageTicks == 1 || stageTicks == 21 || stageTicks == 41)
                    sl.playSound(null, blockPosition(), ModSounds.BEEP, SoundSource.NEUTRAL, 2f, stageTicks == 41 ? 1.4f : 1f);
                if (stageTicks == 1) snd(ModSounds.SHIP_LAUNCH, 6f);
                if (stageTicks >= 60) {
                    burnPad(sl);
                    snd(ModSounds.IGNITION, 7f);
                    setStage(Stage.LIFTOFF);
                }
            }
            case LIFTOFF -> {
                target = 0.7; ease = 0.025;
                smash(sl);
                if (stageTicks % 80 == 1) snd(ModSounds.ENGINE_LOOP, 5f);
                if (m >= 60) setStage(Stage.ATMOSPHERE);
            }
            case ATMOSPHERE -> {
                target = 1.0;
                smash(sl);
                if (stageTicks % 80 == 1) snd(ModSounds.ENGINE_LOOP, 5f);
                if (stageTicks % 120 == 1) snd(ModSounds.ATMOS_WIND, 4f);
                if (m >= 500) {
                    snd(ModSounds.ALARM, 3f);
                    setStage(Stage.EXIT);
                }
            }
            case EXIT -> {
                target = 1.1;
                smash(sl);
                if (stageTicks % 80 == 1) snd(ModSounds.ENGINE_LOOP, 4f);
                if (m >= 800 && stageTicks % 140 == 1) snd(ModSounds.SPACE_HUM, 3f);
                if (m >= JUMP_METERS) setStage(Stage.TIME_JUMP);
            }
            case TIME_JUMP -> {
                target = 0.3;
                if (stageTicks == 1) {
                    snd(ModSounds.SHIP_WARP, 5f);
                    snd(ModSounds.WARP_WHOOSH, 5f);
                    startLoading(sl);
                }
                if (fallbackHome) {
                    if (stageTicks >= 100) {
                        returnHome();
                        return;
                    }
                } else {
                    int prog = updateLoad(sl);
                    if (stageTicks >= 100 && (prog >= 100 || stageTicks >= 700)) {
                        jump();
                        return;
                    }
                }
            }
            case STABILIZE -> {
                target = -0.5;
                if (stageTicks == 1) snd(ModSounds.SPACE_HUM, 4f);
                if (stageTicks >= 160) setStage(Stage.APPROACH);
            }
            case APPROACH -> {
                target = -1.5;
                if (stageTicks == 60) snd(ModSounds.REENTRY_ROAR, 6f);
                if (stageTicks == 1) snd(ModSounds.ALARM, 3f);
                if (stageTicks % 100 == 40) snd(ModSounds.ENGINE_LOOP, 4f);
                if (getY() <= 400) setStage(Stage.LANDING);
            }
            case LANDING -> {
                target = -Mth.clamp((getY() - groundY) * 0.04, 0.1, 1.2);
                if (stageTicks % 80 == 1) snd(ModSounds.ENGINE_LOOP, 5f);
                if (getY() <= groundY + 1.5) {
                    land();
                    return;
                }
            }
            default -> { }
        }

        vy += (target - vy) * ease;
        if (stage != Stage.COUNTDOWN) setPos(getX(), getY() + vy, getZ());
        setDeltaMovement(0, vy, 0);
        int meters = computeMeters(stage);
        entityData.set(D_METERS, meters);
        // combustible funcional: se gasta 50 % durante el ascenso
        double used = stage.ordinal() >= Stage.STABILIZE.ordinal() ? 1.0 : Mth.clamp(meters / (double) JUMP_METERS, 0, 1);
        if (stage.ordinal() >= Stage.LIFTOFF.ordinal())
            entityData.set(D_FUEL, Math.max(0, (int) Math.round(startFuel - ShipBlockEntity.TAKEOFF_FUEL * used)));
        entityData.set(D_SPEED, Math.round((float) Math.abs(vy) * 20f * 10f));
    }

    private int computeMeters(Stage stage) {
        if (stage.ordinal() >= Stage.STABILIZE.ordinal()) {
            double f = Mth.clamp((ARRIVAL_Y - getY()) / (ARRIVAL_Y - groundY), 0, 1);
            return (int) (JUMP_METERS + (END_METERS - JUMP_METERS) * f);
        }
        return (int) Math.max(0, getY() - launchY);
    }

    /** Fase de encendido: hay que mantener CTRL hasta llegar a 100 %. */
    private void chargingTick(ServerLevel sl) {
        List<ServerPlayer> riders = ridersNow();
        if (riders.isEmpty() && stageTicks > 40) {
            cancelCharge(sl, "Despegue cancelado: no hay tripulacion a bordo.");
            return;
        }
        boolean held = false;
        for (ServerPlayer p : riders) {
            Integer t = lastHeld.get(p.getUUID());
            if (t != null && tickCount - t <= 3) held = true;
        }
        if (held) {
            if (!chargeSound) {
                chargeSound = true;
                snd(ModSounds.ENGINE_CHARGE, 5f);
            }
            charge = Math.min(100f, charge + 1f);
        } else {
            charge = Math.max(0f, charge - 2f);
            if (charge <= 0f) chargeSound = false;
        }
        entityData.set(D_CHARGE, (int) charge);
        if (charge >= 100f) {
            startFuel = fuel();
            crew.clear();
            for (ServerPlayer p : riders) crew.add(p.getUUID());
            setStage(Stage.COUNTDOWN);
        } else if (!held && charge <= 0f && stageTicks > 200) {
            cancelCharge(sl, "Despegue cancelado.");
        }
    }

    private void cancelCharge(ServerLevel sl, String msg) {
        for (ServerPlayer p : ridersNow()) {
            p.stopRiding();
            p.displayClientMessage(Component.literal(msg), true);
        }
        sl.setBlock(homePos, ModBlocks.SHIP.defaultBlockState(), 3);
        if (sl.getBlockEntity(homePos) instanceof ShipBlockEntity be) {
            be.fuel = fuel();
            be.setChanged();
        }
        discard();
    }

    /** La nave destruye todo a su paso al subir por la atmosfera. */
    private void smash(ServerLevel sl) {
        int r = 4;
        int y0 = (int) Math.floor(getY() + 0.5);
        int y1 = (int) Math.floor(getY() + 8.0 + Math.max(0, vy) * 2);
        if (y0 >= sl.getMaxBuildHeight()) return;
        y1 = Math.min(y1, sl.getMaxBuildHeight() - 1);
        int cx = Mth.floor(getX()), cz = Mth.floor(getZ());
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r + 1) continue;
                    p.set(cx + dx, y, cz + dz);
                    BlockState st = sl.getBlockState(p);
                    if (st.isAir() || st.getDestroySpeed(sl, p) < 0) continue;
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
    }

    private void burnPad(ServerLevel sl) {
        AABB box = new AABB(getX() - 5, getY() - 2, getZ() - 5, getX() + 5, getY() + 3, getZ() + 5);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> !crew.contains(e.getUUID()))) {
            e.setRemainingFireTicks(120);
            e.hurt(sl.damageSources().inFire(), 6f);
        }
    }

    // ------------------------------------------------------------------ salto (con pantalla de carga de terreno)
    private ServerLevel destLevel(MinecraftServer server) {
        return returning ? server.overworld() : GlebaDimension.get(server);
    }

    private void startLoading(ServerLevel sl) {
        ServerLevel dest = destLevel(sl.getServer());
        if (dest == null) {
            fallbackHome = true;
            for (ServerPlayer p : crewPlayers())
                p.sendSystemMessage(Component.literal("No se pudo preparar el destino (" + GlebaDimension.lastError + "). Volvemos a la base."));
            return;
        }
        int cx = destX >> 4, cz = destZ >> 4;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) dest.setChunkForced(cx + dx, cz + dz, true);
    }

    private int updateLoad(ServerLevel sl) {
        ServerLevel dest = returning ? sl.getServer().overworld() : sl.getServer().getLevel(ModDimensions.GLEBA);
        if (dest == null) return 0;
        int cx = destX >> 4, cz = destZ >> 4, n = 0;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) if (dest.getChunkSource().hasChunk(cx + dx, cz + dz)) n++;
        int pct = n * 100 / 25;
        entityData.set(D_LOAD, pct);
        return pct;
    }

    private void jump() {
        ServerLevel old = (ServerLevel) level();
        ServerLevel dest = destLevel(old.getServer());
        List<ServerPlayer> players = crewPlayers();
        if (dest == null) {
            fallbackHome = true;
            returnHome();
            return;
        }
        for (ServerPlayer p : players) p.stopRiding();
        ShipEntity next = new ShipEntity(ModEntities.SHIP, dest);
        next.setPos(destX + 0.5, ARRIVAL_Y, destZ + 0.5);
        next.setup(homePos, launchY, groundY, Math.max(0, startFuel - ShipBlockEntity.TAKEOFF_FUEL), Stage.STABILIZE, players);
        next.startFuel = startFuel;
        next.setRoute(returning, destX, destZ);
        next.vy = -0.3;
        for (ServerPlayer p : players) p.teleportTo(dest, destX + 0.5, ARRIVAL_Y, destZ + 0.5, p.getYRot(), 0f);
        dest.addFreshEntity(next);
        for (ServerPlayer p : players) p.startRiding(next, true);
        next.announce(Stage.STABILIZE);
        discard();
    }

    /** Si el destino no se pudo crear, la nave baja de vuelta a su base (nadie cae al vacio). */
    private void returnHome() {
        groundY = homePos.getY() - 1.0;
        entityData.set(D_GROUND, (int) groundY);
        setPos(homePos.getX() + 0.5, getY(), homePos.getZ() + 0.5);
        setStage(Stage.LANDING);
    }

    private void placeShip(ServerLevel sl, int cx, int cz, int gy, int fuel, BlockPos returnTo) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                sl.setBlock(new BlockPos(cx + dx, gy, cz + dz), ModBlocks.LAUNCH_PAD.defaultBlockState(), 3);
                for (int dy = 1; dy <= 8; dy++)
                    sl.setBlock(new BlockPos(cx + dx, gy + dy, cz + dz), Blocks.AIR.defaultBlockState(), 3);
            }
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++)
                if (Math.abs(dx) > 1 || Math.abs(dz) > 1)
                    for (int dy = 1; dy <= 8; dy++) sl.setBlock(new BlockPos(cx + dx, gy + dy, cz + dz), Blocks.AIR.defaultBlockState(), 3);
        BlockPos shipPos = new BlockPos(cx, gy + 1, cz);
        sl.setBlock(shipPos, ModBlocks.SHIP.defaultBlockState(), 3);
        if (sl.getBlockEntity(shipPos) instanceof ShipBlockEntity be) {
            be.fuel = fuel;
            be.returnPos = returnTo;
            be.setChanged();
        }
    }

    private void land() {
        ServerLevel sl = (ServerLevel) level();
        if (fallbackHome) {
            landHome(sl);
            return;
        }
        int cx = destX, cz = destZ, gy = (int) Math.round(groundY);
        placeShip(sl, cx, cz, gy, fuel(), returning ? null : homePos);
        snd(ModSounds.LANDING_THUD, 7f);
        int i = 0;
        for (ServerPlayer p : crewPlayers()) {
            p.stopRiding();
            int px = cx + 5, pz = cz - 1 + i++;
            BlockPos top = sl.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(px, 0, pz));
            p.teleportTo(sl, px + 0.5, top.getY(), pz + 0.5, p.getYRot(), 0f);
            p.sendSystemMessage(Component.literal(returning ? "Hemos regresado a la Tierra." : "Hemos aterrizado en Gleba."));
            p.displayClientMessage(Component.literal(returning ? "Bienvenidos a casa" : "Bienvenidos a Gleba"), true);
        }
        int fx = cx >> 4, fz = cz >> 4;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) sl.setChunkForced(fx + dx, fz + dz, false);
        crew.clear();
        discard();
    }

    private void landHome(ServerLevel sl) {
        sl.setBlock(homePos, ModBlocks.SHIP.defaultBlockState(), 3);
        if (sl.getBlockEntity(homePos) instanceof ShipBlockEntity be) {
            be.fuel = fuel();
            be.setChanged();
        }
        for (ServerPlayer p : crewPlayers()) {
            p.stopRiding();
            p.sendSystemMessage(Component.literal("La nave ha regresado a su base."));
        }
        crew.clear();
        discard();
    }

    // ------------------------------------------------------------------ cliente: VFX propios (ver ShipVfx)
    private void clientTick() {
        if (lerpSteps > 0) {
            double k = 1.0 / lerpSteps;
            setPos(Mth.lerp(k, getX(), lx), Mth.lerp(k, getY(), ly), Mth.lerp(k, getZ(), lz));
            lerpSteps--;
        }
        int s = entityData.get(D_STAGE);
        if (s != lastStage) {
            lastStage = s;
            clientStageTicks = 0;
        }
        clientStageTicks++;
        doorPrev = door;
        float target = stage() == Stage.CHARGING ? 1f : 0f;
        door += Math.signum(target - door) * Math.min(Math.abs(target - door), 0.05f);
        ClientHooks.SHIP_TICK.accept(this);
    }
}
