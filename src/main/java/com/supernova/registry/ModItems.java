package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ModItems {
    public static final Item DILITHIUM_TANK = reg("dilithium_tank", new Item(new Item.Properties().stacksTo(16)));
    public static final Item LAUNCH_PAD = block("launch_pad", ModBlocks.LAUNCH_PAD);
    public static final Item SHIP = block("ship", ModBlocks.SHIP);
    public static final Item GLEBA_STONE = block("gleba_stone", ModBlocks.GLEBA_STONE);
    public static final Item GLEBA_SOIL = block("gleba_soil", ModBlocks.GLEBA_SOIL);
    public static final Item GLEBA_MOSS = block("gleba_moss", ModBlocks.GLEBA_MOSS);
    public static final Item GLEBA_SAND = block("gleba_sand", ModBlocks.GLEBA_SAND);
    public static final Item GLEBA_CRYSTAL = block("gleba_crystal", ModBlocks.GLEBA_CRYSTAL);
    public static final Item GLEBA_STALK = block("gleba_stalk", ModBlocks.GLEBA_STALK);
    public static final Item GLEBA_CAP = block("gleba_cap", ModBlocks.GLEBA_CAP);

    public static final Item GLEBA_ORE = block("gleba_ore", ModBlocks.GLEBA_ORE);
    public static final Item GLEBA_FLESH = block("gleba_flesh", ModBlocks.GLEBA_FLESH);
    public static final Item GLEBA_INGOT = reg("gleba_ingot", new Item(new Item.Properties().fireResistant()));
    public static final Item GLEBA_HELMET = armor("gleba_helmet", ArmorItem.Type.HELMET);
    public static final Item GLEBA_CHESTPLATE = armor("gleba_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final Item GLEBA_LEGGINGS = armor("gleba_leggings", ArmorItem.Type.LEGGINGS);
    public static final Item GLEBA_BOOTS = armor("gleba_boots", ArmorItem.Type.BOOTS);

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SupernovaMod.id("main"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.supernova"))
                    .icon(() -> new ItemStack(DILITHIUM_TANK))
                    .displayItems((params, out) -> {
                        out.accept(DILITHIUM_TANK);
                        out.accept(LAUNCH_PAD);
                        out.accept(SHIP);
                        out.accept(GLEBA_STONE);
                        out.accept(GLEBA_SOIL);
                        out.accept(GLEBA_MOSS);
                        out.accept(GLEBA_SAND);
                        out.accept(GLEBA_CRYSTAL);
                        out.accept(GLEBA_STALK);
                        out.accept(GLEBA_CAP);
                        out.accept(GLEBA_ORE);
                        out.accept(GLEBA_FLESH);
                        out.accept(GLEBA_INGOT);
                        out.accept(GLEBA_HELMET);
                        out.accept(GLEBA_CHESTPLATE);
                        out.accept(GLEBA_LEGGINGS);
                        out.accept(GLEBA_BOOTS);
                    })
                    .build());

    private static Item reg(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, SupernovaMod.id(name), item);
    }

    private static Item armor(String name, ArmorItem.Type type) {
        return reg(name, new ArmorItem(ModArmor.GLEBA, type, new Item.Properties().durability(type.getDurability(42)).fireResistant()));
    }

    private static Item block(String name, Block block) {
        return reg(name, new BlockItem(block, new Item.Properties()));
    }

    public static void init() { }
}
