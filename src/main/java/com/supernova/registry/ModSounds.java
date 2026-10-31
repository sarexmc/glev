package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent EXPLOSION = reg("supernova_explosion");
    public static final SoundEvent RUMBLE = reg("rumble");
    public static final SoundEvent SHIP_LAUNCH = reg("ship_launch");
    public static final SoundEvent SHIP_WARP = reg("ship_warp");
    public static final SoundEvent BEEP = reg("beep");
    public static final SoundEvent RINGING = reg("ringing");
    public static final SoundEvent ENGINE_CHARGE = reg("engine_charge");
    public static final SoundEvent IGNITION = reg("ignition");
    public static final SoundEvent ENGINE_LOOP = reg("engine_loop");
    public static final SoundEvent ATMOS_WIND = reg("atmos_wind");
    public static final SoundEvent REENTRY_ROAR = reg("reentry_roar");
    public static final SoundEvent WARP_WHOOSH = reg("warp_whoosh");
    public static final SoundEvent LANDING_THUD = reg("landing_thud");
    public static final SoundEvent ALARM = reg("alarm");
    public static final SoundEvent SPACE_HUM = reg("space_hum");
    public static final SoundEvent SHOCKWAVE = reg("shockwave");
    public static final SoundEvent WIND_GUST = reg("wind_gust");

    private static SoundEvent reg(String name) {
        ResourceLocation id = SupernovaMod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() { }
}
