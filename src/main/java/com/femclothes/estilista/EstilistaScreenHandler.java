package com.femclothes.estilista;

import com.femclothes.estilado.EstiladoBlockEntity;
import com.femclothes.estilado.EstiladoScreenHandler;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Pantalla de la Estilista (2026-10-05, etapa 3: "no quiero perder el tamaño del visualizador"): es la de la Mesa de
 * estilado —su editor interno, con la prenda de muestra— más un panel a la izquierda con la entrada y la salida de la
 * máquina, los botones del diseño y el almacén de insumos (hilo, cuero y objetos).
 */
public class EstilistaScreenHandler extends EstiladoScreenHandler {

    /** Esquina del panel de la máquina, relativa a la ventana de la Mesa (queda afuera, a la izquierda). */
    public static final int X_PANEL = -128, Y_PANEL = 18, ANCHO_PANEL = 124, ALTO_PANEL = 160;
    public static final int X_ENTRADA = X_PANEL + 10, X_SALIDA = X_PANEL + 40, Y_ENTRADA = 40;
    public static final int X_INSUMOS = X_PANEL + 8, Y_INSUMOS = 116, COLUMNAS_INSUMOS = 6;

    /** Dónde empiezan los slots de la máquina (después de los del editor y del inventario del jugador). */
    private static final int BASE = EstiladoBlockEntity.TAMANO, INV_FIN = BASE + 36;

    public final EstilistaBlockEntity host;

    public static EstilistaScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        EstilistaBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof EstilistaBlockEntity e
                ? e : new EstilistaBlockEntity(pos, EstilistaMod.ESTILISTA_BLOCK.getDefaultState());
        return new EstilistaScreenHandler(syncId, inv, be);
    }

    public EstilistaScreenHandler(int syncId, PlayerInventory inv, EstilistaBlockEntity host) {
        super(FemclothesScreenHandlers.ESTILISTA, syncId, inv, host.editor());
        this.host = host;
        host.alAbrirGui();
        addSlot(new Slot(host, EstilistaBlockEntity.SLOT_PRENDA, X_ENTRADA, Y_ENTRADA) {
            @Override public boolean canInsert(ItemStack stack) { return host.isValid(EstilistaBlockEntity.SLOT_PRENDA, stack); }
            @Override public int getMaxItemCount() { return 1; }
        });
        addSlot(new Slot(host, EstilistaBlockEntity.SLOT_SALIDA, X_SALIDA, Y_ENTRADA) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int i = 0; i < EstilistaBlockEntity.INSUMOS; i++) {
            int slot = EstilistaBlockEntity.SLOT_INSUMOS + i;
            addSlot(new Slot(host, slot, X_INSUMOS + (i % COLUMNAS_INSUMOS) * 18, Y_INSUMOS + (i / COLUMNAS_INSUMOS) * 18) {
                @Override public boolean canInsert(ItemStack stack) { return host.isValid(slot, stack); }
            });
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= EstilistaBlockEntity.BTN_APLICAR && id <= EstilistaBlockEntity.BTN_BORRAR) return host.onButtonClick(player, id);
        return super.onButtonClick(player, id);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        host.alCerrarGui();
    }

    @Override
    public boolean canUse(PlayerEntity player) { return host.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot s = slots.get(index);
        if (s == null || !s.hasStack() || !s.canTakeItems(player)) return ItemStack.EMPTY;
        ItemStack stack = s.getStack();
        ItemStack copia = stack.copy();
        if (index < BASE || index >= INV_FIN) {
            if (!insertItem(stack, BASE, INV_FIN, true)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 0, EstiladoBlockEntity.SLOT_BIBLIOTECA, false)       // molde, retazo, muestra
                && !insertItem(stack, INV_FIN, INV_FIN + 1, false)                          // entrada de la máquina
                && !insertItem(stack, INV_FIN + 2, slots.size(), false)) {                  // insumos
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copia;
    }
}
