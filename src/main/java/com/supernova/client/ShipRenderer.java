package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.supernova.SupernovaMod;
import com.supernova.entity.ShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Dibuja la nave en vuelo (modelo Bedrock) con la cabina animada y giros en el espacio. */
public class ShipRenderer extends EntityRenderer<ShipEntity> {
    public ShipRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public boolean shouldRender(ShipEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(ShipEntity ship, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ShipEntity.Stage st = ship.stage();
        float t = ship.stageTicksClient() + pt;
        float rotY = 0, rotX = 0;
        switch (st) {
            case EXIT -> rotY = 90f * SceneFx.ss(850, 950, ship.meters());
            case TIME_JUMP -> { rotY = 90f; rotX = 55f * SceneFx.ss(0, 40, t); }
            case STABILIZE -> { rotY = 90f; rotX = 55f * (1f - SceneFx.ss(0, 60, t)); }
            case APPROACH -> rotY = 90f * (1f - SceneFx.ss(0, 50, t));
            default -> { }
        }
        ps.pushPose();
        float shake = st == ShipEntity.Stage.COUNTDOWN ? 0.03f * Math.min(1f, t / 60f) : (st == ShipEntity.Stage.LIFTOFF ? 0.025f : 0f);
        if (shake > 0) ps.translate((Math.random() - 0.5) * shake, 0, (Math.random() - 0.5) * shake);
        float cy = 3.3f;
        ps.translate(0, cy, 0);
        ps.mulPose(Axis.YP.rotationDegrees(rotY));
        ps.mulPose(Axis.XP.rotationDegrees(rotX));
        ps.translate(0, -cy, 0);
        ShipModelRenderer.render(ps, buf, light, OverlayTexture.NO_OVERLAY, ship.doorProgress(pt));
        ps.popPose();
        super.render(ship, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return BedrockModel.TEXTURE;
    }
}
