package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.menu.ShipMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final MenuType<ShipMenu> SHIP = Registry.register(BuiltInRegistries.MENU, SupernovaMod.id("ship"),
            new MenuType<>(ShipMenu::new, FeatureFlags.VANILLA_SET));

    public static void init() { }
}
