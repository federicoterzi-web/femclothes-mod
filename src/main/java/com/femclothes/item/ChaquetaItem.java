package com.femclothes.item;

import com.femclothes.sublimadora.RemeraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Chaqueta (2026-09-30, "haceme un hoodie oversize para ver, creo que va a
 * ser una nueva categoria de prendas llamadas chaquetas"). Hoy es el hoodie:
 * capucha (caída o puesta, con tecla), cordones, bolsillo canguro y puños y
 * ruedo elásticos.
 *
 * <p>Hereda de {@link RemeraItem} a propósito: comparte su tela (layout de
 * torso + mangas), su corte ({@code Variante}: largo, mangas por lado,
 * cuello), el calce, la red, las zonas de tinte y las estampas — la
 * Modeladora, la Estación de Tintes y la Sublimadora lo tratan como una
 * remera sin cambios. Lo que la hace otra categoría: su propio slot de
 * Trinkets ({@code torso/chaqueta}), su capa ({@code Capa.CHAQUETA}, por
 * encima de la remera) y sus piezas ({@code PiezasDelMod#chaqueta}).
 * Donde el código pregunta "¿es una remera?" con {@code instanceof
 * RemeraItem} y la chaqueta NO tiene que entrar (Guardarropas, tags),
 * preguntar primero por {@code ChaquetaItem}.
 */
public class ChaquetaItem extends RemeraItem {

    public ChaquetaItem(Settings settings) {
        super(settings);
    }

    public static boolean capuchaArriba(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(FemclothesComponents.CAPUCHA_ARRIBA));
    }

    public static void setCapuchaArriba(ItemStack stack, boolean arriba) {
        if (arriba) stack.set(FemclothesComponents.CAPUCHA_ARRIBA, true);
        else stack.remove(FemclothesComponents.CAPUCHA_ARRIBA);
    }

    // ── corte modular (2026-10-07, "como se te ocurre que mejor hacemos las chaquetas" → frente, capucha y remate) ──

    /** Frente cerrado (de siempre) o abierto: se ve lo de abajo. */
    public static ChaquetaFrente frente(ItemStack stack) {
        ChaquetaFrente f = stack.get(FemclothesComponents.CHAQUETA_FRENTE);
        return f == null ? ChaquetaFrente.CERRADA : f;
    }

    public static void setFrente(ItemStack stack, ChaquetaFrente f) {
        if (f == ChaquetaFrente.CERRADA) stack.remove(FemclothesComponents.CHAQUETA_FRENTE);
        else stack.set(FemclothesComponents.CHAQUETA_FRENTE, f);
    }

    /** Puños y ruedo: elástico (el hoodie de siempre) o recto. */
    public static ChaquetaRemate remate(ItemStack stack) {
        ChaquetaRemate r = stack.get(FemclothesComponents.CHAQUETA_REMATE);
        return r == null ? ChaquetaRemate.ELASTICO : r;
    }

    public static void setRemate(ItemStack stack, ChaquetaRemate r) {
        if (r == ChaquetaRemate.ELASTICO) stack.remove(FemclothesComponents.CHAQUETA_REMATE);
        else stack.set(FemclothesComponents.CHAQUETA_REMATE, r);
    }

    /** Solapas: ninguna (de siempre), en pico, redondas o chal. */
    public static ChaquetaSolapa solapa(ItemStack stack) {
        ChaquetaSolapa v = stack.get(FemclothesComponents.CHAQUETA_SOLAPA);
        return v == null ? ChaquetaSolapa.NINGUNA : v;
    }

    public static void setSolapa(ItemStack stack, ChaquetaSolapa v) {
        if (v == ChaquetaSolapa.NINGUNA) stack.remove(FemclothesComponents.CHAQUETA_SOLAPA);
        else stack.set(FemclothesComponents.CHAQUETA_SOLAPA, v);
    }

    /** ¿Lleva capucha (con sus cordones y el bolsillo canguro de hoodie)? Por defecto sí. */
    public static boolean conCapucha(ItemStack stack) {
        return !Boolean.TRUE.equals(stack.get(FemclothesComponents.CHAQUETA_SIN_CAPUCHA));
    }

    public static void setConCapucha(ItemStack stack, boolean con) {
        if (con) stack.remove(FemclothesComponents.CHAQUETA_SIN_CAPUCHA);
        else stack.set(FemclothesComponents.CHAQUETA_SIN_CAPUCHA, true);
    }

    /** Capucha que de verdad se dibuja o se sube con la tecla: la chaqueta la lleva y tiene torso. */
    public static boolean tieneCapucha(ItemStack stack) {
        return stack.getItem() instanceof ChaquetaItem && conCapucha(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        boolean sinMangas = RemeraItem.manga(stack, com.femclothes.region.Lado.IZQUIERDA).filas == 0
                && RemeraItem.manga(stack, com.femclothes.region.Lado.DERECHA).filas == 0;
        boolean abierta = frente(stack) == ChaquetaFrente.ABIERTA;
        boolean saco = solapa(stack) != ChaquetaSolapa.NINGUNA && !conCapucha(stack);
        String clave = sinMangas ? (abierta ? "chaleco_abierto" : "chaleco")
                : saco ? (abierta ? "saco" : "saco_cruzado")
                : conCapucha(stack) ? (abierta ? "hoodie_abierto" : "hoodie")
                : (abierta ? "campera" : "buzo");
        return Text.translatable("item.femclothes.chaqueta." + clave);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        if (conCapucha(stack)) {
            tooltip.add(Text.translatable("femclothes.chaqueta.tooltip.capucha",
                    Text.keybind("key.femclothes.capucha")).formatted(Formatting.GRAY));
        }
    }
}
