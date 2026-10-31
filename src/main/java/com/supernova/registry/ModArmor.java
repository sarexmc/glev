package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/** Material de la armadura de Gleba: netherite mejorada con lingotes de Gleba (negra, con cuernos). */
public class ModArmor {
    public static final Holder<ArmorMaterial> GLEBA = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, SupernovaMod.id("gleba"),
            new ArmorMaterial(Util.make(new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 4);
                m.put(ArmorItem.Type.LEGGINGS, 7);
                m.put(ArmorItem.Type.CHESTPLATE, 9);
                m.put(ArmorItem.Type.HELMET, 4);
                m.put(ArmorItem.Type.BODY, 12);
            }), 18, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ModItems.GLEBA_INGOT),
                    List.of(new ArmorMaterial.Layer(SupernovaMod.id("gleba"))), 4.5f, 0.2f));

    public static void init() { }
}
