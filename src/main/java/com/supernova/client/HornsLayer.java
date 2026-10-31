package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supernova.SupernovaMod;
import com.supernova.registry.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/** Los cuernos del casco de Gleba (se dibujan encima de la cabeza si llevas el casco puesto). */
public class HornsLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TEX = SupernovaMod.id("textures/entity/gleba_horn.png");

    public HornsLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GLEBA_HELMET)) return;
        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        for (int side = -1; side <= 1; side += 2) {
            // 4 segmentos que se curvan hacia fuera y hacia arriba (x0,x1, y0,y1, z semiancho)
            box(ps, vc, light, side, 2.4f, 4.6f, -11f, -8f, 1.3f);
            box(ps, vc, light, side, 3.4f, 5.6f, -14f, -11f, 1.1f);
            box(ps, vc, light, side, 4.6f, 6.4f, -17f, -14f, 0.9f);
            box(ps, vc, light, side, 5.8f, 7.1f, -20f, -17f, 0.65f);
        }
        ps.popPose();
    }

    private static void box(PoseStack ps, VertexConsumer vc, int light, int side, float xa, float xb, float y0, float y1, float zh) {
        float x0 = Math.min(side * xa, side * xb) / 16f, x1 = Math.max(side * xa, side * xb) / 16f;
        float ya = y0 / 16f, yb = y1 / 16f, za = -zh / 16f, zb = zh / 16f;
        float v0 = (y0 + 20f) / 12f, v1 = (y1 + 20f) / 12f;   // degradado: punta brillante (arriba) -> base oscura
        PoseStack.Pose pose = ps.last();
        quad(pose, vc, light, x0, ya, za, x1, ya, za, x1, yb, za, x0, yb, za, v0, v1, 0, 0, -1);
        quad(pose, vc, light, x1, ya, zb, x0, ya, zb, x0, yb, zb, x1, yb, zb, v0, v1, 0, 0, 1);
        quad(pose, vc, light, x0, ya, zb, x0, ya, za, x0, yb, za, x0, yb, zb, v0, v1, -1, 0, 0);
        quad(pose, vc, light, x1, ya, za, x1, ya, zb, x1, yb, zb, x1, yb, za, v0, v1, 1, 0, 0);
        quad(pose, vc, light, x0, yb, za, x1, yb, za, x1, yb, zb, x0, yb, zb, v1, v1, 0, -1, 0);
        quad(pose, vc, light, x0, ya, zb, x1, ya, zb, x1, ya, za, x0, ya, za, v0, v0, 0, 1, 0);
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc, int light,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                             float vTop, float vBot, float nx, float ny, float nz) {
        vtx(p, vc, light, ax, ay, az, 0, vTop, nx, ny, nz);
        vtx(p, vc, light, bx, by, bz, 1, vTop, nx, ny, nz);
        vtx(p, vc, light, cx, cy, cz, 1, vBot, nx, ny, nz);
        vtx(p, vc, light, dx, dy, dz, 0, vBot, nx, ny, nz);
    }

    private static void vtx(PoseStack.Pose p, VertexConsumer vc, int light, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(255, 255, 255, 255).setUv(u, Math.max(0f, Math.min(1f, v)))
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
