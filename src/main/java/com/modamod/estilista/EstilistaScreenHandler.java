package com.modamod.estilista;

import com.modamod.estilado.EstiladoBlockEntity;
import com.modamod.estilado.EstiladoScreenHandler;
import com.modamod.screen.ModamodScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Pantalla de la Estilista (2026-10-05, etapa 3: "no quiero perder el tamaño del visualizador" + "que sea más como las
 * otras guis con un gran slot para input y output y un apply en el medio"): a la izquierda la vista 3D de la Mesa de
 * estilado —su editor interno, con la prenda de muestra— y a la derecha el panel de la máquina: entrada, Aplicar y
 * salida; Fijar y Borrar el diseño; los contadores de hilo y cuero; y el almacén de plantillas, retazos y objetos.
 */
public class EstilistaScreenHandler extends EstiladoScreenHandler {

    /** Esquina del panel de la máquina, relativa a la ventana de la Mesa (queda afuera, a la derecha). */
    public static final int X_PANEL = 390, Y_PANEL = 18, ANCHO_PANEL = 130, ALTO_PANEL = 218;
    /** Marco grande de la entrada y la salida (26 px) con el slot de 16 px centrado adentro. */
    public static final int GRANDE = 26;
    public static final int X_MARCO_ENTRADA = X_PANEL + 8, X_MARCO_SALIDA = X_PANEL + ANCHO_PANEL - 8 - GRANDE, Y_MARCO = Y_PANEL + 20;
    public static final int X_ALMACEN = X_PANEL + 11, Y_ALMACEN = Y_PANEL + 160, COLUMNAS_ALMACEN = 6;
    /** Los contadores (se clickean con un ítem en el cursor para cargarlos). */
    public static final int X_CONTADOR = X_PANEL + 8, ANCHO_CONTADOR = ANCHO_PANEL - 16, ALTO_CONTADOR = 18,
            Y_HILO = Y_PANEL + 108, Y_CUERO = Y_PANEL + 127;

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
        super(ModamodScreenHandlers.ESTILISTA, syncId, inv, host.editor());
        this.host = host;
        host.alAbrirGui();
        int dy = (GRANDE - 16) / 2;
        addSlot(new Slot(host, EstilistaBlockEntity.SLOT_PRENDA, X_MARCO_ENTRADA + dy, Y_MARCO + dy) {
            @Override public boolean canInsert(ItemStack stack) { return host.isValid(EstilistaBlockEntity.SLOT_PRENDA, stack); }
            @Override public int getMaxItemCount() { return 1; }
        });
        addSlot(new Slot(host, EstilistaBlockEntity.SLOT_SALIDA, X_MARCO_SALIDA + dy, Y_MARCO + dy) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int i = 0; i < EstilistaBlockEntity.ALMACEN; i++) {
            int slot = EstilistaBlockEntity.SLOT_ALMACEN + i;
            addSlot(new Slot(host, slot, X_ALMACEN + (i % COLUMNAS_ALMACEN) * 18, Y_ALMACEN + (i / COLUMNAS_ALMACEN) * 18) {
                @Override public boolean canInsert(ItemStack stack) { return host.isValid(slot, stack); }
            });
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == EstilistaBlockEntity.BTN_CARGAR_HILO || id == EstilistaBlockEntity.BTN_CARGAR_CUERO) {
            // Soltar el ítem del cursor sobre el contador: hilo en el de hilo, cuero en el de cuero.
            ItemStack cursor = getCursorStack();
            boolean hilo = id == EstilistaBlockEntity.BTN_CARGAR_HILO;
            if (!cursor.isOf(hilo ? net.minecraft.item.Items.STRING : net.minecraft.item.Items.LEATHER)) return false;
            return host.absorber(cursor) > 0;
        }
        if (id == EstilistaBlockEntity.BTN_LINEA || (id >= EstilistaBlockEntity.BTN_APLICAR && id <= EstilistaBlockEntity.BTN_BORRAR)) return host.onButtonClick(player, id);
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
        if (index < EstiladoBlockEntity.SLOT_BIBLIOTECA || index == EstiladoBlockEntity.SLOT_MATERIAL) {
            // Del editor: al almacén (si es algo que se guarda) y si no al inventario.
            if (!insertItem(stack, INV_FIN + 2, slots.size(), false) && !insertItem(stack, BASE, INV_FIN, true)) return ItemStack.EMPTY;
        } else if (index < BASE || index >= INV_FIN) {
            // De la máquina: molde, retazo y muestra al editor; el resto al inventario.
            if (index >= INV_FIN + 2 && (insertItem(stack, 0, EstiladoBlockEntity.SLOT_BIBLIOTECA, false)
                    || insertItem(stack, EstiladoBlockEntity.SLOT_MATERIAL, EstiladoBlockEntity.SLOT_MATERIAL + 1, false))) {
                // ok
            } else if (!insertItem(stack, BASE, INV_FIN, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Del inventario: hilo y cuero a los contadores; después editor, entrada y almacén.
            if (host.absorber(stack) > 0 && stack.isEmpty()) {
                s.setStack(ItemStack.EMPTY);
                return copia;
            }
            if (!insertItem(stack, 0, EstiladoBlockEntity.SLOT_BIBLIOTECA, false)
                    && !insertItem(stack, EstiladoBlockEntity.SLOT_MATERIAL, EstiladoBlockEntity.SLOT_MATERIAL + 1, false)
                    && !insertItem(stack, INV_FIN, INV_FIN + 1, false)
                    && !insertItem(stack, INV_FIN + 2, slots.size(), false)) {
                return stack.getCount() != copia.getCount() ? copia : ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copia;
    }
}
