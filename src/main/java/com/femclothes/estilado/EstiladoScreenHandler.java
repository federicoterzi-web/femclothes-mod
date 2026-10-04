package com.femclothes.estilado;

import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Mesa de estilado (2026-10-01): prenda, molde y retazo a la derecha de la
 * vista 3D, inventario abajo a la derecha. Mismas coordenadas que
 * {@code tools/generar_textura_estilado.py}.
 */
public class EstiladoScreenHandler extends ScreenHandler {

    public static final int X_DERECHA = 214;
    public static final int Y_SLOTS = 30;
    public static final int Y_INVENTARIO = 172;
    private static final int INV_START = EstiladoBlockEntity.TAMANO;

    public final EstiladoBlockEntity be;

    public static EstiladoScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        EstiladoBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof EstiladoBlockEntity e
                ? e : new EstiladoBlockEntity(pos, EstiladoMod.ESTILADO_BLOCK.getDefaultState());
        return new EstiladoScreenHandler(syncId, inv, be);
    }

    public EstiladoScreenHandler(int syncId, PlayerInventory playerInventory, EstiladoBlockEntity be) {
        super(FemclothesScreenHandlers.ESTILADO, syncId);
        this.be = be;
        // Slot anónimo con canInsert → isValid (trampa conocida, CLAUDE.md).
        for (int i = 0; i < EstiladoBlockEntity.TAMANO; i++) {
            int indice = i;
            addSlot(new Slot(be, i, X_DERECHA + i * 26, Y_SLOTS) {
                @Override
                public boolean canInsert(ItemStack stack) { return be.isValid(indice, stack); }

                /** El slot de objeto es solo de la Mesa creativa (2026-10-04). */
                @Override
                public boolean isEnabled() {
                    return indice != EstiladoBlockEntity.SLOT_OBJETO || com.femclothes.util.MaquinaCreativa.es(be);
                }
            });
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, X_DERECHA + j * 18, Y_INVENTARIO + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(playerInventory, i, X_DERECHA + i * 18, Y_INVENTARIO + 58));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == EstiladoBlockEntity.BTN_QUITAR) {
            be.quitar(player);
            return true;
        }
        return be.onButtonClick(id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return be.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        Slot clicked = this.slots.get(slot);
        if (clicked == null || !clicked.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = clicked.getStack();
        ItemStack result = stack.copy();
        if (slot < INV_START) {
            if (!this.insertItem(stack, INV_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.insertItem(stack, 0, INV_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) clicked.setStack(ItemStack.EMPTY);
        else clicked.markDirty();
        return result;
    }
}
