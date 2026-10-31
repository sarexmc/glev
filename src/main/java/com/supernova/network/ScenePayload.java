package com.supernova.network;

import com.supernova.SupernovaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> cliente: escena activa (0 ninguna, 1 parte 1, 2 parte 2) y ticks transcurridos. */
public record ScenePayload(int scene, int elapsed) implements CustomPacketPayload {
    public static final Type<ScenePayload> TYPE = new Type<>(SupernovaMod.id("scene"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScenePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScenePayload::scene,
            ByteBufCodecs.VAR_INT, ScenePayload::elapsed,
            ScenePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
