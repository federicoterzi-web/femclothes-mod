package com.femclothes.estilado;

import com.femclothes.aplique.Aplique;
import com.femclothes.aplique.Colocacion;
import com.femclothes.aplique.Oscilacion;
import com.femclothes.aplique.ModeloAplique;
import com.femclothes.aplique.MoldeApliqueItem;
import com.femclothes.aplique.ObjetoAplique;
import com.femclothes.aplique.RetazoApliqueItem;
import com.femclothes.garment.Garments;
import com.femclothes.garment.Parte;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesItems;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Mesa de estilado (2026-10-01, "que uno pueda poner cualquier prenda y
 * agregarles modelos 3d anclados en la geometria de la prenda"): tres slots —
 * la PRENDA, el MOLDE de aplique (forma, no se gasta) y el RETAZO (colores, se
 * gasta uno por aplique). En la pantalla se hace click sobre la prenda en la
 * vista 3D y ahí queda el aplique ({@link #poner}); los puestos se eligen de
 * una lista para girarlos, agrandarlos o quitarlos (el retazo vuelve).
 *
 * <p>Se sincroniza al cliente: la vista previa dibuja la prenda del slot con
 * sus apliques al toque.
 */
public class EstiladoBlockEntity extends BlockEntity
        implements GeoBlockEntity, Inventory, ExtendedScreenHandlerFactory<BlockPos>,
        com.femclothes.util.MaquinaCreativa.Cargable {

    /** SLOT_OBJETO (2026-10-04, apliques de objeto): cualquier ítem; si hay uno, el aplique que se pone es ese objeto. */
    public static final int SLOT_PRENDA = 0, SLOT_MOLDE = 1, SLOT_RETAZO = 2, SLOT_OBJETO = 3, TAMANO = 4;

    public static final int BTN_SELECCIONAR_BASE = 0;          // + 0..5
    public static final int BTN_GIRO = 10, BTN_GIRO_ATRAS = 11;
    public static final int BTN_ESCALA = 12, BTN_ESCALA_ATRAS = 13;
    /** Lo atiende el ScreenHandler: necesita al jugador para devolverle el retazo. */
    public static final int BTN_QUITAR = 14;
    /**
     * Pone o saca la textura del Molde de textura que esté en el slot del
     * molde (2026-10-01, relieve: "pongamos un par de moldes de prueba").
     */
    public static final int BTN_TEXTURA = 15;
    /**
     * Mesa creativa (2026-10-01): pasa al siguiente molde (de aplique o de
     * textura) sin tener que tenerlo — la mesa tiene un solo slot de molde,
     * así que "todos los moldes adentro" es poder elegirlos acá.
     */
    public static final int BTN_SIGUIENTE_MOLDE = 16;
    /** Blandura del aplique elegido: BASE + 0..10 = 0..100 % (2026-10-04, "tela blanda... un slider"). */
    public static final int BTN_BLANDURA_BASE = 20;
    public static final int BLANDURA_PASOS = 10;
    /** Aplique de objeto (2026-10-04): Ítem/Bloque, variante del bloque (vela encendida...) e inclinación de a 15°. */
    public static final int BTN_OBJ_MODO = 40, BTN_OBJ_VARIANTE = 41;
    public static final float ESCALA_MIN_OBJETO = 0.25f;
    /** La que trae un aplique recién puesto. */
    public static final float BLANDURA_INICIAL = 0.5f;

    public static final float ESCALA_MIN = 0.5f, ESCALA_MAX = 2.5f, PASO_ESCALA = 0.25f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(TAMANO, ItemStack.EMPTY);
    /** Qué aplique de la prenda editan los botones (-1 = ninguno). */
    private int seleccionado = -1;

    public EstiladoBlockEntity(BlockPos pos, BlockState state) {
        super(EstiladoMod.ESTILADO_BLOCK_ENTITY, pos, state);
    }

    public int seleccionado() { return seleccionado; }

    public List<Aplique> apliques() {
        List<Aplique> a = items.get(SLOT_PRENDA).get(FemclothesComponents.APLIQUES);
        return a == null ? List.of() : a;
    }

    private void guardarApliques(List<Aplique> lista) {
        ItemStack prenda = items.get(SLOT_PRENDA);
        if (lista.isEmpty()) prenda.remove(FemclothesComponents.APLIQUES);
        else prenda.set(FemclothesComponents.APLIQUES, List.copyOf(lista));
        markDirty();
    }

    /**
     * Pone un aplique donde se hizo click (lo valida el servidor): hace falta
     * prenda, molde y retazo, y que la parte sea de la prenda. El punto se
     * acota a la caja de esa parte, por si llega cualquier cosa.
     */
    public boolean poner(Parte parte, float x, float y, float z, Direction cara) {
        return poner(parte, x, y, z, cara, Aplique.Superficie.CAJA);
    }

    /**
     * Con la superficie: en la pollera y la capa (2026-10-02) x, y son la u, v
     * de su tela (px de 64) y la prenda tiene que ser de ese tipo.
     */
    public boolean poner(Parte parte, float x, float y, float z, Direction cara, Aplique.Superficie superficie) {
        ItemStack prenda = items.get(SLOT_PRENDA), molde = items.get(SLOT_MOLDE), retazo = items.get(SLOT_RETAZO);
        ItemStack objeto = items.get(SLOT_OBJETO);
        // Mesa creativa (2026-10-01): el retazo no hace falta ni se gasta (sin retazo, sale blanco).
        boolean gratis = com.femclothes.util.MaquinaCreativa.es(this);
        // Con un objeto en su slot (2026-10-04), el aplique es ese objeto y el molde no cuenta; la muestra de
        // color en el slot del retazo lo tiñe (opcional).
        boolean esObjeto = !objeto.isEmpty();
        if (esObjeto) {
            if (prenda.isEmpty() || !ObjetoAplique.admite(objeto)) return false;
        } else if (prenda.isEmpty() || !(molde.getItem() instanceof MoldeApliqueItem)
                || (!(retazo.getItem() instanceof RetazoApliqueItem) && !gratis)) {
            return false;
        }
        if (!admite(prenda) || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return false;
        List<Aplique> actuales = new ArrayList<>(apliques());
        if (actuales.size() >= Aplique.MAXIMO_POR_PRENDA) return false;
        switch (superficie) {
            case POLLERA -> {
                if (!(prenda.getItem() instanceof com.femclothes.item.PolleraItem)) return false;
                x = MathHelper.clamp(x, 0, 64);
                y = MathHelper.clamp(y, 0, 64);
                z = 0;
            }
            case CAPA -> {
                if (!(prenda.getItem() instanceof com.femclothes.item.CapaItem)) return false;
                x = MathHelper.clamp(x, 0, 64);
                y = MathHelper.clamp(y, 0, 64);
                z = 0;
            }
            default -> {
                x = MathHelper.clamp(x, -8, 8);
                y = MathHelper.clamp(y, -10, 14);
                z = MathHelper.clamp(z, -6, 6);
            }
        }
        if (esObjeto) {
            ItemStack muestra = retazo.getItem() instanceof com.femclothes.item.MuestraColorItem ? retazo : ItemStack.EMPTY;
            actuales.add(new Aplique(ModeloAplique.OBJETO, parte, x, y, z, cara, 0f, 1f, RetazoApliqueItem.BLANCO, superficie,
                    BLANDURA_INICIAL, ObjetoAplique.de(objeto, muestra)));
            if (!gratis) {
                objeto.decrement(1);
                if (!muestra.isEmpty()) muestra.decrement(1);
            }
        } else {
            MoldeApliqueItem m = (MoldeApliqueItem) molde.getItem();
            actuales.add(new Aplique(m.modelo, parte, x, y, z, cara, 0f, 1f, RetazoApliqueItem.colores(retazo), superficie, BLANDURA_INICIAL));
            if (!gratis) retazo.decrement(1);
        }
        seleccionado = actuales.size() - 1;
        guardarApliques(actuales);
        return true;
    }

    /** Saca el aplique elegido y devuelve su retazo (con sus colores). */
    public void quitar(PlayerEntity jugador) {
        List<Aplique> actuales = new ArrayList<>(apliques());
        if (seleccionado < 0 || seleccionado >= actuales.size()) return;
        Aplique a = actuales.remove(seleccionado);
        if (a.objeto() != null) {
            // Vuelve el objeto y la muestra, tal cual (en la Mesa creativa no se gastaron: no se devuelven).
            if (!com.femclothes.util.MaquinaCreativa.es(this)) {
                jugador.getInventory().offerOrDrop(a.objeto().item().copy());
                if (!a.objeto().muestra().isEmpty()) jugador.getInventory().offerOrDrop(a.objeto().muestra().copy());
            }
        } else {
            ItemStack retazo = RetazoApliqueItem.conColores(new ItemStack(FemclothesItems.RETAZO_APLIQUE),
                    a.color(0), a.color(1), a.color(2));
            jugador.getInventory().offerOrDrop(retazo);
        }
        seleccionado = Math.min(seleccionado, actuales.size() - 1);
        guardarApliques(actuales);
    }

    public boolean onButtonClick(int id) {
        if (id == BTN_TEXTURA) return alternarTextura();
        if (id == BTN_SIGUIENTE_MOLDE) return siguienteMolde();
        List<Aplique> actuales = new ArrayList<>(apliques());
        if (id >= BTN_SELECCIONAR_BASE && id < BTN_SELECCIONAR_BASE + Aplique.MAXIMO_POR_PRENDA) {
            int i = id - BTN_SELECCIONAR_BASE;
            if (i >= actuales.size()) return false;
            seleccionado = seleccionado == i ? -1 : i;
            markDirty();
            return true;
        }
        if (seleccionado < 0 || seleccionado >= actuales.size()) return false;
        Aplique a = actuales.get(seleccionado);
        if (id >= BTN_BLANDURA_BASE && id <= BTN_BLANDURA_BASE + BLANDURA_PASOS) {
            actuales.set(seleccionado, a.conBlandura((id - BTN_BLANDURA_BASE) / (float) BLANDURA_PASOS));
            guardarApliques(actuales);
            return true;
        }
        switch (id) {
            case BTN_GIRO -> actuales.set(seleccionado, a.conGiro((Math.round(a.giro()) + 15) % 360));
            case BTN_GIRO_ATRAS -> actuales.set(seleccionado, a.conGiro((Math.round(a.giro()) + 345) % 360));
            case BTN_ESCALA -> actuales.set(seleccionado, a.conEscala(Math.min(ESCALA_MAX, a.escala() + PASO_ESCALA)));
            case BTN_ESCALA_ATRAS -> actuales.set(seleccionado, a.conEscala(
                    Math.max(a.objeto() != null ? ESCALA_MIN_OBJETO : ESCALA_MIN, a.escala() - PASO_ESCALA)));
            case BTN_OBJ_MODO, BTN_OBJ_VARIANTE -> {
                ObjetoAplique o = a.objeto();
                if (o == null) return false;
                switch (id) {
                    case BTN_OBJ_MODO -> {
                        if (ObjetoAplique.bloqueDe(o.item()) == null) return false;
                        o = o.conBloque(!o.bloque()).conVariante(0);
                    }
                    case BTN_OBJ_VARIANTE -> {
                        if (!o.bloque() || o.cantidadDeVariantes() < 2) return false;
                        o = o.conVariante((o.variante() + 1) % o.cantidadDeVariantes());
                    }
                    default -> { return false; }
                }
                actuales.set(seleccionado, a.conObjeto(o));
            }
            default -> { return false; }
        }
        guardarApliques(actuales);
        return true;
    }

    /** Con un Molde de textura: si la prenda ya la tiene, se la saca; si no, se la pone. El molde no se gasta. */
    /**
     * Ajusta la colocación, la oscilación y la blandura del aplique {@code indice} (2026-10-04, panel lateral de
     * la Mesa): llega por {@code AjustarApliquePayload} con valores ya acotados por los records. Para los modelos
     * del mod la cara base no cuenta (siempre se apoyan por atrás).
     */
    public boolean ajustar(int indice, Colocacion colocacion, Oscilacion oscilacion, float blandura) {
        List<Aplique> actuales = new ArrayList<>(apliques());
        if (indice < 0 || indice >= actuales.size() || !Float.isFinite(blandura)) return false;
        Aplique a = actuales.get(indice);
        if (a.objeto() == null) colocacion = colocacion.conCara(Colocacion.DEFECTO.caraBase());
        actuales.set(indice, a.conColocacion(colocacion).conOscilacion(oscilacion)
                .conBlandura(MathHelper.clamp(blandura, 0f, 1f)));
        guardarApliques(actuales);
        return true;
    }

    private boolean alternarTextura() {
        ItemStack prenda = items.get(SLOT_PRENDA);
        if (prenda.isEmpty() || !(items.get(SLOT_MOLDE).getItem() instanceof com.femclothes.item.MoldeTexturaItem m)) return false;
        if (prenda.get(FemclothesComponents.TEXTURA_TELA) == m.textura) prenda.remove(FemclothesComponents.TEXTURA_TELA);
        else prenda.set(FemclothesComponents.TEXTURA_TELA, m.textura);
        markDirty();
        return true;
    }

    /** Todos los moldes que entran en la mesa, en el orden del registro. */
    private static List<net.minecraft.item.Item> moldes() {
        List<net.minecraft.item.Item> lista = new ArrayList<>();
        for (net.minecraft.item.Item item : net.minecraft.registry.Registries.ITEM) {
            if (item instanceof MoldeApliqueItem || item instanceof com.femclothes.item.MoldeTexturaItem) lista.add(item);
        }
        return lista;
    }

    private boolean siguienteMolde() {
        if (!com.femclothes.util.MaquinaCreativa.es(this)) return false;
        List<net.minecraft.item.Item> lista = moldes();
        if (lista.isEmpty()) return false;
        int i = lista.indexOf(items.get(SLOT_MOLDE).getItem());
        items.set(SLOT_MOLDE, new ItemStack(lista.get((i + 1) % lista.size())));
        markDirty();
        return true;
    }

    /** La mesa creativa viene con el primer molde puesto (los demás, con el botón). */
    @Override
    public void cargarCreativa() {
        if (items.get(SLOT_MOLDE).isEmpty() && !moldes().isEmpty()) {
            items.set(SLOT_MOLDE, new ItemStack(moldes().get(0)));
            markDirty();
        }
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────
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
        ItemStack r = Inventories.splitStack(items, slot, amount);
        if (!r.isEmpty()) {
            if (slot == SLOT_PRENDA) seleccionado = -1;
            markDirty();
        }
        return r;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack r = Inventories.removeStack(items, slot);
        if (slot == SLOT_PRENDA) seleccionado = -1;
        markDirty();
        return r;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack antes = items.get(slot);
        items.set(slot, stack);
        // Solo se suelta la selección si cambió la PRENDA (2026-10-04, "cuando clickeo algo del panel lateral se me
        // deselecciona"): el servidor reenvía el slot cada vez que se ajusta un aplique (la prenda cambia de
        // componentes) y ese reenvío no es una prenda nueva.
        if (slot == SLOT_PRENDA && (antes.isEmpty() || stack.isEmpty() || !ItemStack.areItemsEqual(antes, stack))) {
            seleccionado = -1;
        }
        if (seleccionado >= apliques().size()) seleccionado = apliques().size() - 1;
        markDirty();
    }

    /**
     * Qué se puede estilar (2026-10-02, "extender apliques para toda armadura
     * o wearable vanilla o de mods"): las prendas del mod y cualquier ítem que
     * se pone — armaduras vanilla y de mods ({@code Equipment}: cascos,
     * pecheras, pantalones, botas, cabezas, élitros) y los de Trinkets.
     */
    public static boolean admite(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (Garments.esPrenda(stack)) return true;
        if (net.minecraft.item.Equipment.fromStack(stack) != null) return true;
        return stack.getItem() instanceof dev.emi.trinkets.api.Trinket;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_PRENDA -> admite(stack);
            case SLOT_MOLDE -> stack.getItem() instanceof MoldeApliqueItem
                    || stack.getItem() instanceof com.femclothes.item.MoldeTexturaItem;
            case SLOT_RETAZO -> stack.getItem() instanceof RetazoApliqueItem
                    || stack.getItem() instanceof com.femclothes.item.MuestraColorItem;
            case SLOT_OBJETO -> ObjetoAplique.admite(stack);
            default -> false;
        };
    }

    @Override
    public int getMaxCount(ItemStack stack) {
        return stack.getItem() instanceof RetazoApliqueItem ? 64 : 1;
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null && !world.isClient) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
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
        return new EstiladoScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
    }

    // ── persistencia y sincronización ─────────────────────────────────────
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        Inventories.writeNbt(nbt, items, registries);
        nbt.putInt("Seleccionado", seleccionado);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        items.clear();
        Inventories.readNbt(nbt, items, registries);
        seleccionado = nbt.contains("Seleccionado") ? nbt.getInt("Seleccionado") : -1;
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
