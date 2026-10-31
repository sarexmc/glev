package com.supernova.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.supernova.SupernovaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Lector/renderizador de modelos Bedrock (.geo.json) sin dependencias (reemplaza a Veil).
 * Convenciones de Blockbench: rotaciones X e Y invertidas, orden Z-Y-X, UV de caja o por cara, mirror.
 */
public final class BedrockModel {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation GEO = SupernovaMod.id("models/bedrock/starship.geo.json");
    public static final ResourceLocation TEXTURE = SupernovaMod.id("textures/entity/starship.png");
    private static BedrockModel instance;
    private static boolean failed;

    static final class Cube {
        float ox, oy, oz, sx, sy, sz;
        float[] pivot;
        float rx, ry, rz;
        boolean mirror;
        float[] boxUv;
        Map<String, float[]> faces;
    }

    static final class Bone {
        String name, parentName;
        float px, py, pz, rx, ry, rz;
        final List<Cube> cubes = new ArrayList<>();
        final List<Bone> children = new ArrayList<>();
    }

    private final float texW, texH;
    private final List<Bone> roots = new ArrayList<>();
    private float offX, offY, offZ;

    private BedrockModel(float tw, float th) {
        this.texW = tw;
        this.texH = th;
    }

    public static BedrockModel get() {
        if (instance == null && !failed) {
            try {
                instance = load();
            } catch (Throwable t) {
                failed = true;
                LOGGER.error("[Supernova] No se pudo cargar el modelo de la nave (se usa una caja simple)", t);
            }
        }
        return instance;
    }

    public static void reset() {
        instance = null;
        failed = false;
    }

    // ------------------------------------------------------------------ carga
    private static float[] arr(JsonObject o, String key, int n) {
        float[] r = new float[n];
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < n && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        }
        return r;
    }

    private static BedrockModel load() throws Exception {
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResourceOrThrow(GEO).open();
             Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject desc = geo.getAsJsonObject("description");
            BedrockModel m = new BedrockModel(desc.get("texture_width").getAsFloat(), desc.get("texture_height").getAsFloat());
            Map<String, Bone> map = new LinkedHashMap<>();
            for (JsonElement be : geo.getAsJsonArray("bones")) {
                JsonObject bo = be.getAsJsonObject();
                Bone b = new Bone();
                b.name = bo.get("name").getAsString();
                b.parentName = bo.has("parent") ? bo.get("parent").getAsString() : null;
                float[] pv = arr(bo, "pivot", 3), rt = arr(bo, "rotation", 3);
                b.px = pv[0]; b.py = pv[1]; b.pz = pv[2];
                b.rx = rt[0]; b.ry = rt[1]; b.rz = rt[2];
                if (bo.has("cubes")) for (JsonElement ce : bo.getAsJsonArray("cubes")) b.cubes.add(parseCube(ce.getAsJsonObject()));
                map.put(b.name, b);
            }
            for (Bone b : map.values()) {
                Bone p = b.parentName != null ? map.get(b.parentName) : null;
                if (p != null) p.children.add(b); else m.roots.add(b);
            }
            m.computeOffset();
            return m;
        }
    }

    private static Cube parseCube(JsonObject co) {
        Cube c = new Cube();
        float[] o = arr(co, "origin", 3), s = arr(co, "size", 3), rt = arr(co, "rotation", 3);
        c.ox = o[0]; c.oy = o[1]; c.oz = o[2];
        c.sx = s[0]; c.sy = s[1]; c.sz = s[2];
        c.rx = rt[0]; c.ry = rt[1]; c.rz = rt[2];
        if (co.has("pivot")) c.pivot = arr(co, "pivot", 3);
        c.mirror = co.has("mirror") && co.get("mirror").getAsBoolean();
        if (co.has("uv")) {
            JsonElement uv = co.get("uv");
            if (uv.isJsonArray()) {
                JsonArray a = uv.getAsJsonArray();
                c.boxUv = new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat()};
            } else {
                c.faces = new HashMap<>();
                for (Map.Entry<String, JsonElement> e : uv.getAsJsonObject().entrySet()) {
                    JsonObject f = e.getValue().getAsJsonObject();
                    float[] u = arr(f, "uv", 2), us = arr(f, "uv_size", 2);
                    c.faces.put(e.getKey(), new float[]{u[0], u[1], us[0], us[1]});
                }
            }
        }
        return c;
    }

    // ------------------------------------------------------------------ transformaciones
    private static Matrix4f boneMatrix(Bone b, Matrix4f parent, float doorRad) {
        float ex = b.name.equals("move_door") ? doorRad : 0f;
        return new Matrix4f(parent).translate(b.px, b.py, b.pz)
                .rotateZ((float) Math.toRadians(b.rz)).rotateY((float) Math.toRadians(-b.ry)).rotateX((float) Math.toRadians(-b.rx) + ex)
                .translate(-b.px, -b.py, -b.pz);
    }

    private static Matrix4f cubeMatrix(Cube c, Matrix4f bm) {
        if (c.rx == 0 && c.ry == 0 && c.rz == 0) return bm;
        float[] p = c.pivot != null ? c.pivot : new float[3];
        return new Matrix4f(bm).translate(p[0], p[1], p[2])
                .rotateZ((float) Math.toRadians(c.rz)).rotateY((float) Math.toRadians(-c.ry)).rotateX((float) Math.toRadians(-c.rx))
                .translate(-p[0], -p[1], -p[2]);
    }

    private void computeOffset() {
        float[] mm = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Bone b : roots) bounds(b, new Matrix4f(), mm);
        offX = -(mm[0] + mm[3]) / 2f;
        offY = -mm[1];
        offZ = -(mm[2] + mm[5]) / 2f;
    }

    private void bounds(Bone b, Matrix4f parent, float[] mm) {
        Matrix4f bm = boneMatrix(b, parent, 0f);
        for (Cube c : b.cubes) {
            Matrix4f cm = cubeMatrix(c, bm);
            for (int i = 0; i < 8; i++) {
                Vector3f v = cm.transformPosition((i & 1) == 0 ? c.ox : c.ox + c.sx, (i & 2) == 0 ? c.oy : c.oy + c.sy, (i & 4) == 0 ? c.oz : c.oz + c.sz, new Vector3f());
                float x = -v.x / 16f, y = v.y / 16f, z = -v.z / 16f;
                mm[0] = Math.min(mm[0], x); mm[1] = Math.min(mm[1], y); mm[2] = Math.min(mm[2], z);
                mm[3] = Math.max(mm[3], x); mm[4] = Math.max(mm[4], y); mm[5] = Math.max(mm[5], z);
            }
        }
        for (Bone ch : b.children) bounds(ch, bm, mm);
    }

    // ------------------------------------------------------------------ dibujo
    public void render(PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, float doorRad) {
        for (Bone b : roots) renderBone(b, new Matrix4f(), pose, vc, light, overlay, doorRad);
    }

    private void renderBone(Bone b, Matrix4f parent, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay, float doorRad) {
        Matrix4f bm = boneMatrix(b, parent, doorRad);
        for (Cube c : b.cubes) emitCube(c, cubeMatrix(c, bm), pose, vc, light, overlay);
        for (Bone ch : b.children) renderBone(ch, bm, pose, vc, light, overlay, doorRad);
    }

    private static final String[] DIRS = {"north", "south", "east", "west", "up", "down"};

    private float[] region(Cube c, String d) {
        if (c.faces != null) return c.faces.get(d);
        if (c.boxUv == null) return null;
        float u = c.boxUv[0], v = c.boxUv[1], sx = c.sx, sy = c.sy, sz = c.sz;
        String key = d;
        if (c.mirror) key = d.equals("east") ? "west" : d.equals("west") ? "east" : d;
        float[] r = switch (key) {
            case "east" -> new float[]{u, v + sz, sz, sy};
            case "north" -> new float[]{u + sz, v + sz, sx, sy};
            case "west" -> new float[]{u + sz + sx, v + sz, sz, sy};
            case "south" -> new float[]{u + 2 * sz + sx, v + sz, sx, sy};
            case "up" -> new float[]{u + sz, v, sx, sz};
            default -> new float[]{u + sz + sx, v + sz, sx, -sz};
        };
        if (c.mirror) { r[0] = r[0] + r[2]; r[2] = -r[2]; }
        return r;
    }

    private void emitCube(Cube c, Matrix4f m, PoseStack.Pose pose, VertexConsumer vc, int light, int overlay) {
        float x0 = c.ox, y0 = c.oy, z0 = c.oz, x1 = x0 + c.sx, y1 = y0 + c.sy, z1 = z0 + c.sz;
        for (String d : DIRS) {
            float[] reg = region(c, d);
            if (reg == null) continue;
            float tx, ty, tz, rx = 0, ry = 0, rz = 0, dx = 0, dy = 0, dz = 0, nx = 0, ny = 0, nz = 0;
            switch (d) {
                case "north" -> { tx = x1; ty = y1; tz = z0; rx = -c.sx; dy = -c.sy; nz = -1; }
                case "south" -> { tx = x0; ty = y1; tz = z1; rx = c.sx; dy = -c.sy; nz = 1; }
                case "east" -> { tx = x1; ty = y1; tz = z1; rz = -c.sz; dy = -c.sy; nx = 1; }
                case "west" -> { tx = x0; ty = y1; tz = z0; rz = c.sz; dy = -c.sy; nx = -1; }
                case "up" -> { tx = x0; ty = y1; tz = z0; rx = c.sx; dz = c.sz; ny = 1; }
                default -> { tx = x0; ty = y0; tz = z1; rx = c.sx; dz = -c.sz; ny = -1; }
            }
            if ((rx == 0 && ry == 0 && rz == 0) || (dx == 0 && dy == 0 && dz == 0)) continue;
            float[][] corner = {{tx, ty, tz}, {tx + rx, ty + ry, tz + rz}, {tx + rx + dx, ty + ry + dy, tz + rz + dz}, {tx + dx, ty + dy, tz + dz}};
            Vector3f[] p = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                Vector3f v = m.transformPosition(corner[i][0], corner[i][1], corner[i][2], new Vector3f());
                p[i] = new Vector3f(-v.x / 16f + offX, v.y / 16f + offY, -v.z / 16f + offZ);
            }
            Vector3f n = m.transformDirection(nx, ny, nz, new Vector3f());
            n.set(-n.x, n.y, -n.z).normalize();
            float u0 = reg[0] / texW, v0 = reg[1] / texH, u1 = (reg[0] + reg[2]) / texW, v1 = (reg[1] + reg[3]) / texH;
            float[][] uv = {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}};
            Vector3f e1 = new Vector3f(p[1]).sub(p[0]), e2 = new Vector3f(p[2]).sub(p[1]);
            int[] order = e1.cross(e2, new Vector3f()).dot(n) < 0 ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};
            for (int i : order)
                vc.addVertex(pose, p[i].x, p[i].y, p[i].z).setColor(255, 255, 255, 255).setUv(uv[i][0], uv[i][1])
                        .setOverlay(overlay).setLight(light).setNormal(pose, n.x, n.y, n.z);
        }
    }
}
