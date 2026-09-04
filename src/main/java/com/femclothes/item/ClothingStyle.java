package com.femclothes.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Resuelve el aspecto de una prenda por LADO del cuerpo, sin necesitar un
 * Item distinto por combinacion.
 *
 * La pierna izquierda usa los componentes "base" (DYED_COLOR, PATTERN_ID,
 * PATTERN_COLOR). La derecha usa los RIGHT_*, y cuando faltan cae en los
 * base — o sea, un par parejo no guarda nada extra, y las 16 recetas de
 * crafteo (que solo setean DYED_COLOR) siguen dando medias iguales sin
 * tocarlas.
 */
public final class ClothingStyle {

    public enum Side { LEFT, RIGHT }

    private ClothingStyle() {}

    public static int baseColor(ItemStack stack, Side side) {
        if (side == Side.RIGHT) {
            Integer override = stack.get(FemclothesComponents.RIGHT_DYED_COLOR);
            if (override != null) return override;
        }
        if (stack.getItem() instanceof ClothingTrinketItem clothing) return clothing.getColor(stack);
        if (stack.getItem() instanceof ClothingArmorItem clothing) return clothing.getColor(stack);
        return 0xFFFFFF;
    }

    @Nullable
    public static Identifier patternId(ItemStack stack, Side side) {
        if (side == Side.RIGHT) {
            Identifier override = stack.get(FemclothesComponents.RIGHT_PATTERN_ID);
            if (override != null) return override;
            // Si la derecha tiene color propio pero no patron propio, es una
            // pierna configurada aparte: no hereda el patron de la izquierda.
            if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) != null) return null;
        }
        return stack.get(FemclothesComponents.PATTERN_ID);
    }

    public static int patternColor(ItemStack stack, Side side) {
        if (side == Side.RIGHT) {
            Integer override = stack.get(FemclothesComponents.RIGHT_PATTERN_COLOR);
            if (override != null) return override;
        }
        Integer base = stack.get(FemclothesComponents.PATTERN_COLOR);
        return base != null ? base : 0xFFFFFF;
    }

    /** Escribe el color base del lado pedido. */
    public static void setBaseColor(ItemStack stack, Side side, int rgb) {
        if (side == Side.RIGHT) {
            stack.set(FemclothesComponents.RIGHT_DYED_COLOR, rgb);
        } else {
            FemclothesDye.setBaseColor(stack, rgb);
        }
    }

    /** Escribe patron + color de patron del lado pedido. */
    public static void setPattern(ItemStack stack, Side side, Identifier patternId, int rgb) {
        if (side == Side.RIGHT) {
            stack.set(FemclothesComponents.RIGHT_PATTERN_ID, patternId);
            stack.set(FemclothesComponents.RIGHT_PATTERN_COLOR, rgb);
            // La derecha necesita color base propio para no heredar el patron
            // de la izquierda por el fallback de arriba.
            if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) == null) {
                stack.set(FemclothesComponents.RIGHT_DYED_COLOR, baseColor(stack, Side.LEFT));
            }
        } else {
            stack.set(FemclothesComponents.PATTERN_ID, patternId);
            stack.set(FemclothesComponents.PATTERN_COLOR, rgb);
        }
    }

    /**
     * Fija en la derecha lo que hoy hereda de la izquierda, ANTES de tocar la
     * izquierda.
     *
     * Sin esto, tenir "solo la pierna izquierda" tenia el bug de tenir las
     * dos: escribir el color base cambia la izquierda, y la derecha lo estaba
     * heredando por el fallback. Hay que clavarle su valor actual primero
     * para que se quede como estaba.
     */
    public static void pinRight(ItemStack stack) {
        if (stack.get(FemclothesComponents.RIGHT_PATTERN_ID) == null) {
            Identifier leftPattern = stack.get(FemclothesComponents.PATTERN_ID);
            if (leftPattern != null) {
                stack.set(FemclothesComponents.RIGHT_PATTERN_ID, leftPattern);
                stack.set(FemclothesComponents.RIGHT_PATTERN_COLOR, patternColor(stack, Side.LEFT));
            }
        }
        // El color va ultimo: patternId(RIGHT) usa la ausencia de
        // RIGHT_DYED_COLOR para decidir si hereda el patron de la izquierda.
        if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) == null) {
            stack.set(FemclothesComponents.RIGHT_DYED_COLOR, baseColor(stack, Side.LEFT));
        }
    }

    /** Borra los overrides de la derecha: el par vuelve a ser parejo. */
    public static void clearRightOverrides(ItemStack stack) {
        stack.remove(FemclothesComponents.RIGHT_DYED_COLOR);
        stack.remove(FemclothesComponents.RIGHT_PATTERN_ID);
        stack.remove(FemclothesComponents.RIGHT_PATTERN_COLOR);
    }
}
