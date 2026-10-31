package com.supernova.client;

import com.supernova.SupernovaMod;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/** Fuente espacial propia (bitmap en assets/supernova/font/space.json). */
public class SpaceFont {
    private static final ResourceLocation ID = SupernovaMod.id("space");

    public static Style style() {
        return Style.EMPTY.withFont(ID);
    }

    public static Component c(String s) {
        return Component.literal(s).withStyle(style());
    }
}
