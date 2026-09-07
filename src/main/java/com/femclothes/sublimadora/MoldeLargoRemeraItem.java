package com.femclothes.sublimadora;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Molde de largo de remera: FIJA un valor, no cicla.
 *
 * A diferencia de {@link MoldeItem} (manga/cuello, que ciclan), acá hay UN
 * ítem por cada uno de los tres valores de {@code Variante.Largo}. Aplicarlo
 * pone la remera en ESE largo exacto sin importar el que tenía — a pedido
 * del dueño, mismo mecanismo que ya usa {@link com.femclothes.item.PantalonItem}
 * con {@code PantalonLargo}: elegís el valor por el nombre del molde en vez
 * de ir ciclando a ciegas.
 */
public class MoldeLargoRemeraItem extends Item {

    public final Variante.Largo valor;

    public MoldeLargoRemeraItem(Settings settings, Variante.Largo valor) {
        super(settings);
        this.valor = valor;
    }

    public Variante aplicar(Variante v) {
        return new Variante(valor, v.manga(), v.cuello());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
