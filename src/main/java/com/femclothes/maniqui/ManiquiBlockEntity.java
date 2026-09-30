package com.femclothes.maniqui;

import com.femclothes.guardarropas.GuardarropasBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Maniquí (2026-09-30, "guarda y muestra la ropa... con 16 slots, 4 por
 * prenda"): mismo reparto de slots que el Guardarropas (4 categorías x 4
 * capas, ver {@link GuardarropasBlockEntity#categoriaDe}), pero en vez de
 * un borrador para equiparse, la ropa queda PUESTA en la figura del modelo
 * {@code mannequin} y se ve en el mundo — la dibuja {@code ManiquiRenderer}
 * con el mismo código que la ropa del jugador.
 *
 * <p>El plato gira si {@link #girando()} (click derecho agachado o botón de
 * la pantalla). El ángulo NO viaja por red: cada cliente lo acumula por su
 * cuenta ({@link #anguloVisible}), así que al detenerlo la figura se queda
 * donde estaba en vez de saltar al frente.
 *
 * <p>Se sincroniza al cliente ({@link #toUpdatePacket}) a diferencia del
 * Guardarropas: la ropa tiene que verse aunque nadie tenga la pantalla
 * abierta.
 */
public class ManiquiBlockEntity extends BlockEntity
        implements Inventory, ExtendedScreenHandlerFactory<BlockPos>, GeoBlockEntity {

    public static final int POR_CATEGORIA = GuardarropasBlockEntity.POR_CATEGORIA;
    public static final int CATEGORIAS = GuardarropasBlockEntity.CATEGORIAS;
    public static final int TAMANO = GuardarropasBlockEntity.TAMANO;

    public static final int BTN_GIRAR = 0;
    public static final int BTN_INTERCAMBIAR = 1;

    /** Una vuelta entera cada 14 s — lo mismo que {@code animation.mannequin.girar} del zip. */
    public static final float TICKS_POR_VUELTA = 14 * 20;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    private boolean girando = false;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ── solo cliente: ángulo acumulado del plato ─────────────────────────
    private float anguloVisible = 0f;
    private double ultimoTiempo = Double.NaN;

    public ManiquiBlockEntity(BlockPos pos, BlockState state) {
        super(ManiquiMod.MANIQUI_BLOCK_ENTITY, pos, state);
    }

    public boolean girando() { return girando; }

    public void alternarGiro() {
        girando = !girando;
        markDirty();
    }

    /**
     * Avanza el ángulo del plato hasta {@code tiempo} (ticks del mundo +
     * tickDelta) y lo devuelve en radianes. Lo llama el renderer una vez
     * por frame ANTES de dibujar, así el hueso {@code turntable} y la ropa
     * usan el mismo valor.
     */
    public float avanzarAngulo(double tiempo) {
        if (!Double.isNaN(ultimoTiempo) && girando) {
            double delta = Math.max(0, tiempo - ultimoTiempo);
            anguloVisible = (float) ((anguloVisible + delta * (Math.PI * 2) / TICKS_POR_VUELTA) % (Math.PI * 2));
        }
        ultimoTiempo = tiempo;
        return anguloVisible;
    }

    public float anguloVisible() { return anguloVisible; }

    /** Las prendas puestas, sin huecos — lo que dibuja el renderer. */
    public List<ItemStack> prendasPuestas() {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : items) if (!s.isEmpty()) out.add(s);
        return out;
    }

    /**
     * Click derecho con una prenda en la mano: la pone en la primera capa
     * libre de su categoría. Devuelve false si no es prenda o no queda lugar.
     */
    public boolean ponerPrenda(ItemStack stack) {
        int categoria = GuardarropasBlockEntity.categoriaDe(stack);
        if (categoria < 0) return false;
        for (int capa = 0; capa < POR_CATEGORIA; capa++) {
            int slot = categoria * POR_CATEGORIA + capa;
            if (items.get(slot).isEmpty()) {
                setStack(slot, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    /**
     * Intercambia la ropa del maniquí con la que el jugador tiene puesta en
     * Trinkets, capa por capa (slot N del maniquí ↔ slot N de Trinkets de
     * esa categoría, mismo mapeo que {@code GuardarropasBlockEntity#equiparEn}).
     * Nada se pierde: lo que estaba en cada lado pasa al otro, vacíos incluidos.
     */
    public void intercambiarCon(PlayerEntity player) {
        dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).ifPresent(componente -> {
            for (int categoria = 0; categoria < CATEGORIAS; categoria++) {
                dev.emi.trinkets.api.TrinketInventory inv =
                        GuardarropasBlockEntity.inventarioTrinkets(componente, categoria);
                if (inv == null) continue;
                int capas = Math.min(inv.size(), POR_CATEGORIA);
                for (int capa = 0; capa < capas; capa++) {
                    int slot = categoria * POR_CATEGORIA + capa;
                    ItemStack delJugador = inv.getStack(capa).copy();
                    ItemStack delManiqui = items.get(slot).copy();
                    inv.setStack(capa, delManiqui);
                    items.set(slot, delJugador);
                }
            }
        });
        markDirty();
    }

    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == BTN_GIRAR) {
            alternarGiro();
            return true;
        }
        if (id == BTN_INTERCAMBIAR) {
            intercambiarCon(player);
            return true;
        }
        return false;
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────
    /**
     * Sin controladores a propósito: el giro del hueso {@code turntable} lo
     * pone {@code ManiquiGeoModel} a mano con {@link #anguloVisible}, para
     * que la ropa (que no es parte del modelo GeckoLib) gire exactamente igual.
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ── Inventory ─────────────────────────────────────────────────────────
    @Override public int size() { return TAMANO; }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
        markDirty();
    }

    /** Una prenda por capa — el maniquí la tiene puesta, no apilada. */
    @Override
    public int getMaxCountPerStack() { return 1; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        int categoria = GuardarropasBlockEntity.categoriaDe(stack);
        return categoria >= 0 && categoria == slot / POR_CATEGORIA;
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    /** Además de guardar, avisa a los clientes: la ropa se ve en el mundo, no solo en la pantalla. */
    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    // ── pantalla ──────────────────────────────────────────────────────────
    @Override
    public Text getDisplayName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    @Nullable
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ManiquiScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    // ── persistencia y sincronización ─────────────────────────────────────
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        // Los slots vacíos no se escriben: el clear() del readNbt de abajo
        // ya los deja vacíos del lado del cliente.
        Inventories.writeNbt(nbt, items, registries);
        nbt.putBoolean("Girando", girando);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        items.clear();
        Inventories.readNbt(nbt, items, registries);
        girando = nbt.getBoolean("Girando");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
