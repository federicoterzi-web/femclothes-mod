package com.femclothes.estilista;

import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/** Pantalla de la Estilista (provisoria): la prenda, la salida, los insumos (hilo, cuero y objetos) y el inventario. */
public class EstilistaScreenHandler extends ScreenHandler {

    public static final int X_PRENDA = 26, X_SALIDA = 134, Y_SLOTS = 16;
    public static final int X_INSUMOS = 8, Y_INSUMOS = 92;
    public static final int X_INV = 8, Y_INV = 142;

    public final EstilistaBlockEntity be;

    public static EstilistaScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        EstilistaBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof EstilistaBlockEntity e
                ? e : new EstilistaBlockEntity(pos, EstilistaMod.ESTILISTA_BLOCK.getDefaultState());
        return new EstilistaScreenHandler(syncId, inv, be);
    }

    public EstilistaScreenHandler(int syncId, PlayerInventory inv, EstilistaBlockEntity be) {
        super(FemclothesScreenHandlers.ESTILISTA, syncId);
        this.be = be;
        be.alAbrirGui();
        addSlot(new Slot(be, EstilistaBlockEntity.SLOT_PRENDA, X_PRENDA, Y_SLOTS) {
            @Override public boolean canInsert(ItemStack stack) { return be.isValid(EstilistaBlockEntity.SLOT_PRENDA, stack); }
            @Override public int getMaxItemCount() { return 1; }
        });
        addSlot(new Slot(be, EstilistaBlockEntity.SLOT_SALIDA, X_SALIDA, Y_SLOTS) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int i = 0; i < EstilistaBlockEntity.INSUMOS; i++) {
            int slot = EstilistaBlockEntity.SLOT_INSUMOS + i;
            addSlot(new Slot(be, slot, X_INSUMOS + (i % 9) * 18, Y_INSUMOS + (i / 9) * 18) {
                @Override public boolean canInsert(ItemStack stack) { return be.isValid(slot, stack); }
            });
        }
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, X_INV + j * 18, Y_INV + i * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, X_INV + i * 18, Y_INV + 58));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) { return be.onButtonClick(player, id); }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        be.alCerrarGui();
    }

    @Override
    public boolean canUse(PlayerEntity player) { return be.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot s = slots.get(index);
        if (s == null || !s.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = s.getStack();
        ItemStack copia = stack.copy();
        if (index < EstilistaBlockEntity.TAMANO) {
            if (!insertItem(stack, EstilistaBlockEntity.TAMANO, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 0, 1, false)
                && !insertItem(stack, EstilistaBlockEntity.SLOT_INSUMOS, EstilistaBlockEntity.TAMANO, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copia;
    }
}
