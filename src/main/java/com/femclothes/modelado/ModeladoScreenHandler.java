package com.femclothes.modelado;

import com.femclothes.item.FemclothesDye;
import com.femclothes.region.Lado;
import com.femclothes.screen.FemclothesScreenHandlers;
import com.femclothes.sublimadora.MoldeCuelloItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;


public class ModeladoScreenHandler extends ScreenHandler {

    // Índices dentro de la lista de Slots de ESTE handler (no confundir con
    // los índices reales de ModeladoBlockEntity) — reflejan el orden de
    // addSlot() del constructor: almacén(27), activo(1), 5 pines del
    // esquema de remera (2026-09-24), prenda(1), salida(1), porprenda(12),
    // después el inventario del jugador.
    private static final int SLOT_ACTIVO = ModeladoBlockEntity.ALMACEN_TAMANO;
    private static final int CANTIDAD_PINES = ModeladoBlockEntity.PINES_LOGICOS;
    private static final int SLOT_PRENDA = SLOT_ACTIVO + 1 + CANTIDAD_PINES;
    private static final int SLOT_SALIDA = SLOT_PRENDA + 1;
    private static final int SLOT_PORPRENDA_INICIO = SLOT_SALIDA + 1;
    private static final int INV_START = SLOT_PORPRENDA_INICIO + ModeladoBlockEntity.PORPRENDA_TOTAL;

    /**
     * Origen (x relativo a la columna del medio, y absoluto) del ítem de
     * cada pin, por categoría (orden {@code Categoria.ordinal()}) y por pin
     * (orden de {@code ModeladoBlockEntity#ROLES}). y = 36 (donde arranca
     * el esquema, ver ModeladoScreen#ESQUEMA_Y) + el y local medido sobre
     * el PNG de esa categoría; x/y ya son el origen del ítem de 16x16,
     * centrado en cada slot dibujado. Público: ModeladoScreen lo usa.
     */
    public static final int[][][] PIN_POS = {
            {{113, 47}, {58, 51}, {167, 51}, {42, 84}, {113, 89}, {183, 84}, {57, 127}, {113, 149}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{112, 46}, {112, 79}, {112, 100}, {112, 122}, {47, 95}, {49, 143}, {175, 144}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{44, 52}, {180, 52}, {43, 142}, {184, 142}, {112, 142}, {28, 76}, {28, 94}, {28, 113}, {196, 76}, {196, 94}, {196, 113}, {0, 0}},
            {{38, 55}, {185, 55}, {37, 134}, {187, 135}, {112, 144}, {28, 77}, {28, 96}, {28, 115}, {196, 77}, {196, 96}, {196, 115}, {0, 0}},
            // Pollera (2026-09-29): Forma, Largo, Calce, Mat1..3 — sobre esquema_pollera.png (2026-10-04: rehecho por tools/generar_esquemas_modeladora.py).
            {{53, 55}, {39, 134}, {39, 95}, {181, 66}, {181, 91}, {181, 118}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            // Capa (2026-09-29): Largo, Ruedo, Capucha, Cuello, Mat1..3 — sobre esquema_capa.png.
            {{190, 100}, {48, 128}, {175, 54}, {48, 56}, {48, 89}, {190, 132}, {190, 76}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            // Sombrero (2026-10-05): Ala, Punta — sobre esquema_sombrero.png (tools/generar_esquema_sombrero.py).
            {{43, 110}, {182, 58}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            // Banda (2026-10-05): Zona, Ancho, Herraje — sobre esquema_banda.png (tools/generar_esquema_banda.py).
            {{43, 110}, {182, 58}, {179, 91}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
    };

    /**
     * Centro de la chincheta de cada pin (mismo esquema que {@link #PIN_POS}):
     * en el arte de las 3 prendas nuevas ahí estaba horneada la chincheta
     * (se borró del PNG, ver tools/procesar_assets_esquemas.py); en remera
     * (arte sin chinchetas) es la esquina superior derecha del slot.
     */
    public static final int[][][] PIN_BTN = {
            {{130, 46}, {75, 50}, {184, 50}, {59, 83}, {130, 88}, {200, 83}, {74, 126}, {130, 148}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{129, 45}, {129, 78}, {129, 99}, {129, 121}, {64, 94}, {66, 142}, {192, 143}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{61, 51}, {197, 51}, {60, 141}, {201, 141}, {129, 141}, {45, 75}, {45, 93}, {45, 112}, {213, 75}, {213, 93}, {213, 112}, {0, 0}},
            {{55, 54}, {202, 54}, {54, 133}, {204, 134}, {129, 143}, {45, 76}, {45, 95}, {45, 114}, {213, 76}, {213, 95}, {213, 114}, {0, 0}},
            {{70, 54}, {56, 133}, {56, 94}, {198, 65}, {198, 90}, {198, 117}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{207, 99}, {65, 127}, {192, 53}, {65, 55}, {65, 88}, {207, 131}, {207, 75}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{60, 109}, {199, 57}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
            {{60, 109}, {199, 57}, {196, 90}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}, {0, 0}},
    };

    /** Slots grandes de prenda base / resultado (coordenadas del slot 16x16; el marco de 32x32 se hornea en la textura). */
    public static final int ENTRADA_X = 48, SALIDA_X = 176, SLOT_Y_IO = 184;

    public final ModeladoBlockEntity be;

    /**
     * Factory del lado del CLIENTE (registrada en {@code ExtendedScreenHandlerType}):
     * busca el block entity REAL en la posición que mandó el servidor, en vez
     * de crear uno de mentira — así la lista de fijadas (que viaja por NBT del
     * block entity, no por slot) llega actualizada. Si por lo que sea no está
     * (chunk no cargado todavía, condición de carrera rarísima), cae a uno
     * descartable en vez de crashear la apertura de pantalla.
     */
    public static ModeladoScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        ModeladoBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof ModeladoBlockEntity m
                ? m
                : new ModeladoBlockEntity(pos, ModeladoMod.MODELADO_BLOCK.getDefaultState());
        return new ModeladoScreenHandler(syncId, inv, be);
    }

    /**
     * El slot Activo y el banco de storage-por-prenda muestran SIEMPRE la
     * misma posición en pantalla, pero apuntan a la categoría actual — en
     * vez de reposicionar Slots (sus campos x/y son {@code final} en
     * vanilla, no se puede) se los ata a un {@link Inventory} chico que
     * redirige cada acceso al índice real de {@code be} según
     * {@code be.categoria()} EN EL MOMENTO del acceso. Cambiar de categoría
     * con el botón no mueve nada en pantalla: cambia a qué mira.
     */
    private static Inventory activoAdaptador(ModeladoBlockEntity be) {
        return new Inventory() {
            private int real() { return ModeladoBlockEntity.activoSlot(be.categoria()); }

            @Override public int size() { return 1; }
            @Override public boolean isEmpty() { return be.getStack(real()).isEmpty(); }
            @Override public ItemStack getStack(int slot) { return be.getStack(real()); }
            @Override public ItemStack removeStack(int slot, int amount) { return be.removeStack(real(), amount); }
            @Override public ItemStack removeStack(int slot) { return be.removeStack(real()); }
            @Override public void setStack(int slot, ItemStack stack) { be.setStack(real(), stack); }
            @Override public void markDirty() { be.markDirty(); }
            @Override public boolean canPlayerUse(PlayerEntity player) { return be.canPlayerUse(player); }
            @Override public boolean isValid(int slot, ItemStack stack) { return be.isValid(real(), stack); }
            @Override public void clear() { be.setStack(real(), ItemStack.EMPTY); }
        };
    }

    private static Inventory porPrendaAdaptador(ModeladoBlockEntity be) {
        return new Inventory() {
            private int real(int slot) { return ModeladoBlockEntity.porPrendaSlot(be.categoria(), slot); }

            @Override public int size() { return ModeladoBlockEntity.PORPRENDA_TOTAL; }
            @Override public boolean isEmpty() {
                for (int i = 0; i < size(); i++) if (!be.getStack(real(i)).isEmpty()) return false;
                return true;
            }
            @Override public ItemStack getStack(int slot) { return be.getStack(real(slot)); }
            @Override public ItemStack removeStack(int slot, int amount) { return be.removeStack(real(slot), amount); }
            @Override public ItemStack removeStack(int slot) { return be.removeStack(real(slot)); }
            @Override public void setStack(int slot, ItemStack stack) { be.setStack(real(slot), stack); }
            @Override public void markDirty() { be.markDirty(); }
            @Override public boolean canPlayerUse(PlayerEntity player) { return be.canPlayerUse(player); }
            @Override public boolean isValid(int slot, ItemStack stack) { return be.isValid(real(slot), stack); }
            @Override public void clear() { for (int i = 0; i < size(); i++) be.setStack(real(i), ItemStack.EMPTY); }
        };
    }

    public ModeladoScreenHandler(int syncId, PlayerInventory playerInventory, ModeladoBlockEntity be) {
        super(FemclothesScreenHandlers.MODELADO, syncId);
        this.be = be;

        // Columna del medio (config): margen 119 = 19 (centrado original de
        // la grilla de 9 columnas) + 100 (ancho del visor 3D, a la
        // izquierda). Activo/Prenda/Salida viven acá.
        int mMedio = 119;
        // Columna de la derecha (storage): a pedido, separada de la config
        // — 240 (ancho de la columna del medio, agrandada 2026-09-24 para
        // que el esquema de remera entre cómodo) + 20 (separación).
        int mDerecha = mMedio + 240 + 20;

        // Storage COMPARTIDO: el almacén de 27, ahora a la derecha.
        for (int i = 0; i < ModeladoBlockEntity.ALMACEN_TAMANO; i++) {
            int fila = i / 9;
            int col = i % 9;
            addSlot(new SlotValidado(be, ModeladoBlockEntity.ALMACEN_INICIO + i, mDerecha + col * 18, 18 + fila * 18));
        }

        // Activo de la categoría actual — ver activoAdaptador(). Para
        // REMERA queda oculto en pantalla (ModeladoScreen), reemplazado
        // por los 5 pines de abajo — sigue existiendo acá para que el
        // índice de slots no se mueva y las otras 3 categorías (que SÍ
        // lo siguen usando) no se rompan.
        addSlot(new SlotValidado(activoAdaptador(be), 0, mMedio, 86) {
            // Sin uso desde que las 4 categorías tienen pines (2026-09-26).
            @Override
            public boolean isEnabled() { return false; }
        });

        // Los 8 pines del esquema visual de remera (2026-09-24, "un pin
        // por cada eje de corte" + "la idea de materiales/textura es para
        // agregar esas cosas ahi pongamos eso de nuevo") — cada uno aplica
        // DIRECTO al soltar el molde (ver ModeladoBlockEntity#pinearManga/
        // pinearCuello/pinearTorso/pinearCalce/pinearMaterial), sin pasar
        // por Activo+Fijar. Posiciones medidas a mano sobre el asset
        // pergamino curado (esquema_remera.png, ya no es el placeholder
        // generado por tools/generar_esquema_remera.py — ver ese script
        // para el mapeo de coordenadas fuente→lógicas) — si se reemplaza
        // el PNG por otro con otra disposición, hay que remedir esto.
        // Remedidas 2026-09-24 sobre el panel agrandado (240x136, antes
        // 162x92, ver ModeladoScreen#ESQUEMA_ANCHO): con más aire ninguna
        // choca con Prenda/Salida, así que Calce ya no necesita el
        // corrimiento a mano que tenía en el panel chico.
        // (x, y) = origen del ítem de 16x16, centrado en cada pin del dibujo;
        // y = 36 (donde arranca el esquema, ver ModeladoScreen#ESQUEMA_Y) +
        // el y local medido sobre el PNG. Orden = índices PIN_* del BE.
        for (int p = 0; p < ModeladoBlockEntity.PINES_LOGICOS; p++) {
            int cat = p / ModeladoBlockEntity.PINES_POR_CATEGORIA, i = p % ModeladoBlockEntity.PINES_POR_CATEGORIA;
            addSlot(new PinSlot(be, ModeladoBlockEntity.pinSlot(p), mMedio + PIN_POS[cat][i][0], PIN_POS[cat][i][1]));
        }

        addSlot(new Slot(be, ModeladoBlockEntity.PRENDA, mMedio + ENTRADA_X, SLOT_Y_IO) {
            @Override
            public boolean canInsert(ItemStack stack) { return ModeladoBlockEntity.esPrendaModelable(stack); }
        });
        addSlot(new Slot(be, ModeladoBlockEntity.SALIDA, mMedio + SALIDA_X, SLOT_Y_IO) {
            @Override
            public boolean canInsert(ItemStack stack) { return false; }
        });

        // Storage POR PRENDA de la categoría actual (12, 4x3) — ver
        // porPrendaAdaptador(). Debajo del compartido, misma columna derecha.
        // y=90 y no 80 (2026-09-24): deja lugar al rótulo "Moldes de
        // <Categoria>" que dibuja ModeladoScreen#dibujarRotulosStorage.
        // 9x4 = 36 desde 2026-09-29 ("agregaria mas slots a moldes de
        // pantalones moldes de remera") — antes 4x3.
        Inventory porPrenda = porPrendaAdaptador(be);
        for (int i = 0; i < ModeladoBlockEntity.PORPRENDA_TOTAL; i++) {
            int fila = i / 9;
            int col = i % 9;
            addSlot(new SlotValidado(porPrenda, i, mDerecha + col * 18, 90 + fila * 18));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, mMedio + j * 18, 246 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, mMedio + i * 18, 304));
        }

        addProperties(be.getPropertyDelegate());
        if (be.getWorld() != null && !be.getWorld().isClient()) be.guiAbierta();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return be.onButtonClick(id);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return be.canPlayerUse(player);
    }

    /**
     * Sin botón de Encender (a pedido, "cuando se sale se enciende"): cada
     * lado tiene su propio ScreenHandler y su propio onClosed, así que esto
     * corre tanto en cliente como en servidor — se guarda solo del lado
     * servidor porque ahí vive el {@link ModeladoBlockEntity} real; del
     * lado cliente sería una escritura redundante que el próximo sync del
     * servidor pisa igual.
     */
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient()) {
            be.guiCerrada();
            be.encenderAlCerrar();
        }
    }

    /**
     * Reglas nuevas de enrutado (2026-09-24, "en los slots de moldes de
     * remera solo entran moldes que sirvan solo para la remera y
     * remeras... al apretar shift click se pasan los moldes solo para
     * remera y las remeras a ese almacen. en el almacen de prendas van
     * los moldes que sirven para mas de una prenda"): antes CUALQUIER
     * molde (según {@code esMolde}, ya de por sí incompleta) iba directo
     * al almacén general por shift-click, sin distinguir exclusivo de
     * compartido. Ahora: molde EXCLUSIVO de la categoría con la pestaña
     * activa (o su prenda terminada) → banco "Moldes de esa categoría";
     * molde COMPARTIDO (2+ categorías) → almacén general; prenda
     * terminada de OTRA categoría o cualquier otra ropa → slot Prenda.
     * Nota: el banco por-prenda de este handler solo expone la categoría
     * ACTIVA (ver {@code porPrendaAdaptador}) — igual que el Activo, un
     * molde exclusivo de una categoría que no es la seleccionada no tiene
     * dónde ir por shift-click todavía (hay que cambiar de pestaña
     * primero), cae al {@code insertItem} final que simplemente no
     * encuentra slot válido y no mueve nada.
     */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack result = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(slot);
        if (clickedSlot != null && clickedSlot.hasStack()) {
            ItemStack stack = clickedSlot.getStack();
            result = stack.copy();
            if (slot < INV_START) {
                if (!this.insertItem(stack, INV_START, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                ModeladoBlockEntity.Categoria catExclusiva = ModeladoBlockEntity.categoriaExclusivaDe(stack);
                if (catExclusiva != null && catExclusiva == be.categoria()) {
                    if (!this.insertItem(stack, SLOT_PORPRENDA_INICIO,
                            SLOT_PORPRENDA_INICIO + ModeladoBlockEntity.PORPRENDA_TOTAL, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ModeladoBlockEntity.esMoldeCompartido(stack)) {
                    if (!this.insertItem(stack, 0, ModeladoBlockEntity.ALMACEN_TAMANO, false)) { // almacén compartido
                        return ItemStack.EMPTY;
                    }
                } else if (ModeladoBlockEntity.esPrendaModelable(stack)) {
                    if (!this.insertItem(stack, SLOT_PRENDA, SLOT_PRENDA + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(stack, 0, INV_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                clickedSlot.setStack(ItemStack.EMPTY);
            } else {
                clickedSlot.markDirty();
            }
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            clickedSlot.onTakeItem(player, stack);
        }
        return result;
    }

    /**
     * {@link Slot} que SÍ respeta {@code Inventory#isValid} — el
     * {@code Slot.canInsert} de vanilla lo ignora (siempre true), así que
     * antes el almacén y el banco por-prenda aceptaban cualquier ítem
     * arrastrado a mano (2026-09-24, "el almacen de moldes de prendas
     * recibe cualquier item"); solo el shift-click filtraba. Mismo bug ya
     * documentado en memoria (slot_caninsert_gotcha).
     */
    private static class SlotValidado extends Slot {
        SlotValidado(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.inventory.isValid(this.getIndex(), stack);
        }
    }

    /**
     * Un pin del esquema visual de remera (2026-09-24): un slot REAL del
     * inventario ({@code ModeladoBlockEntity#PINES_INICIO}+i) que retiene
     * el molde — se ve lo que tiene puesto, y sacarlo des-aplica el corte
     * (ver {@code ModeladoBlockEntity#pinCambio}). De a 1 ítem, solo del
     * tipo que acepta ese pin, y solo visible/usable con REMERA activa (el
     * dibujo del esquema solo se pinta para esa categoría).
     */
    private static class PinSlot extends SlotValidado {
        private final ModeladoBlockEntity be;

        PinSlot(ModeladoBlockEntity be, int index, int x, int y) {
            super(be, index, x, y);
            this.be = be;
        }

        @Override
        public boolean isEnabled() {
            int p = ModeladoBlockEntity.pinDeSlot(this.getIndex());
            int cat = p / ModeladoBlockEntity.PINES_POR_CATEGORIA;
            return be.categoria().ordinal() == cat
                    && ModeladoBlockEntity.ROLES[cat][p % ModeladoBlockEntity.PINES_POR_CATEGORIA]
                    != ModeladoBlockEntity.Rol.NINGUNO;
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !be.encendida();
        }
    }
}
