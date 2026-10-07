package com.femclothes.telar;

import com.femclothes.util.InventarioUtil;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * El Telar automático (2026-10-07, "la idea es q produzca prendas basicas a base de lana e hilo"; charlado: remera,
 * pantalón, medias, calientabrazos, pollera y capa; el color sale de la lana). Una máquina de la línea textil con la
 * misma base que la Estilista: entran lana e hilo (a mano, por tolva o por cinta, por cualquier lado menos la
 * derecha) y, mientras haya insumos y la bandeja esté libre, teje una prenda lisa del color de la lana y la empuja
 * por la derecha a la próxima máquina (Tintes, Modeladora…).
 *
 * Varias prendas y lote (2026-10-07, "se pueda seleccionar varias prendas y vaya alternando, y se le pueda configurar
 * que cantidad de prenda se quiere que saque" → "cantidad por lote. orden fijo"): las prendas tildadas se tejen de a una
 * en el orden fijo del enum, saltando la que no alcance con los insumos; el lote es el total de prendas (0 = sin límite)
 * y la máquina se detiene al terminarlo.
 */
public class TelarBlockEntity extends BlockEntity implements SidedInventory, com.femclothes.util.ConSalida,
        ExtendedScreenHandlerFactory<BlockPos>, GeoBlockEntity {

    public static final int SLOT_LANA = 0, SLOT_HILO = 1, SLOT_SALIDA = 2, TAMANO = 3;
    /** Lo que dura tejer: los 4 s de la animación `trabajo`, hasta tres pasadas (12 s) por prenda. */
    public static final int TICKS_PROCESO = 240;
    /** Botones de la pantalla: tildar o destildar la prenda {@code BTN_PRENDA_BASE + ordinal}, y los del lote. */
    public static final int BTN_PRENDA_BASE = 620;
    public static final int BTN_LOTE_MENOS = 630, BTN_LOTE_MAS = 631, BTN_LOTE_MENOS_10 = 632, BTN_LOTE_MAS_10 = 633,
            BTN_LOTE_REINICIAR = 634;
    /** Tope del lote (0 = sin límite). */
    public static final int LOTE_MAX = 64;

    public enum Estado { REPOSO, PROCESANDO, LISTO }

    private static final RawAnimation TRABAJO = RawAnimation.begin().thenLoop("animation.telar.trabajo");
    private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("animation.telar.quieto");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    private Estado estado = Estado.REPOSO;
    private int progreso;
    /** Prendas tildadas (un bit por ordinal de {@link TelarPrenda}), el lote, cuántas faltan arrancar y por dónde sigue la rotación. */
    private int seleccion = 1;
    private int lote;
    private int restantes;
    private int cursor;
    /** Lo que se está tejiendo (se anota al arrancar: la lana ya se gastó). */
    private int prendaEnCurso;
    private int colorEnCurso = 0xFFFFFF;

    private ItemStack previa = ItemStack.EMPTY;
    private int previaClave = -1;

    public TelarBlockEntity(BlockPos pos, BlockState state) {
        super(TelarMod.TELAR_BLOCK_ENTITY, pos, state);
    }

    // ── estado visible ──

    public Estado estado() { return estado; }
    public int progreso() { return progreso; }
    public int duracion() { return com.femclothes.util.MaquinaCreativa.duracion(this, TICKS_PROCESO); }
    public boolean elegida(int ordinal) { return (seleccion & (1 << ordinal)) != 0; }
    public int lote() { return lote; }
    public int restantes() { return restantes; }

    private static boolean alcanzaCon(ItemStack lana, ItemStack hilo, TelarPrenda p) {
        return lana.getCount() >= p.lana && hilo.getCount() >= p.hilo;
    }

    /** La próxima prenda a tejer (la rotación sigue el orden fijo desde {@code cursor}), o -1 si no hay: lote cumplido, nada tildado o faltan insumos. */
    public int proxima() {
        if (lote > 0 && restantes <= 0) return -1;
        boolean gratis = com.femclothes.util.MaquinaCreativa.es(this);
        int n = TelarPrenda.values().length;
        for (int k = 0; k < n; k++) {
            int i = (cursor + k) % n;
            if (!elegida(i)) continue;
            if (gratis || alcanzaCon(items.get(SLOT_LANA), items.get(SLOT_HILO), TelarPrenda.values()[i])) return i;
        }
        return -1;
    }

    /** La prenda que muestra la pantallita y el costo: la próxima, o si no alcanza la primera tildada que sigue en la rotación. */
    public int prendaVista() {
        int i = proxima();
        if (i >= 0) return i;
        int n = TelarPrenda.values().length;
        for (int k = 0; k < n; k++) if (elegida((cursor + k) % n)) return (cursor + k) % n;
        return -1;
    }
    public ItemStack salidaVisible() { return items.get(SLOT_SALIDA); }

    /** El color que daría la lana cargada (blanco si no hay). */
    public int colorDeLaLana() {
        ItemStack l = items.get(SLOT_LANA);
        int c = l.isEmpty() ? -1 : TelarPrenda.colorDeLana(l.getItem());
        return c < 0 ? 0xFFFFFF : c;
    }

    /** La prenda de la pantallita: la tejida si hay, si no la elegida teñida con la lana (se rearma solo si cambia algo). */
    public ItemStack vistaPrevia() {
        if (!items.get(SLOT_SALIDA).isEmpty()) return items.get(SLOT_SALIDA);
        int v = prendaVista();
        if (v < 0) return ItemStack.EMPTY;
        int clave = v * 0x1000000 + colorDeLaLana();
        if (clave != previaClave || previa.isEmpty()) {
            previaClave = clave;
            previa = tejer(TelarPrenda.values()[v], colorDeLaLana());
        }
        return previa;
    }

    private ItemStack tejiendo = ItemStack.EMPTY;
    private int tejiendoClave = -1;

    /**
     * La prenda que se está tejiendo, para dibujarla sobre el telar (2026-10-07, "la representacion grafica de la prenda
     * arriba"): la del lote en curso, del color de la lana que se gastó; vacía si la máquina no está tejiendo.
     */
    public ItemStack prendaTejiendo() {
        if (estado != Estado.PROCESANDO) return ItemStack.EMPTY;
        int clave = prendaEnCurso * 0x1000000 + (colorEnCurso & 0xFFFFFF);
        if (clave != tejiendoClave || tejiendo.isEmpty()) {
            tejiendoClave = clave;
            tejiendo = tejer(TelarPrenda.values()[prendaEnCurso], colorEnCurso);
        }
        return tejiendo;
    }

    private static ItemStack tejer(TelarPrenda p, int rgb) {
        ItemStack s = new ItemStack(p.item());
        s.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, true));
        return s;
    }

    public boolean hayInsumos() { return proxima() >= 0; }

    /** Los botones de la pantalla (los llama el handler): tildar prendas y configurar el lote. */
    public boolean boton(int id) {
        int n = TelarPrenda.values().length;
        if (id >= BTN_PRENDA_BASE && id < BTN_PRENDA_BASE + n) {
            seleccion ^= 1 << (id - BTN_PRENDA_BASE);
            sincronizar();
            return true;
        }
        int delta = switch (id) {
            case BTN_LOTE_MENOS -> -1;
            case BTN_LOTE_MAS -> 1;
            case BTN_LOTE_MENOS_10 -> -10;
            case BTN_LOTE_MAS_10 -> 10;
            default -> 0;
        };
        if (delta != 0) {
            lote = Math.max(0, Math.min(LOTE_MAX, lote + delta));
            restantes = lote;
            sincronizar();
            return true;
        }
        if (id == BTN_LOTE_REINICIAR) {
            restantes = lote;
            sincronizar();
            return true;
        }
        return false;
    }

    // ── tejido ──

    private void arrancar() {
        int elegida = proxima();
        if (elegida < 0) return;
        TelarPrenda p = TelarPrenda.values()[elegida];
        boolean creativa = com.femclothes.util.MaquinaCreativa.es(this);
        colorEnCurso = colorDeLaLana();
        if (!creativa) {
            items.get(SLOT_LANA).decrement(p.lana);
            items.get(SLOT_HILO).decrement(p.hilo);
        }
        prendaEnCurso = elegida;
        cursor = (elegida + 1) % TelarPrenda.values().length;
        if (lote > 0) restantes--;
        estado = Estado.PROCESANDO;
        progreso = 0;
        sincronizar();
    }

    public static void tick(World world, BlockPos pos, BlockState state, TelarBlockEntity be) {
        if (world.isClient) return;
        // Con señal de redstone la máquina se detiene del todo.
        if (com.femclothes.util.Redstone.pausada(world, pos)) return;
        com.femclothes.util.LuzMaquina.actualizar(world, pos, state, be.estado != Estado.REPOSO);
        switch (be.estado) {
            case REPOSO -> {
                if (be.items.get(SLOT_SALIDA).isEmpty() && be.hayInsumos()) be.arrancar();
            }
            case PROCESANDO -> {
                be.progreso++;
                if (be.progreso % 12 == 0) world.playSound(null, pos, SoundEvents.BLOCK_WOOL_STEP, SoundCategory.BLOCKS, 0.5f, 1.2f);
                if (be.progreso % 20 == 0) be.sincronizar();
                if (be.progreso >= be.duracion()) {
                    be.items.set(SLOT_SALIDA, tejer(TelarPrenda.values()[be.prendaEnCurso], be.colorEnCurso));
                    be.estado = Estado.LISTO;
                    world.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 0.8f, 1.1f);
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

    // ── animación ──

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "trabajo", 0,
                state -> estado == Estado.PROCESANDO ? state.setAndContinue(TRABAJO) : state.setAndContinue(QUIETO)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ── la cadena (igual que la Modeladora y la Estilista) ──

    private Direction ladoIzquierdo() {
        return getCachedState().get(TelarBlock.FACING).getOpposite().rotateYCounterclockwise();
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
        if (side == ladoDerecho()) return new int[]{SLOT_SALIDA};
        return new int[]{SLOT_LANA, SLOT_HILO, SLOT_SALIDA};      // se llena por cualquier lado; solo se saca la prenda
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (dir != null && com.femclothes.util.Redstone.pausada(this)) return false;
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_SALIDA; }

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
        if (!r.isEmpty()) sincronizar();
        return r;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack r = Inventories.removeStack(items, slot);
        if (!r.isEmpty()) sincronizar();
        return r;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 64; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_LANA -> TelarPrenda.esLana(stack.getItem());
            case SLOT_HILO -> stack.isOf(Items.STRING);
            default -> false;
        };
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() { items.clear(); }

    // ── pantalla ──

    @Override
    public Text getDisplayName() { return Text.translatable("block.femclothes.telar"); }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) { return pos; }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new TelarScreenHandler(syncId, inv, this);
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
        nbt.putInt("seleccion", seleccion);
        nbt.putInt("lote", lote);
        nbt.putInt("restantes", restantes);
        nbt.putInt("cursor", cursor);
        nbt.putInt("prenda_en_curso", prendaEnCurso);
        nbt.putInt("color_en_curso", colorEnCurso);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        estado = Estado.values()[Math.max(0, Math.min(Estado.values().length - 1, nbt.getInt("estado")))];
        progreso = nbt.getInt("progreso");
        int max = TelarPrenda.values().length - 1;
        // Lo guardado con la versión de una sola prenda (clave "prenda") pasa a ser la única tildada.
        seleccion = nbt.contains("seleccion") ? nbt.getInt("seleccion") & ((1 << (max + 1)) - 1)
                : 1 << Math.max(0, Math.min(max, nbt.getInt("prenda")));
        lote = Math.max(0, Math.min(LOTE_MAX, nbt.getInt("lote")));
        restantes = Math.max(0, Math.min(LOTE_MAX, nbt.getInt("restantes")));
        cursor = Math.max(0, Math.min(max, nbt.getInt("cursor")));
        prendaEnCurso = Math.max(0, Math.min(max, nbt.getInt("prenda_en_curso")));
        colorEnCurso = nbt.contains("color_en_curso") ? nbt.getInt("color_en_curso") : 0xFFFFFF;
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) { return createNbt(registries); }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
