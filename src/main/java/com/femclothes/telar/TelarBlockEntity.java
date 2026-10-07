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
 * derecha), se elige la prenda en la pantalla y, mientras haya insumos y la bandeja esté libre, teje una prenda lisa
 * del color de la lana y la empuja por la derecha a la próxima máquina (Tintes, Modeladora…).
 */
public class TelarBlockEntity extends BlockEntity implements SidedInventory, com.femclothes.util.ConSalida,
        ExtendedScreenHandlerFactory<BlockPos>, GeoBlockEntity {

    public static final int SLOT_LANA = 0, SLOT_HILO = 1, SLOT_SALIDA = 2, TAMANO = 3;
    /** Lo que dura tejer: los 4 s de la animación `trabajo`, hasta tres pasadas (12 s) por prenda. */
    public static final int TICKS_PROCESO = 240;
    /** Botones de la pantalla: elegir la prenda {@code BTN_PRENDA_BASE + ordinal}. */
    public static final int BTN_PRENDA_BASE = 620;

    public enum Estado { REPOSO, PROCESANDO, LISTO }

    private static final RawAnimation TRABAJO = RawAnimation.begin().thenLoop("animation.telar.trabajo");
    private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("animation.telar.quieto");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    private Estado estado = Estado.REPOSO;
    private int progreso;
    private int prenda;
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
    public TelarPrenda prenda() { return TelarPrenda.values()[prenda]; }
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
        int clave = prenda * 0x1000000 + colorDeLaLana();
        if (clave != previaClave || previa.isEmpty()) {
            previaClave = clave;
            previa = tejer(prenda(), colorDeLaLana());
        }
        return previa;
    }

    private static ItemStack tejer(TelarPrenda p, int rgb) {
        ItemStack s = new ItemStack(p.item());
        s.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(rgb, true));
        return s;
    }

    public boolean hayInsumos() {
        if (com.femclothes.util.MaquinaCreativa.es(this)) return true;
        TelarPrenda p = prenda();
        return items.get(SLOT_LANA).getCount() >= p.lana && items.get(SLOT_HILO).getCount() >= p.hilo;
    }

    /** Elige la prenda (lo llama el handler). */
    public boolean elegir(int ordinal) {
        if (ordinal < 0 || ordinal >= TelarPrenda.values().length || ordinal == prenda) return false;
        prenda = ordinal;
        sincronizar();
        return true;
    }

    // ── tejido ──

    private void arrancar() {
        TelarPrenda p = prenda();
        boolean creativa = com.femclothes.util.MaquinaCreativa.es(this);
        colorEnCurso = colorDeLaLana();
        if (!creativa) {
            items.get(SLOT_LANA).decrement(p.lana);
            items.get(SLOT_HILO).decrement(p.hilo);
        }
        prendaEnCurso = prenda;
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
        nbt.putInt("prenda", prenda);
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
        prenda = Math.max(0, Math.min(max, nbt.getInt("prenda")));
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
