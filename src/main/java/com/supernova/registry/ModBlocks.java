package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.block.ShipBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    public static final Block LAUNCH_PAD = reg("launch_pad", new Block(Properties.of().mapColor(MapColor.METAL)
            .strength(4f, 12f).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final Block SHIP = reg("ship", new ShipBlock(Properties.of().mapColor(MapColor.METAL)
            .strength(4f, 12f).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));
    public static final Block GLEBA_STONE = reg("gleba_stone", new Block(Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(1.8f, 6f).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final Block GLEBA_SOIL = reg("gleba_soil", new Block(Properties.of().mapColor(MapColor.COLOR_BROWN)
            .strength(0.6f).sound(SoundType.GRAVEL)));
    public static final Block GLEBA_MOSS = reg("gleba_moss", new Block(Properties.of().mapColor(MapColor.COLOR_RED)
            .strength(0.6f).sound(SoundType.MOSS)));
    public static final Block GLEBA_SAND = reg("gleba_sand", new Block(Properties.of().mapColor(MapColor.COLOR_PINK)
            .strength(0.5f).sound(SoundType.SAND)));
    public static final Block GLEBA_CRYSTAL = reg("gleba_crystal", new Block(Properties.of().mapColor(MapColor.COLOR_CYAN)
            .strength(1.5f).sound(SoundType.AMETHYST).lightLevel(s -> 12).requiresCorrectToolForDrops()));
    public static final Block GLEBA_STALK = reg("gleba_stalk", new RotatedPillarBlock(Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(1.2f).sound(SoundType.STEM)));
    public static final Block GLEBA_CAP = reg("gleba_cap", new Block(Properties.of().mapColor(MapColor.COLOR_PINK)
            .strength(0.8f).sound(SoundType.SHROOMLIGHT).lightLevel(s -> 9)));

    public static final Block GLEBA_ORE = reg("gleba_ore", new Block(Properties.of().mapColor(MapColor.COLOR_PURPLE)
            .strength(3.0f, 6f).sound(SoundType.DEEPSLATE).lightLevel(s -> 7).requiresCorrectToolForDrops()));
    public static final Block GLEBA_FLESH = reg("gleba_flesh", new Block(Properties.of().mapColor(MapColor.COLOR_RED)
            .strength(0.9f).sound(SoundType.HONEY_BLOCK)));

    private static <T extends Block> T reg(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, SupernovaMod.id(name), block);
    }

    public static void init() { }
}
