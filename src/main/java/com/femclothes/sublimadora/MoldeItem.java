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
 * estandarte. Cicla su eje, en vez de fijar un valor.
 *
 * El eje LARGO se fue de aca: pasó a {@link MoldeLargoRemeraItem}, uno por
 * valor (crop/normal/largo) en vez de uno que cicla — a pedido del dueño,
 * mismo cambio que ya tenía el molde de pantalón. MANGA y CUELLO siguen
 * cíclicos: con 36 combinaciones entre los tres ejes, un item por valor de
 * ESOS dos seguiría siendo ruido; solo largo (3 valores, y ahora compartido
 * en espíritu con el largo de pantalón) pasó al otro mecanismo.
 */
public class MoldeItem extends Item {

    /** Que eje del corte mueve este molde. */
    public enum Eje {
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
