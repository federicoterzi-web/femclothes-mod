package com.femclothes.cinta;

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
    /** Hora del mundo en que la señal de redstone la congeló, o -1 (2026-10-05, "dejan de circular items con señal de redstone"). */
    private long pausadoDesde = -1;

    public CintaBlockEntity(BlockPos pos, BlockState state) {
        super(CintaMod.CINTA_BLOCK_ENTITY, pos, state);
    }

    public ItemStack carga() { return carga; }

    public long llegada() { return llegada; }

    /** La hora (con el cuarto de tick) que cuenta para el avance: la de la pausa si está congelada, la del mundo si no. */
    public float tiempoEfectivo(float tickDelta) {
        if (pausadoDesde >= 0) return pausadoDesde;
        return world == null ? 0f : world.getTime() + tickDelta;
    }

    public static int ticksDe(BlockState estado) {
        return estado.get(CintaBlock.FORMA).esCurva() ? TICKS_CURVA : TICKS_RECTA;   // la rampa va a la misma velocidad de banda
    }

    /** Servidor: cuando la prenda llegó al final, la empuja al inventario de enfrente (si no puede, espera). */
    public static void tick(World world, BlockPos pos, BlockState state, CintaBlockEntity be) {
        // La forma se vuelve a mirar sola cada tanto (2026-10-05, "el recalculado que se haga automatico al updatear
        // bloques cercanos"): una máquina o una cinta puesta en diagonal después no avisa por actualización de vecinos.
        if ((world.getTime() + pos.asLong()) % 20 == 0) {
            CintaBlock.recalcular(world, pos, state);
            state = world.getBlockState(pos);
            boolean debe = CintaFisica.debePausar(world, pos);   // por si se perdió un aviso de la cadena
            if (debe != state.get(CintaBlock.POWERED)) {
                state = state.with(CintaBlock.POWERED, debe);
                world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }
        // Señal de redstone: la prenda se congela donde está y al soltarse sigue desde ahí.
        boolean pausada = state.get(CintaBlock.POWERED);
        if (pausada && be.pausadoDesde < 0) {
            be.pausadoDesde = world.getTime();
            be.sincronizar();
        } else if (!pausada && be.pausadoDesde >= 0) {
            be.llegada += world.getTime() - be.pausadoDesde;
            be.pausadoDesde = -1;
            be.sincronizar();
        }
        if (pausada) return;
        if (be.carga.isEmpty()) return;
        if (world.getTime() - be.llegada < ticksDe(state)) return;
        Direction frente = state.get(CintaBlock.FACING);
        BlockPos destino = pos.offset(frente);
        CintaBlock.Forma forma = state.get(CintaBlock.FORMA);
        if (forma == CintaBlock.Forma.RAMPA_SUBE) {
            destino = destino.up();
        } else if (!(world.getBlockEntity(destino) instanceof net.minecraft.inventory.Inventory)) {
            // Adelante no hay nada: si abajo hay una rampa de bajada que mira igual, se la entrega a ella.
            BlockState abajo = world.getBlockState(destino.down());
            if (abajo.getBlock() instanceof CintaBlock && abajo.get(CintaBlock.FORMA) == CintaBlock.Forma.RAMPA_BAJA
                    && abajo.get(CintaBlock.FACING) == frente) {
                destino = destino.down();
            }
        }
        ItemStack resto = InventarioUtil.empujarA(world, destino, frente.getOpposite(), be.carga);
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
        pausadoDesde = getCachedState().get(CintaBlock.POWERED) && world != null ? world.getTime() : -1;
        sincronizar();
    }

    @Override public int getMaxCountPerStack() { return 1; }

    @Override public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) { return false; }

    @Override public void clear() { carga = ItemStack.EMPTY; sincronizar(); }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        // Cualquier ítem (2026-10-05, "que no solo transporte ropa"); lleva de a uno.
        return carga.isEmpty() && !stack.isEmpty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        // Todo menos el frente (por ahí sale) y el fondo.
        if (side == Direction.DOWN || side == getCachedState().get(CintaBlock.FACING)) return new int[0];
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        // Congelada por redstone, no recibe nada por automatización.
        return !getCachedState().get(CintaBlock.POWERED) && isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return false; }

    // ── NBT y sincronización con el cliente ──

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!carga.isEmpty()) nbt.put("Carga", carga.encode(registries));
        nbt.putLong("Llegada", llegada);
        nbt.putLong("PausadoDesde", pausadoDesde);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        carga = nbt.contains("Carga") ? ItemStack.fromNbtOrEmpty(registries, nbt.getCompound("Carga")) : ItemStack.EMPTY;
        llegada = nbt.getLong("Llegada");
        pausadoDesde = nbt.contains("PausadoDesde") ? nbt.getLong("PausadoDesde") : -1;
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
