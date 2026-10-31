package com.supernova.network;

import com.supernova.SupernovaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente -> servidor: "estoy manteniendo CTRL" (cada tick mientras se cargan los motores). */
public record HoldPayload() implements CustomPacketPayload {
    public static final Type<HoldPayload> TYPE = new Type<>(SupernovaMod.id("hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoldPayload> CODEC = StreamCodec.unit(new HoldPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
