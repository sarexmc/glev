package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.supernova.block.ShipBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** La nave colocada sobre la base. La cabina se abre cuando hay un jugador cerca. */
public class ShipBlockRenderer implements BlockEntityRenderer<ShipBlockEntity> {
    public ShipBlockRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(ShipBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        float target = mc.player != null && mc.player.distanceToSqr(be.getBlockPos().getCenter()) < 49 ? 1f : 0f;
        be.doorProgress += (target - be.doorProgress) * 0.08f;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ShipModelRenderer.render(ps, buf, light, OverlayTexture.NO_OVERLAY, be.doorProgress);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(ShipBlockEntity be) {
        return true;
    }
}
