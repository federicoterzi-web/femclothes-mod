package com.femclothes.maniqui;

import com.femclothes.guardarropas.GuardarropasScreenHandler;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Maniquí: 16 slots (4 categorías x 4 capas) + inventario del jugador, en
 * las MISMAS posiciones que el Guardarropas — así reusa su fondo
 * ({@code guardarropas.png}) sin arte nuevo.
 */
public class ManiquiScreenHandler extends ScreenHandler {

    public static final int M_MEDIO = GuardarropasScreenHandler.M_MEDIO;
    private static final int INV_START = ManiquiBlockEntity.TAMANO;

    public final ManiquiBlockEntity be;

    /** Factory del lado del CLIENTE: busca el block entity REAL en la posición que mandó el servidor. */
    public static ManiquiScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        ManiquiBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof ManiquiBlockEntity m
                ? m
                : new ManiquiBlockEntity(pos, ManiquiMod.MANIQUI_BLOCK.getDefaultState());
        return new ManiquiScreenHandler(syncId, inv, be);
    }

    public ManiquiScreenHandler(int syncId, PlayerInventory playerInventory, ManiquiBlockEntity be) {
        super(FemclothesScreenHandlers.MANIQUI, syncId);
        this.be = be;

        // Slot anónimo con canInsert → isValid: Slot.canInsert no consulta
        // Inventory.isValid por su cuenta (trampa conocida, ver CLAUDE.md).
        for (int categoria = 0; categoria < ManiquiBlockEntity.CATEGORIAS; categoria++) {
            for (int capa = 0; capa < ManiquiBlockEntity.POR_CATEGORIA; capa++) {
                int index = categoria * ManiquiBlockEntity.POR_CATEGORIA + capa;
                addSlot(new Slot(be, index, M_MEDIO + categoria * 20, 20 + capa * 20) {
                    @Override
                    public boolean canInsert(ItemStack stack) { return be.isValid(index, stack); }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, M_MEDIO + j * 18, 180 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, M_MEDIO + i * 18, 238));
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return be.onButtonClick(player, id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return be.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        Slot clickedSlot = this.slots.get(slot);
        if (clickedSlot == null || !clickedSlot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = clickedSlot.getStack();
        ItemStack result = stack.copy();
        if (slot < INV_START) {
            if (!this.insertItem(stack, INV_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.insertItem(stack, 0, INV_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) clickedSlot.setStack(ItemStack.EMPTY);
        else clickedSlot.markDirty();
        return result;
    }
}
