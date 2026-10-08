package com.modamod.item;

import com.modamod.sublimadora.RemeraItem;
import net.minecraft.item.ItemStack;

/**
 * Los rasgos de "chaqueta" de un Top (2026-10-07, "un solo top": la remera y la chaqueta se fusionan en un solo ítem,
 * {@link RemeraItem}): frente abierto, capucha (con su tecla), solapas. Reemplaza a {@code ChaquetaItem}. Ninguno es
 * obligatorio: una remera sin capucha, con frente cerrado y sin solapas es la remera de siempre. Dónde se dibuja
 * (capa interior o exterior) lo decide el slot de Trinkets en que se lleva ({@code torso/prenda} o
 * {@code torso/chaqueta}), ver {@link #exterior}.
 */
public final class TopCorte {
    private TopCorte() {}

    public static boolean capuchaArriba(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModamodComponents.CAPUCHA_ARRIBA));
    }

    public static void setCapuchaArriba(ItemStack stack, boolean arriba) {
        if (arriba) stack.set(ModamodComponents.CAPUCHA_ARRIBA, true);
        else stack.remove(ModamodComponents.CAPUCHA_ARRIBA);
    }

    /** Frente cerrado (de siempre) o abierto: se ve lo de abajo. */
    public static ChaquetaFrente frente(ItemStack stack) {
        ChaquetaFrente f = stack.get(ModamodComponents.CHAQUETA_FRENTE);
        return f == null ? ChaquetaFrente.CERRADA : f;
    }

    public static void setFrente(ItemStack stack, ChaquetaFrente f) {
        if (f == ChaquetaFrente.CERRADA) stack.remove(ModamodComponents.CHAQUETA_FRENTE);
        else stack.set(ModamodComponents.CHAQUETA_FRENTE, f);
    }

    /** Solapas: ninguna (de siempre), en pico, redondas o chal. */
    public static ChaquetaSolapa solapa(ItemStack stack) {
        ChaquetaSolapa v = stack.get(ModamodComponents.CHAQUETA_SOLAPA);
        return v == null ? ChaquetaSolapa.NINGUNA : v;
    }

    public static void setSolapa(ItemStack stack, ChaquetaSolapa v) {
        if (v == ChaquetaSolapa.NINGUNA) stack.remove(ModamodComponents.CHAQUETA_SOLAPA);
        else stack.set(ModamodComponents.CHAQUETA_SOLAPA, v);
    }

    /** ¿Lleva capucha (con sus cordones y el bolsillo canguro de hoodie)? Por defecto no. */
    public static boolean conCapucha(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModamodComponents.CON_CAPUCHA));
    }

    public static void setConCapucha(ItemStack stack, boolean con) {
        if (con) stack.set(ModamodComponents.CON_CAPUCHA, true);
        else stack.remove(ModamodComponents.CON_CAPUCHA);
    }

    /** Capucha que de verdad se dibuja o se sube con la tecla: un Top con capucha. */
    public static boolean tieneCapucha(ItemStack stack) {
        return stack.getItem() instanceof RemeraItem && conCapucha(stack);
    }

    /**
     * ¿Se dibuja en la capa exterior (la de las chaquetas, por encima de la remera y la pollera)? Es una marca que
     * solo llevan las COPIAS que se arman para dibujar (ver {@code GarmentFeatureRenderer#equipadas}) cuando el Top está
     * en el slot de Trinkets {@code torso/chaqueta}; nunca se guarda en la prenda real.
     */
    public static boolean exterior(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ModamodComponents.CAPA_EXTERIOR));
    }

    /** Copia para dibujar en la capa exterior. */
    public static ItemStack comoExterior(ItemStack stack) {
        ItemStack copia = stack.copy();
        copia.set(ModamodComponents.CAPA_EXTERIOR, true);
        return copia;
    }

    /** ¿Tiene algún rasgo de chaqueta (capucha, solapas o frente abierto)? Si no, es la remera de siempre. */
    public static boolean conRasgosDeChaqueta(ItemStack stack) {
        return conCapucha(stack) || solapa(stack) != ChaquetaSolapa.NINGUNA || frente(stack) != ChaquetaFrente.CERRADA;
    }

    /**
     * El hoodie de fábrica (2026-10-07, "un solo top"; era el ítem {@code chaqueta}): una remera larga, de manga larga,
     * cuello redondo, calce Oversize, con capucha y ruedo y puños ajustados. Para la pestaña creativa y la receta.
     */
    public static ItemStack hoodie() {
        ItemStack s = new ItemStack(com.modamod.sublimadora.ModItems.REMERA);
        s.set(com.modamod.sublimadora.ModItems.VARIANTE, new com.modamod.sublimadora.Variante(
                com.modamod.sublimadora.Variante.Largo.LARGO, com.modamod.sublimadora.Variante.Manga.LARGA,
                com.modamod.sublimadora.Variante.Cuello.REDONDO));
        s.set(ModamodComponents.CALCE, Calce.OVERSIZE);
        setConCapucha(s, true);
        Ruedos.set(s, ZonaRuedo.TORSO, Ruedo.AJUSTADO);
        Ruedos.set(s, ZonaRuedo.PUNO_IZQ, Ruedo.AJUSTADO);
        Ruedos.set(s, ZonaRuedo.PUNO_DER, Ruedo.AJUSTADO);
        return s;
    }
}
