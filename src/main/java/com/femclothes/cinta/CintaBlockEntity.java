package com.femclothes.cinta;

import com.femclothes.item.FemclothesDye;
import com.femclothes.util.InventarioUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * La prenda que viaja por una cinta. Se guarda cuándo subió ({@link #llegada},
 * hora del mundo): el servidor la entrega al pasar {@link #ticksDe}, y el
 * cliente calcula la posición con esa misma hora, así no hace falta mandarle
 * el avance cada tick.
 */
public class CintaBlockEntity extends BlockEntity implements SidedInventory {

    /** 1 px por tick, igual que el avance de la banda animada. */
    public static final int TICKS_RECTA = 16;
    /** Arco de radio 8 px = 12,6 px de recorrido a 1 px por tick. */
    public static final int TICKS_CURVA = 13;

    private ItemStack carga = ItemStack.EMPTY;
    private long llegada;

    public CintaBlockEntity(BlockPos pos, BlockState state) {
        super(CintaMod.CINTA_BLOCK_ENTITY, pos, state);
    }

    public ItemStack carga() { return carga; }

    public long llegada() { return llegada; }

    public static int ticksDe(BlockState estado) {
        return estado.get(CintaBlock.FORMA) == CintaBlock.Forma.RECTA ? TICKS_RECTA : TICKS_CURVA;
    }

    /** Servidor: cuando la prenda llegó al final, la empuja al inventario de enfrente (si no puede, espera). */
    public static void tick(World world, BlockPos pos, BlockState state, CintaBlockEntity be) {
        if (be.carga.isEmpty()) return;
        if (world.getTime() - be.llegada < ticksDe(state)) return;
        Direction frente = state.get(CintaBlock.FACING);
        ItemStack resto = InventarioUtil.empujarA(world, pos.offset(frente), frente.getOpposite(), be.carga);
        if (resto.getCount() != be.carga.getCount()) {
            be.carga = resto;
            be.sincronizar();
        }
    }

    private void sincronizar() {
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    // ── Inventory (un slot, una prenda): lo usan las máquinas y tolvas vía InventarioUtil ──

    @Override public int size() { return 1; }

    @Override public boolean isEmpty() { return carga.isEmpty(); }

    @Override public ItemStack getStack(int slot) { return carga; }

    @Override
    public ItemStack removeStack(int slot, int cantidad) {
        return removeStack(slot);
    }

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
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 1; }

    @Override public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) { return false; }

    @Override public void clear() { carga = ItemStack.EMPTY; sincronizar(); }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return carga.isEmpty() && FemclothesDye.isClothing(stack);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // Todo menos el frente (por ahí sale) y el fondo.
        if (side == Direction.DOWN || side == getCachedState().get(CintaBlock.FACING)) return new int[0];
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return false; }

    // ── NBT y sincronización con el cliente ──

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!carga.isEmpty()) nbt.put("Carga", carga.encode(registries));
        nbt.putLong("Llegada", llegada);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        carga = nbt.contains("Carga") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Carga")) : ItemStack.EMPTY;
        llegada = nbt.getLong("Llegada");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public net.minecraft.network.packet.Packet<net.minecraft.network.listener.ClientPlayPacketListener> toUpdatePacket() {
        return net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket.create(this);
    }
}
