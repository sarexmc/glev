package com.supernova.network;

import com.supernova.SupernovaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente -> servidor: boton ENCENDER MOTORES (el servidor usa el menu abierto para saber que nave es). */
public record TakeoffPayload() implements CustomPacketPayload {
    public static final Type<TakeoffPayload> TYPE = new Type<>(SupernovaMod.id("takeoff"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeoffPayload> CODEC = StreamCodec.unit(new TakeoffPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
