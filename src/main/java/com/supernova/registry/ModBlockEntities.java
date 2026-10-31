package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.block.ShipBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<ShipBlockEntity> SHIP = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            SupernovaMod.id("ship"), BlockEntityType.Builder.of(ShipBlockEntity::new, ModBlocks.SHIP).build(null));

    public static void init() { }
}
