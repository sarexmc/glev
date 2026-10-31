package com.supernova.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.supernova.ClientHooks;
import com.supernova.entity.ShipEntity;
import com.supernova.network.HoldPayload;
import com.supernova.network.ScenePayload;
import com.supernova.registry.ModBlockEntities;
import com.supernova.registry.ModEntities;
import com.supernova.registry.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import org.lwjgl.glfw.GLFW;

public class SupernovaClient implements ClientModInitializer {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.SHIP, ShipScreen::new);
        EntityRendererRegistry.register(ModEntities.SHIP, ShipRenderer::new);
        BlockEntityRenderers.register(ModBlockEntities.SHIP, ShipBlockRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(ScenePayload.TYPE, (payload, ctx) -> ClientSceneState.set(payload.scene(), payload.elapsed()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientSceneState.set(0, 0);
            VfxClient.clear();
        });

        ClientHooks.SHIP_TICK = ShipVfx::tick;
        ClientTickEvents.END_CLIENT_TICK.register(SupernovaClient::tick);
        HudRenderCallback.EVENT.register((g, delta) -> FxOverlay.render(g, ClientClock.partial()));
        WorldRenderEvents.AFTER_SETUP.register(SkyRenderer::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(VfxClient::render);
        WorldRenderEvents.END.register(StarshipPost::render);
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return com.supernova.SupernovaMod.id("reload");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                BedrockModel.reset();
                StarshipPost.reset();
            }
        });

        // cuernos del casco de Gleba
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer.getModel() instanceof HumanoidModel) helper.register(new HornsLayer(renderer));
        });
    }

    private static void tick(Minecraft mc) {
        ClientClock.onTick();
        ClientSceneState.tick();
        if (mc.level == null || mc.player == null) return;
        if (!mc.isPaused()) VfxClient.tick();
        // mantener CTRL para cargar los motores
        if (mc.screen == null && mc.player.getVehicle() instanceof ShipEntity ship && ship.stage() == ShipEntity.Stage.CHARGING) {
            long win = mc.getWindow().getWindow();
            if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL))
                ClientPlayNetworking.send(new HoldPayload());
        }
        // sacudida de camara (explosiones, onda expansiva, vuelo)
        SceneFx.update(0f);
        if (SceneFx.shake > 0.02f && !mc.isPaused()) {
            float s = SceneFx.shake;
            mc.player.setYRot(mc.player.getYRot() + (float) (Math.random() - 0.5) * s * 1.1f);
            mc.player.setXRot(Math.max(-90f, Math.min(90f, mc.player.getXRot() + (float) (Math.random() - 0.5) * s * 0.9f)));
        }
    }
}
