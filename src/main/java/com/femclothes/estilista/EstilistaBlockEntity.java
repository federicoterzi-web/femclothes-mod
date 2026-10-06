package com.femclothes.estilista;

import com.femclothes.estilado.EstiladoBlockEntity;
import com.femclothes.util.InventarioUtil;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * La Estilista automática, etapa 1 (2026-10-05, "rearmemos la estiladora como un bloque de la cadena"): una máquina de
 * la línea textil con la misma base que la Modeladora — entra la prenda por arriba o por la izquierda (cinta, tolva o
 * la salida de la máquina de al lado), sale por la derecha — y el pórtico de la tapa. Todavía NO aplica ningún diseño
 * (eso es la etapa 2): una prenda que llega por la cadena pasa de largo y a mano el botón Probar corre la animación.
 */
public class EstilistaBlockEntity extends BlockEntity implements SidedInventory, com.femclothes.util.ConSalida,
        ExtendedScreenHandlerFactory<BlockPos>, GeoBlockEntity, com.femclothes.util.MaquinaCreativa.Cargable {

    /**
     * El buzón (2026-10-05, "los insumos pueden cargar invisibles") es un casillero que nadie ve: lo que entra por
     * tolva o cinta (hilo y cuero) se convierte al toque en los contadores. El almacén es visible: guarda plantillas,
     * retazos y los objetos que gastan los apliques de objeto.
     */
    public static final int SLOT_PRENDA = 0, SLOT_SALIDA = 1, SLOT_BUZON = 2, SLOT_ALMACEN = 3, ALMACEN = 18,
            TAMANO = SLOT_ALMACEN + ALMACEN;
    /** Cuánto rinde cada ítem (2026-10-05, "rinde más por ítem") y cuánto aguanta cada contador. */
    public static final int APLIQUES_POR_HILO = 4, CORREAS_POR_CUERO = 2, TOPE_CONTADOR = 64;
    /** Cuántos diseños (uno por tipo de prenda) guarda la máquina. */
    public static final int MAX_DISENOS = 32;
    /** Lo que dura el trabajo del pórtico: los 13 s de la animación. */
    public static final int TICKS_PROCESO = 260;
    /** Botones: aplicar el diseño a la prenda de la entrada, fijar esa prenda como muestra, olvidar el diseño de su tipo. */
    public static final int BTN_APLICAR = 600, BTN_FIJAR = 601, BTN_BORRAR = 602, BTN_CARGAR_HILO = 603, BTN_CARGAR_CUERO = 604;

    /** Lo que sale de intentar aplicar el diseño: ver {@link #planear}. */
    public enum Resultado { OK, SIN_DISENO, FALTA }

    /** Lo que copia un diseño de la prenda de muestra (lo demás de la prenda no se toca). */
    private static final java.util.List<net.minecraft.component.ComponentType<?>> COMPONENTES_DE_DISENO = java.util.List.of(
            com.femclothes.item.FemclothesComponents.APLIQUES, com.femclothes.item.FemclothesComponents.CORREAS,
            com.femclothes.item.FemclothesComponents.TEXTURA_TELA, com.femclothes.item.FemclothesComponents.ACABADO_TRIM,
            com.femclothes.item.FemclothesComponents.COLORES_SOMBRERO, com.femclothes.item.FemclothesComponents.PATRONES_SOMBRERO,
            com.femclothes.item.FemclothesComponents.COLORES_BANDA, com.femclothes.item.FemclothesComponents.PATRONES_BANDA);

    /** Los tics del trabajo (de la animación `trabajo`) en que el pórtico arranca un recorrido: horizontal o de subida y bajada. */
    private static final int[] TICS_HORIZONTAL = {0, 18, 42, 52, 70, 94, 104, 122, 146, 156, 174, 198};
    private static final int[] TICS_VERTICAL = {10, 14, 31, 36, 62, 66, 83, 88, 114, 118, 135, 140, 166, 170, 187, 192};

    public enum Estado { REPOSO, PROCESANDO, LISTO }

    private static final RawAnimation TRABAJO = RawAnimation.begin().thenLoop("animation.estilista.trabajo");
    private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("animation.estilista.quieto");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    private Estado estado = Estado.REPOSO;
    private int progreso;
    /** Cuántas pantallas hay abiertas: con alguna abierta no arranca nada solo. */
    private int guiAbiertas;
    /** La prenda de la entrada llegó por la cadena (arranca sola apenas haya insumos) o la puso alguien a mano (espera los botones). */
    private boolean deCadena;
    /** Los contadores: cuántos apliques y cuántas correas alcanzan con lo cargado (unidades, no ítems). */
    private int hilo, cuero;
    /** Un diseño por tipo de prenda: el ítem de la muestra con solo lo que se copia (apliques, correas, colores). */
    private final DefaultedList<ItemStack> disenos = DefaultedList.ofSize(MAX_DISENOS, ItemStack.EMPTY);

    /**
     * El editor (2026-10-05, etapa 3: "no quiero perder el tamaño del visualizador"): una Mesa de estilado interna, fuera
     * del mundo, cuya prenda es la MUESTRA del diseño. La pantalla y los clicks de la vista 3D son los de la Mesa.
     */
    private final EstiladoBlockEntity editor;

    public EstilistaBlockEntity(BlockPos pos, BlockState state) {
        super(EstilistaMod.ESTILISTA_BLOCK_ENTITY, pos, state);
        editor = new EstiladoBlockEntity(pos, com.femclothes.estilado.EstiladoMod.ESTILADO_BLOCK.getDefaultState());
        editor.anfitrionar(this);
    }

    public EstiladoBlockEntity editor() { return editor; }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        editor.setWorld(world);
    }

    /** Lo llama el editor cuando cambia algo: se guarda y se manda al cliente con el resto de la máquina. */
    public void sincronizarEditor() { sincronizar(); }

    // ── estado visible ──

    public Estado estado() { return estado; }
    public int progreso() { return progreso; }

    public ItemStack prendaVisible() {
        return !items.get(SLOT_PRENDA).isEmpty() ? items.get(SLOT_PRENDA) : items.get(SLOT_SALIDA);
    }

    private ItemStack ultimaVistaPrevia = ItemStack.EMPTY;

    /** La prenda de la pantallita: la última que hubo (salida o entrada), así no se apaga entre prendas. */
    public ItemStack vistaPreviaPersistente() {
        ItemStack actual = !items.get(SLOT_SALIDA).isEmpty() ? items.get(SLOT_SALIDA) : items.get(SLOT_PRENDA);
        if (!actual.isEmpty()) ultimaVistaPrevia = actual;
        return ultimaVistaPrevia;
    }

    public ItemStack salidaVisible() { return items.get(SLOT_SALIDA); }

    // ── diseños (etapa 2, 2026-10-05: "la primera que entre o la del input, vamos con 13 s, que gaste y recupere") ──

    /** El diseño guardado para el tipo de esa prenda, o null. */
    @Nullable
    public ItemStack diseno(ItemStack prenda) {
        if (prenda.isEmpty()) return null;
        for (ItemStack d : disenos) if (!d.isEmpty() && d.getItem() == prenda.getItem()) return d;
        return null;
    }

    public int cuantosDisenos() {
        int n = 0;
        for (ItemStack d : disenos) if (!d.isEmpty()) n++;
        return n;
    }

    private static <T> void copiar(net.minecraft.component.ComponentType<T> tipo, ItemStack de, ItemStack a) {
        T valor = de.get(tipo);
        if (valor != null) a.set(tipo, valor);
        else a.remove(tipo);
    }

    private static void copiarDiseno(ItemStack de, ItemStack a) {
        for (net.minecraft.component.ComponentType<?> t : COMPONENTES_DE_DISENO) copiar(t, de, a);
    }

    private static int cuenta(ItemStack s, net.minecraft.component.ComponentType<? extends java.util.Collection<?>> t) {
        java.util.Collection<?> c = s.get(t);
        return c == null ? 0 : c.size();
    }

    public static int apliquesDe(ItemStack s) { return cuenta(s, com.femclothes.item.FemclothesComponents.APLIQUES); }
    public static int correasDe(ItemStack s) { return cuenta(s, com.femclothes.item.FemclothesComponents.CORREAS); }

    /** Los objetos que gasta (y devuelve al quitarlos) lo que lleva una prenda: el de cada aplique de objeto y su muestra. */
    private static java.util.List<ItemStack> costoDe(ItemStack s) {
        java.util.List<ItemStack> costo = new java.util.ArrayList<>();
        java.util.List<com.femclothes.aplique.Aplique> apl = s.get(com.femclothes.item.FemclothesComponents.APLIQUES);
        if (apl != null) {
            for (com.femclothes.aplique.Aplique a : apl) {
                // Un objeto "de molde" no gastó nada al ponerse (ni vuelve).
                if (a.objeto() != null && !a.objeto().deMolde()) {
                    costo.add(a.objeto().item().copy());
                    if (!a.objeto().muestra().isEmpty()) costo.add(a.objeto().muestra().copy());
                }
            }
        }
        return costo;
    }

    public int hilo() { return hilo; }
    public int cuero() { return cuero; }

    /** Carga hilo o cuero en los contadores con lo que haya en {@code pila} (se le restan los ítems usados); devuelve cuántos. */
    public int absorber(ItemStack pila) {
        boolean esHilo = pila.isOf(net.minecraft.item.Items.STRING), esCuero = pila.isOf(net.minecraft.item.Items.LEATHER);
        if (!esHilo && !esCuero) return 0;
        int porItem = esHilo ? APLIQUES_POR_HILO : CORREAS_POR_CUERO;
        int actual = esHilo ? hilo : cuero;
        int caben = Math.max(0, (TOPE_CONTADOR - actual) / porItem);
        int n = Math.min(pila.getCount(), caben);
        if (n <= 0) return 0;
        pila.decrement(n);
        if (esHilo) hilo += n * porItem; else cuero += n * porItem;
        sincronizar();
        return n;
    }

    /** Lo que quedó en el buzón se vuelca a los contadores apenas hay lugar. */
    private void drenarBuzon() {
        ItemStack b = items.get(SLOT_BUZON);
        if (b.isEmpty()) return;
        absorber(b);
        if (b.isEmpty()) items.set(SLOT_BUZON, ItemStack.EMPTY);
    }

    /** Existencias del almacén (o una copia para simular): pila por tipo de ítem + componentes. */
    private static final class Existencias {
        final java.util.List<ItemStack> pilas = new java.util.ArrayList<>();

        void sumar(ItemStack s) {
            if (s.isEmpty()) return;
            for (ItemStack e : pilas) if (ItemStack.areItemsAndComponentsEqual(e, s)) { e.increment(s.getCount()); return; }
            pilas.add(s.copy());
        }

        boolean sacar(ItemStack s) {
            for (ItemStack e : pilas) {
                if (ItemStack.areItemsAndComponentsEqual(e, s)) {
                    if (e.getCount() < s.getCount()) return false;
                    e.decrement(s.getCount());
                    return true;
                }
            }
            return false;
        }

        Existencias copia() {
            Existencias c = new Existencias();
            for (ItemStack e : pilas) c.pilas.add(e.copy());
            return c;
        }
    }

    private Existencias existencias() {
        Existencias e = new Existencias();
        for (int i = SLOT_ALMACEN; i < TAMANO; i++) e.sumar(items.get(i));
        return e;
    }

    private void escribirExistencias(Existencias e) {
        for (int i = SLOT_ALMACEN; i < TAMANO; i++) items.set(i, ItemStack.EMPTY);
        int slot = SLOT_ALMACEN;
        for (ItemStack pila : e.pilas) {
            ItemStack resto = pila.copy();
            while (!resto.isEmpty()) {
                ItemStack parte = resto.split(Math.min(resto.getCount(), Math.max(1, resto.getMaxCount())));
                if (slot < TAMANO) items.set(slot++, parte);
                else if (world != null) net.minecraft.block.Block.dropStack(world, pos.up(), parte);   // sin lugar: cae al piso
            }
        }
    }

    /** Resultado de simular aplicar el diseño a esa prenda: sin tocar nada. {@code falta} (si lo hay) es lo que no alcanza. */
    public record Plan(Resultado resultado, @Nullable Existencias despues, int hilo, int cuero, ItemStack falta) {}

    public Plan planear(ItemStack prenda) {
        ItemStack d = diseno(prenda);
        if (d == null) return new Plan(Resultado.SIN_DISENO, null, hilo, cuero, ItemStack.EMPTY);
        if (com.femclothes.util.MaquinaCreativa.es(this)) return new Plan(Resultado.OK, null, hilo, cuero, ItemStack.EMPTY);
        // Lo que la prenda ya traía vuelve (hilo, cuero y objetos); después se paga el diseño nuevo.
        int h = Math.min(TOPE_CONTADOR, hilo + apliquesDe(prenda)) - apliquesDe(d);
        int c = Math.min(TOPE_CONTADOR, cuero + correasDe(prenda)) - correasDe(d);
        if (h < 0) return new Plan(Resultado.FALTA, null, hilo, cuero,
                new ItemStack(net.minecraft.item.Items.STRING, (-h + APLIQUES_POR_HILO - 1) / APLIQUES_POR_HILO));
        if (c < 0) return new Plan(Resultado.FALTA, null, hilo, cuero,
                new ItemStack(net.minecraft.item.Items.LEATHER, (-c + CORREAS_POR_CUERO - 1) / CORREAS_POR_CUERO));
        Existencias e = existencias();
        for (ItemStack o : costoDe(prenda)) e.sumar(o);
        for (ItemStack o : costoDe(d)) {
            if (!e.sacar(o)) return new Plan(Resultado.FALTA, null, hilo, cuero, o);
        }
        return new Plan(Resultado.OK, e, h, c, ItemStack.EMPTY);
    }

    /** Lo que falta para aplicar el diseño a la prenda de la entrada (para la pantalla), o vacío. */
    public ItemStack faltaParaLaEntrada() {
        Plan p = planear(items.get(SLOT_PRENDA));
        return p.resultado() == Resultado.FALTA ? p.falta() : ItemStack.EMPTY;
    }

    /** Arranca el trabajo: gasta los insumos, devuelve los de lo que la prenda ya traía y le pone el diseño. */
    public Resultado iniciar() {
        ItemStack prenda = items.get(SLOT_PRENDA);
        if (estado != Estado.REPOSO || prenda.isEmpty() || !items.get(SLOT_SALIDA).isEmpty()) return Resultado.SIN_DISENO;
        Plan plan = planear(prenda);
        if (plan.resultado() != Resultado.OK) return plan.resultado();
        if (plan.despues() != null) {
            escribirExistencias(plan.despues());
            hilo = plan.hilo();
            cuero = plan.cuero();
        }
        copiarDiseno(diseno(prenda), prenda);
        estado = Estado.PROCESANDO;
        progreso = 0;
        sincronizar();
        return Resultado.OK;
    }

    /**
     * Fija como diseño de su tipo la prenda de muestra del editor (o, si no hay, la de la entrada). La muestra del editor
     * queda ahí para seguir editándola; la de la entrada sale por la derecha como terminada.
     */
    private boolean fijar(PlayerEntity jugador) {
        ItemStack delEditor = editor.getStack(EstiladoBlockEntity.SLOT_PRENDA);
        boolean usaEditor = !delEditor.isEmpty();
        ItemStack muestra = usaEditor ? delEditor : items.get(SLOT_PRENDA);
        if (muestra.isEmpty()) return false;
        ItemStack proto = new ItemStack(muestra.getItem());
        copiarDiseno(muestra, proto);
        boolean hayAlgo = false;
        for (net.minecraft.component.ComponentType<?> t : COMPONENTES_DE_DISENO) hayAlgo |= proto.get(t) != null;
        if (!hayAlgo) {
            jugador.sendMessage(Text.translatable("femclothes.estilista.muestra_vacia"), true);
            return false;
        }
        int libre = -1, propio = -1;
        for (int i = 0; i < MAX_DISENOS; i++) {
            if (disenos.get(i).isEmpty()) { if (libre < 0) libre = i; }
            else if (disenos.get(i).getItem() == muestra.getItem()) propio = i;
        }
        int donde = propio >= 0 ? propio : libre;
        if (donde < 0) {
            jugador.sendMessage(Text.translatable("femclothes.estilista.sin_lugar"), true);
            return false;
        }
        disenos.set(donde, proto);
        if (!usaEditor) {
            items.set(SLOT_SALIDA, muestra);
            items.set(SLOT_PRENDA, ItemStack.EMPTY);
            estado = Estado.LISTO;
        }
        sincronizar();
        jugador.sendMessage(Text.translatable("femclothes.estilista.fijado", apliquesDe(proto), correasDe(proto)), true);
        return true;
    }

    // ── animación + ticker ──

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "trabajo", 0,
                state -> estado == Estado.PROCESANDO ? state.setAndContinue(TRABAJO) : state.setAndContinue(QUIETO)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    public static void tick(World world, BlockPos pos, BlockState state, EstilistaBlockEntity be) {
        if (world.isClient) return;
        // Con señal de redstone la máquina se detiene del todo.
        if (com.femclothes.util.Redstone.pausada(world, pos)) return;
        com.femclothes.util.LuzMaquina.actualizar(world, pos, state, be.estado != Estado.REPOSO);
        switch (be.estado) {
            case REPOSO -> {
                if (world.getTime() % 20 == 0) be.drenarBuzon();
                // Una prenda que llegó por la cadena espera los insumos que le falten y arranca sola.
                if (be.deCadena && !be.items.get(SLOT_PRENDA).isEmpty() && be.items.get(SLOT_SALIDA).isEmpty()
                        && world.getTime() % 20 == 0) {
                    be.iniciar();
                }
            }
            case PROCESANDO -> {
                be.progreso++;
                be.sonidoDelPortico(world, pos);
                if (be.progreso % 20 == 0) be.sincronizar();
                if (be.progreso >= TICKS_PROCESO) { // 2026-10-05, "no se movio para nada el brazo": la demo dura siempre los 13 s, también en la creativa
                    be.items.set(SLOT_SALIDA, be.items.get(SLOT_PRENDA));
                    be.items.set(SLOT_PRENDA, ItemStack.EMPTY);
                    be.estado = Estado.LISTO;
                    world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.0f, 1.5f);
                    be.sincronizar();
                }
            }
            case LISTO -> {
                if (!be.items.get(SLOT_SALIDA).isEmpty()) be.empujarSalida(world, pos);
                if (be.items.get(SLOT_SALIDA).isEmpty()) {
                    be.estado = Estado.REPOSO;
                    be.progreso = 0;
                    be.sincronizar();
                }
            }
        }
    }

    /** Botones de la pantalla (los atiende el handler porque necesitan al jugador para avisarle). */
    public boolean onButtonClick(PlayerEntity jugador, int id) {
        if (id == BTN_FIJAR) return fijar(jugador);
        if (id == BTN_BORRAR) {
            // El tipo de la prenda de la entrada o, si no hay, el de la muestra del editor.
            ItemStack ref = !items.get(SLOT_PRENDA).isEmpty() ? items.get(SLOT_PRENDA) : editor.getStack(EstiladoBlockEntity.SLOT_PRENDA);
            for (int i = 0; i < MAX_DISENOS; i++) {
                if (!ref.isEmpty() && !disenos.get(i).isEmpty() && disenos.get(i).getItem() == ref.getItem()) {
                    disenos.set(i, ItemStack.EMPTY);
                    sincronizar();
                    jugador.sendMessage(Text.translatable("femclothes.estilista.borrado"), true);
                    return true;
                }
            }
            jugador.sendMessage(Text.translatable("femclothes.estilista.sin_diseno"), true);
            return false;
        }
        if (id != BTN_APLICAR) return false;
        if (estado != Estado.REPOSO || items.get(SLOT_PRENDA).isEmpty() || !items.get(SLOT_SALIDA).isEmpty()) return false;
        Resultado r = iniciar();
        switch (r) {
            case SIN_DISENO -> jugador.sendMessage(Text.translatable("femclothes.estilista.sin_diseno"), true);
            case FALTA -> jugador.sendMessage(Text.translatable("femclothes.estilista.falta", faltaParaLaEntrada().getName()), true);
            default -> { }
        }
        return r == Resultado.OK;
    }

    /** Los pistones del pórtico: un chasquido en cada recorrido de la animación (el reloj del cliente va a la par, a ojo). */
    private void sonidoDelPortico(World world, BlockPos pos) {
        for (int t : TICS_HORIZONTAL) {
            if (progreso == t) world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 0.25f, 1.9f);
        }
        for (int t : TICS_VERTICAL) {
            if (progreso == t) world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.3f, 1.6f);
        }
    }

    public void alAbrirGui() { guiAbiertas++; }

    public void alCerrarGui() { guiAbiertas = Math.max(0, guiAbiertas - 1); }

    // ── la cadena (igual que la Modeladora) ──

    private Direction ladoIzquierdo() {
        return getCachedState().get(EstilistaBlock.FACING).getOpposite().rotateYCounterclockwise();
    }

    private Direction ladoDerecho() { return ladoIzquierdo().getOpposite(); }

    @Override
    public Direction ladoSalida() { return ladoDerecho(); }

    private void empujarSalida(World world, BlockPos pos) {
        ItemStack actual = items.get(SLOT_SALIDA);
        if (actual.isEmpty()) return;
        Direction derecha = ladoDerecho();
        ItemStack sobrante = InventarioUtil.empujarA(world, pos.offset(derecha), derecha.getOpposite(), actual);
        if (sobrante.getCount() != actual.getCount()) {
            items.set(SLOT_SALIDA, sobrante);
            markDirty();
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP || side == ladoIzquierdo()) return new int[]{SLOT_PRENDA};
        if (side == ladoDerecho()) return new int[0];
        return new int[]{SLOT_BUZON};                          // por los otros lados solo entra hilo y cuero (invisible)
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && com.femclothes.util.Redstone.pausada(this)) return false;
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return false; }

    // ── inventario ──

    @Override public int size() { return TAMANO; }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int cantidad) {
        ItemStack r = Inventories.splitStack(items, slot, cantidad);
        if (slot == SLOT_PRENDA && items.get(slot).isEmpty()) deCadena = false;
        if (!r.isEmpty()) sincronizar();
        return r;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack r = Inventories.removeStack(items, slot);
        if (slot == SLOT_PRENDA) deCadena = false;
        if (!r.isEmpty()) sincronizar();
        return r;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot != SLOT_PRENDA && slot != SLOT_SALIDA) {          // buzón y almacén: pilas normales
            items.set(slot, stack);
            if (slot == SLOT_BUZON) drenarBuzon();
            sincronizar();
            return;
        }
        // Una prenda que llega por la cadena: con diseño para su tipo arranca sola (cuando haya insumos); sin diseño
        // pasa de largo (como en las otras máquinas).
        if (slot == SLOT_PRENDA && !stack.isEmpty() && InventarioUtil.enCadena
                && items.get(SLOT_SALIDA).isEmpty() && estado == Estado.REPOSO) {
            if (diseno(stack) == null) {
                items.set(SLOT_SALIDA, stack.copyWithCount(1));
                estado = Estado.LISTO;
                sincronizar();
                return;
            }
            items.set(SLOT_PRENDA, stack.copyWithCount(1));
            deCadena = true;
            iniciar();                                              // si faltan insumos queda esperando (el tick reintenta)
            sincronizar();
            return;
        }
        if (slot == SLOT_PRENDA) deCadena = false;
        items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 64; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == SLOT_BUZON) return stack.isOf(net.minecraft.item.Items.STRING) || stack.isOf(net.minecraft.item.Items.LEATHER);
        if (slot >= SLOT_ALMACEN) {
            return !stack.isEmpty() && !com.femclothes.garment.Garments.esPrenda(stack)
                    && !stack.isOf(net.minecraft.item.Items.STRING) && !stack.isOf(net.minecraft.item.Items.LEATHER)
                    && com.femclothes.aplique.ObjetoAplique.admite(stack);
        }
        return slot == SLOT_PRENDA && items.get(SLOT_PRENDA).isEmpty() && estado == Estado.REPOSO
                && EstiladoBlockEntity.admite(stack);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() { items.clear(); }

    @Override
    public void cargarCreativa() { editor.cargarCreativa(); }

    // ── pantalla ──

    @Override
    public Text getDisplayName() { return Text.translatable("block.femclothes.estilista"); }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) { return pos; }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new EstilistaScreenHandler(syncId, inv, this);
    }

    // ── NBT y sincronización ──

    private void sincronizar() {
        markDirty();
        if (world != null && !world.isClient) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);
        nbt.putInt("estado", estado.ordinal());
        nbt.putInt("progreso", progreso);
        nbt.putBoolean("de_cadena", deCadena);
        nbt.putInt("hilo", hilo);
        nbt.putInt("cuero", cuero);
        NbtCompound d = new NbtCompound();
        Inventories.writeNbt(d, disenos, registries);
        nbt.put("disenos", d);
        nbt.put("editor", editor.createNbt(registries));
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        estado = Estado.values()[Math.max(0, Math.min(Estado.values().length - 1, nbt.getInt("estado")))];
        progreso = nbt.getInt("progreso");
        deCadena = nbt.getBoolean("de_cadena");
        hilo = Math.min(TOPE_CONTADOR, nbt.getInt("hilo"));
        cuero = Math.min(TOPE_CONTADOR, nbt.getInt("cuero"));
        disenos.clear();
        if (nbt.contains("disenos")) Inventories.readNbt(nbt.getCompound("disenos"), disenos, registries);
        if (nbt.contains("editor")) editor.read(nbt.getCompound("editor"), registries);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) { return createNbt(registries); }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
