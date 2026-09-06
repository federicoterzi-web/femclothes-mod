package com.femclothes.screen;

import com.femclothes.sublimadora.ModItems;
import com.femclothes.sublimadora.MoldeItem;
import com.femclothes.sublimadora.RemeraItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import com.femclothes.item.ClothingPatternItem;
import com.femclothes.region.Lado;
import com.femclothes.region.RegionResolver;
import com.femclothes.item.FemclothesDye;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;

/**
 * Estación de personalización, misma idea visual que el Telar vanilla
 * (referencia: LoomBlock/LoomScreenHandler) pero código 100% nuestro —
 * sin Mixins sobre las clases de Minecraft.
 *
 * Diferencia clave con el Telar: cada ClothingPatternItem tiene UN solo
 * patrón fijo (no como los patrones de estandarte, que representan
 * varios patrones posibles a elegir) — así que no hace falta la lógica
 * de "seleccionar entre varios" que tiene LoomScreenHandler.
 *
 * Dos modos, según qué hay en los slots:
 * - Prenda + tinte (sin patrón) → re-tiñe el color BASE (como el cuero).
 * - Prenda + tinte + patrón → aplica el patrón con el tinte como color
 *   de patrón, sin tocar el color base.
 */
public class ClothingLoomScreenHandler extends ScreenHandler {

    private static final int INVENTORY_START = 4;
    private static final int INVENTORY_END = 31;
    private static final int HOTBAR_START = 31;
    private static final int HOTBAR_END = 40;

    private final ScreenHandlerContext context;

    /**
     * A que region se aplica lo que se arma.
     *
     * Es Lado y no un enum propio del telar: "ambas / izquierda / derecha" es
     * EL MISMO concepto que ya usan las medias por componente y que van a
     * usar las mesas nuevas. Tenerlo escrito distinto en cada lugar es lo que
     * este refactor viene a sacar.
     */
    private Lado target = Lado.AMBAS;
    Runnable inventoryChangeListener = () -> {};

    final Slot garmentSlot;
    final Slot dyeSlot;
    final Slot patternSlot;
    private final Slot outputSlot;
    long lastTakeResultTime;

    private final Inventory input = new SimpleInventory(3) {
        @Override
        public void markDirty() {
            super.markDirty();
            ClothingLoomScreenHandler.this.onContentChanged(this);
            ClothingLoomScreenHandler.this.inventoryChangeListener.run();
        }
    };
    private final Inventory output = new SimpleInventory(1) {
        @Override
        public void markDirty() {
            super.markDirty();
            ClothingLoomScreenHandler.this.inventoryChangeListener.run();
        }
    };

    public ClothingLoomScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public ClothingLoomScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(FemclothesScreenHandlers.CLOTHING_LOOM, syncId);
        this.context = context;

        // Coordenadas iguales a las del Telar vanilla (mismo layout visual).
        this.garmentSlot = this.addSlot(new Slot(this.input, 0, 13, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return FemclothesDye.isClothing(stack);
            }
        });
        this.dyeSlot = this.addSlot(new Slot(this.input, 1, 33, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof DyeItem;
            }
        });
        this.patternSlot = this.addSlot(new Slot(this.input, 2, 23, 45) {
            @Override
            public boolean canInsert(ItemStack stack) {
                // Los moldes de corte viajan por el mismo slot que los
                // patrones: los dos son algo que se le aplica a la prenda y
                // que no se consume.
                return stack.getItem() instanceof ClothingPatternItem
                        || stack.getItem() instanceof MoldeItem;
            }
        });
        this.outputSlot = this.addSlot(new Slot(this.output, 0, 143, 57) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }

            @Override
            public void onTakeItem(PlayerEntity player, ItemStack stack) {
                ClothingLoomScreenHandler.this.garmentSlot.takeStack(1);
                ClothingLoomScreenHandler.this.dyeSlot.takeStack(1);
                // El patrón NO se consume, igual que un patrón de estandarte vanilla.

                context.run((world, pos) -> {
                    long l = world.getTime();
                    if (ClothingLoomScreenHandler.this.lastTakeResultTime != l) {
                        world.playSound(null, pos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        ClothingLoomScreenHandler.this.lastTakeResultTime = l;
                    }
                });
                super.onTakeItem(player, stack);
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // El bloque es el Telar vanilla: nuestra UI se abre encima de el
        // via UseBlockCallback (ver ClothingLoomInteraction).
        return canUse(this.context, player, net.minecraft.block.Blocks.LOOM);
    }

    public Lado getTarget() {
        return this.target;
    }

    /**
     * El cliente cicla el destino con un boton. Se usa onButtonClick, el mismo
     * mecanismo con el que el Telar vanilla elige patron de bandera, asi no
     * hace falta definir paquetes propios.
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id >= Lado.values().length) return false;
        this.target = Lado.values()[id];
        this.onContentChanged(this.input);
        return true;
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        ItemStack garment = this.garmentSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack pattern = this.patternSlot.getStack();

        if (garment.isEmpty()) {
            this.outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
            return;
        }

        // La remera de la sublimadora tiene su propio sistema de color y de
        // corte, asi que no pasa por el camino de patrones de FemClothes.
        if (garment.getItem() instanceof RemeraItem) {
            this.outputSlot.setStackNoCallbacks(reformarRemera(garment, dye, pattern));
            this.sendContentUpdates();
            return;
        }

        // Prenda SOLA, sin tinte ni patron: saca el patron y la deja lisa.
        // Sin esto no habia forma de volver atras — una vez aplicado un
        // patron, el modo re-tenido solo cambia el color base y el patron
        // quedaba puesto para siempre.
        if (dye.isEmpty() && pattern.isEmpty()) {
            ItemStack stripped = stripPattern(garment);
            this.outputSlot.setStackNoCallbacks(stripped);
            this.sendContentUpdates();
            return;
        }

        if (dye.isEmpty() || !(dye.getItem() instanceof DyeItem dyeItem)) {
            this.outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
            return;
        }

        DyeColor dyeColor = dyeItem.getColor();
        int rgb = dyeColor.getFireworkColor();

        ItemStack result = garment.copyWithCount(1);
        boolean hasPattern = !pattern.isEmpty() && pattern.getItem() instanceof ClothingPatternItem;

        // La regla de AMBAS -escribir el primario y borrar el override- y el
        // guard de "clavar la derecha antes de tocar la izquierda" viven en
        // RegionResolver. Estaban escritos aca y en el renderer, y el bug de
        // "tenir una pierna tine las dos" salio justo de tener dos copias.
        apply(result, this.target, pattern, hasPattern, rgb);

        this.outputSlot.setStackNoCallbacks(result);
        this.sendContentUpdates();
    }

    /**
     * Lo que sale del telar con una remera adentro.
     *
     * El molde le mueve un eje del corte y el tinte le cambia el color; se
     * pueden usar juntos o por separado. Nada de esto toca la estampa: se
     * puede reformar una remera ya impresa sin perder la foto, que es
     * justamente lo que no permitiria hacerlo por receta.
     */
    private ItemStack reformarRemera(ItemStack remera, ItemStack dye, ItemStack pattern) {
        boolean hayMolde = pattern.getItem() instanceof MoldeItem;
        boolean hayTinte = dye.getItem() instanceof DyeItem;
        if (!hayMolde && !hayTinte) return ItemStack.EMPTY;

        ItemStack out = remera.copyWithCount(1);
        if (hayMolde) {
            out.set(ModItems.VARIANTE,
                    ((MoldeItem) pattern.getItem()).aplicar(RemeraItem.variante(remera)));
        }
        if (hayTinte) {
            out.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(
                    ((DyeItem) dye.getItem()).getColor().getFireworkColor(), false));
        }
        return out;
    }

    /** Copia de la prenda sin patron en el lado elegido, o vacio si no tenia. */
    private ItemStack stripPattern(ItemStack garment) {
        boolean izq = RegionResolver.tienePatron(garment, Lado.IZQUIERDA);
        boolean der = RegionResolver.tienePatron(garment, Lado.DERECHA);

        // Sin patron del lado elegido no hay nada que sacar: la salida vacia
        // es lo que evita que el telar ofrezca una copia identica.
        boolean hay = switch (this.target) {
            case AMBAS -> izq || der;
            case IZQUIERDA -> izq;
            case DERECHA -> der;
        };
        if (!hay) return ItemStack.EMPTY;

        ItemStack result = garment.copyWithCount(1);
        RegionResolver.quitarPatron(result, this.target);
        return result;
    }

    private static void apply(ItemStack result, Lado lado, ItemStack pattern,
                              boolean hasPattern, int rgb) {
        if (hasPattern) {
            // Modo patron: no toca el color base de ese lado.
            RegionResolver.ponerPatron(result, lado,
                    ((ClothingPatternItem) pattern.getItem()).patternId, rgb);
        } else {
            // Modo re-tenido: recolorea el color base de ese lado, como el cuero.
            RegionResolver.ponerColorBase(result, lado, rgb);
        }
    }

    public void setInventoryChangeListener(Runnable inventoryChangeListener) {
        this.inventoryChangeListener = inventoryChangeListener;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack result = ItemStack.EMPTY;
        Slot clickedSlot = this.slots.get(slot);
        if (clickedSlot != null && clickedSlot.hasStack()) {
            ItemStack stack = clickedSlot.getStack();
            result = stack.copy();
            if (slot == this.outputSlot.id) {
                if (!this.insertItem(stack, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                clickedSlot.onQuickTransfer(stack, result);
            } else if (slot != this.dyeSlot.id && slot != this.garmentSlot.id && slot != this.patternSlot.id) {
                if (FemclothesDye.isClothing(stack)) {
                    if (!this.insertItem(stack, this.garmentSlot.id, this.garmentSlot.id + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getItem() instanceof DyeItem) {
                    if (!this.insertItem(stack, this.dyeSlot.id, this.dyeSlot.id + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (stack.getItem() instanceof ClothingPatternItem) {
                    if (!this.insertItem(stack, this.patternSlot.id, this.patternSlot.id + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slot >= INVENTORY_START && slot < INVENTORY_END) {
                    if (!this.insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slot >= HOTBAR_START && slot < HOTBAR_END
                        && !this.insertItem(stack, INVENTORY_START, INVENTORY_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(stack, INVENTORY_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                clickedSlot.setStack(ItemStack.EMPTY);
            } else {
                clickedSlot.markDirty();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            clickedSlot.onTakeItem(player, stack);
        }
        return result;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.context.run((world, pos) -> this.dropInventory(player, this.input));
    }
}
