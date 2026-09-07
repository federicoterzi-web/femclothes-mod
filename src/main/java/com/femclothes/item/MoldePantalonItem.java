package com.femclothes.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * El molde del pantalón: cicla {@link PantalonLargo} en el telar.
 *
 * Mismo mecanismo que {@code MoldeItem} de la remera —no se consume, cicla
 * en vez de fijar un valor—, pero acá es una clase propia y no un caso más
 * de {@code MoldeItem.Eje} porque ese molde vive en el paquete de la
 * sublimadora y está tipado a {@code Variante}, que es de la remera. El
 * pantalón tiene un solo eje hoy; si suma otros (fit, tiro, botamanga — ver
 * PRENDAS.md §6) ES el momento de generalizar, no antes.
 */
public class MoldePantalonItem extends Item {

    public MoldePantalonItem(Settings settings) {
        super(settings);
    }

    public PantalonLargo aplicar(PantalonLargo actual) {
        return actual.siguiente();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("femclothes.sublimadora.molde.ayuda").formatted(Formatting.DARK_GRAY));
    }
}
