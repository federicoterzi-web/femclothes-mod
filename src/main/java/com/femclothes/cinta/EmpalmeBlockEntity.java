package com.femclothes.cinta;

import com.femclothes.util.InventarioUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Un ítem por vez (cualquiera), entra por atrás, izquierda, derecha o arriba y sale por el frente. El turno rota:
 * después de aceptar por una entrada, la siguiente con algo esperando pasa primero.
 */
public class EmpalmeBlockEntity extends BlockEntity implements SidedInventory {

    public static final int TICKS = 8;

    private ItemStack carga = ItemStack.EMPTY;
    private long llegada;
    private long pausadoDesde = -1;
    /** Índice en {@link #entradas} de la entrada con prioridad. */
    private int turno;
    /** La cara por la que se consultó la última inserción (el setStack que sigue es de esa entrada). */
    private Direction ultimaEntrada;

    public EmpalmeBlockEntity(BlockPos pos, BlockState state) {
        super(CintaMod.EMPALME_BLOCK_ENTITY, pos, state);
    }

    public ItemStack carga() { return carga; }

    public long llegada() { return llegada; }

    public float tiempoEfectivo(float tickDelta) {
        if (pausadoDesde >= 0) return pausadoDesde;
        return world == null ? 0f : world.getTime() + tickDelta;
    }

    private Direction[] entradas() {
        Direction f = getCachedState().get(EmpalmeBlock.FACING);
        return new Direction[]{f.getOpposite(), f.rotateYCounterclockwise(), f.rotateYClockwise(), Direction.UP};
    }

    private boolean hayAlgoEsperando(Direction lado) {
        if (world == null) return false;
        Inventory inv = world.getBlockEntity(pos.offset(lado)) instanceof Inventory i ? i : null;
        return inv != null && !inv.isEmpty();
    }

    public static void tick(World world, BlockPos pos, BlockState state, EmpalmeBlockEntity be) {
        boolean pausada = state.get(EmpalmeBlock.POWERED);
        if (pausada && be.pausadoDesde < 0) {
            be.pausadoDesde = world.getTime();
            be.sincronizar();
        } else if (!pausada && be.pausadoDesde >= 0) {
            be.llegada += world.getTime() - be.pausadoDesde;
            be.pausadoDesde = -1;
            be.sincronizar();
        }
        if (pausada || be.carga.isEmpty() || world.getTime() - be.llegada < TICKS) return;
        Direction frente = state.get(EmpalmeBlock.FACING);
        BlockPos destino = pos.offset(frente);
        BlockState abajo = world.getBlockState(destino.down());
        if (!(world.getBlockEntity(destino) instanceof Inventory) && abajo.getBlock() instanceof CintaBlock
                && abajo.get(CintaBlock.FORMA) == CintaBlock.Forma.RAMPA_BAJA && abajo.get(CintaBlock.FACING) == frente) {
            destino = destino.down();
        }
        ItemStack resto = InventarioUtil.empujarA(world, destino, frente.getOpposite(), be.carga);
        if (resto.getCount() != be.carga.getCount()) {
            be.carga = resto;
            be.sincronizar();
        }
    }

    private void sincronizar() {
        markDirty();
        if (world != null && !world.isClient) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override public int size() { return 1; }
    @Override public boolean isEmpty() { return carga.isEmpty(); }
    @Override public ItemStack getStack(int slot) { return carga; }
    @Override public ItemStack removeStack(int slot, int cantidad) { return removeStack(slot); }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack r = carga;
        carga = ItemStack.EMPTY;
        if (!r.isEmpty()) sincronizar();
        return r;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        carga = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        llegada = world != null ? world.getTime() : 0;
        if (ultimaEntrada != null && !carga.isEmpty()) aceptoPor(ultimaEntrada);
        ultimaEntrada = null;
        pausadoDesde = getCachedState().get(EmpalmeBlock.POWERED) && world != null ? world.getTime() : -1;
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) { return false; }
    @Override public void clear() { carga = ItemStack.EMPTY; sincronizar(); }
    @Override public boolean isValid(int slot, ItemStack stack) { return carga.isEmpty() && !stack.isEmpty(); }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN || side == getCachedState().get(EmpalmeBlock.FACING)) return new int[0];
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (!isValid(slot, stack) || getCachedState().get(EmpalmeBlock.POWERED)) return false;
        if (dir == null) return true;
        ultimaEntrada = dir;
        // Rotativo: si la entrada con el turno tiene algo esperando, pasa ella; si no, cualquiera.
        Direction[] e = entradas();
        Direction prioritaria = e[turno % e.length];
        return dir == prioritaria || !hayAlgoEsperando(prioritaria);
    }

    /** Al aceptar algo por una entrada, el turno pasa a la siguiente. */
    public void aceptoPor(Direction dir) {
        Direction[] e = entradas();
        for (int i = 0; i < e.length; i++) if (e[i] == dir) turno = (i + 1) % e.length;
    }

    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return false; }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!carga.isEmpty()) nbt.put("Carga", carga.encode(registries));
        nbt.putLong("Llegada", llegada);
        nbt.putLong("PausadoDesde", pausadoDesde);
        nbt.putInt("Turno", turno);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        carga = nbt.contains("Carga") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Carga")) : ItemStack.EMPTY;
        llegada = nbt.getLong("Llegada");
        pausadoDesde = nbt.contains("PausadoDesde") ? nbt.getLong("PausadoDesde") : -1;
        turno = nbt.getInt("Turno");
    }

    @Override public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) { return createNbt(registries); }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
