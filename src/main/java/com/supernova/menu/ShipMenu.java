package com.supernova.menu;

import com.supernova.registry.ModBlocks;
import com.supernova.registry.ModItems;
import com.supernova.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class ShipMenu extends AbstractContainerMenu {
    public final BlockPos pos;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Constructor del cliente. */
    public ShipMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(1), new SimpleContainerData(1), ContainerLevelAccess.NULL, BlockPos.ZERO);
    }

    public ShipMenu(int id, Inventory inv, Container fuelSlot, ContainerData data, ContainerLevelAccess access, BlockPos pos) {
        super(ModMenus.SHIP, id);
        this.pos = pos;
        this.data = data;
        this.access = access;
        addSlot(new Slot(fuelSlot, 0, 26, 40) {
            @Override
            public boolean mayPlace(ItemStack s) {
                return s.is(ModItems.DILITHIUM_TANK);
            }
        });
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 116 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 174));
        addDataSlots(data);
    }

    public int fuel() {
        return data.get(0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack st = slot.getItem();
            copy = st.copy();
            if (index == 0) {
                if (!moveItemStackTo(st, 1, slots.size(), true)) return ItemStack.EMPTY;
            } else if (!st.is(ModItems.DILITHIUM_TANK) || !moveItemStackTo(st, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (st.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SHIP);
    }
}
