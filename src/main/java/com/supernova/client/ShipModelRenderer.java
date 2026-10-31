package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Dibuja la nave (modelo Bedrock de dedsafio) para el bloque y para la entidad en vuelo. door = 0..1 (compuerta/cabina). */
public class ShipModelRenderer {
    public static void render(PoseStack ps, MultiBufferSource buf, int light, int overlay, float door) {
        BedrockModel m = BedrockModel.get();
        if (m == null) return;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(BedrockModel.TEXTURE));
        m.render(ps.last(), vc, light, overlay, (float) Math.toRadians(70.0 * door));
    }
}
