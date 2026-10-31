package com.supernova.network;

import com.supernova.entity.ShipEntity;
import com.supernova.menu.ShipMenu;
import com.supernova.server.ShipLauncher;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class ModNetwork {
    public static void registerCommon() {
        PayloadTypeRegistry.playS2C().register(ScenePayload.TYPE, ScenePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TakeoffPayload.TYPE, TakeoffPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HoldPayload.TYPE, HoldPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TakeoffPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            if (p.containerMenu instanceof ShipMenu menu && p.blockPosition().distSqr(menu.pos) <= 100)
                ShipLauncher.tryLaunch(p.serverLevel(), menu.pos, p);
        });
        ServerPlayNetworking.registerGlobalReceiver(HoldPayload.TYPE, (payload, ctx) -> {
            ServerPlayer p = ctx.player();
            if (p.getVehicle() instanceof ShipEntity ship) ship.markHeld(p);
        });
    }
}
