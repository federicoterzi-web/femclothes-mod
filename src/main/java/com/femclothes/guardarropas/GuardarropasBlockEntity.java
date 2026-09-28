package com.femclothes.guardarropas;

import com.femclothes.item.CalientabrazosItem;
import com.femclothes.item.FemclothesItems;
import com.femclothes.item.PantalonItem;
import com.femclothes.sublimadora.RemeraItem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardarropas (2026-09-20, "quiero que el guardarropas me permita
 * combinar un croptop con un remeron largo de medias red o un pantalon
 * con una pollera y una calza"): cada categoría (remera/pantalón/medias/
 * calientabrazos) tiene {@link #POR_CATEGORIA} slots propios en vez de
 * uno solo — así entran varias prendas de la MISMA categoría a la vez
 * (croptop + remerón largo, o pantalón + pollera + calza) y de verdad
 * se pueden llevar puestas juntas, no solo previsualizar.
 *
 * <p>Esto necesitó subir {@code amount} de 1 a {@link #POR_CATEGORIA} en
 * las 4 definiciones de slot de Trinkets ({@code data/trinkets/slots/
 * torso/prenda.json} y las otras 3) — antes "torso/prenda" era un slot
 * único y mutuamente excluyente a propósito (ver el comentario viejo de
 * ese json, "dos prendas... se pisarian... mutuamente excluyentes"); a
 * pedido expreso ("hay q modificar algo en el trinkets? si hacelo") esa
 * exclusividad se levantó. El renderer ({@link com.femclothes.render.GarmentFeatureRenderer#equipadas})
 * ya recorre {@code TrinketComponent#getAllEquipped()} (TODO lo puesto en
 * TODOS los slots, no solo el índice 0), así que no necesitó ningún
 * cambio para dibujar varias prendas de una misma categoría a la vez —
 * el orden entre ellas ya lo decide {@code Capa}, como con cualquier otro
 * par de prendas de distinta categoría.
 */
public class GuardarropasBlockEntity extends BlockEntity implements Inventory, ExtendedScreenHandlerFactory<BlockPos> {

    public static final int POR_CATEGORIA = 4;

    public static final int REMERA = 0;
    public static final int PANTALON = 1;
    public static final int MEDIAS = 2;
    public static final int CALIENTABRAZOS = 3;
    public static final int CATEGORIAS = 4;

    public static final int TAMANO = CATEGORIAS * POR_CATEGORIA;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);

    private static int base(int categoria) { return categoria * POR_CATEGORIA; }

    /** Un outfit guardado: hasta {@link #TAMANO} prendas (4 categorías x 4 capas), cualquiera puede estar vacía. */
    public record OutfitFijado(List<ItemStack> prendas) {
        public ItemStack de(int slot) { return prendas.get(slot); }
    }

    public static final int FIJADAS_MAXIMO = 8;
    private final List<OutfitFijado> fijadas = new ArrayList<>();
    private int fijadaSeleccionada = -1;

    public GuardarropasBlockEntity(BlockPos pos, BlockState state) {
        super(GuardarropasMod.GUARDARROPAS_BLOCK_ENTITY, pos, state);
    }

    public List<OutfitFijado> fijadas() { return fijadas; }
    public int fijadaSeleccionada() { return fijadaSeleccionada; }

    /** Guarda el borrador actual (las {@link #TAMANO} prendas) como un outfit nuevo. */
    private void fijar() {
        if (fijadas.size() >= FIJADAS_MAXIMO) return;
        List<ItemStack> copia = new ArrayList<>(TAMANO);
        for (ItemStack s : items) copia.add(s.copy());
        fijadas.add(new OutfitFijado(copia));
        fijadaSeleccionada = fijadas.size() - 1;
        markDirty();
    }

    /**
     * Click en un outfit guardado: si NO es el seleccionado, carga sus
     * prendas al borrador (mismo criterio que Tinturas/Sublimadora:
     * clickear el ya seleccionado lo vacía en vez de recargarlo).
     */
    private boolean aplicarOQuitarFijada(int idx) {
        if (idx < 0 || idx >= fijadas.size()) return false;
        if (idx == fijadaSeleccionada) {
            for (int i = 0; i < TAMANO; i++) items.set(i, ItemStack.EMPTY);
            fijadaSeleccionada = -1;
        } else {
            OutfitFijado f = fijadas.get(idx);
            for (int i = 0; i < TAMANO; i++) items.set(i, f.de(i).copy());
            fijadaSeleccionada = idx;
        }
        markDirty();
        return true;
    }

    public static final int BTN_FIJAR = 0;
    public static final int BTN_FIJADA_BASE = 1; // .. + FIJADAS_MAXIMO
    public static final int BTN_EQUIPAR = BTN_FIJADA_BASE + FIJADAS_MAXIMO;

    /**
     * {@code BTN_EQUIPAR} NO pasa por acá — necesita el {@link PlayerEntity}
     * de verdad (para devolver lo reemplazado a SU inventario), y este
     * método no lo tiene. Lo maneja {@code GuardarropasScreenHandler
     * #onButtonClick} directo, llamando a {@link #equiparEn}.
     */
    public boolean onButtonClick(int id) {
        if (id == BTN_FIJAR) {
            fijar();
            return true;
        }
        if (id >= BTN_FIJADA_BASE && id < BTN_FIJADA_BASE + FIJADAS_MAXIMO) {
            return aplicarOQuitarFijada(id - BTN_FIJADA_BASE);
        }
        return false;
    }

    private static final String[] GRUPO = {"torso", "piernas", "socks", "arms"};
    private static final String[] NOMBRE_SLOT = {"prenda", "exterior", "pair", "armwarmer"};

    /**
     * Pone de verdad las prendas del borrador en los slots reales de
     * Trinkets del jugador — a pedido (2026-09-20, "si sino no me sirve").
     * Cada categoría tiene {@link #POR_CATEGORIA} slots tanto acá como en
     * Trinkets (mismo índice: el slot de capa N del borrador va al slot N
     * de Trinkets de esa categoría), así que no hace falta compactar ni
     * elegir dónde va cada una.
     *
     * <p>Una capa vacía en el borrador NO desequipa lo que el jugador ya
     * tenía puesto ahí. Lo que SÍ reemplaza vuelve al inventario del
     * jugador (o se tira si no entra), y la prenda usada se saca del
     * borrador — ya está puesta, no puede seguir "guardada" acá también.
     */
    public void equiparEn(PlayerEntity player) {
        dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).ifPresent(componente -> {
            for (int categoria = 0; categoria < CATEGORIAS; categoria++) {
                equiparCategoria(componente, GRUPO[categoria], NOMBRE_SLOT[categoria], categoria, player);
            }
        });
    }

    private void equiparCategoria(dev.emi.trinkets.api.TrinketComponent componente, String grupo, String nombreSlot,
                                   int categoria, PlayerEntity player) {
        var grupoDeSlots = componente.getInventory().get(grupo);
        if (grupoDeSlots == null) return;
        dev.emi.trinkets.api.TrinketInventory inv = grupoDeSlots.get(nombreSlot);
        if (inv == null) return;

        int slotsReales = Math.min(inv.size(), POR_CATEGORIA);
        for (int capa = 0; capa < slotsReales; capa++) {
            int slotBorrador = base(categoria) + capa;
            ItemStack nueva = items.get(slotBorrador);
            if (nueva.isEmpty()) continue;

            ItemStack anterior = inv.getStack(capa);
            if (!anterior.isEmpty()) player.getInventory().offerOrDrop(anterior.copy());
            inv.setStack(capa, nueva.copy());

            items.set(slotBorrador, ItemStack.EMPTY);
        }
        markDirty();
    }

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

    @Override public ItemStack removeStack(int slot) { return Inventories.removeStack(items, slot); }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
        markDirty();
    }

    /** Las 4 capas de una misma categoría comparten el mismo chequeo de tipo — el slot solo cambia en qué capa queda. */
    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot / POR_CATEGORIA) {
            case REMERA -> stack.getItem() instanceof RemeraItem;
            case PANTALON -> stack.getItem() instanceof PantalonItem;
            case MEDIAS -> stack.isOf(FemclothesItems.SOCKS_SOLID);
            case CALIENTABRAZOS -> stack.getItem() instanceof CalientabrazosItem;
            default -> false;
        };
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Text getDisplayName() {
        return getCachedState().getBlock().getName();
    }

    @Override
    @Nullable
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GuardarropasScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);

        NbtList fijadasNbt = new NbtList();
        for (OutfitFijado f : fijadas) {
            // ItemStack.encode() tira IllegalStateException con un stack
            // vacío ("Cannot encode empty ItemStack") — y la mayoría de las
            // 16 posiciones de un outfit típicamente lo están. Guardar un
            // NbtCompound vacío en su lugar: fromNbtOrEmpty (en readNbt) ya
            // devuelve ItemStack.EMPTY para un compound sin "id"/"count".
            // Bug real (2026-09-20): "Failed to save chunk" repetido en el
            // log apenas se fijaba el primer outfit.
            NbtList prendasNbt = new NbtList();
            for (ItemStack s : f.prendas()) prendasNbt.add(s.isEmpty() ? new NbtCompound() : s.encode(registries));
            NbtCompound fc = new NbtCompound();
            fc.put("Prendas", prendasNbt);
            fijadasNbt.add(fc);
        }
        nbt.put("Outfits", fijadasNbt);
        nbt.putInt("OutfitSeleccionado", fijadaSeleccionada);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        items.clear();
        Inventories.readNbt(nbt, items, registries);

        fijadas.clear();
        if (nbt.contains("Outfits")) {
            NbtList lista = nbt.getList("Outfits", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < lista.size(); i++) {
                NbtCompound fc = lista.getCompound(i);
                List<ItemStack> prendas = new ArrayList<>(TAMANO);
                NbtList prendasNbt = fc.getList("Prendas", net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
                for (int j = 0; j < TAMANO; j++) {
                    prendas.add(j < prendasNbt.size()
                            ? ItemStack.fromNbtOrEmpty(registries, prendasNbt.getCompound(j))
                            : ItemStack.EMPTY);
                }
                fijadas.add(new OutfitFijado(prendas));
            }
        }
        fijadaSeleccionada = nbt.contains("OutfitSeleccionado") ? nbt.getInt("OutfitSeleccionado") : -1;
    }
}
