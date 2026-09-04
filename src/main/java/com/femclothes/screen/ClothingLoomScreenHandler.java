package com.femclothes.screen;

import com.femclothes.item.ClothingPatternItem;
import com.femclothes.item.ClothingStyle;
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

    /** A que pierna se aplica lo que se arma: las dos, solo izquierda, solo derecha. */
    public enum Target { BOTH, LEFT, RIGHT }

    private final ScreenHandlerContext context;
    private Target target = Target.BOTH;
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
                return stack.getItem() instanceof ClothingPatternItem;
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

    public Target getTarget() {
        return this.target;
    }

    /**
     * El cliente cicla el destino con un boton. Se usa onButtonClick, el mismo
     * mecanismo con el que el Telar vanilla elige patron de bandera, asi no
     * hace falta definir paquetes propios.
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id >= Target.values().length) return false;
        this.target = Target.values()[id];
        this.onContentChanged(this.input);
        return true;
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        ItemStack garment = this.garmentSlot.getStack();
        ItemStack dye = this.dyeSlot.getStack();
        ItemStack pattern = this.patternSlot.getStack();

        if (garment.isEmpty() || dye.isEmpty() || !(dye.getItem() instanceof DyeItem dyeItem)) {
            this.outputSlot.setStackNoCallbacks(ItemStack.EMPTY);
            return;
        }

        DyeColor dyeColor = dyeItem.getColor();
        int rgb = dyeColor.getFireworkColor();

        ItemStack result = garment.copyWithCount(1);
        boolean hasPattern = !pattern.isEmpty() && pattern.getItem() instanceof ClothingPatternItem;

        if (this.target == Target.BOTH) {
            // Par parejo: se escribe solo el lado base y se borran los
            // overrides de la derecha, para no dejar datos colgados de una
            // configuracion anterior.
            apply(result, ClothingStyle.Side.LEFT, pattern, hasPattern, rgb);
            ClothingStyle.clearRightOverrides(result);
        } else {
            ClothingStyle.Side side = this.target == Target.LEFT
                    ? ClothingStyle.Side.LEFT : ClothingStyle.Side.RIGHT;
            apply(result, side, pattern, hasPattern, rgb);
        }

        this.outputSlot.setStackNoCallbacks(result);
        this.sendContentUpdates();
    }

    private static void apply(ItemStack result, ClothingStyle.Side side, ItemStack pattern,
                              boolean hasPattern, int rgb) {
        if (hasPattern) {
            // Modo patron: no toca el color base de ese lado.
            ClothingStyle.setPattern(result, side, ((ClothingPatternItem) pattern.getItem()).patternId, rgb);
        } else {
            // Modo re-tenido: recolorea el color base de ese lado, como el cuero.
            ClothingStyle.setBaseColor(result, side, rgb);
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
