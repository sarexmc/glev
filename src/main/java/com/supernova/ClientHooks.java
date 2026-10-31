package com.supernova;

import com.supernova.entity.ShipEntity;

import java.util.function.Consumer;

/** Ganchos que el cliente rellena (asi las clases comunes no tocan clases solo-cliente). */
public final class ClientHooks {
    public static volatile Consumer<ShipEntity> SHIP_TICK = e -> { };

    private ClientHooks() {}
}
