package com.supernova.block;

import com.supernova.menu.ShipMenu;
import com.supernova.registry.ModBlockEntities;
import com.supernova.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ShipBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_FUEL = 25;     // % de combustible por tanque de dilitio
    public static final int TAKEOFF_FUEL = 50;  // % minimo para despegar

    public final SimpleContainer items = new SimpleContainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(ModItems.DILITHIUM_TANK);
        }
    };
    public int fuel = 0; // 0..100
    /** Donde esta la base de la Tierra (si esta nave aterrizo en Gleba): sirve para el boton de regreso. */
    public BlockPos returnPos = null;
    /** Solo cliente: apertura de la cabina (0..1). */
    public float doorProgress = 0f;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return fuel; }
        @Override public void set(int i, int v) { fuel = v; }
        @Override public int getCount() { return 1; }
    };

    public ShipBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHIP, pos, state);
        items.addListener(c -> setChanged());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ShipBlockEntity be) {
        if (be.fuel <= 100 - TANK_FUEL && !be.items.getItem(0).isEmpty()) {
            be.items.removeItem(0, 1);
            be.fuel += TANK_FUEL;
            be.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.supernova.ship");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ShipMenu(id, inv, items, data, ContainerLevelAccess.create(level, worldPosition), worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Fuel", fuel);
        tag.putInt("Tanks", items.getItem(0).getCount());
        if (returnPos != null) tag.putIntArray("Return", new int[]{returnPos.getX(), returnPos.getY(), returnPos.getZ()});
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("Fuel");
        int n = tag.getInt("Tanks");
        int[] r = tag.getIntArray("Return");
        returnPos = r.length == 3 ? new BlockPos(r[0], r[1], r[2]) : null;
        items.setItem(0, n > 0 ? new ItemStack(ModItems.DILITHIUM_TANK, n) : ItemStack.EMPTY);
    }
}
