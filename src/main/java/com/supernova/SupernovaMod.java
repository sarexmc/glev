package com.supernova;

import com.supernova.network.ModNetwork;
import com.supernova.registry.*;
import com.supernova.server.SupernovaCommands;
import com.supernova.server.SupernovaManager;
import com.supernova.world.GlebaChunkGenerator;
import com.supernova.world.GlebaDimension;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class SupernovaMod implements ModInitializer {
    public static final String MODID = "supernova";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        ModBlocks.init();
        ModArmor.init();
        ModItems.init();
        ModSounds.init();
        ModEntities.init();
        ModBlockEntities.init();
        ModMenus.init();
        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("gleba"), GlebaChunkGenerator.CODEC);
        ModNetwork.registerCommon();

        CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> SupernovaCommands.register(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(SupernovaManager::onTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SupernovaManager.onJoin(handler.getPlayer()));
        // la dimension Gleba se crea por codigo (no toca level.dat, no hay avisos "experimentales")
        ServerLifecycleEvents.SERVER_STARTED.register(GlebaDimension::get);
    }
}
