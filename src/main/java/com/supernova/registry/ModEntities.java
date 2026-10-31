package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.entity.ShipEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityType<ShipEntity> SHIP = Registry.register(BuiltInRegistries.ENTITY_TYPE, SupernovaMod.id("ship"),
            EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(6.5f, 7.0f).clientTrackingRange(16).updateInterval(1).fireImmune().build("ship"));

    public static void init() { }
}
