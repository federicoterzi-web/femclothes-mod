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

    public static final int SLOT_PRENDA = 0, SLOT_SALIDA = 1, TAMANO = 2;
    /** Lo que dura el trabajo del pórtico: los 13 s de la animación. */
    public static final int TICKS_PROCESO = 260;
    public static final int BTN_PROBAR = 0;

    public enum Estado { REPOSO, PROCESANDO, LISTO }

    private static final RawAnimation TRABAJO = RawAnimation.begin().thenLoop("animation.estilista.trabajo");
    private static final RawAnimation QUIETO = RawAnimation.begin().thenLoop("animation.estilista.quieto");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    private Estado estado = Estado.REPOSO;
    private int progreso;
    /** Cuántas pantallas hay abiertas: con alguna abierta no arranca nada solo. */
    private int guiAbiertas;

    public EstilistaBlockEntity(BlockPos pos, BlockState state) {
        super(EstilistaMod.ESTILISTA_BLOCK_ENTITY, pos, state);
    }

    // ── estado visible ──

    public Estado estado() { return estado; }
    public int progreso() { return progreso; }

    public ItemStack prendaVisible() {
        return !items.get(SLOT_PRENDA).isEmpty() ? items.get(SLOT_PRENDA) : items.get(SLOT_SALIDA);
    }

    public ItemStack salidaVisible() { return items.get(SLOT_SALIDA); }

    /** ¿Hay un diseño fijado que aplicar? Etapa 2; hoy nunca. */
    public boolean tieneDiseno() { return false; }

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
            case REPOSO -> { }
            case PROCESANDO -> {
                be.progreso++;
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

    /** Etapa 1: arranca el trabajo con la prenda cargada (solo para ver el pórtico andar). */
    public boolean onButtonClick(int id) {
        if (id != BTN_PROBAR) return false;
        if (estado != Estado.REPOSO || items.get(SLOT_PRENDA).isEmpty() || !items.get(SLOT_SALIDA).isEmpty()) return false;
        estado = Estado.PROCESANDO;
        progreso = 0;
        sincronizar();
        return true;
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
        return side == Direction.UP || side == ladoIzquierdo() ? new int[]{SLOT_PRENDA} : new int[0];
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
        // Una prenda que llega por la cadena y no tiene diseño que aplicar pasa de largo (como en las otras máquinas).
        if (slot == SLOT_PRENDA && !stack.isEmpty() && InventarioUtil.enCadena && !tieneDiseno()
                && items.get(SLOT_SALIDA).isEmpty() && estado == Estado.REPOSO) {
            items.set(SLOT_SALIDA, stack.copyWithCount(1));
            estado = Estado.LISTO;
            sincronizar();
            return;
        }
        items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 1; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
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
    public void cargarCreativa() {}

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
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        Inventories.readNbt(nbt, items, registries);
        estado = Estado.values()[Math.max(0, Math.min(Estado.values().length - 1, nbt.getInt("estado")))];
        progreso = nbt.getInt("progreso");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) { return createNbt(registries); }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
