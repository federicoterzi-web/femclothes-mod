package com.femclothes.region;

import com.femclothes.item.ClothingArmorItem;
import com.femclothes.item.ClothingTrinketItem;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * EL resolvedor: dado un ItemStack y una region, que valor corresponde.
 *
 * Absorbe lo que era ClothingStyle y le suma dos cosas que antes no existian
 * en ningun lado: la {@link Orientacion} —que se aplica al resolver, no
 * permutando datos— y el eje {@link Cara}.
 *
 * Por que una sola clase: la regla "la izquierda guarda, la derecha hereda"
 * estaba escrita en el telar, en el renderer y en el tooltip, y cada vez que
 * se tocaba una habia que acordarse de las otras. El bug de "tenir la
 * izquierda tine las dos" salio justamente de eso.
 *
 * <h2>La regla del eje Lado</h2>
 * La IZQUIERDA es primaria: guarda el valor en los componentes base. La
 * DERECHA guarda overrides opcionales (RIGHT_*) y cuando faltan hereda. Un
 * par parejo no ocupa data extra.
 *
 * <h2>La trampa de AMBAS</h2>
 * AMBAS no es "aplicar dos veces". Al escribir, setea el primario y BORRA el
 * override del otro lado: es el caso "parejo", y dejar los dos escritos haria
 * que un par igual pesara el doble sin ninguna ganancia. Al leer, AMBAS
 * devuelve el primario.
 *
 * <h2>Orientacion</h2>
 * Todo lo que entra por aca pasa antes por {@link #fisico}. Se aplica igual a
 * lecturas y a escrituras a proposito: si el jugador ve la prenda espejada y
 * pide tenir "la izquierda", quiere que cambie la que VE. Guardar del otro
 * lado y leer espejado da esa ilusion completa, y sacar el flag deja la
 * prenda exactamente como estaba.
 */
public final class RegionResolver {

    private RegionResolver() {}

    // --- Orientacion ---

    public static Orientacion orientacion(ItemStack stack) {
        Orientacion o = stack.get(FemclothesComponents.ORIENTACION);
        return o == null ? Orientacion.NORMAL : o;
    }

    /**
     * Guarda la orientacion, o borra el componente si volvio a la normal.
     *
     * Borrarlo importa: una prenda sin girar ni espejar tiene que apilar con
     * otra igual, y un componente presente con los dos bits en false no
     * apilaria con uno ausente.
     */
    public static void ponerOrientacion(ItemStack stack, Orientacion o) {
        if (o.esNormal()) stack.remove(FemclothesComponents.ORIENTACION);
        else stack.set(FemclothesComponents.ORIENTACION, o);
    }

    /**
     * El lado donde REALMENTE vive el dato, dado el lado que se ve.
     *
     * AMBAS no se toca: no tiene lado opuesto.
     */
    public static Lado fisico(ItemStack stack, Lado visible) {
        return orientacion(stack).espejado() ? visible.opuesto() : visible;
    }

    /** Lo mismo para el eje frente/espalda. */
    public static Cara fisica(ItemStack stack, Cara visible) {
        return orientacion(stack).girado() ? visible.opuesta() : visible;
    }

    // --- Lectura ---

    public static int colorBase(ItemStack stack, Lado visible) {
        Lado lado = fisico(stack, visible);
        if (lado == Lado.DERECHA) {
            Integer override = stack.get(FemclothesComponents.RIGHT_DYED_COLOR);
            if (override != null) return override;
        }
        if (stack.getItem() instanceof ClothingTrinketItem prenda) return prenda.getColor(stack);
        if (stack.getItem() instanceof ClothingArmorItem prenda) return prenda.getColor(stack);
        return 0xFFFFFF;
    }

    @Nullable
    public static Identifier patronId(ItemStack stack, Lado visible) {
        Lado lado = fisico(stack, visible);
        if (lado == Lado.DERECHA) {
            Identifier override = stack.get(FemclothesComponents.RIGHT_PATTERN_ID);
            if (override != null) return override;
            // Una derecha con color propio pero sin patron propio es una
            // pierna configurada aparte: lisa, no hereda el patron de la otra.
            if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) != null) return null;
        }
        return stack.get(FemclothesComponents.PATTERN_ID);
    }

    public static int colorPatron(ItemStack stack, Lado visible) {
        Lado lado = fisico(stack, visible);
        if (lado == Lado.DERECHA) {
            Integer override = stack.get(FemclothesComponents.RIGHT_PATTERN_COLOR);
            if (override != null) return override;
        }
        Integer base = stack.get(FemclothesComponents.PATTERN_COLOR);
        return base != null ? base : 0xFFFFFF;
    }

    public static boolean tienePatron(ItemStack stack, Lado visible) {
        return patronId(stack, visible) != null;
    }

    /** True si los dos lados se ven exactamente igual. */
    public static boolean parejo(ItemStack stack) {
        return Objects.equals(patronId(stack, Lado.IZQUIERDA), patronId(stack, Lado.DERECHA))
                && colorBase(stack, Lado.IZQUIERDA) == colorBase(stack, Lado.DERECHA)
                && colorPatron(stack, Lado.IZQUIERDA) == colorPatron(stack, Lado.DERECHA);
    }

    // --- Escritura ---

    public static void ponerColorBase(ItemStack stack, Lado visible, int rgb) {
        if (visible == Lado.AMBAS) {
            FemclothesDye.setBaseColor(stack, rgb);
            limpiarDerecha(stack);
            return;
        }
        if (fisico(stack, visible) == Lado.DERECHA) {
            stack.set(FemclothesComponents.RIGHT_DYED_COLOR, rgb);
        } else {
            fijarDerecha(stack);
            FemclothesDye.setBaseColor(stack, rgb);
        }
    }

    public static void ponerPatron(ItemStack stack, Lado visible, Identifier patronId, int rgb) {
        if (visible == Lado.AMBAS) {
            stack.set(FemclothesComponents.PATTERN_ID, patronId);
            stack.set(FemclothesComponents.PATTERN_COLOR, rgb);
            limpiarDerecha(stack);
            return;
        }
        if (fisico(stack, visible) == Lado.DERECHA) {
            stack.set(FemclothesComponents.RIGHT_PATTERN_ID, patronId);
            stack.set(FemclothesComponents.RIGHT_PATTERN_COLOR, rgb);
            // Sin color base propio la derecha vuelve a heredar el patron de
            // la izquierda por el fallback de patronId.
            if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) == null) {
                stack.set(FemclothesComponents.RIGHT_DYED_COLOR, colorBase(stack, Lado.IZQUIERDA));
            }
        } else {
            fijarDerecha(stack);
            stack.set(FemclothesComponents.PATTERN_ID, patronId);
            stack.set(FemclothesComponents.PATTERN_COLOR, rgb);
        }
    }

    public static void quitarPatron(ItemStack stack, Lado visible) {
        if (visible == Lado.AMBAS) {
            stack.remove(FemclothesComponents.PATTERN_ID);
            stack.remove(FemclothesComponents.PATTERN_COLOR);
            limpiarDerecha(stack);
            return;
        }
        if (fisico(stack, visible) == Lado.DERECHA) {
            stack.remove(FemclothesComponents.RIGHT_PATTERN_ID);
            stack.remove(FemclothesComponents.RIGHT_PATTERN_COLOR);
            if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) == null) {
                stack.set(FemclothesComponents.RIGHT_DYED_COLOR, colorBase(stack, Lado.IZQUIERDA));
            }
        } else {
            fijarDerecha(stack);
            stack.remove(FemclothesComponents.PATTERN_ID);
            stack.remove(FemclothesComponents.PATTERN_COLOR);
        }
    }

    /**
     * Clava en la derecha lo que hoy hereda, ANTES de tocar la izquierda.
     *
     * Es EL unico camino para tocar un lado sin arrastrar el otro, y por eso
     * ahora lo llaman solos los setters de aca arriba en vez de dejarselo al
     * que llama: olvidarselo era exactamente el bug de "tenir una pierna tine
     * las dos", y un guard que hay que acordarse de invocar vuelve a fallar
     * tarde o temprano.
     */
    public static void fijarDerecha(ItemStack stack) {
        if (stack.get(FemclothesComponents.RIGHT_PATTERN_ID) == null) {
            Identifier patronIzq = stack.get(FemclothesComponents.PATTERN_ID);
            if (patronIzq != null) {
                stack.set(FemclothesComponents.RIGHT_PATTERN_ID, patronIzq);
                stack.set(FemclothesComponents.RIGHT_PATTERN_COLOR, colorPatron(stack, Lado.IZQUIERDA));
            }
        }
        // El color va ULTIMO: patronId(DERECHA) usa la ausencia de
        // RIGHT_DYED_COLOR para decidir si hereda el patron de la izquierda.
        if (stack.get(FemclothesComponents.RIGHT_DYED_COLOR) == null) {
            stack.set(FemclothesComponents.RIGHT_DYED_COLOR, colorBase(stack, Lado.IZQUIERDA));
        }
    }

    /** Borra los overrides: el par vuelve a ser parejo. */
    public static void limpiarDerecha(ItemStack stack) {
        stack.remove(FemclothesComponents.RIGHT_DYED_COLOR);
        stack.remove(FemclothesComponents.RIGHT_PATTERN_ID);
        stack.remove(FemclothesComponents.RIGHT_PATTERN_COLOR);
    }

    /**
     * Intercambia izquierda y derecha permutando los datos guardados.
     *
     * Es la accion de estacion "espejar esta capa", distinta del flag
     * {@code espejado} de la orientacion: aca los valores cambian de lugar de
     * verdad, asi que despues se pueden seguir editando por separado. Si la
     * derecha heredaba, la que pasa a heredar es la izquierda.
     */
    public static void espejarCapa(ItemStack stack) {
        int colorIzq = colorBase(stack, Lado.IZQUIERDA);
        Identifier patronIzq = patronId(stack, Lado.IZQUIERDA);
        int colorPatronIzq = colorPatron(stack, Lado.IZQUIERDA);

        int colorDer = colorBase(stack, Lado.DERECHA);
        Identifier patronDer = patronId(stack, Lado.DERECHA);
        int colorPatronDer = colorPatron(stack, Lado.DERECHA);

        limpiarDerecha(stack);
        FemclothesDye.setBaseColor(stack, colorDer);
        if (patronDer != null) {
            stack.set(FemclothesComponents.PATTERN_ID, patronDer);
            stack.set(FemclothesComponents.PATTERN_COLOR, colorPatronDer);
        } else {
            stack.remove(FemclothesComponents.PATTERN_ID);
            stack.remove(FemclothesComponents.PATTERN_COLOR);
        }
        // Si los dos lados eran iguales no se escribe override: un par parejo
        // espejado sigue sin ocupar data extra.
        if (colorIzq != colorDer || !Objects.equals(patronIzq, patronDer)
                || colorPatronIzq != colorPatronDer) {
            stack.set(FemclothesComponents.RIGHT_DYED_COLOR, colorIzq);
            if (patronIzq != null) {
                stack.set(FemclothesComponents.RIGHT_PATTERN_ID, patronIzq);
                stack.set(FemclothesComponents.RIGHT_PATTERN_COLOR, colorPatronIzq);
            }
        }
    }
}
