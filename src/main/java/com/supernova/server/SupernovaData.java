package com.supernova.server;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Guarda las zonas de los bunkers trazados con /supernova bunker. */
public class SupernovaData extends SavedData {
    private final List<int[]> bunkers = new ArrayList<>();

    public static SupernovaData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SupernovaData::new, SupernovaData::load, null), "supernova_bunkers");
    }

    public static SupernovaData load(CompoundTag tag, HolderLookup.Provider registries) {
        SupernovaData d = new SupernovaData();
        ListTag list = tag.getList("b", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) d.bunkers.add(list.getIntArray(i));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (int[] a : bunkers) list.add(new IntArrayTag(a));
        tag.put("b", list);
        return tag;
    }

    public void add(int x1, int y1, int z1, int x2, int y2, int z2) {
        bunkers.add(new int[]{x1, y1, z1, x2, y2, z2});
        setDirty();
    }

    public boolean contains(Vec3 p) {
        for (int[] b : bunkers)
            if (p.x >= b[0] && p.x <= b[3] && p.y >= b[1] && p.y <= b[4] && p.z >= b[2] && p.z <= b[5]) return true;
        return false;
    }
}
