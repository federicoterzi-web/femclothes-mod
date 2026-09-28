package com.femclothes.sublimadora;

import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Pantalla de escala/posición/cara/foto — a pedido (2026-09-19, "hace gui
 * con preview con escalado y flechitas para posicion y asi sacamos los
 * controles del frente", después "las imagenes deberian agregarse aqui
 * no en la estampadora"). Antes eran 3 presets fijos (Logo/Centrada/
 * Completo) elegidos con una palanca física del modelo viejo, y la foto
 * se cargaba con click derecho directo sobre el bloque — las dos cosas
 * pasan a vivir acá: 2 slots de foto (Frente/Espalda, ver
 * {@code SublimadoraBlockEntity} que ahora implementa {@code Inventory})
 * + flechitas de escala/x/y.
 *
 * <p>La remera sigue pudiéndose cargar con click derecho directo sobre el
 * bloque, pero desde 2026-09-21 ("slot de entrada y de salida, asi vemos
 * la vista previa") también tiene su propio slot de entrada
 * ({@code SublimadoraBlockEntity#SLOT_REMERA}) y uno de salida
 * ({@code SublimadoraBlockEntity#SLOT_SALIDA}) — mismo criterio que
 * Modeladora/Tinturas.
 *
 * <p>Mismas constantes de columna que {@code TinturasScreenHandler}
 * (izquierda preview, centro controles) — desde 2026-09-21 ("slots" tras
 * "tambien podemos agregarle slots a la derecha para guardar imagenes")
 * también tiene columna derecha: 9 slots de almacén de fotos, 3x3, mismo
 * tamaño que el almacén de Tinturas/Modeladora pero en grilla en vez de
 * una sola fila (acá no hay capas "Activo" que compartan la columna).
 */
public class SublimadoraScreenHandler extends ScreenHandler {

    public static final int M_MEDIO = 119;
    public static final int M_DERECHA = M_MEDIO + 162 + 20;

    private static final int SLOT_FOTO_FRENTE = 0;
    private static final int SLOT_FOTO_ESPALDA = 1;
    // Entrada/salida — a pedido (2026-09-21, "slot de entrada y de
    // salida, asi vemos la vista previa"): antes la remera solo entraba/
    // salía con click derecho directo sobre el bloque (sigue funcionando
    // igual, las dos formas conviven).
    private static final int SLOT_ENTRADA = 2;
    private static final int SLOT_SALIDA = 3;
    // Almacén de fotos, columna derecha — índices dentro de ESTA lista de
    // slots (this.slots), no confundir con SublimadoraBlockEntity.SLOT_ALMACEN_INICIO
    // (el índice de guardado real, que es distinto porque en el medio del
    // inventario del block entity van papel/tinta, que no tienen slot de
    // GUI propio).
    private static final int SLOT_ALMACEN_INICIO_UI = 4;
    private static final int INV_START = SLOT_ALMACEN_INICIO_UI + SublimadoraBlockEntity.ALMACEN_TAMANO;

    public final SublimadoraBlockEntity be;

    /** Factory del lado del CLIENTE: mismo motivo que TinturasScreenHandler#deCliente. */
    public static SublimadoraScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        SublimadoraBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof SublimadoraBlockEntity s
                ? s
                : new SublimadoraBlockEntity(pos, ModBlocks.SUBLIMADORA.getDefaultState());
        return new SublimadoraScreenHandler(syncId, inv, be);
    }

    public SublimadoraScreenHandler(int syncId, PlayerInventory playerInventory, SublimadoraBlockEntity be) {
        super(FemclothesScreenHandlers.SUBLIMADORA, syncId);
        this.be = be;

        // 2 slots de foto, uno por cara — a pedido (2026-09-19, "las
        // imagenes deberian agregarse aqui no en la estampadora"). Debajo
        // del preview, en la columna izquierda.
        addSlot(new Slot(be, SLOT_FOTO_FRENTE, 24, 172));
        addSlot(new Slot(be, SLOT_FOTO_ESPALDA, 62, 172));

        // Entrada/salida de la remera — mismo "canInsert delega en
        // isValid" que ya usa ModeladoScreenHandler.
        addSlot(new Slot(be, SublimadoraBlockEntity.SLOT_REMERA, 24, 186) {
            @Override
            public boolean canInsert(ItemStack stack) { return be.isValid(SublimadoraBlockEntity.SLOT_REMERA, stack); }
        });
        addSlot(new Slot(be, SublimadoraBlockEntity.SLOT_SALIDA, 62, 186) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Almacén de fotos, 3x3 — a pedido (2026-09-21, "slots"): guardado
        // nomás para no perder una imagen al cargar otra en Frente/Espalda.
        for (int i = 0; i < SublimadoraBlockEntity.ALMACEN_TAMANO; i++) {
            int fila = i / 3, columna = i % 3;
            addSlot(new Slot(be, SublimadoraBlockEntity.SLOT_ALMACEN_INICIO + i, M_DERECHA + columna * 18, 40 + fila * 18));
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
        return be.onButtonClick(id);
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
        } else if (SublimadoraBlock.esFoto(stack)) {
            // Frente/Espalda primero, el resto al almacén — sin pasar por
            // remera/salida en el medio (esos dos NO tienen restricción de
            // tipo en su Slot, ver comentario de SLOT_ALMACEN_INICIO_UI).
            boolean movido = this.insertItem(stack, SLOT_FOTO_FRENTE, SLOT_ENTRADA, false);
            if (!stack.isEmpty()) movido |= this.insertItem(stack, SLOT_ALMACEN_INICIO_UI, INV_START, false);
            if (!movido) return ItemStack.EMPTY;
        } else if (ModItems.esEstampable(stack)) {
            if (!this.insertItem(stack, SLOT_ENTRADA, SLOT_ENTRADA + 1, false)) return ItemStack.EMPTY;
        } else if (!this.insertItem(stack, INV_START, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) clickedSlot.setStack(ItemStack.EMPTY);
        else clickedSlot.markDirty();
        return result;
    }
}
