package com.modamod.guardarropas;

import com.modamod.item.CalientabrazosItem;
import com.modamod.item.ModamodItems;
import com.modamod.item.PantalonItem;
import com.modamod.item.PolleraItem;
import com.modamod.sublimadora.RemeraItem;
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

import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

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
 * exclusividad se levantó. El renderer ({@link com.modamod.render.GarmentFeatureRenderer#equipadas})
 * ya recorre {@code TrinketComponent#getAllEquipped()} (TODO lo puesto en
 * TODOS los slots, no solo el índice 0), así que no necesitó ningún
 * cambio para dibujar varias prendas de una misma categoría a la vez —
 * el orden entre ellas ya lo decide {@code Capa}, como con cualquier otro
 * par de prendas de distinta categoría.
 */
public class GuardarropasBlockEntity extends BlockEntity
        implements net.minecraft.inventory.SidedInventory, ExtendedScreenHandlerFactory<BlockPos>, GeoBlockEntity {

    public static final int POR_CATEGORIA = 3;

    public static final int REMERA = 0;
    public static final int PANTALON = 1;
    public static final int MEDIAS = 2;
    public static final int CALIENTABRAZOS = 3;
    /**
     * Chaquetas (2026-09-30): quinta categoría, al FINAL — sus 4 slots van
     * del 16 al 19, así los guardados de antes (0..15) no se corren.
     */
    public static final int CHAQUETA = 4;
    public static final int CATEGORIAS = 5;

    /** Slots de prenda (5 categorías x 4 capas); la armadura va después. */
    public static final int PRENDAS = CATEGORIAS * POR_CATEGORIA;

    /**
     * Columna de armadura (2026-09-30, "agreguemos una columna de slots de
     * armadura"): casco, pechera, pantalones, botas — al FINAL (20..23), así
     * los índices de las prendas no se corren. Entra en los outfits guardados
     * y "Equipar" la pone en los slots de armadura del jugador.
     */
    public static final int ARMADURA_INICIO = PRENDAS;
    public static final net.minecraft.entity.EquipmentSlot[] SLOTS_ARMADURA = {
            net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST,
            net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET,
    };

    /**
     * Sombrero de bruja (2026-10-05, "segunda tanda del sombrero... Maniquí/Guardarropas"): un slot más, al FINAL
     * (24), así los guardados de antes no se corren. Va en el slot de Trinkets {@code head/sombrero}.
     */
    public static final int SLOT_SOMBRERO = ARMADURA_INICIO + SLOTS_ARMADURA.length;

    public static final int TAMANO = SLOT_SOMBRERO + 1;

    /** El inventario de Trinkets del sombrero del jugador, o null si no existe. */
    @Nullable
    public static dev.emi.trinkets.api.TrinketInventory inventarioSombrero(dev.emi.trinkets.api.TrinketComponent componente) {
        var grupo = componente.getInventory().get("head");
        return grupo == null ? null : grupo.get("sombrero");
    }

    /**
     * ¿Entra {@code stack} en el slot {@code slot}? Prendas por categoría,
     * armadura por su parte del cuerpo ({@code Equipment}: armaduras,
     * élitros, calabaza, cabezas). Compartido con el Maniquí.
     */
    public static boolean esValidoEn(int slot, ItemStack stack) {
        if (slot == SLOT_SOMBRERO) return stack.getItem() instanceof com.modamod.item.SombreroBrujaItem;
        if (slot >= ARMADURA_INICIO) {
            int i = slot - ARMADURA_INICIO;
            if (i >= SLOTS_ARMADURA.length) return false;
            net.minecraft.item.Equipment equipo = net.minecraft.item.Equipment.fromStack(stack);
            return equipo != null && equipo.getSlotType() == SLOTS_ARMADURA[i];
        }
        int categoria = categoriaDe(stack);
        // Un Top vale también en la columna de chaquetas (2026-10-07, "un solo top").
        if (categoria == REMERA && slot / POR_CATEGORIA == CHAQUETA) return true;
        return categoria >= 0 && categoria == slot / POR_CATEGORIA;
    }

    /** El slot de armadura que le toca a {@code stack}, o -1 si no es armadura. */
    public static int slotArmaduraDe(ItemStack stack) {
        if (stack.getItem() instanceof com.modamod.item.SombreroBrujaItem) return SLOT_SOMBRERO;
        for (int i = 0; i < SLOTS_ARMADURA.length; i++) {
            if (esValidoEn(ARMADURA_INICIO + i, stack)) return ARMADURA_INICIO + i;
        }
        return -1;
    }

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

    /**
     * Categoría de una prenda (REMERA/PANTALON/MEDIAS/CALIENTABRAZOS), o -1
     * si no entra en el Guardarropas. Mismo reparto que los tags de
     * Trinkets ({@code data/trinkets/tags/item/...}) — la pollera va con el
     * pantalón porque comparte su slot ({@code piernas/exterior}); antes
     * {@link #isValid} la rechazaba aunque el manual prometía "un pantalón
     * con una pollera y una calza". Compartido con el Maniquí.
     */
    public static int categoriaDe(ItemStack stack) {
        if (stack.getItem() instanceof RemeraItem) return REMERA;
        if (stack.getItem() instanceof PantalonItem || stack.getItem() instanceof PolleraItem) return PANTALON;
        if (stack.isOf(ModamodItems.SOCKS_SOLID)) return MEDIAS;
        if (stack.getItem() instanceof CalientabrazosItem) return CALIENTABRAZOS;
        return -1;
    }

    // ── puerta (modelo GeckoLib "wardrobe", 2026-09-30) ─────────────────
    private static final RawAnimation ABRIR = RawAnimation.begin()
            .thenPlay("animation.wardrobe.abrir")
            .thenLoop("animation.wardrobe.abierta");
    private static final RawAnimation CERRAR = RawAnimation.begin()
            .thenPlay("animation.wardrobe.cerrar")
            .thenLoop("animation.wardrobe.cerrada");
    private static final RawAnimation ABIERTA = RawAnimation.begin().thenLoop("animation.wardrobe.abierta");
    private static final RawAnimation CERRADA = RawAnimation.begin().thenLoop("animation.wardrobe.cerrada");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Mismo mecanismo que la tapa de {@code SublimadoraBlockEntity}: pose al cargar, transición solo en un cambio real. */
    private RawAnimation animacionPuerta = null;
    private boolean puertaAbierta = false;

    /** Cuántos jugadores tienen la pantalla abierta ahora — solo servidor, no se guarda. */
    private int mirando = 0;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "puerta", 0, state -> {
            boolean abierta = getCachedState().get(GuardarropasBlock.OPEN);
            if (animacionPuerta == null) {
                puertaAbierta = abierta;
                animacionPuerta = abierta ? ABIERTA : CERRADA;
            } else if (abierta != puertaAbierta) {
                puertaAbierta = abierta;
                animacionPuerta = abierta ? ABRIR : CERRAR;
                state.getController().forceAnimationReset();
            }
            return state.setAndContinue(animacionPuerta);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Llamado por {@code GuardarropasScreenHandler} al abrirse — abre la puerta con el primero que mira. */
    @Override
    public void onOpen(PlayerEntity player) {
        if (world == null || world.isClient || player.isSpectator()) return;
        mirando++;
        actualizarPuerta();
    }

    /** Llamado por {@code GuardarropasScreenHandler#onClosed} — cierra la puerta cuando ya no mira nadie. */
    @Override
    public void onClose(PlayerEntity player) {
        if (world == null || world.isClient || player.isSpectator()) return;
        mirando = Math.max(0, mirando - 1);
        actualizarPuerta();
    }

    private void actualizarPuerta() {
        BlockState estado = getCachedState();
        if (!estado.contains(GuardarropasBlock.OPEN)) return;
        boolean abrir = mirando > 0;
        if (estado.get(GuardarropasBlock.OPEN) == abrir) return;
        world.setBlockState(pos, estado.with(GuardarropasBlock.OPEN, abrir), 3);
        world.playSound(null, pos,
                abrir ? net.minecraft.sound.SoundEvents.BLOCK_BARREL_OPEN : net.minecraft.sound.SoundEvents.BLOCK_BARREL_CLOSE,
                net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 1.1f);
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
    /** Cierra/abre el candado (2026-10-04, "le pongamos un lock... y tambien al guardarropas"). */
    public static final int BTN_CANDADO = BTN_EQUIPAR + 1;

    private final com.modamod.util.Candado candado = new com.modamod.util.Candado();

    public com.modamod.util.Candado candado() { return candado; }

    public boolean alternarCandado(PlayerEntity player) {
        if (!candado.alternar(player)) return false;
        markDirty();
        return true;
    }

    // ── SidedInventory: las tolvas respetan el candado ────────────────────
    private static final int[] TODOS = java.util.stream.IntStream.range(0, TAMANO).toArray();

    @Override
    public int[] getAvailableSlots(net.minecraft.util.math.Direction side) { return TODOS; }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable net.minecraft.util.math.Direction dir) {
        return !candado.cerrado() && isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, net.minecraft.util.math.Direction dir) {
        return !candado.cerrado();
    }

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

    /** Grupo/slot de Trinkets de cada categoría, en el orden de REMERA..CALIENTABRAZOS — compartido con el Maniquí. */
    public static final String[] GRUPO = {"torso", "piernas", "socks", "arms", "torso"};
    public static final String[] NOMBRE_SLOT = {"prenda", "exterior", "pair", "armwarmer", "chaqueta"};

    /** El inventario de Trinkets de una categoría del jugador, o null si ese slot no existe (datapack sin cargar). */
    @Nullable
    public static dev.emi.trinkets.api.TrinketInventory inventarioTrinkets(dev.emi.trinkets.api.TrinketComponent componente,
                                                                             int categoria) {
        var grupoDeSlots = componente.getInventory().get(GRUPO[categoria]);
        return grupoDeSlots == null ? null : grupoDeSlots.get(NOMBRE_SLOT[categoria]);
    }

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
                equiparCategoria(componente, categoria, player);
            }
        });
        // Sombrero: mismo criterio, en el slot head/sombrero de Trinkets.
        ItemStack sombrero = items.get(SLOT_SOMBRERO);
        if (!sombrero.isEmpty()) {
            dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).ifPresent(componente -> {
                var inv = inventarioSombrero(componente);
                if (inv == null || inv.size() < 1) return;
                ItemStack anterior = inv.getStack(0);
                if (!anterior.isEmpty()) player.getInventory().offerOrDrop(anterior.copy());
                inv.setStack(0, sombrero.copy());
                items.set(SLOT_SOMBRERO, ItemStack.EMPTY);
            });
        }
        // Armadura: mismo criterio (un slot vacío no desequipa; lo reemplazado
        // vuelve al inventario del jugador).
        for (int i = 0; i < SLOTS_ARMADURA.length; i++) {
            ItemStack nueva = items.get(ARMADURA_INICIO + i);
            if (nueva.isEmpty()) continue;
            ItemStack anterior = player.getEquippedStack(SLOTS_ARMADURA[i]);
            if (!anterior.isEmpty()) player.getInventory().offerOrDrop(anterior.copy());
            player.equipStack(SLOTS_ARMADURA[i], nueva.copy());
            items.set(ARMADURA_INICIO + i, ItemStack.EMPTY);
        }
        markDirty();
    }

    private void equiparCategoria(dev.emi.trinkets.api.TrinketComponent componente, int categoria, PlayerEntity player) {
        dev.emi.trinkets.api.TrinketInventory inv = inventarioTrinkets(componente, categoria);
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
        return esValidoEn(slot, stack);
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
        candado.guardar(nbt);
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
        candado.leer(nbt);
    }
}
