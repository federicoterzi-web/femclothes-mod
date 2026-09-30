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

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.femclothes.chaqueta.hoodie");
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.chaqueta.tooltip.capucha",
                Text.keybind("key.femclothes.capucha")).formatted(Formatting.GRAY));
    }
}
