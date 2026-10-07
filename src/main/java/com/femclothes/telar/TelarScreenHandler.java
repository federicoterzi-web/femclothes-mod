package com.femclothes.telar;

import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Pantalla del Telar (2026-10-07): lana, hilo y la prenda tejida, más el inventario; las prendas se tildan con botones
 * ({@link TelarBlockEntity#BTN_PRENDA_BASE}) y el lote se configura debajo. Dibujada por código en {@link com.femclothes.client.TelarScreen}.
 */
public class TelarScreenHandler extends ScreenHandler {

    public static final int ANCHO = 176, ALTO = 226;
    public static final int X_LANA = 22, Y_LANA = 34, X_HILO = 22, Y_HILO = 62, X_SALIDA = 138, Y_SALIDA = 48;
    public static final int Y_INV = 146;

    public final TelarBlockEntity host;

    public static TelarScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        TelarBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof TelarBlockEntity e
                ? e : new TelarBlockEntity(pos, TelarMod.TELAR_BLOCK.getDefaultState());
        return new TelarScreenHandler(syncId, inv, be);
    }

    public TelarScreenHandler(int syncId, PlayerInventory inv, TelarBlockEntity host) {
        super(FemclothesScreenHandlers.TELAR, syncId);
        this.host = host;
        addSlot(new Slot(host, TelarBlockEntity.SLOT_LANA, X_LANA, Y_LANA) {
            @Override public boolean canInsert(ItemStack stack) { return host.isValid(TelarBlockEntity.SLOT_LANA, stack); }
        });
        addSlot(new Slot(host, TelarBlockEntity.SLOT_HILO, X_HILO, Y_HILO) {
            @Override public boolean canInsert(ItemStack stack) { return host.isValid(TelarBlockEntity.SLOT_HILO, stack); }
        });
        addSlot(new Slot(host, TelarBlockEntity.SLOT_SALIDA, X_SALIDA, Y_SALIDA) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int f = 0; f < 3; f++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, 9 + f * 9 + c, 8 + c * 18, Y_INV + f * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, Y_INV + 58));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return host.boton(id);
    }

    @Override
    public boolean canUse(PlayerEntity player) { return host.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot s = slots.get(index);
        if (s == null || !s.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = s.getStack();
        ItemStack copia = stack.copy();
        if (index < TelarBlockEntity.TAMANO) {
            if (!insertItem(stack, TelarBlockEntity.TAMANO, slots.size(), true)) return ItemStack.EMPTY;
        } else if (host.isValid(TelarBlockEntity.SLOT_LANA, stack)) {
            if (!insertItem(stack, TelarBlockEntity.SLOT_LANA, TelarBlockEntity.SLOT_LANA + 1, false)) return ItemStack.EMPTY;
        } else if (host.isValid(TelarBlockEntity.SLOT_HILO, stack)) {
            if (!insertItem(stack, TelarBlockEntity.SLOT_HILO, TelarBlockEntity.SLOT_HILO + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copia;
    }
}
