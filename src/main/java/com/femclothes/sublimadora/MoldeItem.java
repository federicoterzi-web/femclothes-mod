package com.femclothes.sublimadora;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Un molde: cambia un eje del corte de la prenda en el telar.
 *
 * Va en el slot de patron del telar y NO se consume, igual que un patron de
 * estandarte. Cicla su eje, en vez de fijar un valor, por la misma razon que
 * los tres ejes no se craftean: con 36 combinaciones, un item por valor serian
 * diez items y diez recetas para algo que se resuelve con tres.
 */
public class MoldeItem extends Item {

    /** Que eje del corte mueve este molde. */
    public enum Eje {
        LARGO("largo"),
        MANGA("manga"),
        CUELLO("cuello");

        public final String clave;

        Eje(String clave) {
            this.clave = clave;
        }
    }

    public final Eje eje;

    public MoldeItem(Settings settings, Eje eje) {
        super(settings);
        this.eje = eje;
    }

    /** El corte que sale de pasar este molde por esa prenda. */
    public Variante aplicar(Variante v) {
        return switch (eje) {
            case LARGO -> new Variante(siguiente(Variante.Largo.values(), v.largo()), v.manga(), v.cuello());
            case MANGA -> new Variante(v.largo(), siguiente(Variante.Manga.values(), v.manga()), v.cuello());
            case CUELLO -> new Variante(v.largo(), v.manga(), siguiente(Variante.Cuello.values(), v.cuello()));
        };
    }

    private static <T extends Enum<T>> T siguiente(T[] valores, T actual) {
        return valores[(actual.ordinal() + 1) % valores.length];
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
