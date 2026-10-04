package com.femclothes.tinturas;

import com.femclothes.modelado.ModeladoBlockEntity;
import com.femclothes.modelado.ModeladoScreenHandler;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Slots reales para el almacén de patrones (9, una sola fila — "un solo
 * almacén", acá no hay categorías que lo dividan) y los Activo de patrón
 * (capas apiladas, tope {@link TinturasBlockEntity#CAPAS_MAXIMO}) — a
 * diferencia de la v1, que no tenía ningún slot. La mezcla CMYK, el lado,
 * el tamaño, la orientación y las fijadas siguen siendo botones (sin slot
 * posible: no son ítems). Aplicar a una prenda sigue sin pasar por acá —
 * eso es click derecho directo sobre el bloque (ver
 * {@link TinturasBlock#onUseWithItem}).
 *
 * <p>2026-09-27, capas por cuadradito: un slot de molde en CADA
 * cuadradito del esquema de la Modeladora (mismas coordenadas, ver
 * {@link #posCasilla}), uno por capa de color. {@link CasillaSlot#isEnabled}
 * oculta los de las otras categorías, mismo mecanismo que
 * {@code ModeladoScreenHandler.PinSlot}.
 *
 * <p>Layout de 3 columnas — mismas constantes que
 * {@code ModeladoScreenHandler}: {@code M_MEDIO}=119 (controles),
 * {@code M_DERECHA} (acá, la mezcla CMYK en vez de un almacén de cortes).
 * La columna izquierda (preview 3D) no tiene slots.
 */
public class TinturasScreenHandler extends ScreenHandler {

    public static final int M_MEDIO = 119;
    // 240 y no 162 (2026-09-27, "armemos la gui de tinturas" — ensanchada
    // para el esquema con pines de región, mismo criterio que ya usa
    // ModeladoScreenHandler.M_MEDIO_ANCHO).
    public static final int M_MEDIO_ANCHO = 240;
    public static final int M_DERECHA = M_MEDIO + M_MEDIO_ANCHO + 20;
    /** Mismo Y que {@code ModeladoScreen#ESQUEMA_Y} — el esquema arranca debajo del botón de Categoría. */
    public static final int ESQUEMA_Y = 36;
    /** Slots grandes de prenda base/resultado — mismas coordenadas relativas que {@code ModeladoScreenHandler}. */
    public static final int ENTRADA_X = 48, SALIDA_X = 176, SLOT_Y_IO = 184;

    private static final int SLOT_ALMACEN_INICIO = 0;
    private static final int SLOT_CASILLAS_INICIO = SLOT_ALMACEN_INICIO + TinturasBlockEntity.ALMACEN_TOTAL;
    /**
     * Almacén en la columna IZQUIERDA, 5x6 debajo de Guardar diseño
     * (2026-09-28, "quiero mas espacios de almacenamiento"): a la derecha
     * ya no entraba más que la fila de 9.
     */
    public static final int ALMACEN_X = 8, ALMACEN_Y = 280, ALMACEN_COLUMNAS = 5;
    private static final int SLOT_CASILLAS_TAMANO =
            TinturasBlockEntity.CASILLAS * TinturasBlockEntity.Categoria.values().length;
    // Entrada/salida — a pedido (2026-09-21, "slot de entrada y de
    // salida, asi vemos la vista previa").
    private static final int SLOT_ENTRADA = SLOT_CASILLAS_INICIO + SLOT_CASILLAS_TAMANO;
    private static final int SLOT_SALIDA = SLOT_ENTRADA + 1;
    private static final int INV_START = SLOT_SALIDA + 1;

    public final TinturasBlockEntity be;

    /**
     * Factory del lado del CLIENTE: busca el block entity REAL en la
     * posición que mandó el servidor (mismo motivo que
     * {@code ModeladoScreenHandler.deCliente} — las fijadas viajan por
     * NBT del block entity, no caben en un {@code PropertyDelegate}).
     */
    public static TinturasScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        TinturasBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof TinturasBlockEntity t
                ? t
                : new TinturasBlockEntity(pos, TinturasMod.TINTURAS_BLOCK.getDefaultState());
        return new TinturasScreenHandler(syncId, inv, be);
    }

    public TinturasScreenHandler(int syncId, PlayerInventory playerInventory, TinturasBlockEntity be) {
        super(FemclothesScreenHandlers.TINTURAS, syncId);
        this.be = be;

        // Almacén PRIMERO (mismo orden que sus índices en el block entity,
        // 0..8) — el orden de addSlot() define el índice en this.slots
        // (usado por quickMove), NO dónde se dibuja: eso lo decide x/y de
        // cada Slot, así que Activo puede vivir MÁS ARRIBA en pantalla
        // (dentro del esquema) aunque se agregue DESPUÉS acá. Columna
        // DERECHA, debajo de la vista previa de color — mismo criterio que
        // ModeladoScreenHandler (el storage vive a la derecha, no en medio).
        for (int i = 0; i < TinturasBlockEntity.ALMACEN_TOTAL; i++) {
            int indice = TinturasBlockEntity.slotAlmacen(i);
            addSlot(new Slot(be, indice, ALMACEN_X + (i % ALMACEN_COLUMNAS) * 18, ALMACEN_Y + (i / ALMACEN_COLUMNAS) * 18) {
                @Override
                public boolean canInsert(ItemStack stack) { return be.isValid(indice, stack); }
            });
        }

        // Un slot de molde por CUADRADITO del esquema, por categoría
        // (2026-09-27, capas por cuadradito). Los de las otras categorías
        // (y los lugares del esquema que no pintan nada, como Calce) quedan
        // isEnabled()==false — se ocultan y no aceptan clicks.
        for (TinturasBlockEntity.Categoria cat : TinturasBlockEntity.Categoria.values()) {
            for (int i = 0; i < TinturasBlockEntity.CASILLAS; i++) {
                int[] pos = posCasilla(cat, i);
                addSlot(new CasillaSlot(be, TinturasBlockEntity.casillaSlot(cat, i), pos[0], pos[1], cat, i));
            }
        }

        // Entrada/salida GRANDES en la columna del MEDIO (2026-09-27,
        // "copia el layout de la gui de la modeladora" — antes eran slots
        // chicos en la columna izquierda; ahora es el mismo cinturón
        // Prenda base -> Resultado de 32x32 que ya tiene la Modeladora,
        // mismo x relativo (ENTRADA_X/SALIDA_X) y mismo truco de escala
        // 1.5x en el Screen (ver TinturasScreen#esSlotGrande). "canInsert
        // delega en isValid" — misma memoria "slot_caninsert_gotcha".
        addSlot(new Slot(be, TinturasBlockEntity.SLOT_PRENDA_ENTRADA, M_MEDIO + ENTRADA_X, SLOT_Y_IO) {
            @Override
            public boolean canInsert(ItemStack stack) { return be.isValid(TinturasBlockEntity.SLOT_PRENDA_ENTRADA, stack); }

            /** De a uno: el retazo de aplique se apila a 64 y el resto se perdería (2026-10-04). */
            @Override
            public int getMaxItemCount() { return 1; }

            // Desde la GUI la prenda solo se CARGA — arranca con el botón
            // Teñir (2026-09-28, "sigue empezando a funcionar apenas pongo
            // la prenda"). El hopper/click derecho siguen arrancando solos
            // (pasan por be.setStack, no por este slot).
            @Override
            public void setStackNoCallbacks(ItemStack stack) {
                be.cargarEntradaSinArrancar(stack);
                this.markDirty();
            }
        });
        addSlot(new Slot(be, TinturasBlockEntity.SLOT_SALIDA, M_MEDIO + SALIDA_X, SLOT_Y_IO) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // y=324/382 (2026-09-27, "copia el layout de la modeladora" — con
        // Activo en el esquema y Almacén movido a la derecha, la columna
        // del medio quedó igual de angosta que la Modeladora en lo que
        // usan, ver TinturasScreen para el resto de las filas).
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, M_MEDIO + j * 18, 324 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, M_MEDIO + i * 18, 382));
        }
    }

    /**
     * Dónde va el slot del cuadradito {@code i} (origen del ítem de 16x16,
     * relativo al panel). {@code ModeladoScreenHandler.PIN_POS} ya trae la
     * Y ABSOLUTA del panel (el offset del esquema está incluido) — bug real
     * de la v3 (2026-09-27, "los slots estan mal"): se le sumaba ESQUEMA_Y
     * otra vez y todo quedaba 36px más abajo que el dibujo. Pollera no tiene
     * esquema: sus 3 van en una fila fija dentro del hueco del dibujo.
     */
    private static final int[][] POS_POLLERA = {{53, 19}, {181, 55}, {39, 98}};
    private static final int[][] POS_CAPA = {{48, 53}, {189, 96}, {175, 18}};

    public static int[] posCasilla(TinturasBlockEntity.Categoria cat, int i) {
        if (cat == TinturasBlockEntity.Categoria.POLLERA || cat == TinturasBlockEntity.Categoria.CAPA
                || cat == TinturasBlockEntity.Categoria.APLIQUE) {
            // Sobre el dibujo de esquema_tintes_<prenda>.png (2026-10-04, "adaptalos"): origen del ítem de 16x16
            // en px de GUI dentro del esquema de 240x136 (arranca en ESQUEMA_Y). Pollera: cintura, falda, ruedo.
            // Capa: exterior (izq.), forro (abajo der.), detalles (capucha, arriba der.).
            int[][] p = cat == TinturasBlockEntity.Categoria.POLLERA ? POS_POLLERA : POS_CAPA;   // el retazo usa el esquema de la capa (provisorio)
            return new int[]{M_MEDIO + p[i][0], ESQUEMA_Y + p[i][1]};
        }
        int[] p = ModeladoScreenHandler.PIN_POS[cat.ordinal()][i];
        return new int[]{M_MEDIO + p[0], p[1]};
    }

    /** Centro de la chincheta del cuadradito {@code i} (relativo al panel) — {@code PIN_BTN} de la Modeladora, o arriba a la derecha del slot en Pollera. */
    public static int[] posChincheta(TinturasBlockEntity.Categoria cat, int i) {
        if (cat == TinturasBlockEntity.Categoria.POLLERA || cat == TinturasBlockEntity.Categoria.CAPA
                || cat == TinturasBlockEntity.Categoria.APLIQUE) {
            int[] s = posCasilla(cat, i);
            return new int[]{s[0] + 17, s[1] - 1};
        }
        int[] p = ModeladoScreenHandler.PIN_BTN[cat.ordinal()][i];
        return new int[]{M_MEDIO + p[0], p[1]};
    }

    /** Un cuadradito solo existe (se ve/acepta moldes) mientras su categoría sea la que muestra la GUI y ese lugar pinte algo. */
    public static class CasillaSlot extends Slot {
        private final TinturasBlockEntity be;
        public final TinturasBlockEntity.Categoria cat;
        public final int casilla;

        CasillaSlot(TinturasBlockEntity be, int index, int x, int y, TinturasBlockEntity.Categoria cat, int casilla) {
            super(be, index, x, y);
            this.be = be;
            this.cat = cat;
            this.casilla = casilla;
        }

        @Override
        public boolean isEnabled() {
            return be.categoria() == cat && TinturasBlockEntity.regionDe(cat, casilla) != null;
        }

        // canInsert chequea isEnabled a mano: insertItem (shift-click) no
        // mira isEnabled, y sin esto llenaría cuadraditos ocultos.
        @Override
        public boolean canInsert(ItemStack stack) {
            return isEnabled() && stack.getItem() instanceof com.femclothes.item.ClothingPatternItem;
        }

        @Override
        public int getMaxItemCount() { return 1; }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == TinturasBlockEntity.BTN_TENIR) {
            // Solo el servidor arranca de verdad (gasta tinta); el cliente
            // se entera por la sincronización del block entity.
            if (player.getWorld().isClient) return true;
            net.minecraft.text.Text motivo = be.reintentarTenido();
            if (motivo != null) player.sendMessage(motivo, true);
            return motivo == null;
        }
        if (id == TinturasBlockEntity.BTN_ENVASAR || id == TinturasBlockEntity.BTN_USAR_MUESTRA) {
            if (player.getWorld().isClient) return true;
            net.minecraft.text.Text motivo = id == TinturasBlockEntity.BTN_ENVASAR ? envasar(player) : usarMuestra(player);
            if (motivo != null) player.sendMessage(motivo, true);
            return motivo == null;
        }
        return be.onButtonClick(id);
    }

    /**
     * Envasar (2026-09-30, "que la estacion de tintes genere un mezcla de
     * color por si la gente se quiere pasar colores"): gasta un frasco de
     * vidrio del inventario y da una muestra con el color que se está
     * editando. No gasta tinta: es la receta del color, no tinta.
     */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.text.Text envasar(PlayerEntity player) {
        var inv = player.getInventory();
        int frasco = -1;
        for (int i = 0; i < inv.size() && frasco < 0; i++) {
            if (inv.getStack(i).isOf(net.minecraft.item.Items.GLASS_BOTTLE)) frasco = i;
        }
        boolean gratis = player.isCreative() || com.femclothes.util.MaquinaCreativa.es(be);
        if (frasco < 0 && !gratis) {
            return net.minecraft.text.Text.translatable("femclothes.muestra.sin_frasco");
        }
        if (frasco >= 0 && !gratis) inv.getStack(frasco).decrement(1);
        ItemStack muestra = com.femclothes.item.MuestraColorItem.con(
                new ItemStack(com.femclothes.item.FemclothesItems.TINTE_MEZCLA), be.mezclaEnEdicion());
        inv.offerOrDrop(muestra);
        return null;
    }

    /** Usar muestra: la del cursor, o si no la primera del inventario; no se gasta. */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.text.Text usarMuestra(PlayerEntity player) {
        int[] mezcla = com.femclothes.item.MuestraColorItem.mezcla(getCursorStack());
        var inv = player.getInventory();
        for (int i = 0; i < inv.size() && mezcla == null; i++) {
            if (inv.getStack(i).isOf(com.femclothes.item.FemclothesItems.TINTE_MEZCLA)) {
                mezcla = com.femclothes.item.MuestraColorItem.mezcla(inv.getStack(i));
            }
        }
        if (mezcla == null) return net.minecraft.text.Text.translatable("femclothes.muestra.sin_muestra");
        be.ponerMezclaEnEdicion(mezcla);
        return null;
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
        } else if (stack.getItem() instanceof com.femclothes.item.ClothingPatternItem) {
            // Almacén primero, y si no entra ahí, los cuadraditos de la
            // categoría ACTUAL nada más (CasillaSlot#canInsert rechaza los
            // ocultos).
            int base = SLOT_CASILLAS_INICIO + be.categoria().ordinal() * TinturasBlockEntity.CASILLAS;
            boolean movio = this.insertItem(stack, SLOT_ALMACEN_INICIO, SLOT_CASILLAS_INICIO, false);
            if (!stack.isEmpty()) movio |= this.insertItem(stack, base, base + TinturasBlockEntity.CASILLAS, false);
            if (!movio) return ItemStack.EMPTY;
        } else if (TinturasBlockEntity.aceptaEntrada(stack)) {
            if (!this.insertItem(stack, SLOT_ENTRADA, SLOT_ENTRADA + 1, false)) return ItemStack.EMPTY;
        } else if (!this.insertItem(stack, INV_START, this.slots.size(), false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) clickedSlot.setStack(ItemStack.EMPTY);
        else clickedSlot.markDirty();
        return result;
    }
}
